package com.example.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.engine.EngineStyle
import com.example.ui.theme.ChessGreenPrimary
import com.example.ui.theme.ChessHelperTheme
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TacticalAmber
import kotlin.math.abs

class FloatingBubbleView(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onPauseResume: (Boolean) -> Unit,
    private val onStyleChanged: (EngineStyle) -> Unit,
    private val onExplainClick: () -> Unit,
    private val onCloseClick: () -> Unit
) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val density = context.resources.displayMetrics.density
    private val bubbleSizePx = (64 * density).toInt()
    private val drawerWidthPx = (290 * density).toInt()

    private val composeView: ComposeView = ComposeView(context)
    val layoutParams = WindowManager.LayoutParams(
        bubbleSizePx,
        bubbleSizePx,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        },
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 40
        y = 260
    }

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    var isPausedState by mutableStateOf(false)
    var isExpanded by mutableStateOf(false)
    var explanationText by mutableStateOf<String?>(null)
    var currentMoveInfo by mutableStateOf("Готов к ходам")

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)

        composeView.setContent {
            ChessHelperTheme {
                FloatingControlContent()
            }
        }

        setupDragListener()
    }

    fun getView(): View = composeView

    fun showExplanation(text: String) {
        explanationText = text
        setExpandedState(true)
    }

    private fun setExpandedState(expanded: Boolean) {
        isExpanded = expanded
        if (expanded) {
            layoutParams.width = drawerWidthPx
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        } else {
            layoutParams.width = bubbleSizePx
            layoutParams.height = bubbleSizePx
        }
        windowManager.updateViewLayout(composeView, layoutParams)
    }

    private fun setupDragListener() {
        composeView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true // Return true to receive subsequent moves and up
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 12 || abs(dy) > 12) {
                        isDragging = true
                        layoutParams.x = initialX + dx
                        layoutParams.y = initialY + dy
                        windowManager.updateViewLayout(composeView, layoutParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Toggle drawer expand
                        setExpandedState(!isExpanded)
                    } else {
                        // Snap smoothly to closest screen edge
                        val screenWidth = context.resources.displayMetrics.widthPixels
                        layoutParams.x = if (layoutParams.x + bubbleSizePx / 2 > screenWidth / 2) {
                            screenWidth - (if (isExpanded) drawerWidthPx else bubbleSizePx) - 10
                        } else {
                            10
                        }
                        windowManager.updateViewLayout(composeView, layoutParams)
                    }
                    true
                }
                else -> false
            }
        }
    }

    @Composable
    private fun FloatingControlContent() {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(4.dp)
        ) {
            // Floating Bubble Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(DarkSurface)
            ) {
                Icon(
                    imageVector = if (isPausedState) Icons.Default.PlayArrow else Icons.Default.SportsEsports,
                    contentDescription = "Chess Helper Bubble",
                    tint = if (isPausedState) TacticalAmber else ChessGreenPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Expanded Mini Control Drawer
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.98f)),
                    modifier = Modifier
                        .width(276.dp)
                        .padding(top = 8.dp)
                        .shadow(16.dp, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isPausedState) TacticalAmber else ChessGreenPrimary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Chess Helper",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            }
                            IconButton(
                                onClick = { setExpandedState(false) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Свернуть", tint = Color.LightGray)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Pause / Resume Button
                        Button(
                            onClick = {
                                isPausedState = !isPausedState
                                onPauseResume(isPausedState)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPausedState) TacticalAmber else ChessGreenPrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (isPausedState) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPausedState) "Возобновить" else "Пауза стрелок")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Explain Move Button
                        Button(
                            onClick = onExplainClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkBackground,
                                contentColor = ChessGreenPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Объяснить ход")
                        }

                        // Explanation popup text
                        explanationText?.let { explanation ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DarkBackground,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = explanation,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Close service action
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Не блокирует экран",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Закрыть",
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onCloseClick() }
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    fun destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}
