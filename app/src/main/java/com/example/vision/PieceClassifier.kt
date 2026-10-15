package com.example.vision

import android.graphics.Bitmap
import android.graphics.Color
import com.example.core.model.Piece
import com.example.core.model.PieceColor
import com.example.core.model.PieceType
import kotlin.math.abs

class PieceClassifier {

    private val cachedSquarePixels = IntArray(64 * 64)

    /**
     * Calculates the mean absolute difference between two square bitmaps.
     * Returns a normalized diff score between 0.0f (identical) and 1.0f (completely different).
     */
    fun computeDiff(squareA: Bitmap, squareB: Bitmap): Float {
        val w = minOf(squareA.width, squareB.width, 32)
        val h = minOf(squareA.height, squareB.height, 32)
        var diffSum = 0L

        for (y in 0 until h step 2) {
            for (x in 0 until w step 2) {
                val p1 = squareA.getPixel(x, y)
                val p2 = squareB.getPixel(x, y)
                val dr = abs(Color.red(p1) - Color.red(p2))
                val dg = abs(Color.green(p1) - Color.green(p2))
                val db = abs(Color.blue(p1) - Color.blue(p2))
                diffSum += (dr + dg + db)
            }
        }
        val sampledPixels = (w / 2) * (h / 2)
        return (diffSum.toFloat() / (sampledPixels * 3 * 255)).coerceIn(0f, 1f)
    }

    /**
     * Classifies a 64x64 square bitmap into a Piece or null (empty).
     * @param squareBitmap 64x64 bitmap of the square
     * @param isLightSquare whether the square is light or dark on the board
     * @param boardLightRgb calibrated light square color
     * @param boardDarkRgb calibrated dark square color
     */
    fun classifySquare(
        squareBitmap: Bitmap,
        isLightSquare: Boolean,
        boardLightRgb: Int = 0xEEEED2,
        boardDarkRgb: Int = 0x769656
    ): Piece? {
        val w = squareBitmap.width
        val h = squareBitmap.height
        if (w < 16 || h < 16) return null

        val bgR = if (isLightSquare) (boardLightRgb shr 16) and 0xFF else (boardDarkRgb shr 16) and 0xFF
        val bgG = if (isLightSquare) (boardLightRgb shr 8) and 0xFF else (boardDarkRgb shr 8) and 0xFF
        val bgB = if (isLightSquare) boardLightRgb and 0xFF else boardDarkRgb and 0xFF

        var fgPixelCount = 0
        var fgLuminanceSum = 0L
        var minX = w
        var maxX = 0
        var minY = h
        var maxY = 0

        // Analyze center 70% to avoid grid lines
        val startX = (w * 0.15f).toInt()
        val endX = (w * 0.85f).toInt()
        val startY = (h * 0.15f).toInt()
        val endY = (h * 0.85f).toInt()

        for (y in startY until endY) {
            for (x in startX until endX) {
                val pixel = squareBitmap.getPixel(x, y)
                val pr = Color.red(pixel)
                val pg = Color.green(pixel)
                val pb = Color.blue(pixel)

                val diffFromBg = abs(pr - bgR) + abs(pg - bgG) + abs(pb - bgB)
                // Threshold difference to consider foreground piece pixel
                if (diffFromBg > 75) {
                    fgPixelCount++
                    val lum = (0.299 * pr + 0.587 * pg + 0.114 * pb).toInt()
                    fgLuminanceSum += lum

                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        val totalSampled = (endX - startX) * (endY - startY)
        val fillRatio = fgPixelCount.toFloat() / totalSampled

        // If very few foreground pixels, square is empty
        if (fillRatio < 0.12f) {
            return null
        }

        val avgFgLuminance = if (fgPixelCount > 0) fgLuminanceSum / fgPixelCount else 128
        val pieceColor = if (avgFgLuminance > 130) PieceColor.WHITE else PieceColor.BLACK

        // Piece type heuristics from bounding box and density
        val pieceHeight = (maxY - minY).coerceAtLeast(1)
        val pieceWidth = (maxX - minX).coerceAtLeast(1)
        val heightRatio = pieceHeight.toFloat() / h
        val widthRatio = pieceWidth.toFloat() / w
        val aspectRatio = pieceWidth.toFloat() / pieceHeight

        val pieceType = when {
            // Pawns are smallest in height and fill
            heightRatio < 0.48f && fillRatio < 0.28f -> PieceType.PAWN
            // Kings and Queens are tall and fill substantial area
            heightRatio > 0.62f && fillRatio > 0.38f -> {
                if (aspectRatio > 0.85f) PieceType.QUEEN else PieceType.KING
            }
            // Knights are asymmetrical and compact
            aspectRatio > 0.82f && fillRatio in 0.26f..0.42f -> PieceType.KNIGHT
            // Rooks have castle shape with wide top
            heightRatio in 0.48f..0.60f && aspectRatio in 0.72f..0.90f -> PieceType.ROOK
            // Bishops have pointed mitre
            else -> PieceType.BISHOP
        }

        return Piece(pieceType, pieceColor)
    }
}
