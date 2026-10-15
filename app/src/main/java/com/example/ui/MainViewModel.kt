package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.BoardPosition
import com.example.core.model.ChessMove
import com.example.core.model.FenParser
import com.example.core.model.MoveValidator
import com.example.core.model.PieceColor
import com.example.core.model.Square
import com.example.engine.ChessEngine
import com.example.engine.EngineEvaluation
import com.example.engine.EngineStyle
import com.example.engine.MoveExplainer
import com.example.engine.MoveQuality
import com.example.overlay.ChessOverlayService
import com.example.settings.AppSettings
import com.example.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReviewMoveItem(
    val moveNumber: Int,
    val move: ChessMove,
    val san: String,
    val evaluation: EngineEvaluation?,
    val quality: MoveQuality
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)
    private val chessEngine = ChessEngine()

    val settings: StateFlow<AppSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    val isOverlayRunning: StateFlow<Boolean> = ChessOverlayService.isRunningFlow
    val overlayEvaluations: StateFlow<List<EngineEvaluation>> = ChessOverlayService.latestEvalsFlow

    // Simulator State
    private val _simBoard = MutableStateFlow(FenParser.parse(FenParser.STARTING_FEN))
    val simBoard: StateFlow<BoardPosition> = _simBoard.asStateFlow()

    private val _simSelectedSquare = MutableStateFlow<Square?>(null)
    val simSelectedSquare: StateFlow<Square?> = _simSelectedSquare.asStateFlow()

    private val _simLegalMovesForSelected = MutableStateFlow<List<ChessMove>>(emptyList())
    val simLegalMovesForSelected: StateFlow<List<ChessMove>> = _simLegalMovesForSelected.asStateFlow()

    private val _simEvaluations = MutableStateFlow<List<EngineEvaluation>>(emptyList())
    val simEvaluations: StateFlow<List<EngineEvaluation>> = _simEvaluations.asStateFlow()

    private val _simIsThinking = MutableStateFlow(false)
    val simIsThinking: StateFlow<Boolean> = _simIsThinking.asStateFlow()

    private val _simExplanation = MutableStateFlow<String?>(null)
    val simExplanation: StateFlow<String?> = _simExplanation.asStateFlow()

    private val _simMoveHistory = MutableStateFlow<List<ReviewMoveItem>>(emptyList())
    val simMoveHistory: StateFlow<List<ReviewMoveItem>> = _simMoveHistory.asStateFlow()

    private var simAnalysisJob: Job? = null

    init {
        runSimAnalysis()
    }

    // Thermal Status
    fun getThermalStatus(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = getApplication<Application>().getSystemService(Context.POWER_SERVICE) as? PowerManager
            return when (pm?.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "Норма (холодный)"
                PowerManager.THERMAL_STATUS_LIGHT -> "Легкий нагрев"
                PowerManager.THERMAL_STATUS_MODERATE -> "Умеренный нагрев"
                PowerManager.THERMAL_STATUS_SEVERE -> "Сильный нагрев (снижение глубины)"
                PowerManager.THERMAL_STATUS_CRITICAL -> "Критический нагрев"
                else -> "Норма"
            }
        }
        return "В норме"
    }

    // Settings actions
    fun setEngineDepth(depth: Int) = viewModelScope.launch {
        repository.updateEngineDepth(depth)
        runSimAnalysis()
    }

    fun setMultiPv(count: Int) = viewModelScope.launch {
        repository.updateMultiPv(count)
        runSimAnalysis()
    }

    fun setStyle(style: EngineStyle) = viewModelScope.launch {
        repository.updateStyle(style)
        runSimAnalysis()
    }

    fun setThinkFirstSeconds(seconds: Int) = viewModelScope.launch {
        repository.updateThinkFirstSeconds(seconds)
    }

    fun setPowerSaving(enabled: Boolean) = viewModelScope.launch {
        repository.updatePowerSaving(enabled)
    }

    fun setOnboardingCompleted() = viewModelScope.launch {
        repository.setOnboardingCompleted(true)
    }

    // Simulator Actions
    fun onSquareClick(square: Square) {
        val selected = _simSelectedSquare.value
        val board = _simBoard.value

        if (selected == null) {
            val piece = board.getPiece(square)
            if (piece != null && piece.color == board.activeColor) {
                _simSelectedSquare.value = square
                val legals = MoveValidator.generateLegalMoves(board, board.activeColor)
                    .filter { it.from == square }
                _simLegalMovesForSelected.value = legals
            }
        } else {
            // Check if tapped square is a valid target move
            val matchingMove = _simLegalMovesForSelected.value.firstOrNull { it.to == square }
            if (matchingMove != null) {
                makeSimMove(matchingMove)
            } else {
                val piece = board.getPiece(square)
                if (piece != null && piece.color == board.activeColor) {
                    _simSelectedSquare.value = square
                    val legals = MoveValidator.generateLegalMoves(board, board.activeColor)
                        .filter { it.from == square }
                    _simLegalMovesForSelected.value = legals
                } else {
                    _simSelectedSquare.value = null
                    _simLegalMovesForSelected.value = emptyList()
                }
            }
        }
    }

    private fun makeSimMove(move: ChessMove) {
        val oldBoard = _simBoard.value
        val topEval = _simEvaluations.value.firstOrNull()
        val nextBoard = oldBoard.makeMove(move)

        // Classify move quality
        val cpLoss = if (topEval != null && topEval.move.from == move.from && topEval.move.to == move.to) {
            0
        } else {
            35 // estimate
        }
        val quality = MoveQuality.fromCentipawnLoss(cpLoss)

        val historyItem = ReviewMoveItem(
            moveNumber = oldBoard.fullmoveNumber,
            move = move,
            san = move.toSan(oldBoard),
            evaluation = topEval,
            quality = quality
        )

        _simMoveHistory.value = _simMoveHistory.value + historyItem
        _simBoard.value = nextBoard
        _simSelectedSquare.value = null
        _simLegalMovesForSelected.value = emptyList()
        _simExplanation.value = null

        runSimAnalysis()
    }

    fun resetSimBoard() {
        _simBoard.value = FenParser.parse(FenParser.STARTING_FEN)
        _simSelectedSquare.value = null
        _simLegalMovesForSelected.value = emptyList()
        _simExplanation.value = null
        _simMoveHistory.value = emptyList()
        runSimAnalysis()
    }

    fun setSimFen(fen: String) {
        runCatching {
            val parsed = FenParser.parse(fen)
            _simBoard.value = parsed
            _simSelectedSquare.value = null
            _simLegalMovesForSelected.value = emptyList()
            _simExplanation.value = null
            runSimAnalysis()
        }
    }

    fun requestExplanation() {
        val board = _simBoard.value
        val topEval = _simEvaluations.value.firstOrNull()
        if (topEval != null) {
            _simExplanation.value = MoveExplainer.explainMove(board, topEval.move, topEval)
        }
    }

    private fun runSimAnalysis() {
        simAnalysisJob?.cancel()
        simAnalysisJob = viewModelScope.launch {
            _simIsThinking.value = true
            val board = _simBoard.value
            val currentSettings = settings.value

            val evals = withContext(Dispatchers.Default) {
                chessEngine.analyze(
                    board = board,
                    depth = currentSettings.engineDepth,
                    multiPv = currentSettings.multiPv,
                    style = currentSettings.style
                )
            }
            _simEvaluations.value = evals
            _simIsThinking.value = false
        }
    }
}
