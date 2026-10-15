package com.example.core.model

data class CastlingRights(
    val whiteKingside: Boolean = true,
    val whiteQueenside: Boolean = true,
    val blackKingside: Boolean = true,
    val blackQueenside: Boolean = true
) {
    val fenString: String
        get() {
            val sb = StringBuilder()
            if (whiteKingside) sb.append('K')
            if (whiteQueenside) sb.append('Q')
            if (blackKingside) sb.append('k')
            if (blackQueenside) sb.append('q')
            return if (sb.isEmpty()) "-" else sb.toString()
        }

    companion object {
        fun fromFen(fen: String): CastlingRights = CastlingRights(
            whiteKingside = fen.contains('K'),
            whiteQueenside = fen.contains('Q'),
            blackKingside = fen.contains('k'),
            blackQueenside = fen.contains('q')
        )
    }
}

class BoardPosition(
    private val squares: Array<Piece?> = Array(64) { null },
    var activeColor: PieceColor = PieceColor.WHITE,
    var castlingRights: CastlingRights = CastlingRights(),
    var enPassantTarget: Square? = null,
    var halfmoveClock: Int = 0,
    var fullmoveNumber: Int = 1
) {
    fun getPiece(square: Square): Piece? = squares[square.index]
    fun getPiece(index: Int): Piece? = squares[index]
    fun getPiece(file: Int, rank: Int): Piece? = squares[rank * 8 + file]

    fun setPiece(square: Square, piece: Piece?) {
        squares[square.index] = piece
    }

    fun setPiece(file: Int, rank: Int, piece: Piece?) {
        squares[rank * 8 + file] = piece
    }

    fun clear() {
        for (i in 0 until 64) {
            squares[i] = null
        }
        activeColor = PieceColor.WHITE
        castlingRights = CastlingRights()
        enPassantTarget = null
        halfmoveClock = 0
        fullmoveNumber = 1
    }

    fun copy(): BoardPosition {
        val newSquares = Array(64) { i -> squares[i] }
        return BoardPosition(
            squares = newSquares,
            activeColor = activeColor,
            castlingRights = castlingRights.copy(),
            enPassantTarget = enPassantTarget?.copy(),
            halfmoveClock = halfmoveClock,
            fullmoveNumber = fullmoveNumber
        )
    }

    fun makeMove(move: ChessMove): BoardPosition {
        val next = this.copy()
        val piece = next.getPiece(move.from) ?: return next
        val isCapture = next.getPiece(move.to) != null || move.isEnPassant

        // Clear origin square
        next.setPiece(move.from, null)

        // En passant capture removal
        if (piece.type == PieceType.PAWN && move.to == next.enPassantTarget) {
            val capturedPawnRank = if (next.activeColor == PieceColor.WHITE) move.to.rank - 1 else move.to.rank + 1
            next.setPiece(move.to.file, capturedPawnRank, null)
        }

        // Castling rook movement
        if (piece.type == PieceType.KING && kotlin.math.abs(move.from.file - move.to.file) > 1) {
            if (move.to.file == 6) { // Kingside
                val rookFrom = Square(7, move.from.rank)
                val rookTo = Square(5, move.from.rank)
                val rook = next.getPiece(rookFrom)
                next.setPiece(rookFrom, null)
                next.setPiece(rookTo, rook)
            } else if (move.to.file == 2) { // Queenside
                val rookFrom = Square(0, move.from.rank)
                val rookTo = Square(3, move.from.rank)
                val rook = next.getPiece(rookFrom)
                next.setPiece(rookFrom, null)
                next.setPiece(rookTo, rook)
            }
        }

        // Place moving piece or promoted piece
        val finalPiece = if (move.promotion != null) {
            Piece(move.promotion, next.activeColor)
        } else {
            piece
        }
        next.setPiece(move.to, finalPiece)

        // Update en-passant target
        next.enPassantTarget = if (piece.type == PieceType.PAWN && kotlin.math.abs(move.to.rank - move.from.rank) == 2) {
            Square(move.from.file, (move.from.rank + move.to.rank) / 2)
        } else {
            null
        }

        // Update castling rights
        var wk = next.castlingRights.whiteKingside
        var wq = next.castlingRights.whiteQueenside
        var bk = next.castlingRights.blackKingside
        var bq = next.castlingRights.blackQueenside

        if (piece.type == PieceType.KING) {
            if (next.activeColor == PieceColor.WHITE) {
                wk = false
                wq = false
            } else {
                bk = false
                bq = false
            }
        }
        if (move.from == Square(0, 0) || move.to == Square(0, 0)) wq = false
        if (move.from == Square(7, 0) || move.to == Square(7, 0)) wk = false
        if (move.from == Square(0, 7) || move.to == Square(0, 7)) bq = false
        if (move.from == Square(7, 7) || move.to == Square(7, 7)) bk = false

        next.castlingRights = CastlingRights(wk, wq, bk, bq)

        // Halfmove clock & fullmove counter
        if (piece.type == PieceType.PAWN || isCapture) {
            next.halfmoveClock = 0
        } else {
            next.halfmoveClock++
        }
        if (next.activeColor == PieceColor.BLACK) {
            next.fullmoveNumber++
        }

        next.activeColor = next.activeColor.opposite()
        return next
    }

    fun findKing(color: PieceColor): Square? {
        for (rank in 0..7) {
            for (file in 0..7) {
                val sq = Square(file, rank)
                val p = getPiece(sq)
                if (p?.type == PieceType.KING && p.color == color) {
                    return sq
                }
            }
        }
        return null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BoardPosition) return false
        if (activeColor != other.activeColor) return false
        if (castlingRights != other.castlingRights) return false
        if (enPassantTarget != other.enPassantTarget) return false
        for (i in 0 until 64) {
            if (squares[i] != other.squares[i]) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = squares.contentHashCode()
        result = 31 * result + activeColor.hashCode()
        result = 31 * result + castlingRights.hashCode()
        result = 31 * result + (enPassantTarget?.hashCode() ?: 0)
        return result
    }
}
