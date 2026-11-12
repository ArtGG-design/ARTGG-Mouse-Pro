package com.artgg.mousepro.activation

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.artgg.mousepro.service.ArtggAccessibilityService
import com.artgg.mousepro.ui.theme.ARTGGMouseProTheme
import com.artgg.mousepro.ui.theme.*

/**
 * Guides the user through activation methods:
 * 1. Accessibility Service (primary, no root)
 * 2. ADB wireless/USB activation
 * 3. Shizuku (optional)
 */
class ActivationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ARTGGMouseProTheme {
                ActivationScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var isA11yEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isA11yEnabled = isAccessibilityServiceEnabled(context)
    }

    Scaffold(
        containerColor = ArtggBackground,
        topBar = {
            TopAppBar(
                title = { Text("Ativação", color = ArtggTextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar", tint = ArtggTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ArtggSurface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isA11yEnabled) ArtggSuccess.copy(alpha = 0.15f)
                    else ArtggWarning.copy(alpha = 0.15f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        if (isA11yEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isA11yEnabled) ArtggSuccess else ArtggWarning
                    )
                    Text(
                        text = if (isA11yEnabled) "Serviço de acessibilidade ativado"
                        else "Ativação necessária para funcionar",
                        color = if (isA11yEnabled) ArtggSuccess else ArtggWarning,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            // Method 1: Accessibility
            ActivationMethodCard(
                title = "Método 1: Acessibilidade (Recomendado)",
                description = "Funciona sem root. O Android solicitará permissão nas Configurações.",
                icon = Icons.Default.Accessibility,
                isActive = isA11yEnabled,
                actionLabel = if (isA11yEnabled) "Já ativado" else "Abrir Configurações",
                enabled = !isA11yEnabled,
                onClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                }
            )

            // Method 2: ADB
            ActivationMethodCard(
                title = "Método 2: ADB (Computador)",
                description = "Conecte via USB e execute:\nadb shell pm grant com.artgg.mousepro android.permission.WRITE_SECURE_SETTINGS",
                icon = Icons.Default.Computer,
                isActive = false,
                actionLabel = "Verificar Status",
                enabled = true,
                onClick = { /* TODO: Check ADB permission status */ }
            )

            // Method 3: Shizuku
            ActivationMethodCard(
                title = "Método 3: Shizuku (Opcional)",
                description = "Requer app Shizuku instalado e ativado. Oferece mais recursos avançados.",
                icon = Icons.Default.DeveloperMode,
                isActive = false,
                actionLabel = "Verificar Shizuku",
                enabled = true,
                onClick = { /* TODO: Check Shizuku availability */ }
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = "⚠️ O ARTGG Mouse Pro NÃO coleta dados pessoais. O serviço de acessibilidade é usado exclusivamente para simular toques na tela.",
                style = MaterialTheme.typography.bodySmall,
                color = ArtggTextSecondary
            )
        }
    }
}

@Composable
fun ActivationMethodCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    actionLabel: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = ArtggSurface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = ArtggElectricBlue)
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, color = ArtggTextPrimary)
            }
            Spacer(Modifier.height(8.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = ArtggTextSecondary)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onClick,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) ArtggSuccess else ArtggElectricBlue
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(actionLabel)
            }
        }
    }
}

private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val am = context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        ?: return false
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    val myServiceName = "${context.packageName}/${ArtggAccessibilityService::class.java.canonicalName}"
    return enabledServices.any { info ->
        info.resolveInfo?.serviceInfo?.let {
            "${it.packageName}/${it.name}" == myServiceName
        } == true
    }
}