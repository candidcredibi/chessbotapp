package com.example.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.example.engine.EngineEvaluation
import com.example.vision.BoardRoi
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class OverlayArrowView(context: Context) : View(context) {

    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xEE131923.toInt()
        style = Paint.Style.FILL
    }

    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private var evaluations: List<EngineEvaluation> = emptyList()
    private var boardRoi: BoardRoi? = null
    private var animatedAlpha: Float = 1f
    private var fadeAnimator: ValueAnimator? = null

    private val arrowColors = intArrayOf(
        0xEE00E676.toInt(), // Green best
        0xEEFFD600.toInt(), // Yellow 2nd
        0xEE00E5FF.toInt()  // Cyan 3rd
    )

    fun updateEvaluations(newEvals: List<EngineEvaluation>, roi: BoardRoi?) {
        this.evaluations = newEvals
        this.boardRoi = roi

        fadeAnimator?.cancel()
        fadeAnimator = ValueAnimator.ofFloat(0.2f, 1f).apply {
            duration = 180
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                animatedAlpha = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun clearArrows() {
        evaluations = emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val roi = boardRoi ?: return
        if (evaluations.isEmpty()) return

        val bounds = roi.bounds
        val sqW = roi.squareWidth
        val sqH = roi.squareHeight

        // Draw from 3rd down to 1st so the best move is on top
        for (i in (evaluations.size - 1) downTo 0) {
            val eval = evaluations[i]
            val move = eval.move
            val fromRect = roi.getSquareRect(move.from.file, move.from.rank)
            val toRect = roi.getSquareRect(move.to.file, move.to.rank)

            val startX = fromRect.centerX().toFloat()
            val startY = fromRect.centerY().toFloat()
            val endX = toRect.centerX().toFloat()
            val endY = toRect.centerY().toFloat()

            val baseColor = arrowColors.getOrElse(i) { 0xEE00E5FF.toInt() }
            val alphaInt = ((baseColor ushr 24) * animatedAlpha).toInt()
            val colorWithAlpha = (alphaInt shl 24) or (baseColor and 0x00FFFFFF)

            arrowPaint.color = colorWithAlpha
            val shaftWidth = when (i) {
                0 -> sqW * 0.22f
                1 -> sqW * 0.17f
                else -> sqW * 0.14f
            }

            drawSmoothArrow(canvas, startX, startY, endX, endY, shaftWidth)
        }

        // Draw Evaluation Badge on the top corner of the board
        val topEval = evaluations.firstOrNull()
        if (topEval != null) {
            drawEvaluationBadge(canvas, bounds, topEval)
        }
    }

    private fun drawSmoothArrow(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        shaftWidth: Float
    ) {
        val dx = endX - startX
        val dy = endY - startY
        val length = sqrt(dx * dx + dy * dy)
        if (length < 10f) return

        val angle = atan2(dy.toDouble(), dx.toDouble())
        val headLength = shaftWidth * 2.2f
        val headWidth = shaftWidth * 2.6f

        // End of the shaft before head
        val shaftEndX = endX - (headLength * cos(angle)).toFloat()
        val shaftEndY = endY - (headLength * sin(angle)).toFloat()

        // Draw Shaft
        arrowPaint.strokeWidth = shaftWidth
        arrowPaint.style = Paint.Style.STROKE
        canvas.drawLine(startX, startY, shaftEndX, shaftEndY, arrowPaint)

        // Draw Arrowhead Path
        val path = Path()
        val perpAngle = angle + Math.PI / 2
        val leftX = shaftEndX + (headWidth / 2f * cos(perpAngle)).toFloat()
        val leftY = shaftEndY + (headWidth / 2f * sin(perpAngle)).toFloat()
        val rightX = shaftEndX - (headWidth / 2f * cos(perpAngle)).toFloat()
        val rightY = shaftEndY - (headWidth / 2f * sin(perpAngle)).toFloat()

        path.moveTo(endX, endY)
        path.lineTo(leftX, leftY)
        path.lineTo(rightX, rightY)
        path.close()

        arrowPaint.style = Paint.Style.FILL
        canvas.drawPath(path, arrowPaint)
    }

    private fun drawEvaluationBadge(canvas: Canvas, boardBounds: Rect, eval: EngineEvaluation) {
        val text = eval.formattedScore
        val badgeW = 140f
        val badgeH = 58f
        val left = boardBounds.left + 16f
        val top = boardBounds.top - badgeH - 12f
        val rectF = RectF(left, top, left + badgeW, top + badgeH)

        // Background pill
        canvas.drawRoundRect(rectF, 16f, 16f, badgeBgPaint)

        // Accent indicator dot
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (eval.scoreCentipawns >= 0) 0xFF00E676.toInt() else 0xFFFF5252.toInt()
        }
        canvas.drawCircle(left + 24f, top + badgeH / 2f, 8f, dotPaint)

        // Score text
        canvas.drawText(text, left + badgeW * 0.62f, top + badgeH * 0.68f, badgeTextPaint)
    }
}
