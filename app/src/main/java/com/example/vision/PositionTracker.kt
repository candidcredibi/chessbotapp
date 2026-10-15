package com.example.vision

import android.graphics.Bitmap
import com.example.core.model.BoardPosition
import com.example.core.model.ChessMove
import com.example.core.model.FenParser
import com.example.core.model.MoveValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TrackerState {
    IDLE,
    SEARCHING_BOARD,
    CALIBRATING,
    TRACKING,
    PAUSED,
    BOARD_LOST,
    DISABLED
}

data class TrackerResult(
    val state: TrackerState,
    val currentPosition: BoardPosition? = null,
    val lastRecognizedMove: ChessMove? = null,
    val boardRoi: BoardRoi? = null,
    val confidence: Float = 0f,
    val moveHistory: List<ChessMove> = emptyList()
)

class PositionTracker(
    private val boardDetector: BoardDetector = BoardDetector()
) {
    private val _resultFlow = MutableStateFlow(TrackerResult(TrackerState.IDLE))
    val resultFlow: StateFlow<TrackerResult> = _resultFlow.asStateFlow()

    private var activeRoi: BoardRoi? = null
    private var lastStablePosition: BoardPosition? = null
    private var candidatePosition: BoardPosition? = null
    private var consecutiveFrameMatches = 0
    private val moveHistory = mutableListOf<ChessMove>()
    private var isUserPaused = false

    fun start() {
        isUserPaused = false
        _resultFlow.value = _resultFlow.value.copy(state = TrackerState.SEARCHING_BOARD)
    }

    fun pause() {
        isUserPaused = true
        _resultFlow.value = _resultFlow.value.copy(state = TrackerState.PAUSED)
    }

    fun resume() {
        isUserPaused = false
        _resultFlow.value = _resultFlow.value.copy(
            state = if (activeRoi != null) TrackerState.TRACKING else TrackerState.SEARCHING_BOARD
        )
    }

    fun stop() {
        isUserPaused = false
        activeRoi = null
        lastStablePosition = null
        candidatePosition = null
        consecutiveFrameMatches = 0
        _resultFlow.value = TrackerResult(TrackerState.IDLE)
    }

    fun processFrame(screenBitmap: Bitmap): TrackerResult {
        if (isUserPaused) {
            return _resultFlow.value.copy(state = TrackerState.PAUSED)
        }

        // 1. Locate ROI if not yet found
        var roi = activeRoi
        if (roi == null) {
            roi = boardDetector.detectBoard(screenBitmap)
            if (roi == null) {
                val res = _resultFlow.value.copy(state = TrackerState.SEARCHING_BOARD)
                _resultFlow.value = res
                return res
            }
            activeRoi = roi
        }

        // 2. Extract current board from ROI
        val extracted = boardDetector.extractPosition(screenBitmap, roi)

        // 3. Frame stability check (2-3 consecutive frames must agree)
        if (candidatePosition != null && areBoardsEqual(candidatePosition!!, extracted)) {
            consecutiveFrameMatches++
        } else {
            candidatePosition = extracted
            consecutiveFrameMatches = 1
        }

        if (consecutiveFrameMatches >= 2) {
            // Position confirmed stable!
            val stable = candidatePosition!!
            if (lastStablePosition == null) {
                // Initial board established
                lastStablePosition = stable
                val res = TrackerResult(
                    state = TrackerState.TRACKING,
                    currentPosition = stable,
                    boardRoi = roi,
                    confidence = 0.95f,
                    moveHistory = moveHistory
                )
                _resultFlow.value = res
                return res
            } else if (!areBoardsEqual(lastStablePosition!!, stable)) {
                // Board changed! Check if there is a legal move transitioning old -> new
                val transitionMove = MoveValidator.findTransitionMove(lastStablePosition!!, stable)
                if (transitionMove != null) {
                    moveHistory.add(transitionMove)
                    lastStablePosition = stable
                    val res = TrackerResult(
                        state = TrackerState.TRACKING,
                        currentPosition = stable,
                        lastRecognizedMove = transitionMove,
                        boardRoi = roi,
                        confidence = 0.98f,
                        moveHistory = moveHistory.toList()
                    )
                    _resultFlow.value = res
                    return res
                } else {
                    // Could be new game, or user adjusted piece
                    lastStablePosition = stable
                    val res = TrackerResult(
                        state = TrackerState.TRACKING,
                        currentPosition = stable,
                        boardRoi = roi,
                        confidence = 0.80f,
                        moveHistory = moveHistory.toList()
                    )
                    _resultFlow.value = res
                    return res
                }
            }
        }

        return _resultFlow.value
    }

    private fun areBoardsEqual(b1: BoardPosition, b2: BoardPosition): Boolean {
        for (i in 0 until 64) {
            val p1 = b1.getPiece(i)
            val p2 = b2.getPiece(i)
            if (p1?.type != p2?.type || p1?.color != p2?.color) return false
        }
        return true
    }
}
