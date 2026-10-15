package com.example.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.view.WindowManager

class ScreenCaptureManager(
    private val context: Context,
    private val mediaProjection: MediaProjection,
    private val onFrameCaptured: (Bitmap) -> Unit
) {
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var handlerThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var reusableBitmap: Bitmap? = null
    private var isCapturing = false
    private var lastCaptureTime = 0L
    private var minFrameIntervalMs = 400L // ~2.5 FPS default adaptive rate

    fun setAdaptiveFps(isMovingDetected: Boolean) {
        minFrameIntervalMs = if (isMovingDetected) 180L else 500L
    }

    fun startCapture() {
        if (isCapturing) return
        isCapturing = true

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        // Capture scaled down to max width 512 for high performance and low battery consumption
        val targetWidth = 512
        val targetHeight = (512f * (metrics.heightPixels.toFloat() / metrics.widthPixels.toFloat())).toInt()
        val density = metrics.densityDpi

        handlerThread = HandlerThread("ScreenCaptureThread").apply { start() }
        backgroundHandler = Handler(handlerThread!!.looper)

        imageReader = ImageReader.newInstance(targetWidth, targetHeight, PixelFormat.RGBA_8888, 2)
        imageReader?.setOnImageAvailableListener({ reader ->
            val now = System.currentTimeMillis()
            if (now - lastCaptureTime < minFrameIntervalMs) {
                // Drop frame to preserve battery and respect adaptive rate
                val img = reader.acquireLatestImage()
                img?.close()
                return@setOnImageAvailableListener
            }
            lastCaptureTime = now

            var image: Image? = null
            try {
                image = reader.acquireLatestImage()
                if (image != null) {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * targetWidth

                    val bitmapWidth = targetWidth + rowPadding / pixelStride
                    if (reusableBitmap == null || reusableBitmap?.width != bitmapWidth || reusableBitmap?.height != targetHeight) {
                        reusableBitmap = Bitmap.createBitmap(bitmapWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    }

                    reusableBitmap?.copyPixelsFromBuffer(buffer)

                    // Crop to exact width if padding exists
                    val finalBitmap = if (bitmapWidth != targetWidth) {
                        Bitmap.createBitmap(reusableBitmap!!, 0, 0, targetWidth, targetHeight)
                    } else {
                        reusableBitmap!!
                    }

                    onFrameCaptured(finalBitmap)
                }
            } catch (e: Exception) {
                // Handle transient surface lock exceptions safely
            } finally {
                image?.close()
            }
        }, backgroundHandler)

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ChessHelperProjection",
            targetWidth,
            targetHeight,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            backgroundHandler
        )
    }

    fun stopCapture() {
        isCapturing = false
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        handlerThread?.quitSafely()
        handlerThread = null
        backgroundHandler = null
        reusableBitmap?.recycle()
        reusableBitmap = null
    }
}
