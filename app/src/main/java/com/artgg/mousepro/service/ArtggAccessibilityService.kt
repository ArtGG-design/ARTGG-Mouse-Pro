package com.artgg.mousepro.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * AccessibilityService used to inject touch gestures on screen.
 * This is the primary non-root method for simulating taps, swipes and holds.
 *
 * IMPORTANT: Android restricts gesture injection to the accessibility framework.
 * This service MUST be enabled by the user in Settings > Accessibility.
 * It does NOT collect any personal data — it only dispatches gestures.
 */
class ArtggAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ArtggA11y"

        @Volatile
        var instance: ArtggAccessibilityService? = null
            private set

        /** Returns true if the service is running and ready to inject gestures. */
        fun isReady(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onDestroy() {
        instance = null
        Log.i(TAG, "Accessibility service destroyed")
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used — we only need gesture injection capability
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    // ─── Gesture Injection API ────────────────────────────────────────

    /**
     * Dispatch a single tap at the given screen coordinates.
     * @param x Screen X coordinate (pixels)
     * @param y Screen Y coordinate (pixels)
     * @param durationMs Tap duration in ms (default 50ms = quick tap)
     */
    fun dispatchTap(x: Float, y: Float, durationMs: Long = 50L): Boolean {
        if (!isReady()) return false
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return try {
            dispatchGesture(gesture, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "dispatchTap failed", e)
            false
        }
    }

    /**
     * Dispatch a press-and-hold gesture.
     */
    fun dispatchHold(x: Float, y: Float, durationMs: Long): Boolean {
        if (!isReady()) return false
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(100))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return try {
            dispatchGesture(gesture, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "dispatchHold failed", e)
            false
        }
    }

    /**
     * Dispatch a swipe gesture from (startX,startY) to (endX,endY).
     * @param durationMs Total swipe duration
     */
    fun dispatchSwipe(
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        durationMs: Long = 300L
    ): Boolean {
        if (!isReady()) return false
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(50))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return try {
            dispatchGesture(gesture, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "dispatchSwipe failed", e)
            false
        }
    }

    /**
     * Dispatch multiple simultaneous strokes (e.g., multi-touch).
     * Each entry is (path, startTimeMs, durationMs).
     */
    fun dispatchMultiStroke(
        strokes: List<Triple<Path, Long, Long>>
    ): Boolean {
        if (!isReady() || strokes.isEmpty()) return false
        val builder = GestureDescription.Builder()
        for ((path, start, duration) in strokes) {
            builder.addStroke(GestureDescription.StrokeDescription(path, start, duration))
        }
        return try {
            dispatchGesture(builder.build(), null, null)
            true
        } catch (e: Exception) {
            Log.e(TAG, "dispatchMultiStroke failed", e)
            false
        }
    }

    /**
     * Perform a global action (back, home, recents, etc.).
     */
    fun performGlobalActionCompat(action: Int): Boolean {
        if (!isReady()) return false
        return try {
            performGlobalAction(action)
        } catch (e: Exception) {
            Log.e(TAG, "performGlobalAction failed", e)
            false
        }
    }
}