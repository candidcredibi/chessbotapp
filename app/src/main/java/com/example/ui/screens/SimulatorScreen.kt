package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.BoardPosition
import com.example.core.model.PieceColor
import com.example.core.model.Square
import com.example.engine.EngineEvaluation
import com.example.ui.MainViewModel
import com.example.ui.theme.ArrowAlternative2
import com.example.ui.theme.ArrowAlternative3
import com.example.ui.theme.ArrowBestMove
import com.example.ui.theme.ChessGreenOnPrimary
import com.example.ui.theme.ChessGreenPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TacticalAmber
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun SimulatorScreen(viewModel: MainViewModel) {
    val board by viewModel.simBoard.collectAsState()
    val selectedSquare by viewModel.simSelectedSquare.collectAsState()
    val legalMoves by viewModel.simLegalMovesForSelected.collectAsState()
    val evals by viewModel.simEvaluations.collectAsState()
    val isThinking by viewModel.simIsThinking.collectAsState()
    val explanation by viewModel.simExplanation.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Тестовый симулятор",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Ход: ${if (board.activeColor == PieceColor.WHITE) "Белые" else "Чёрные"}",
                    fontSize = 13.sp,
                    color = if (board.activeColor == PieceColor.WHITE) Color.White else TacticalAmber
                )
            }

            if (isThinking) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = ChessGreenPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Расчёт…", color = TextSecondary, fontSize = 12.sp)
                }
            } else {
                evals.firstOrNull()?.let { top ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (top.scoreCentipawns >= 0) Color(0xFF0D2517) else Color(0xFF2E1212)
                    ) {
                        Text(
                            text = "Оценка: ${top.formattedScore}",
                            color = if (top.scoreCentipawns >= 0) ChessGreenPrimary else Color(0xFFFF5252),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive Chessboard Container with Arrows Canvas Overlay
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
        ) {
            val boardSizePx = constraints.maxWidth.toFloat()
            val squareSizePx = boardSizePx / 8f

            // 1. Squares & Pieces Grid
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 7 downTo 0) {
                    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        for (f in 0..7) {
                            val square = Square(f, r)
                            val piece = board.getPiece(square)
                            val isLight = square.isLight
                            val isSelected = square == selectedSquare
                            val isLegalTarget = legalMoves.any { it.to == square }

                            val bgColor = when {
                                isSelected -> Color(0xFFBACA44)
                                isLight -> Color(0xFFEEEED2)
                                else -> Color(0xFF769656)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .background(bgColor)
                                    .clickable { viewModel.onSquareClick(square) },
                                contentAlignment = Alignment.Center
                            ) {
                                // Draw Piece Symbol
                                piece?.let {
                                    Text(
                                        text = it.unicodeSymbol,
                                        fontSize = 32.sp,
                                        color = if (it.color == PieceColor.WHITE) Color.White else Color.Black,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                // Legal Target Marker
                                if (isLegalTarget) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (piece != null) 34.dp else 14.dp)
                                            .border(
                                                width = if (piece != null) 3.dp else 0.dp,
                                                color = Color(0x99000000),
                                                shape = CircleShape
                                            )
                                            .background(
                                                if (piece != null) Color.Transparent else Color(0x55000000),
                                                CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Vector Arrows Canvas Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                evals.forEachIndexed { index, eval ->
                    val move = eval.move
                    // Convert move coords to canvas px
                    val startX = (move.from.file + 0.5f) * squareSizePx
                    val startY = (7 - move.from.rank + 0.5f) * squareSizePx
                    val endX = (move.to.file + 0.5f) * squareSizePx
                    val endY = (7 - move.to.rank + 0.5f) * squareSizePx

                    val arrowColor = when (index) {
                        0 -> ArrowBestMove
                        1 -> ArrowAlternative2
                        else -> ArrowAlternative3
                    }
                    val shaftW = squareSizePx * (if (index == 0) 0.22f else 0.16f)

                    drawCanvasArrow(this, startX, startY, endX, endY, arrowColor, shaftW)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Explain Move Action Button
        Button(
            onClick = { viewModel.requestExplanation() },
            colors = ButtonDefaults.buttonColors(
                containerColor = ChessGreenPrimary,
                contentColor = ChessGreenOnPrimary
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Объяснить лучший ход", fontWeight = FontWeight.Bold)
        }

        // Explanation Card
        AnimatedVisibility(visible = explanation != null) {
            explanation?.let { text ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = ChessGreenPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Тактический анализ", color = ChessGreenPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text, color = Color(0xFFF1F5F9), fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Actions: Reset & Flip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.resetSimBoard() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Сброс доски", fontSize = 13.sp)
            }
        }
    }
}

private fun drawCanvasArrow(
    scope: androidx.compose.ui.graphics.drawscope.DrawScope,
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
    color: Color,
    shaftWidth: Float
) {
    val dx = endX - startX
    val dy = endY - startY
    val length = sqrt(dx * dx + dy * dy)
    if (length < 8f) return

    val angle = atan2(dy.toDouble(), dx.toDouble())
    val headLength = shaftWidth * 2.2f
    val headWidth = shaftWidth * 2.6f

    val shaftEndX = (endX - headLength * cos(angle)).toFloat()
    val shaftEndY = (endY - headLength * sin(angle)).toFloat()

    // Draw Shaft
    scope.drawLine(
        color = color.copy(alpha = 0.9f),
        start = Offset(startX, startY),
        end = Offset(shaftEndX, shaftEndY),
        strokeWidth = shaftWidth,
        cap = StrokeCap.Round
    )

    // Draw Head
    val perpAngle = angle + Math.PI / 2
    val leftX = (shaftEndX + (headWidth / 2f * cos(perpAngle))).toFloat()
    val leftY = (shaftEndY + (headWidth / 2f * sin(perpAngle))).toFloat()
    val rightX = (shaftEndX - (headWidth / 2f * cos(perpAngle))).toFloat()
    val rightY = (shaftEndY - (headWidth / 2f * sin(perpAngle))).toFloat()

    val path = Path().apply {
        moveTo(endX, endY)
        lineTo(leftX, leftY)
        lineTo(rightX, rightY)
        close()
    }

    scope.drawPath(path = path, color = color.copy(alpha = 0.95f))
}
