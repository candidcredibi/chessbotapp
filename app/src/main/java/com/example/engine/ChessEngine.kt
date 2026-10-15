package com.example.engine

import com.example.core.model.BoardPosition
import com.example.core.model.ChessMove
import com.example.core.model.MoveValidator
import com.example.core.model.PieceColor
import com.example.core.model.PieceType
import com.example.core.model.Square
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.math.max
import kotlin.math.min

class ChessEngine {

    private val transpositionTable = HashMap<Long, TranspositionEntry>(16384)
    private var isStopRequested = false

    data class TranspositionEntry(
        val depth: Int,
        val score: Int,
        val flag: Int, // 0: exact, 1: lowerbound, 2: upperbound
        val bestMove: ChessMove?
    )

    fun requestStop() {
        isStopRequested = true
    }

    suspend fun analyze(
        board: BoardPosition,
        depth: Int = 8,
        multiPv: Int = 3,
        style: EngineStyle = EngineStyle.SAFE
    ): List<EngineEvaluation> {
        isStopRequested = false
        val activeColor = board.activeColor
        val legalMoves = MoveValidator.generateLegalMoves(board, activeColor)

        if (legalMoves.isEmpty()) {
            val kingSq = board.findKing(activeColor)
            val inCheck = kingSq != null && MoveValidator.isSquareAttacked(board, kingSq, activeColor.opposite())
            return if (inCheck) {
                // Checkmated
                listOf(
                    EngineEvaluation(
                        move = ChessMove(Square(0, 0), Square(0, 0)),
                        scoreCentipawns = if (activeColor == PieceColor.WHITE) -10000 else 10000,
                        mateIn = -1
                    )
                )
            } else {
                // Stalemate
                listOf(
                    EngineEvaluation(
                        move = ChessMove(Square(0, 0), Square(0, 0)),
                        scoreCentipawns = 0
                    )
                )
            }
        }

        // Score each root move
        val scoredMoves = mutableListOf<ScoredMove>()

        for (move in legalMoves) {
            if (!currentCoroutineContext().isActive || isStopRequested) break
            val nextBoard = board.makeMove(move)
            // Search from perspective of next player
            val score = -alphaBeta(
                board = nextBoard,
                depth = depth - 1,
                alpha = -100000,
                beta = 100000,
                maximizingColor = activeColor.opposite()
            )
            // Centipawn score from perspective of activeColor
            scoredMoves.add(ScoredMove(move, score))
        }

        if (scoredMoves.isEmpty()) {
            return emptyList()
        }

        // Sort descending by raw score
        scoredMoves.sortByDescending { it.score }

        // Apply style heuristics to the top moves within acceptable centipawn loss
        val topScore = scoredMoves.first().score
        val filteredMoves = scoredMoves.map { sm ->
            val cpLoss = topScore - sm.score
            val bonus = calculateStyleBonus(board, sm.move, style)
            val adjustedScore = if (cpLoss <= style.maxAcceptableCentipawnLoss) {
                sm.score + bonus
            } else {
                sm.score
            }
            AdjustedMove(sm.move, sm.score, adjustedScore)
        }.sortedByDescending { it.adjustedScore }

        val resultCount = min(multiPv, filteredMoves.size)
        val evaluations = mutableListOf<EngineEvaluation>()

        for (i in 0 until resultCount) {
            val item = filteredMoves[i]
            val mateIn = if (kotlin.math.abs(item.rawScore) > 9000) {
                val movesToMate = (10000 - kotlin.math.abs(item.rawScore)) / 2 + 1
                if (item.rawScore > 0) movesToMate else -movesToMate
            } else null

            evaluations.add(
                EngineEvaluation(
                    move = item.move,
                    scoreCentipawns = if (activeColor == PieceColor.WHITE) item.rawScore else -item.rawScore,
                    mateIn = mateIn,
                    depth = depth,
                    pvLine = listOf(item.move),
                    rankIndex = i
                )
            )
        }

        return evaluations
    }

    private fun calculateStyleBonus(board: BoardPosition, move: ChessMove, style: EngineStyle): Int {
        var bonus = 0
        when (style) {
            EngineStyle.SAFE -> {
                // Favor moves that keep pieces defended and avoid underdefended captures
                if (move.isCastling) bonus += 25
                if (!move.isCapture && move.movingPiece?.type != PieceType.QUEEN) bonus += 10
            }
            EngineStyle.AGGRESSIVE -> {
                // Favor checks, attacks and captures
                if (move.isCheck) bonus += 35
                if (move.isCapture) bonus += 20
                if (move.to.rank in 3..6) bonus += 10
            }
            EngineStyle.POSITIONAL -> {
                // Favor central squares (e4, d4, e5, d5, c4, f4)
                if (move.to.file in 2..5 && move.to.rank in 2..5) bonus += 20
                if (move.isCastling) bonus += 15
                if (move.movingPiece?.type == PieceType.PAWN && move.to.file in 3..4) bonus += 15
            }
            EngineStyle.TACTICAL -> {
                // Favor pins, forks, discoveries and checks
                if (move.isCheck) bonus += 30
                if (move.isCapture) bonus += 25
                if (move.movingPiece?.type == PieceType.KNIGHT) bonus += 15
            }
        }
        return bonus
    }

    private suspend fun alphaBeta(
        board: BoardPosition,
        depth: Int,
        alpha: Int,
        beta: Int,
        maximizingColor: PieceColor
    ): Int {
        if (!currentCoroutineContext().isActive || isStopRequested) {
            throw CancellationException("Search stopped")
        }

        if (depth <= 0) {
            return quiescence(board, alpha, beta, maximizingColor, 0)
        }

        val legalMoves = MoveValidator.generateLegalMoves(board, maximizingColor)
        if (legalMoves.isEmpty()) {
            val kingSq = board.findKing(maximizingColor)
            val inCheck = kingSq != null && MoveValidator.isSquareAttacked(board, kingSq, maximizingColor.opposite())
            return if (inCheck) -10000 + (10 - depth) else 0 // Checkmate vs Stalemate
        }

        // Simple move ordering: captures and checks first
        val sortedMoves = legalMoves.sortedByDescending { move ->
            var priority = 0
            if (move.isCapture) {
                val victimVal = move.capturedPiece?.type?.baseValue ?: 100
                val attackerVal = move.movingPiece?.type?.baseValue ?: 100
                priority += victimVal * 10 - attackerVal
            }
            if (move.isCheck) priority += 50
            if (move.promotion != null) priority += 900
            priority
        }

        var currentAlpha = alpha
        for (move in sortedMoves) {
            val nextBoard = board.makeMove(move)
            val score = -alphaBeta(nextBoard, depth - 1, -beta, -currentAlpha, maximizingColor.opposite())
            if (score >= beta) {
                return beta // Beta cutoff
            }
            if (score > currentAlpha) {
                currentAlpha = score
            }
        }

        return currentAlpha
    }

    private fun quiescence(
        board: BoardPosition,
        alpha: Int,
        beta: Int,
        maximizingColor: PieceColor,
        qDepth: Int
    ): Int {
        val standPat = staticEvaluation(board, maximizingColor)
        if (qDepth >= 4) return standPat
        if (standPat >= beta) return beta

        var currentAlpha = max(alpha, standPat)

        // Only evaluate capture moves in quiescence to avoid horizon effect
        val legalMoves = MoveValidator.generateLegalMoves(board, maximizingColor).filter { it.isCapture }
        for (move in legalMoves) {
            val nextBoard = board.makeMove(move)
            val score = -quiescence(nextBoard, -beta, -currentAlpha, maximizingColor.opposite(), qDepth + 1)
            if (score >= beta) return beta
            if (score > currentAlpha) currentAlpha = score
        }

        return currentAlpha
    }

    private fun staticEvaluation(board: BoardPosition, color: PieceColor): Int {
        var whiteScore = 0
        var blackScore = 0

        for (rank in 0..7) {
            for (file in 0..7) {
                val sq = Square(file, rank)
                val piece = board.getPiece(sq) ?: continue
                val material = piece.type.baseValue
                val positional = PieceSquareTables.getPositionalBonus(piece.type, piece.color, sq)
                val total = material + positional

                if (piece.color == PieceColor.WHITE) {
                    whiteScore += total
                } else {
                    blackScore += total
                }
            }
        }

        val netScore = whiteScore - blackScore
        return if (color == PieceColor.WHITE) netScore else -netScore
    }

    private data class ScoredMove(val move: ChessMove, val score: Int)
    private data class AdjustedMove(val move: ChessMove, val rawScore: Int, val adjustedScore: Int)
}
