package com.artgg.mousepro.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.artgg.mousepro.data.model.GameProfile
import com.artgg.mousepro.data.repository.ProfileRepository
import com.artgg.mousepro.service.ArtggAccessibilityService
import com.artgg.mousepro.service.InputCaptureService
import com.artgg.mousepro.ui.theme.ARTGGMouseProTheme
import com.artgg.mousepro.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: ProfileRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = ProfileRepository(this)

        setContent {
            ARTGGMouseProTheme {
                MainScreen(
                    repository = repository,
                    onEditProfile = { profile ->
                        startActivity(
                            Intent(this, MappingEditorActivity::class.java).apply {
                                putExtra("profile_id", profile.id)
                            }
                        )
                    },
                    onCreateProfile = {
                        lifecycleScope.launch {
                            val newProfile = GameProfile(name = "Novo Perfil")
                            repository.saveProfile(newProfile)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: ProfileRepository,
    onEditProfile: (GameProfile) -> Unit,
    onCreateProfile: () -> Unit
) {
    val context = LocalContext.current
    val profiles by repository.getAllProfiles().collectAsState(initial = emptyList())
    val isA11yReady = ArtggAccessibilityService.isReady()
    val isServiceRunning = InputCaptureService.isRunning

    Scaffold(
        containerColor = ArtggBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "ARTGG Mouse Pro",
                        fontWeight = FontWeight.Bold,
                        color = ArtggTextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ArtggSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateProfile,
                containerColor = ArtggElectricBlue,
                contentColor = ArtggTextPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Novo Perfil")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Status Card
            item {
                StatusCard(
                    isActivated = isA11yReady,
                    isServiceRunning = isServiceRunning,
                    onActivate = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                    onStartService = {
                        if (!isServiceRunning) {
                            val intent = Intent(context, InputCaptureService::class.java)
                            context.startForegroundService(intent)
                        }
                    }
                )
            }

            // Profiles Header
            item {
                Text(
                    "Perfis de Jogo",
                    style = MaterialTheme.typography.titleMedium,
                    color = ArtggTextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Profile List
            items(profiles, key = { it.id }) { profile ->
                ProfileCard(
                    profile = profile,
                    onEdit = { onEditProfile(profile) }
                )
            }

            if (profiles.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Nenhum perfil criado.\nToque em + para começar.",
                            color = ArtggTextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusCard(
    isActivated: Boolean,
    isServiceRunning: Boolean,
    onActivate: () -> Unit,
    onStartService: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ArtggSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        "Status do Sistema",
                        style = MaterialTheme.typography.titleSmall,
                        color = ArtggTextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val statusColor = when {
                            isActivated && isServiceRunning -> ArtggSuccess
                            isActivated -> ArtggWarning
                            else -> ArtggError
                        }
                        val statusText = when {
                            isActivated && isServiceRunning -> "Ativado e Rodando"
                            isActivated -> "Ativado — Serviço Parado"
                            else -> "Não Ativado"
                        }
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = statusColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = statusText,
                                color = statusColor,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isActivated) {
                    Button(
                        onClick = onActivate,
                        colors = ButtonDefaults.buttonColors(containerColor = ArtggNeonPurple)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ativar Acessibilidade")
                    }
                }
                if (isActivated && !isServiceRunning) {
                    Button(
                        onClick = onStartService,
                        colors = ButtonDefaults.buttonColors(containerColor = ArtggElectricBlue)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Iniciar Serviço")
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileCard(
    profile: GameProfile,
    onEdit: () -> Unit
) {
    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ArtggSurfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = ArtggTextPrimary
                )
                if (!profile.packageName.isNullOrEmpty()) {
                    Text(
                        text = profile.packageName!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = ArtggTextSecondary
                    )
                }
                Text(
                    text = "${profile.components.size} componentes",
                    style = MaterialTheme.typography.labelSmall,
                    color = ArtggElectricBlue
                )
            }
            Icon(
                Icons.Default.Edit,
                contentDescription = "Editar",
                tint = ArtggTextSecondary
            )
        }
    }
}