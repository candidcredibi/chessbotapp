package com.example.core.model

object FenParser {
    const val STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    fun parse(fen: String): BoardPosition {
        val parts = fen.trim().split("\\s+".toRegex())
        val board = BoardPosition()

        val piecePlacement = parts[0]
        val ranks = piecePlacement.split("/")
        require(ranks.size == 8) { "FEN piece placement must contain 8 ranks: $piecePlacement" }

        // FEN ranks are specified from 8 (top, rank 7) down to 1 (bottom, rank 0)
        for (r in 0..7) {
            val rankIndex = 7 - r
            val rankStr = ranks[r]
            var fileIndex = 0

            for (char in rankStr) {
                if (char.isDigit()) {
                    fileIndex += char.digitToInt()
                } else {
                    val color = if (char.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
                    val type = PieceType.fromChar(char) ?: error("Invalid FEN piece char: $char")
                    board.setPiece(fileIndex, rankIndex, Piece(type, color))
                    fileIndex++
                }
            }
        }

        // Active color
        if (parts.size > 1) {
            board.activeColor = if (parts[1].lowercase() == "b") PieceColor.BLACK else PieceColor.WHITE
        }

        // Castling rights
        if (parts.size > 2) {
            board.castlingRights = CastlingRights.fromFen(parts[2])
        }

        // En passant
        if (parts.size > 3 && parts[3] != "-") {
            board.enPassantTarget = runCatching { Square.fromAlgebraic(parts[3]) }.getOrNull()
        }

        // Halfmove
        if (parts.size > 4) {
            board.halfmoveClock = parts[4].toIntOrNull() ?: 0
        }

        // Fullmove
        if (parts.size > 5) {
            board.fullmoveNumber = parts[5].toIntOrNull() ?: 1
        }

        return board
    }

    fun toFen(board: BoardPosition): String {
        val sb = StringBuilder()

        // 1. Piece placement: rank 7 down to 0
        for (r in 7 downTo 0) {
            var emptyCount = 0
            for (f in 0..7) {
                val piece = board.getPiece(f, r)
                if (piece == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    sb.append(piece.fenChar)
                }
            }
            if (emptyCount > 0) {
                sb.append(emptyCount)
            }
            if (r > 0) {
                sb.append('/')
            }
        }

        // 2. Active color
        sb.append(' ').append(board.activeColor.fenChar)

        // 3. Castling
        sb.append(' ').append(board.castlingRights.fenString)

        // 4. En passant
        sb.append(' ').append(board.enPassantTarget?.algebraic ?: "-")

        // 5. Halfmove clock
        sb.append(' ').append(board.halfmoveClock)

        // 6. Fullmove number
        sb.append(' ').append(board.fullmoveNumber)

        return sb.toString()
    }
}
