package com.example.vision

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.core.model.BoardPosition
import com.example.core.model.Piece
import com.example.core.model.PieceColor
import com.example.core.model.PieceType
import com.example.core.model.Square

class BoardDetector(
    private val pieceClassifier: PieceClassifier = PieceClassifier()
) {
    private var cachedRoi: BoardRoi? = null

    fun detectBoard(screenBitmap: Bitmap): BoardRoi? {
        val width = screenBitmap.width
        val height = screenBitmap.height
        if (width < 200 || height < 200) return null

        // 1. Initial heuristic: In Chess.com mobile, the board is centered horizontally
        // and square width ≈ screen width * 0.96 (with small margins)
        val boardSide = (width * 0.98f).toInt()
        val left = ((width - boardSide) / 2).coerceAtLeast(0)
        // Center vertically around ~45% of height
        val centerY = (height * 0.48f).toInt()
        val top = (centerY - boardSide / 2).coerceIn(0, height - boardSide)
        val right = left + boardSide
        val bottom = top + boardSide

        val candidateRect = Rect(left, top, right, bottom)

        // 2. Sample light and dark square colors from candidate grid
        val sqW = boardSide / 8
        val sqH = boardSide / 8

        // Sample center of square (file 0, rank 0) and (file 1, rank 0)
        val p00 = screenBitmap.getPixel(left + sqW / 2, bottom - sqH / 2) // a1 (dark in standard Chess.com)
        val p10 = screenBitmap.getPixel(left + sqW + sqW / 2, bottom - sqH / 2) // b1 (light)

        val roi = BoardRoi(
            bounds = candidateRect,
            isFlipped = false,
            lightSquareColorRgb = p10,
            darkSquareColorRgb = p00,
            confidence = 0.92f
        )

        cachedRoi = roi
        return roi
    }

    fun extractPosition(screenBitmap: Bitmap, roi: BoardRoi): BoardPosition {
        val board = BoardPosition()
        val bounds = roi.bounds
        val sqW = (bounds.width() / 8).coerceAtLeast(8)
        val sqH = (bounds.height() / 8).coerceAtLeast(8)

        // Target size for piece classification: 64x64
        val targetSize = 64
        val squareBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(squareBitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
        val srcRect = Rect()
        val dstRect = Rect(0, 0, targetSize, targetSize)

        for (rank in 0..7) {
            for (file in 0..7) {
                val sqRect = roi.getSquareRect(file, rank)
                // Ensure rect is within screen bounds
                srcRect.set(
                    sqRect.left.coerceIn(0, screenBitmap.width),
                    sqRect.top.coerceIn(0, screenBitmap.height),
                    sqRect.right.coerceIn(0, screenBitmap.width),
                    sqRect.bottom.coerceIn(0, screenBitmap.height)
                )

                if (srcRect.width() > 4 && srcRect.height() > 4) {
                    canvas.drawBitmap(screenBitmap, srcRect, dstRect, paint)
                    val isLight = Square(file, rank).isLight
                    val piece = pieceClassifier.classifySquare(
                        squareBitmap = squareBitmap,
                        isLightSquare = isLight,
                        boardLightRgb = roi.lightSquareColorRgb,
                        boardDarkRgb = roi.darkSquareColorRgb
                    )
                    board.setPiece(file, rank, piece)
                }
            }
        }

        // Determine orientation if not yet fixed
        detectOrientation(board, roi)

        return board
    }

    private fun detectOrientation(board: BoardPosition, roi: BoardRoi) {
        var whiteInBottomRanks = 0
        var blackInBottomRanks = 0
        for (r in 0..1) {
            for (f in 0..7) {
                val p = board.getPiece(f, r) ?: continue
                if (p.color == PieceColor.WHITE) whiteInBottomRanks++ else blackInBottomRanks++
            }
        }
        // If bottom ranks contain predominantly black pieces, the board is flipped
        if (blackInBottomRanks > whiteInBottomRanks && blackInBottomRanks >= 4) {
            // Flipped
        }
    }
}
