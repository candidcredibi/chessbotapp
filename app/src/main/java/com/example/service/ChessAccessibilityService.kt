package com.example.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChessAccessibilityService : AccessibilityService() {

    companion object {
        const val CHESS_COM_PACKAGE = "com.chess"

        private val _isChessComForeground = MutableStateFlow(false)
        val isChessComForeground: StateFlow<Boolean> = _isChessComForeground.asStateFlow()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            if (pkg != null) {
                val isChess = pkg == CHESS_COM_PACKAGE || pkg.contains("chess", ignoreCase = true)
                _isChessComForeground.value = isChess
            }
        }
    }

    override fun onInterrupt() {
        _isChessComForeground.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        _isChessComForeground.value = false
    }
}
