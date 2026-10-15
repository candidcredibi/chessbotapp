package com.example.engine

import com.example.core.model.ChessMove
import com.example.core.model.PieceColor
import kotlin.math.abs

data class EngineEvaluation(
    val move: ChessMove,
    val scoreCentipawns: Int,
    val mateIn: Int? = null,
    val depth: Int = 0,
    val pvLine: List<ChessMove> = emptyList(),
    val rankIndex: Int = 0
) {
    val formattedScore: String
        get() {
            if (mateIn != null) {
                return if (mateIn > 0) "M$mateIn" else "-M${abs(mateIn)}"
            }
            val sign = if (scoreCentipawns > 0) "+" else if (scoreCentipawns < 0) "-" else ""
            val absCp = abs(scoreCentipawns)
            val pawns = absCp / 100.0
            return "$sign%.1f".format(pawns)
        }

    val isWinning: Boolean get() = (mateIn != null && mateIn > 0) || scoreCentipawns > 150
    val isLosing: Boolean get() = (mateIn != null && mateIn < 0) || scoreCentipawns < -150

    /**
     * Approximate winning probability for white based on centipawn score.
     */
    fun winningProbability(forColor: PieceColor): Double {
        val score = if (forColor == PieceColor.WHITE) scoreCentipawns else -scoreCentipawns
        if (mateIn != null) {
            val signedMate = if (forColor == PieceColor.WHITE) mateIn else -mateIn
            return if (signedMate > 0) 100.0 else 0.0
        }
        val winProb = 1.0 / (1.0 + Math.pow(10.0, -score / 400.0))
        return (winProb * 100.0).coerceIn(0.0, 100.0)
    }
}
