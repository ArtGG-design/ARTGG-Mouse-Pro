package com.artgg.mousepro.data.model

import kotlinx.serialization.Serializable

/**
 * Represents a single mappable control element placed on the overlay.
 * Each component maps one or more physical inputs to a touch/gesture action.
 */
@Serializable
data class MappingComponent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: ComponentType,
    /** X position as fraction of screen width (0.0 – 1.0) */
    val x: Float,
    /** Y position as fraction of screen height (0.0 – 1.0) */
    val y: Float,
    /** Width as fraction of screen width (for joystick, swipe zones) */
    val width: Float = 0.08f,
    /** Height as fraction of screen height */
    val height: Float = 0.08f,
    /** Label shown in the editor */
    val label: String = "",
    /** Bound key codes (KeyEvent.KEYCODE_*) */
    val keyCodes: List<Int> = emptyList(),
    /** Bound mouse button (0=left, 1=right, 2=middle, 3+=extra) */
    val mouseButton: Int? = null,
    /** Component-specific configuration */
    val config: ComponentConfig = ComponentConfig()
)

@Serializable
enum class ComponentType {
    TOUCH,          // Single tap at position
    HOLD,           // Press-and-hold at position
    SWIPE,          // Swipe gesture from position
    JOYSTICK,       // Virtual analog stick
    FPS_VIEW,       // Mouse-driven camera look
    FREE_LOOK,      // Free camera movement
    MOUSE_SCROLL,   // Scroll wheel action
    MOUSE_PICK,     // Click-to-interact
    MACRO           // Sequence of actions
}

@Serializable
data class ComponentConfig(
    // Touch / Hold
    val holdDurationMs: Long = 0,

    // Swipe
    val swipeDirectionX: Float = 0f,
    val swipeDirectionY: Float = -1f,
    val swipeDistancePx: Int = 200,
    val swipeDurationMs: Long = 300,

    // Joystick
    val joystickDeadzone: Float = 0.15f,
    val joystickInvertY: Boolean = false,

    // FPS View / Free Look
    val sensitivityX: Float = 1.0f,
    val sensitivityY: Float = 1.0f,
    val smoothing: Float = 0f,
    val invertX: Boolean = false,
    val invertY: Boolean = false,

    // Macro
    val macroActions: List<MacroAction> = emptyList(),
    val macroTrigger: MacroTrigger = MacroTrigger.KEY_DOWN,
    val macroLoopWhileHeld: Boolean = false
)

@Serializable
data class MacroAction(
    val type: MacroActionType,
    val x: Float = 0f,
    val y: Float = 0f,
    val durationMs: Long = 0,
    val delayAfterMs: Long = 0
)

@Serializable
enum class MacroActionType {
    TAP, PRESS_RELEASE, SWIPE, DELAY
}

@Serializable
enum class MacroTrigger {
    KEY_DOWN, KEY_UP, DOWN_UP, LOOP_WHILE_HELD
}