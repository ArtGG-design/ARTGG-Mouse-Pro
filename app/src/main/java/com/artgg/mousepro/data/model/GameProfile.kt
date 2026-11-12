package com.artgg.mousepro.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A saved mapping profile linked to a specific game package.
 */
@Serializable
@Entity(tableName = "game_profiles")
data class GameProfile(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val packageName: String? = null,
    val components: List<MappingComponent> = emptyList(),
    val globalSensitivityX: Float = 1.0f,
    val globalSensitivityY: Float = 1.0f,
    val cursorSpeed: Float = 1.0f,
    val fpsModeEnabled: Boolean = true,
    val fpsToggleKeyCode: Int = 0, // 0 = right-click default
    val typingToggleKeyCode: Int = android.view.KeyEvent.KEYCODE_ENTER,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)