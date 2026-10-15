package com.example.core.model

object MoveValidator {

    private val KNIGHT_OFFSETS = arrayOf(
        intArrayOf(1, 2), intArrayOf(2, 1), intArrayOf(2, -1), intArrayOf(1, -2),
        intArrayOf(-1, -2), intArrayOf(-2, -1), intArrayOf(-2, 1), intArrayOf(-1, 2)
    )

    private val BISHOP_DIRS = arrayOf(
        intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1)
    )

    private val ROOK_DIRS = arrayOf(
        intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1)
    )

    private val KING_OFFSETS = arrayOf(
        intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1),
        intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1)
    )

    fun generateLegalMoves(board: BoardPosition, color: PieceColor = board.activeColor): List<ChessMove> {
        val pseudoMoves = generatePseudoLegalMoves(board, color)
        val legalMoves = ArrayList<ChessMove>(pseudoMoves.size)

        for (move in pseudoMoves) {
            val nextBoard = board.makeMove(move)
            // After makeMove, nextBoard.activeColor is opposite, so check if original color's king is safe
            val kingSq = nextBoard.findKing(color)
            if (kingSq != null && !isSquareAttacked(nextBoard, kingSq, color.opposite())) {
                val opponentKingSq = nextBoard.findKing(color.opposite())
                val isCheck = opponentKingSq != null && isSquareAttacked(nextBoard, opponentKingSq, color)
                val checkmate = if (isCheck) {
                    generateLegalMoves(nextBoard, color.opposite()).isEmpty()
                } else false

                legalMoves.add(
                    move.copy(
                        isCheck = isCheck,
                        isCheckmate = checkmate
                    )
                )
            }
        }

        return legalMoves
    }

    fun isSquareAttacked(board: BoardPosition, square: Square, byColor: PieceColor): Boolean {
        // 1. Attacked by pawns
        val pawnDir = if (byColor == PieceColor.WHITE) 1 else -1
        val pawnRank = square.rank - pawnDir
        if (pawnRank in 0..7) {
            if (square.file - 1 in 0..7) {
                val p = board.getPiece(square.file - 1, pawnRank)
                if (p?.type == PieceType.PAWN && p.color == byColor) return true
            }
            if (square.file + 1 in 0..7) {
                val p = board.getPiece(square.file + 1, pawnRank)
                if (p?.type == PieceType.PAWN && p.color == byColor) return true
            }
        }

        // 2. Attacked by knights
        for (offset in KNIGHT_OFFSETS) {
            val f = square.file + offset[0]
            val r = square.rank + offset[1]
            if (f in 0..7 && r in 0..7) {
                val p = board.getPiece(f, r)
                if (p?.type == PieceType.KNIGHT && p.color == byColor) return true
            }
        }

        // 3. Attacked by bishops / queens (diagonals)
        for (dir in BISHOP_DIRS) {
            var f = square.file + dir[0]
            var r = square.rank + dir[1]
            while (f in 0..7 && r in 0..7) {
                val p = board.getPiece(f, r)
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                f += dir[0]
                r += dir[1]
            }
        }

        // 4. Attacked by rooks / queens (orthogonal)
        for (dir in ROOK_DIRS) {
            var f = square.file + dir[0]
            var r = square.rank + dir[1]
            while (f in 0..7 && r in 0..7) {
                val p = board.getPiece(f, r)
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                f += dir[0]
                r += dir[1]
            }
        }

        // 5. Attacked by King
        for (offset in KING_OFFSETS) {
            val f = square.file + offset[0]
            val r = square.rank + offset[1]
            if (f in 0..7 && r in 0..7) {
                val p = board.getPiece(f, r)
                if (p?.type == PieceType.KING && p.color == byColor) return true
            }
        }

        return false
    }

    private fun generatePseudoLegalMoves(board: BoardPosition, color: PieceColor): List<ChessMove> {
        val moves = ArrayList<ChessMove>(40)
        val forward = if (color == PieceColor.WHITE) 1 else -1
        val startRank = if (color == PieceColor.WHITE) 1 else 6
        val promoRank = if (color == PieceColor.WHITE) 7 else 0

        for (rank in 0..7) {
            for (file in 0..7) {
                val piece = board.getPiece(file, rank) ?: continue
                if (piece.color != color) continue
                val from = Square(file, rank)

                when (piece.type) {
                    PieceType.PAWN -> {
                        // Forward 1
                        val nextRank = rank + forward
                        if (nextRank in 0..7 && board.getPiece(file, nextRank) == null) {
                            if (nextRank == promoRank) {
                                for (promo in arrayOf(PieceType.QUEEN, PieceType.KNIGHT, PieceType.ROOK, PieceType.BISHOP)) {
                                    moves.add(ChessMove(from, Square(file, nextRank), promo, movingPiece = piece))
                                }
                            } else {
                                moves.add(ChessMove(from, Square(file, nextRank), movingPiece = piece))
                                // Forward 2
                                val doubleRank = rank + 2 * forward
                                if (rank == startRank && board.getPiece(file, doubleRank) == null) {
                                    moves.add(ChessMove(from, Square(file, doubleRank), movingPiece = piece))
                                }
                            }
                        }

                        // Diagonal Captures
                        for (df in intArrayOf(-1, 1)) {
                            val capFile = file + df
                            if (capFile in 0..7 && nextRank in 0..7) {
                                val targetSquare = Square(capFile, nextRank)
                                val targetPiece = board.getPiece(targetSquare)
                                if (targetPiece != null && targetPiece.color != color) {
                                    if (nextRank == promoRank) {
                                        for (promo in arrayOf(PieceType.QUEEN, PieceType.KNIGHT, PieceType.ROOK, PieceType.BISHOP)) {
                                            moves.add(ChessMove(from, targetSquare, promo, isCapture = true, movingPiece = piece, capturedPiece = targetPiece))
                                        }
                                    } else {
                                        moves.add(ChessMove(from, targetSquare, isCapture = true, movingPiece = piece, capturedPiece = targetPiece))
                                    }
                                } else if (targetSquare == board.enPassantTarget) {
                                    // En passant
                                    val capturedPawn = board.getPiece(capFile, rank)
                                    moves.add(ChessMove(from, targetSquare, isCapture = true, isEnPassant = true, movingPiece = piece, capturedPiece = capturedPawn))
                                }
                            }
                        }
                    }

                    PieceType.KNIGHT -> {
                        for (offset in KNIGHT_OFFSETS) {
                            val tf = file + offset[0]
                            val tr = rank + offset[1]
                            if (tf in 0..7 && tr in 0..7) {
                                val target = Square(tf, tr)
                                val targetPiece = board.getPiece(target)
                                if (targetPiece == null) {
                                    moves.add(ChessMove(from, target, movingPiece = piece))
                                } else if (targetPiece.color != color) {
                                    moves.add(ChessMove(from, target, isCapture = true, movingPiece = piece, capturedPiece = targetPiece))
                                }
                            }
                        }
                    }

                    PieceType.BISHOP -> {
                        generateRayMoves(board, from, piece, BISHOP_DIRS, moves)
                    }

                    PieceType.ROOK -> {
                        generateRayMoves(board, from, piece, ROOK_DIRS, moves)
                    }

                    PieceType.QUEEN -> {
                        generateRayMoves(board, from, piece, BISHOP_DIRS, moves)
                        generateRayMoves(board, from, piece, ROOK_DIRS, moves)
                    }

                    PieceType.KING -> {
                        for (offset in KING_OFFSETS) {
                            val tf = file + offset[0]
                            val tr = rank + offset[1]
                            if (tf in 0..7 && tr in 0..7) {
                                val target = Square(tf, tr)
                                val targetPiece = board.getPiece(target)
                                if (targetPiece == null) {
                                    moves.add(ChessMove(from, target, movingPiece = piece))
                                } else if (targetPiece.color != color) {
                                    moves.add(ChessMove(from, target, isCapture = true, movingPiece = piece, capturedPiece = targetPiece))
                                }
                            }
                        }

                        // Castling
                        val kingRank = if (color == PieceColor.WHITE) 0 else 7
                        if (rank == kingRank && file == 4) {
                            val canKingside = if (color == PieceColor.WHITE) board.castlingRights.whiteKingside else board.castlingRights.blackKingside
                            val canQueenside = if (color == PieceColor.WHITE) board.castlingRights.whiteQueenside else board.castlingRights.blackQueenside

                            if (canKingside &&
                                board.getPiece(5, kingRank) == null &&
                                board.getPiece(6, kingRank) == null &&
                                board.getPiece(7, kingRank)?.type == PieceType.ROOK &&
                                !isSquareAttacked(board, Square(4, kingRank), color.opposite()) &&
                                !isSquareAttacked(board, Square(5, kingRank), color.opposite()) &&
                                !isSquareAttacked(board, Square(6, kingRank), color.opposite())
                            ) {
                                moves.add(ChessMove(from, Square(6, kingRank), isCastling = true, movingPiece = piece))
                            }

                            if (canQueenside &&
                                board.getPiece(3, kingRank) == null &&
                                board.getPiece(2, kingRank) == null &&
                                board.getPiece(1, kingRank) == null &&
                                board.getPiece(0, kingRank)?.type == PieceType.ROOK &&
                                !isSquareAttacked(board, Square(4, kingRank), color.opposite()) &&
                                !isSquareAttacked(board, Square(3, kingRank), color.opposite()) &&
                                !isSquareAttacked(board, Square(2, kingRank), color.opposite())
                            ) {
                                moves.add(ChessMove(from, Square(2, kingRank), isCastling = true, movingPiece = piece))
                            }
                        }
                    }
                }
            }
        }

        return moves
    }

    private fun generateRayMoves(
        board: BoardPosition,
        from: Square,
        piece: Piece,
        directions: Array<IntArray>,
        outMoves: MutableList<ChessMove>
    ) {
        for (dir in directions) {
            var f = from.file + dir[0]
            var r = from.rank + dir[1]
            while (f in 0..7 && r in 0..7) {
                val target = Square(f, r)
                val targetPiece = board.getPiece(target)
                if (targetPiece == null) {
                    outMoves.add(ChessMove(from, target, movingPiece = piece))
                } else {
                    if (targetPiece.color != piece.color) {
                        outMoves.add(ChessMove(from, target, isCapture = true, movingPiece = piece, capturedPiece = targetPiece))
                    }
                    break
                }
                f += dir[0]
                r += dir[1]
            }
        }
    }

    /**
     * Finds the legal chess move that transitions prevBoard into currentBoard.
     * Useful for computer vision stability & move tracking.
     */
    fun findTransitionMove(prevBoard: BoardPosition, currentBoard: BoardPosition): ChessMove? {
        val legalMoves = generateLegalMoves(prevBoard, prevBoard.activeColor)
        for (move in legalMoves) {
            val candidate = prevBoard.makeMove(move)
            var matches = true
            for (i in 0 until 64) {
                val p1 = candidate.getPiece(i)
                val p2 = currentBoard.getPiece(i)
                if (p1?.type != p2?.type || p1?.color != p2?.color) {
                    matches = false
                    break
                }
            }
            if (matches) return move
        }
        return null
    }
}
