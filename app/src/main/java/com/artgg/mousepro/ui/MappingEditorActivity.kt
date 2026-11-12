package com.artgg.mousepro.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.fontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.artgg.mousepro.data.model.ComponentType
import com.artgg.mousepro.data.model.GameProfile
import com.artgg.mousepro.data.model.MappingComponent
import com.artgg.mousepro.data.repository.ProfileRepository
import com.artgg.mousepro.ui.theme.ARTGGMouseProTheme
import com.artgg.mousepro.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Full-screen landscape mapping editor overlay.
 * Users drag components onto screen positions to define key-to-touch mappings.
 */
class MappingEditorActivity : ComponentActivity() {

    private lateinit var repository: ProfileRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = ProfileRepository(this)

        val profileId = intent.getStringExtra("profile_id")

        setContent {
            ARTGGMouseProTheme {
                MappingEditorScreen(
                    repository = repository,
                    profileId = profileId,
                    onExit = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MappingEditorScreen(
    repository: ProfileRepository,
    profileId: String?,
    onExit: () -> Unit
) {
    var profile by remember { mutableStateOf<GameProfile?>(null) }
    var components by remember { mutableStateOf<List<MappingComponent>>(emptyList()) }
    var selectedComponentId by remember { mutableStateOf<String?>(null) }
    var showAddMenu by remember { mutableStateOf(false) }

    // Load profile
    LaunchedEffect(profileId) {
        if (profileId != null) {
            val p = repository.getProfileById(profileId)
            if (p != null) {
                profile = p
                components = p.components.toMutableList()
            }
        }
    }

    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }

    Box(modifier = Modifier.fillMaxSize().background(ArtggBackground.copy(alpha = 0.85f))) {
        // Component layer — draggable items
        components.forEachIndexed { index, comp ->
            DraggableComponent(
                component = comp,
                isSelected = comp.id == selectedComponentId,
                onSelect = { selectedComponentId = comp.id },
                onPositionChanged = { newX, newY ->
                    components = components.toMutableList().apply {
                        this[index] = this[index].copy(x = newX, y = newY)
                    }
                },
                onDelete = {
                    components = components.filter { it.id != comp.id }
                    if (selectedComponentId == comp.id) selectedComponentId = null
                },
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx
            )
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onExit) {
                Icon(Icons.Default.Close, "Sair", tint = ArtggTextPrimary)
            }

            Text(
                text = profile?.name ?: "Editor",
                color = ArtggTextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { showAddMenu = !showAddMenu }) {
                    Icon(Icons.Default.Add, "Adicionar", tint = ArtggElectricBlue)
                }
                IconButton(onClick = {
                    val ctx = context as? ComponentActivity
                    val p = profile
                    if (ctx != null && p != null) {
                        val updated = p.copy(
                            components = components,
                            updatedAt = System.currentTimeMillis()
                        )
                        ctx.lifecycleScope.launch {
                            repository.saveProfile(updated)
                        }
                    }
                }) {
                    Icon(Icons.Default.Save, "Salvar", tint = ArtggSuccess)
                }
            }
        }

        // Add component menu dropdown
        if (showAddMenu) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 56.dp, end = 12.dp),
                colors = CardDefaults.cardColors(containerColor = ArtggSurface)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    ComponentType.entries.forEach { type ->
                        TextButton(onClick = {
                            components = components + MappingComponent(
                                type = type,
                                x = 0.5f,
                                y = 0.5f,
                                label = type.name
                            )
                            showAddMenu = false
                        }) {
                            Text(type.name, color = ArtggTextPrimary)
                        }
                    }
                }
            }
        }

        // Bottom info bar
        Text(
            text = "${components.size} componentes | Toque em + para adicionar",
            color = ArtggTextSecondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(12.dp)
        )
    }
}

@Composable
fun DraggableComponent(
    component: MappingComponent,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPositionChanged: (Float, Float) -> Unit,
    onDelete: () -> Unit,
    screenWidthPx: Float,
    screenHeightPx: Float
) {
    val sizePx = component.width * screenWidthPx
    val offsetX = component.x * screenWidthPx - sizePx / 2
    val offsetY = component.y * screenHeightPx - sizePx / 2

    Box(
        modifier = Modifier
            .offset(
                x = with(LocalDensity.current) { offsetX.toDp() },
                y = with(LocalDensity.current) { offsetY.toDp() }
            )
            .size(with(LocalDensity.current) { sizePx.toDp() })
            .clip(CircleShape)
            .background(
                when (component.type) {
                    ComponentType.TOUCH -> ArtggElectricBlue.copy(alpha = 0.6f)
                    ComponentType.HOLD -> ArtggNeonPurple.copy(alpha = 0.6f)
                    ComponentType.SWIPE -> Color(0xFF00BCD4).copy(alpha = 0.6f)
                    ComponentType.JOYSTICK -> ArtggWarning.copy(alpha = 0.6f)
                    ComponentType.FPS_VIEW -> ArtggError.copy(alpha = 0.6f)
                    else -> ArtggTextSecondary.copy(alpha = 0.4f)
                }
            )
            .then(
                if (isSelected) Modifier.border(2.dp, ArtggTextPrimary, CircleShape)
                else Modifier
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onSelect() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newX = ((component.x * screenWidthPx + dragAmount.x) / screenWidthPx)
                            .coerceIn(0f, 1f)
                        val newY = ((component.y * screenHeightPx + dragAmount.y) / screenHeightPx)
                            .coerceIn(0f, 1f)
                        onPositionChanged(newX, newY)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = component.label.take(3),
            color = ArtggTextPrimary,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

