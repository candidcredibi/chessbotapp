package com.example.core.model

enum class PieceType(val symbolChar: Char, val baseValue: Int) {
    PAWN('p', 100),
    KNIGHT('n', 320),
    BISHOP('b', 330),
    ROOK('r', 500),
    QUEEN('q', 900),
    KING('k', 20000);

    val uciChar: Char get() = symbolChar

    companion object {
        fun fromChar(char: Char): PieceType? = when (char.lowercaseChar()) {
            'p' -> PAWN
            'n' -> KNIGHT
            'b' -> BISHOP
            'r' -> ROOK
            'q' -> QUEEN
            'k' -> KING
            else -> null
        }
    }
}

enum class PieceColor(val fenChar: Char, val displayNameRu: String) {
    WHITE('w', "Белые"),
    BLACK('b', "Чёрные");

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

data class Piece(
    val type: PieceType,
    val color: PieceColor
) {
    val fenChar: Char
        get() = if (color == PieceColor.WHITE) {
            type.symbolChar.uppercaseChar()
        } else {
            type.symbolChar.lowercaseChar()
        }

    val unicodeSymbol: String
        get() = when (color) {
            PieceColor.WHITE -> when (type) {
                PieceType.PAWN -> "♙"
                PieceType.KNIGHT -> "♘"
                PieceType.BISHOP -> "♗"
                PieceType.ROOK -> "♖"
                PieceType.QUEEN -> "♕"
                PieceType.KING -> "♔"
            }
            PieceColor.BLACK -> when (type) {
                PieceType.PAWN -> "♟"
                PieceType.KNIGHT -> "♞"
                PieceType.BISHOP -> "♝"
                PieceType.ROOK -> "♜"
                PieceType.QUEEN -> "♛"
                PieceType.KING -> "♚"
            }
        }

    val russianName: String
        get() = when (type) {
            PieceType.PAWN -> "Пешка"
            PieceType.KNIGHT -> "Конь"
            PieceType.BISHOP -> "Слон"
            PieceType.ROOK -> "Ладья"
            PieceType.QUEEN -> "Ферзь"
            PieceType.KING -> "Король"
        }
}
