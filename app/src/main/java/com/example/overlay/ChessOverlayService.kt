package com.example.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.core.model.BoardPosition
import com.example.core.model.ChessMove
import com.example.core.model.FenParser
import com.example.core.model.Square
import com.example.engine.ChessEngine
import com.example.engine.EngineEvaluation
import com.example.engine.EngineStyle
import com.example.engine.MoveExplainer
import com.example.vision.BoardRoi
import com.example.vision.PositionTracker
import com.example.vision.TrackerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChessOverlayService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var engineJob: Job? = null

    private lateinit var windowManager: WindowManager
    private var arrowView: OverlayArrowView? = null
    private var bubbleView: FloatingBubbleView? = null
    private var screenCaptureManager: ScreenCaptureManager? = null

    private val positionTracker = PositionTracker()
    private val chessEngine = ChessEngine()

    private var currentStyle = EngineStyle.SAFE
    private var engineDepth = 8
    private var multiPvCount = 3
    private var lastAnalyzedPosition: BoardPosition? = null
    private var latestEvaluations: List<EngineEvaluation> = emptyList()
    private var currentBoardRoi: BoardRoi? = null

    companion object {
        const val ACTION_START = "com.example.chesshelper.START"
        const val ACTION_STOP = "com.example.chesshelper.STOP"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "chess_helper_overlay"

        private val _isRunningFlow = MutableStateFlow(false)
        val isRunningFlow: StateFlow<Boolean> = _isRunningFlow.asStateFlow()

        private val _latestEvalsFlow = MutableStateFlow<List<EngineEvaluation>>(emptyList())
        val latestEvalsFlow: StateFlow<List<EngineEvaluation>> = _latestEvalsFlow.asStateFlow()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)

                val notification = buildNotification(
                    if (resultCode != 0) "Анализ экрана активен" else "Оверлей активен поверх окон"
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (resultCode != 0 && resultData != null) {
                        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                initOverlays()
                _isRunningFlow.value = true

                if (resultCode != 0 && resultData != null) {
                    initScreenCapture(resultCode, resultData)
                } else {
                    // Show initial sample arrows so user sees the overlay immediately working!
                    showInitialDemoArrows()
                }
            }
        }
        return START_STICKY
    }

    private fun showInitialDemoArrows() {
        serviceScope.launch {
            val startBoard = FenParser.parse(FenParser.STARTING_FEN)
            lastAnalyzedPosition = startBoard
            val evals = withContext(Dispatchers.Default) {
                chessEngine.analyze(startBoard, depth = 8, multiPv = 3, style = currentStyle)
            }
            latestEvaluations = evals
            _latestEvalsFlow.value = evals

            // Provide default screen region for arrows
            val metrics = resources.displayMetrics
            val boardW = (metrics.widthPixels * 0.96f).toInt()
            val left = (metrics.widthPixels - boardW) / 2
            val top = (metrics.heightPixels * 0.35f).toInt()
            val dummyRoi = BoardRoi(
                bounds = android.graphics.Rect(left, top, left + boardW, top + boardW)
            )
            currentBoardRoi = dummyRoi
            arrowView?.updateEvaluations(evals, dummyRoi)
        }
    }

    private fun initOverlays() {
        if (arrowView != null && bubbleView != null) return

        // 1. Fullscreen transparent pass-through canvas for arrows
        val arrowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        arrowView = OverlayArrowView(this)
        windowManager.addView(arrowView, arrowParams)

        // 2. Floating interactive bubble (strictly sized to 64x64 dp when collapsed so screen is NOT blocked)
        bubbleView = FloatingBubbleView(
            context = this,
            windowManager = windowManager,
            onPauseResume = { isPaused ->
                if (isPaused) {
                    positionTracker.pause()
                    arrowView?.clearArrows()
                } else {
                    positionTracker.resume()
                    reanalyzeCurrent()
                }
            },
            onStyleChanged = { newStyle ->
                currentStyle = newStyle
                reanalyzeCurrent()
            },
            onExplainClick = {
                val top = latestEvaluations.firstOrNull()
                val pos = lastAnalyzedPosition
                if (pos != null && top != null) {
                    val explanation = MoveExplainer.explainMove(pos, top.move, top)
                    bubbleView?.showExplanation(explanation)
                } else {
                    bubbleView?.showExplanation("Анализ позиции активен. Откройте доску для расчёта.")
                }
            },
            onCloseClick = {
                stopSelf()
            }
        )
        windowManager.addView(bubbleView!!.getView(), bubbleView!!.layoutParams)
    }

    private fun initScreenCapture(resultCode: Int, data: Intent) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mediaProjection = projectionManager.getMediaProjection(resultCode, data) ?: return

        screenCaptureManager = ScreenCaptureManager(
            context = this,
            mediaProjection = mediaProjection,
            onFrameCaptured = { bitmap ->
                val result = positionTracker.processFrame(bitmap)
                if (result.state == TrackerState.TRACKING && result.currentPosition != null) {
                    currentBoardRoi = result.boardRoi
                    if (lastAnalyzedPosition == null || lastAnalyzedPosition != result.currentPosition) {
                        lastAnalyzedPosition = result.currentPosition
                        triggerEngineAnalysis(result.currentPosition, result.boardRoi)
                    }
                }
            }
        )
        positionTracker.start()
        screenCaptureManager?.startCapture()
    }

    private fun triggerEngineAnalysis(position: BoardPosition, roi: BoardRoi?) {
        engineJob?.cancel()
        engineJob = serviceScope.launch {
            val evals = withContext(Dispatchers.Default) {
                chessEngine.analyze(
                    board = position,
                    depth = engineDepth,
                    multiPv = multiPvCount,
                    style = currentStyle
                )
            }
            latestEvaluations = evals
            _latestEvalsFlow.value = evals
            arrowView?.updateEvaluations(evals, roi)
        }
    }

    private fun reanalyzeCurrent() {
        val pos = lastAnalyzedPosition
        if (pos != null) {
            triggerEngineAnalysis(pos, currentBoardRoi)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, ChessOverlayService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, getString(R.string.notification_action_stop), stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunningFlow.value = false
        positionTracker.stop()
        screenCaptureManager?.stopCapture()
        screenCaptureManager = null

        arrowView?.let { windowManager.removeView(it) }
        arrowView = null

        bubbleView?.let {
            it.destroy()
            windowManager.removeView(it.getView())
        }
        bubbleView = null

        serviceScope.cancel()
    }
}
