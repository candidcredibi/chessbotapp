package com.example.core.model

data class ChessMove(
    val from: Square,
    val to: Square,
    val promotion: PieceType? = null,
    val isCapture: Boolean = false,
    val isCheck: Boolean = false,
    val isCheckmate: Boolean = false,
    val isCastling: Boolean = false,
    val isEnPassant: Boolean = false,
    val movingPiece: Piece? = null,
    val capturedPiece: Piece? = null
) {
    val uci: String
        get() = "${from.algebraic}${to.algebraic}${promotion?.uciChar ?: ""}"

    fun toSan(board: BoardPosition? = null): String {
        if (isCastling) {
            return if (to.file > from.file) "O-O" else "O-O-O"
        }
        val piece = movingPiece ?: board?.getPiece(from)
        val sb = StringBuilder()
        if (piece != null && piece.type != PieceType.PAWN) {
            sb.append(piece.type.symbolChar.uppercaseChar())
        }
        if (isCapture) {
            if (piece?.type == PieceType.PAWN) {
                sb.append('a' + from.file)
            }
            sb.append('x')
        }
        sb.append(to.algebraic)
        if (promotion != null) {
            sb.append("=").append(promotion.symbolChar.uppercaseChar())
        }
        if (isCheckmate) {
            sb.append("#")
        } else if (isCheck) {
            sb.append("+")
        }
        return sb.toString()
    }

    companion object {
        fun fromUci(uci: String, board: BoardPosition? = null): ChessMove? {
            if (uci.length < 4) return null
            val from = runCatching { Square.fromAlgebraic(uci.substring(0, 2)) }.getOrNull() ?: return null
            val to = runCatching { Square.fromAlgebraic(uci.substring(2, 4)) }.getOrNull() ?: return null
            val promo = if (uci.length >= 5) PieceType.fromChar(uci[4]) else null
            val piece = board?.getPiece(from)
            val captured = board?.getPiece(to)
            val isCastling = piece?.type == PieceType.KING && kotlin.math.abs(from.file - to.file) > 1
            val isEnPassant = piece?.type == PieceType.PAWN && from.file != to.file && captured == null
            val isCapture = captured != null || isEnPassant
            return ChessMove(
                from = from,
                to = to,
                promotion = promo,
                movingPiece = piece,
                capturedPiece = captured,
                isCapture = isCapture,
                isCastling = isCastling,
                isEnPassant = isEnPassant
            )
        }
    }
}
