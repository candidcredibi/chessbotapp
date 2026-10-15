package com.example

import com.example.core.model.FenParser
import com.example.core.model.MoveValidator
import com.example.core.model.PieceColor
import com.example.core.model.Square
import com.example.engine.MoveExplainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessCoreUnitTest {

    @Test
    fun startingFen_parseAndSerialize_matches() {
        val board = FenParser.parse(FenParser.STARTING_FEN)
        assertEquals(PieceColor.WHITE, board.activeColor)
        val serialized = FenParser.toFen(board)
        assertEquals(FenParser.STARTING_FEN, serialized)
    }

    @Test
    fun startingPosition_has20LegalMovesForWhite() {
        val board = FenParser.parse(FenParser.STARTING_FEN)
        val legalMoves = MoveValidator.generateLegalMoves(board, PieceColor.WHITE)
        // 16 pawn moves (8 single + 8 double) + 4 knight moves = 20
        assertEquals(20, legalMoves.size)
    }

    @Test
    fun scholarMate_detectedAsCheckmate() {
        // Scholar's mate FEN
        val fen = "r1bqkb1r/pppp1ppp/2n5/4p3/2B1n3/5Q2/PPPP1PPP/RNB1K1NR w KQkq - 0 4"
        val board = FenParser.parse(fen)
        val moves = MoveValidator.generateLegalMoves(board, PieceColor.WHITE)
        val mateMove = moves.firstOrNull { it.from == Square.fromAlgebraic("f3") && it.to == Square.fromAlgebraic("f7") }
        assertNotNull(mateMove)
        assertTrue(mateMove!!.isCheckmate)

        val explanation = MoveExplainer.explainMove(board, mateMove, null)
        assertTrue(explanation.contains("Мат"))
    }
}
