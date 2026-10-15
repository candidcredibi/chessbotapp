package com.example.engine

import com.example.core.model.BoardPosition
import com.example.core.model.ChessMove
import com.example.core.model.MoveValidator
import com.example.core.model.Piece
import com.example.core.model.PieceColor
import com.example.core.model.PieceType
import com.example.core.model.Square

enum class MoveQuality(val titleRu: String, val badgeColorHex: Long) {
    BRILLIANT("Блестящий ход", 0xFF00E5FF),
    GREAT("Отлично", 0xFF00E676),
    GOOD("Хорошо", 0xFF4CAF50),
    INACCURACY("Неточность", 0xFFFFD600),
    MISTAKE("Ошибка", 0xFFFF9800),
    BLUNDER("Зевок", 0xFFFF5252);

    companion object {
        fun fromCentipawnLoss(cpLoss: Int): MoveQuality = when {
            cpLoss <= 10 -> GREAT
            cpLoss <= 30 -> GOOD
            cpLoss <= 80 -> INACCURACY
            cpLoss <= 180 -> MISTAKE
            else -> BLUNDER
        }
    }
}

object MoveExplainer {

    fun explainMove(board: BoardPosition, move: ChessMove, eval: EngineEvaluation?): String {
        val movingPiece = move.movingPiece ?: board.getPiece(move.from)
        val pieceName = movingPiece?.russianName ?: "Фигура"
        val afterBoard = board.makeMove(move)
        val opponentColor = board.activeColor.opposite()

        // 1. Checkmate or check
        if (move.isCheckmate) {
            return "Мат! $pieceName объявляет мат королю соперника, победа!"
        }
        if (eval?.mateIn != null && eval.mateIn > 0) {
            return "Форсированная матовая атака: мат в ${eval.mateIn} ${pluralMoves(eval.mateIn)}!"
        }

        // 2. Castling
        if (move.isCastling) {
            return "Рокировка: уводим короля в безопасный угол и вводим ладью в активную игру."
        }

        // 3. Captures
        if (move.isCapture) {
            val captured = move.capturedPiece ?: board.getPiece(move.to)
            val capturedName = captured?.russianName ?: "фигуру"
            val isDefended = MoveValidator.isSquareAttacked(board, move.to, opponentColor)

            if (!isDefended) {
                return "Забираем незащищённую $capturedName соперника на ${move.to.algebraic} без компенсации!"
            }
            if ((captured?.type?.baseValue ?: 0) > (movingPiece?.type?.baseValue ?: 0)) {
                return "Выгодный тактический размен: выигрываем качество или более сильную фигуру ($capturedName)."
            }
        }

        // 4. Tactical Fork detection
        val attackedSquares = findAttackedValuablePieces(afterBoard, move.to, opponentColor)
        if (attackedSquares.size >= 2) {
            val targets = attackedSquares.mapNotNull { afterBoard.getPiece(it)?.russianName }
            return "Двойной удар (вилка)! $pieceName атакует одновременно: ${targets.joinToString(" и ")}."
        }

        // 5. Pin detection
        if (isCausingPin(afterBoard, move.to, opponentColor)) {
            return "Связка! $pieceName прижимает фигуру противника к королю или ферзю."
        }

        // 6. Checks
        if (move.isCheck) {
            return "Атакующий шах королю: заставляем соперника тратить темп на защиту."
        }

        // 7. Center Control & Pawn advancement
        if (movingPiece?.type == PieceType.PAWN) {
            if (move.to.file in 3..4 && move.to.rank in 3..4) {
                return "Захват пространства и контроль ключевых центральных полей пешкой."
            }
            if (move.to.rank in 5..6) {
                return "Продвижение проходной или опасной пешки ближе к превращению."
            }
        }

        // 8. Piece development
        if (movingPiece?.type == PieceType.KNIGHT || movingPiece?.type == PieceType.BISHOP) {
            if (move.from.rank == 0 || move.from.rank == 7) {
                return "Гармоничное развитие лёгкой фигуры в центр для подготовки атаки."
            }
            return "Активизация фигуры: улучшаем позицию $pieceName на поле ${move.to.algebraic}."
        }

        // Default strategic comment
        return "Сильный позиционный ход: укрепляет оборону и создаёт давление на поле ${move.to.algebraic}."
    }

    private fun findAttackedValuablePieces(board: BoardPosition, attackerSq: Square, opponentColor: PieceColor): List<Square> {
        val valuable = mutableListOf<Square>()
        for (r in 0..7) {
            for (f in 0..7) {
                val sq = Square(f, r)
                val p = board.getPiece(sq)
                if (p != null && p.color == opponentColor && p.type != PieceType.PAWN) {
                    // Check if this square is attacked by attackerSq
                    if (isPieceAttackingSquare(board, attackerSq, sq)) {
                        val isDefended = MoveValidator.isSquareAttacked(board, sq, opponentColor)
                        val attackerPiece = board.getPiece(attackerSq)
                        if (!isDefended || (p.type.baseValue > (attackerPiece?.type?.baseValue ?: 0))) {
                            valuable.add(sq)
                        }
                    }
                }
            }
        }
        return valuable
    }

    private fun isPieceAttackingSquare(board: BoardPosition, pieceSq: Square, targetSq: Square): Boolean {
        val piece = board.getPiece(pieceSq) ?: return false
        val df = kotlin.math.abs(pieceSq.file - targetSq.file)
        val dr = kotlin.math.abs(pieceSq.rank - targetSq.rank)

        when (piece.type) {
            PieceType.KNIGHT -> return (df == 1 && dr == 2) || (df == 2 && dr == 1)
            PieceType.BISHOP -> {
                if (df != dr || df == 0) return false
                val stepF = if (targetSq.file > pieceSq.file) 1 else -1
                val stepR = if (targetSq.rank > pieceSq.rank) 1 else -1
                var cf = pieceSq.file + stepF
                var cr = pieceSq.rank + stepR
                while (cf != targetSq.file) {
                    if (board.getPiece(cf, cr) != null) return false
                    cf += stepF
                    cr += stepR
                }
                return true
            }
            PieceType.ROOK -> {
                if (df != 0 && dr != 0) return false
                val stepF = if (targetSq.file == pieceSq.file) 0 else if (targetSq.file > pieceSq.file) 1 else -1
                val stepR = if (targetSq.rank == pieceSq.rank) 0 else if (targetSq.rank > pieceSq.rank) 1 else -1
                var cf = pieceSq.file + stepF
                var cr = pieceSq.rank + stepR
                while (cf != targetSq.file || cr != targetSq.rank) {
                    if (board.getPiece(cf, cr) != null) return false
                    cf += stepF
                    cr += stepR
                }
                return true
            }
            PieceType.QUEEN -> {
                // Combines rook and bishop
                return isPieceAttackingSquare(board.copy().apply { setPiece(pieceSq, Piece(PieceType.ROOK, piece.color)) }, pieceSq, targetSq) ||
                        isPieceAttackingSquare(board.copy().apply { setPiece(pieceSq, Piece(PieceType.BISHOP, piece.color)) }, pieceSq, targetSq)
            }
            PieceType.KING -> return df <= 1 && dr <= 1
            PieceType.PAWN -> {
                val forward = if (piece.color == PieceColor.WHITE) 1 else -1
                return targetSq.rank == pieceSq.rank + forward && df == 1
            }
        }
    }

    private fun isCausingPin(board: BoardPosition, pieceSq: Square, opponentColor: PieceColor): Boolean {
        val piece = board.getPiece(pieceSq) ?: return false
        if (piece.type != PieceType.BISHOP && piece.type != PieceType.ROOK && piece.type != PieceType.QUEEN) {
            return false
        }
        val kingSq = board.findKing(opponentColor) ?: return false
        val df = kotlin.math.abs(pieceSq.file - kingSq.file)
        val dr = kotlin.math.abs(pieceSq.rank - kingSq.rank)

        val isAligned = (piece.type != PieceType.ROOK && df == dr) ||
                (piece.type != PieceType.BISHOP && (df == 0 || dr == 0))
        if (!isAligned) return false

        // Check if there is exactly 1 opponent piece between pieceSq and kingSq
        val stepF = if (kingSq.file == pieceSq.file) 0 else if (kingSq.file > pieceSq.file) 1 else -1
        val stepR = if (kingSq.rank == pieceSq.rank) 0 else if (kingSq.rank > pieceSq.rank) 1 else -1

        var count = 0
        var cf = pieceSq.file + stepF
        var cr = pieceSq.rank + stepR
        while (cf != kingSq.file || cr != kingSq.rank) {
            val p = board.getPiece(cf, cr)
            if (p != null) {
                if (p.color == opponentColor) count++ else return false
            }
            cf += stepF
            cr += stepR
        }
        return count == 1
    }

    private fun pluralMoves(n: Int): String = when {
        n % 10 == 1 && n % 100 != 11 -> "ход"
        n % 10 in 2..4 && n % 100 !in 12..14 -> "хода"
        else -> "ходов"
    }
}
