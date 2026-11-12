package com.artgg.mousepro.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.input.InputManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.*
import androidx.core.app.NotificationCompat
import com.artgg.mousepro.ArtggApp
import com.artgg.mousepro.R
import com.artgg.mousepro.data.model.ComponentType
import com.artgg.mousepro.data.model.GameProfile
import com.artgg.mousepro.data.model.MappingComponent
import com.artgg.mousepro.ui.MainActivity
import kotlinx.coroutines.*

/**
 * Foreground service that captures keyboard/mouse input events and converts
 * them to touch gestures via AccessibilityService.
 *
 * INPUT CAPTURE LIMITATION (Android non-root):
 * Android does NOT allow background services to globally intercept KeyEvent/MotionEvent
 * from external keyboards/mice. The only reliable non-root approaches are:
 *   1. AccessibilityService with flagRequestFilterKeyEvents — captures key events only
 *      when the service is enabled AND the user grants permission. Mouse motion is NOT captured.
 *   2. ADB injection via "input" commands — requires prior ADB activation.
 *   3. Shizuku API — requires Shizuku app + prior activation.
 *
 * This service uses approach #1 for key capture and delegates gesture injection
 * to ArtggAccessibilityService. For full mouse tracking, the overlay window
 * receives MotionEvents directly when it has focus.
 */
class InputCaptureService : Service(), InputManager.InputDeviceListener {

    companion object {
        private const val TAG = "InputCapture"
        private const val NOTIFICATION_ID = 1001

        @Volatile
        var isRunning: Boolean = false
            private set

        @Volatile
        var activeProfile: GameProfile? = null
            private set

        fun setActiveProfile(profile: GameProfile?) {
            activeProfile = profile
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var overlayWindow: View? = null
    private var windowManager: WindowManager? = null
    private var inputManager: InputManager? = null

    // Screen dimensions for coordinate mapping
    private var screenWidth = 1920f
    private var screenHeight = 1080f

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        inputManager = getSystemService(INPUT_SERVICE) as InputManager

        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            windowManager?.defaultDisplay
        }
        display?.let {
            val metrics = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            it.getRealMetrics(metrics)
            screenWidth = metrics.widthPixels.toFloat()
            screenHeight = metrics.heightPixels.toFloat()
        }

        inputManager?.registerInputDeviceListener(this, null)
        createOverlay()
        Log.i(TAG, "Service created. Screen: ${screenWidth}x${screenHeight}")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        removeOverlay()
        inputManager?.unregisterInputDeviceListener(this)
        serviceScope.cancel()
        isRunning = false
        Log.i(TAG, "Service destroyed")
        super.onDestroy()
    }

    // ─── Overlay Window ──────────────────────────────────────────────

    private fun createOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        overlayWindow = View(this).apply {
            // Transparent overlay — touch events pass through to the game below
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        try {
            windowManager?.addView(overlayWindow, params)
            Log.i(TAG, "Overlay window added")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay window", e)
        }
    }

    private fun removeOverlay() {
        overlayWindow?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        overlayWindow = null
    }

    // ─── Input Device Detection ──────────────────────────────────────

    override fun onInputDeviceAdded(deviceId: Int) {
        val device = InputDevice.getDevice(deviceId) ?: return
        val sources = device.sources
        val isKeyboard = sources and InputDevice.SOURCE_KEYBOARD == InputDevice.SOURCE_KEYBOARD
        val isMouse = sources and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE
        if (isKeyboard || isMouse) {
            Log.i(TAG, "Input device connected: ${device.name} (keyboard=$isKeyboard, mouse=$isMouse)")
        }
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        Log.i(TAG, "Input device removed: $deviceId")
    }

    override fun onInputDeviceChanged(deviceId: Int) {
        // No-op
    }

    // ─── Key Event Processing ────────────────────────────────────────

    /**
     * Called by ArtggAccessibilityService or overlay when a key event is received.
     * Maps the key code to the active profile's components and dispatches gestures.
     */
    fun processKeyEvent(keyCode: Int, action: Int): Boolean {
        val profile = activeProfile ?: return false
        val a11y = ArtggAccessibilityService.instance ?: return false

        val matchingComponents = profile.components.filter { comp ->
            comp.keyCodes.contains(keyCode)
        }

        if (matchingComponents.isEmpty()) return false

        for (component in matchingComponents) {
            when (component.type) {
                ComponentType.TOUCH -> {
                    if (action == KeyEvent.ACTION_DOWN) {
                        val x = component.x * screenWidth
                        val y = component.y * screenHeight
                        a11y.dispatchTap(x, y)
                    }
                }
                ComponentType.HOLD -> {
                    if (action == KeyEvent.ACTION_DOWN) {
                        val x = component.x * screenWidth
                        val y = component.y * screenHeight
                        val duration = component.config.holdDurationMs.coerceAtLeast(100)
                        a11y.dispatchHold(x, y, duration)
                    }
                }
                ComponentType.SWIPE -> {
                    if (action == KeyEvent.ACTION_DOWN) {
                        val startX = component.x * screenWidth
                        val startY = component.y * screenHeight
                        val endX = startX + component.config.swipeDirectionX * component.config.swipeDistancePx
                        val endY = startY + component.config.swipeDirectionY * component.config.swipeDistancePx
                        a11y.dispatchSwipe(startX, startY, endX, endY, component.config.swipeDurationMs)
                    }
                }
                ComponentType.JOYSTICK -> {
                    // Joystick requires continuous axis input — handled separately
                    // For key-based joystick, we simulate directional swipes
                    if (action == KeyEvent.ACTION_DOWN) {
                        val centerX = component.x * screenWidth
                        val centerY = component.y * screenHeight
                        val radius = component.width * screenWidth * 0.5f
                        // Map WASD-like keys to directions based on key position in list
                        val directionIndex = component.keyCodes.indexOf(keyCode)
                        val angle = when (directionIndex % 4) {
                            0 -> -Math.PI / 2  // Up
                            1 -> Math.PI / 2   // Down
                            2 -> Math.PI       // Left
                            else -> 0.0        // Right
                        }
                        val endX = centerX + (Math.cos(angle) * radius).toFloat()
                        val endY = centerY + (Math.sin(angle) * radius).toFloat()
                        a11y.dispatchSwipe(centerX, centerY, endX, endY, 100)
                    }
                }
                else -> {
                    // FPS_VIEW, FREE_LOOK, MACRO etc. handled in Phase 2+
                }
            }
        }
        return true
    }

    // ─── Notification ────────────────────────────────────────────────

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ArtggApp.CHANNEL_MAPPING_SERVICE)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Mapeamento ativo")
            .setSmallIcon(android.R.drawable.ic_media_play) // Placeholder icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}