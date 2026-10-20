package com.feryaeljustice.docmask.presentation.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.feryaeljustice.docmask.core.model.AppPermissionStatus
import com.feryaeljustice.docmask.core.model.AppPermissionType
import com.feryaeljustice.docmask.core.model.PermissionItemState
import com.feryaeljustice.docmask.core.util.BiometricHelper
import com.feryaeljustice.docmask.core.util.CacheManager
import com.feryaeljustice.docmask.core.util.PermissionHelper
import com.feryaeljustice.docmask.presentation.editor.EditorUiEvent
import com.feryaeljustice.docmask.presentation.editor.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: EditorViewModel,
    onNavigateToPro: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showInfoDialog by remember { mutableStateOf(false) }
    var selectedPermissionForRationale by remember { mutableStateOf<PermissionItemState?>(null) }
    var storageStats by remember { mutableStateOf(CacheManager.getTemporaryStorageStats(context)) }

    // Listen to ON_RESUME lifecycle event to refresh system permissions in real-time
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onEvent(EditorUiEvent.RefreshPermissions(context))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onEvent(EditorUiEvent.RefreshPermissions(context))
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onEvent(EditorUiEvent.RefreshPermissions(context))
        if (isGranted) {
            Toast.makeText(context, "Permiso de cámara concedido.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permiso de cámara no concedido.", Toast.LENGTH_SHORT).show()
        }
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        viewModel.onEvent(EditorUiEvent.RefreshPermissions(context))
        val granted = permissionsMap.values.any { it }
        if (granted) {
            Toast.makeText(context, "Permiso multimedia concedido.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permiso multimedia no concedido.", Toast.LENGTH_SHORT).show()
        }
    }

    val onPermissionItemClick: (PermissionItemState) -> Unit = { item ->
        if (item.isSafAccess) {
            selectedPermissionForRationale = item
        } else if (item.status == AppPermissionStatus.GRANTED) {
            context.startActivity(PermissionHelper.createAppSettingsIntent(context))
        } else {
            selectedPermissionForRationale = item
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)

                val docFile = DocumentFile.fromTreeUri(context, treeUri)
                if (docFile != null && docFile.canWrite()) {
                    viewModel.updateCustomStorageUri(treeUri.toString())
                    Toast.makeText(context, "Carpeta de guardado configurada correctamente.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No se otorgaron permisos de escritura. Por favor selecciona otra carpeta.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al configurar permisos de carpeta: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes y Seguridad") },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Información de la aplicación"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Zero-Network Privacy Certificate Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Garantía Zero-Network (100% Offline)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "DocMask no tiene el permiso INTERNET en su AndroidManifest.xml. El propio sistema operativo Android impide cualquier transmisión de datos. Tus documentos nunca salen de tu dispositivo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Permissions and System Privacy Card (Audited in real-time)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Permisos y Privacidad",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Estado verificado en tiempo real a nivel de sistema Android. DocMask procesa todos tus documentos 100% en local y sin conexión.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.permissions.isEmpty()) {
                        Text(
                            text = "Auditoría de permisos en curso...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.permissions.forEach { perm ->
                                PermissionRowItem(
                                    item = perm,
                                    onClick = { onPermissionItemClick(perm) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Toca cualquier permiso para solicitarlo o abrir los Ajustes de Android para gestionarlo.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            // DocMask Pro Navigation Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToPro() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "DocMask Pro",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (uiState.isProUser) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "ACTIVO",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Gestiona tu suscripción Pro y las ventajas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Abrir DocMask Pro",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Biometric Protection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Bloqueo Biométrico",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Solicitar huella dactilar, rostro o PIN al abrir DocMask.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isBiometricEnabled,
                        onCheckedChange = { isEnabled ->
                            if (isEnabled) {
                                if (!uiState.isProUser) {
                                    Toast.makeText(
                                        context,
                                        "El bloqueo biométrico es exclusivo de DocMask Pro",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onNavigateToPro()
                                    return@Switch
                                }
                                val canAuth = BiometricHelper.isBiometricAvailable(context)
                                if (!canAuth) {
                                    Toast.makeText(
                                        context,
                                        "No hay huella, rostro ni PIN configurado en este dispositivo",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@Switch
                                }
                            }
                            viewModel.onEvent(EditorUiEvent.ToggleBiometric(isEnabled))
                            Toast.makeText(
                                context,
                                if (isEnabled) "Bloqueo biométrico activado" else "Bloqueo biométrico desactivado",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }

            // AEPD & Police Advice Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recomendaciones Oficiales AEPD",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. No entregues el DNI sin tachar el número de soporte y código CAN.\n2. La marca de agua debe especificar el receptor y la fecha.\n3. Convierte siempre a escala de grises para evitar su uso en altas de telefonía o créditos fraudulentos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Custom Storage Location Card (SAF)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Carpeta de Guardado (Almacenamiento)",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val currentFolderText = if (uiState.customStorageUri != null) {
                        try {
                            val uri = Uri.parse(uiState.customStorageUri)
                            val doc = DocumentFile.fromTreeUri(context, uri)
                            "Carpeta actual: ${doc?.name ?: uri.lastPathSegment ?: "Personalizada"}"
                        } catch (e: Exception) {
                            "Carpeta personalizada configurada"
                        }
                    } else {
                        "Carpeta actual: Almacenamiento privado de la app (Predeterminado)"
                    }
                    Text(
                        text = currentFolderText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Indica la carpeta de tu dispositivo donde se descargarán y guardarán tus fotos y documentos protegidos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { folderPickerLauncher.launch(null) },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cambiar Carpeta")
                        }
                        if (uiState.customStorageUri != null) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.updateCustomStorageUri(null)
                                    Toast.makeText(context, "Se restableció a la carpeta predeterminada.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restablecer")
                            }
                        }
                    }
                }
            }

            // Temporary Storage & Cache Cleaner Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Almacenamiento Temporal y Caché",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tienes tus generaciones pasadas temporalmente en caché en la carpeta de la aplicación para poder compartirlas. Puedes vaciarlas en cualquier momento para liberar espacio, ya que tus documentos guardados se conservan permanentemente en tu carpeta de destino configurada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Espacio ocupado en caché: ${storageStats.first} archivo(s) (${CacheManager.formatBytes(storageStats.second)})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            val (deletedCount, freedBytes) = CacheManager.clearTemporaryStorage(context)
                            storageStats = CacheManager.getTemporaryStorageStats(context)
                            val message = if (deletedCount > 0) {
                                "Se eliminaron $deletedCount archivos de caché (${CacheManager.formatBytes(freedBytes)} liberados). Tus archivos en la carpeta de destino permanecen intactos."
                            } else {
                                "No hay documentos temporales para eliminar."
                            }
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 44.dp)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vaciar Caché Temporal")
                    }
                }
            }
        }
    }

    // Centered Floating Modal Dialog with App Info
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "DocMask",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Versión 1.0 (Oficial)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Made with ❤️ by Feryael Justice",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "DocMask es una utilidad de privacidad radical diseñada para proteger documentos de identidad españoles y europeos (DNI, Pasaporte, NIE, carnet de conducir) al enviarlos a terceros.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Privacidad 100% Offline: La aplicación no solicita ni posee el permiso INTERNET en el sistema operativo. Tus fotos e información confidencial jamás abandonan tu teléfono.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• IA Local Empaquetada: Detección inteligente en el dispositivo de número de soporte, código CAN, firma y rostro mediante Google ML Kit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Protección Antifraude: Pixelado interactivo con un toque, conversión a escala de grises y superposición de marcas de agua disuasorias con fecha y propósito específico.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showInfoDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }

    // Permission Details & Rationale Dialog
    if (selectedPermissionForRationale != null) {
        val perm = selectedPermissionForRationale!!
        val isGranted = perm.status == AppPermissionStatus.GRANTED
        AlertDialog(
            onDismissRequest = { selectedPermissionForRationale = null },
            icon = {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Security,
                    contentDescription = null,
                    tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = perm.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = perm.purposeRationale,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (perm.isSafAccess) {
                                "Estado: Acceso Seguro nativo garantizado mediante el Selector del Sistema (SAF). No requiere permisos intrusivos."
                            } else if (isGranted) {
                                "Estado actual: Concedido en Android. Toca abajo para abrir los Ajustes del sistema si deseas revocarlo o inspeccionarlo."
                            } else {
                                "Estado actual: No concedido. Puedes solicitarlo de inmediato o abrir la configuración de la app."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                if (perm.isSafAccess) {
                    Button(onClick = { selectedPermissionForRationale = null }) {
                        Text("Entendido")
                    }
                } else if (!isGranted) {
                    Button(
                        onClick = {
                            val target = perm
                            selectedPermissionForRationale = null
                            if (target.type == AppPermissionType.CAMERA) {
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            } else if (target.type == AppPermissionType.GALLERY_MEDIA) {
                                val perms = PermissionHelper.getRequiredPermissions(AppPermissionType.GALLERY_MEDIA)
                                mediaPermissionLauncher.launch(perms.toTypedArray())
                            }
                        }
                    ) {
                        Text("Solicitar Permiso")
                    }
                } else {
                    Button(
                        onClick = {
                            selectedPermissionForRationale = null
                            context.startActivity(PermissionHelper.createAppSettingsIntent(context))
                        }
                    ) {
                        Text("Ajustes de Android")
                    }
                }
            },
            dismissButton = {
                if (!perm.isSafAccess && !isGranted) {
                    OutlinedButton(
                        onClick = {
                            selectedPermissionForRationale = null
                            context.startActivity(PermissionHelper.createAppSettingsIntent(context))
                        }
                    ) {
                        Text("Ajustes del Sistema")
                    }
                } else if (!perm.isSafAccess && isGranted) {
                    OutlinedButton(onClick = { selectedPermissionForRationale = null }) {
                        Text("Cerrar")
                    }
                }
            }
        )
    }
}

@Composable
private fun PermissionRowItem(
    item: PermissionItemState,
    onClick: () -> Unit
) {
    val isGranted = item.status == AppPermissionStatus.GRANTED
    val icon = when (item.type) {
        AppPermissionType.CAMERA -> Icons.Default.CameraAlt
        AppPermissionType.GALLERY_MEDIA -> Icons.Default.PhotoLibrary
        AppPermissionType.DOCUMENTS_SAF -> Icons.Default.FolderOpen
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isGranted) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                FlowRow(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isGranted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = if (item.isSafAccess) "Nativo (SAF)" else if (isGranted) "Concedido" else "No concedido",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isGranted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (isGranted) Icons.AutoMirrored.Filled.OpenInNew else Icons.Default.Warning,
                contentDescription = if (isGranted) "Abrir ajustes" else "Atención",
                tint = if (isGranted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
