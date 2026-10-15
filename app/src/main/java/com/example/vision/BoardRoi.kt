package com.example.vision

import android.graphics.Rect

data class BoardRoi(
    val bounds: Rect,
    val isFlipped: Boolean = false, // true if Black is at the bottom
    val lightSquareColorRgb: Int = 0xEEEED2,
    val darkSquareColorRgb: Int = 0x769656,
    val confidence: Float = 0.9f
) {
    val squareWidth: Float get() = bounds.width() / 8f
    val squareHeight: Float get() = bounds.height() / 8f

    fun getSquareRect(file: Int, rank: Int): Rect {
        val col = if (isFlipped) 7 - file else file
        val row = if (isFlipped) rank else 7 - rank

        val left = (bounds.left + col * squareWidth).toInt()
        val top = (bounds.top + row * squareHeight).toInt()
        val right = (left + squareWidth).toInt()
        val bottom = (top + squareHeight).toInt()

        return Rect(left, top, right, bottom)
    }
}
