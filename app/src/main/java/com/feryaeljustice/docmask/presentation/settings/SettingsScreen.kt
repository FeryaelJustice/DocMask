package com.feryaeljustice.docmask.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.feryaeljustice.docmask.BuildConfig
import com.feryaeljustice.docmask.R
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
    modifier: Modifier = Modifier,
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

    val resources = LocalResources.current

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onEvent(EditorUiEvent.RefreshPermissions(context))
        if (isGranted) {
            Toast.makeText(context, resources.getString(R.string.settings_permission_camera_granted), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, resources.getString(R.string.settings_permission_camera_denied), Toast.LENGTH_SHORT).show()
        }
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onEvent(EditorUiEvent.RefreshPermissions(context))
        val status = PermissionHelper.checkMediaPermissionStatus(context)
        when (status) {
            AppPermissionStatus.GRANTED -> {
                Toast.makeText(context, resources.getString(R.string.settings_permission_gallery_granted), Toast.LENGTH_SHORT).show()
            }
            AppPermissionStatus.PARTIAL -> {
                Toast.makeText(context, resources.getString(R.string.settings_permission_gallery_partial), Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(context, resources.getString(R.string.settings_permission_gallery_denied), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val onPermissionItemClick: (PermissionItemState) -> Unit = { item ->
        if (item.isSafAccess) {
            selectedPermissionForRationale = item
        } else if (item.status == AppPermissionStatus.GRANTED || item.status == AppPermissionStatus.PARTIAL) {
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
                    Toast.makeText(context, resources.getString(R.string.settings_storage_configured_success), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, resources.getString(R.string.settings_storage_no_write_permission), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, resources.getString(R.string.settings_storage_config_error, e.message ?: ""), Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
                            contentDescription = stringResource(R.string.settings_info_button_content_description)
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
                            text = stringResource(R.string.settings_zero_network_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_zero_network_desc),
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
                                text = stringResource(R.string.settings_permissions_section),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_permissions_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.permissions.isEmpty()) {
                        Text(
                            text = stringResource(R.string.settings_permissions_auditing),
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
                        text = stringResource(R.string.settings_permissions_desc),
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
                                    text = stringResource(R.string.settings_pro_card_title),
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
                                            text = stringResource(R.string.settings_pro_card_badge_active),
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
                                text = stringResource(R.string.settings_pro_card_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = stringResource(R.string.settings_pro_card_content_description),
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
                                text = stringResource(R.string.settings_biometric_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.settings_biometric_desc),
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
                                        resources.getString(R.string.settings_biometric_pro_exclusive),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onNavigateToPro()
                                    return@Switch
                                }
                                val canAuth = BiometricHelper.isBiometricAvailable(context)
                                if (!canAuth) {
                                    Toast.makeText(
                                        context,
                                        resources.getString(R.string.settings_biometric_not_available),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@Switch
                                }
                            }
                            viewModel.onEvent(EditorUiEvent.ToggleBiometric(isEnabled))
                            Toast.makeText(
                                context,
                                if (isEnabled) resources.getString(R.string.settings_biometric_enabled) else resources.getString(R.string.settings_biometric_disabled),
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
                            text = stringResource(R.string.settings_aepd_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.settings_aepd_advice),
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
                            text = stringResource(R.string.settings_storage_folder_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val customFolderName = if (uiState.customStorageUri != null) {
                        try {
                            val uri = Uri.parse(uiState.customStorageUri)
                            val doc = DocumentFile.fromTreeUri(context, uri)
                            doc?.name ?: uri.lastPathSegment
                        } catch (_: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                    val currentFolderText = if (customFolderName != null) {
                        stringResource(R.string.settings_storage_current_folder, customFolderName)
                    } else if (uiState.customStorageUri != null) {
                        stringResource(R.string.settings_storage_custom_folder)
                    } else {
                        stringResource(R.string.settings_storage_default_folder)
                    }
                    Text(
                        text = currentFolderText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_storage_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { folderPickerLauncher.launch(null) },
                            modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.settings_storage_change_folder))
                        }
                        OutlinedButton(
                            onClick = {
                                val customUriString = uiState.customStorageUri
                                try {
                                    if (!customUriString.isNullOrBlank()) {
                                        val treeUri = customUriString.toUri()
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(treeUri, "resource/folder")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        val intent = Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    }
                                } catch (_: Exception) {
                                    try {
                                        val fallbackIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                            type = "*/*"
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(fallbackIntent)
                                    } catch (e2: Exception) {
                                        Toast.makeText(context, resources.getString(R.string.settings_storage_open_error, e2.message ?: ""), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.settings_storage_open_folder))
                        }
                        if (uiState.customStorageUri != null) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.updateCustomStorageUri(null)
                                    Toast.makeText(context, resources.getString(R.string.settings_storage_reset_success), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.settings_storage_reset_folder))
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
                            text = stringResource(R.string.settings_cache_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.settings_cache_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_cache_usage, storageStats.first, CacheManager.formatBytes(storageStats.second)),
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
                                resources.getString(R.string.settings_cache_cleared, deletedCount, CacheManager.formatBytes(freedBytes))
                            } else {
                                resources.getString(R.string.settings_cache_cleared_empty)
                            }
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 44.dp)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_cache_clear_button))
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
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_app_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.made_with_by_feryael_justice),
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
                        text = stringResource(R.string.settings_app_info_p1),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.settings_app_info_p2),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.settings_app_info_p3),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.settings_app_info_p4),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showInfoDialog = false }) {
                    Text(stringResource(R.string.common_understood))
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
                                stringResource(R.string.settings_permission_status_saf)
                            } else if (perm.status == AppPermissionStatus.GRANTED) {
                                stringResource(R.string.settings_permission_status_granted)
                            } else if (perm.status == AppPermissionStatus.PARTIAL) {
                                stringResource(R.string.settings_permission_status_partial)
                            } else {
                                stringResource(R.string.settings_permission_status_denied)
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
                        Text(stringResource(R.string.common_understood))
                    }
                } else if (!isGranted && perm.status != AppPermissionStatus.PARTIAL) {
                    Button(
                        onClick = {
                            val target = perm
                            selectedPermissionForRationale = null
                            if (target.type == AppPermissionType.CAMERA) {
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            } else if (target.type == AppPermissionType.GALLERY_MEDIA) {
                                mediaPermissionLauncher.launch(
                                    PermissionHelper.getRequiredMediaPermissions().toTypedArray()
                                )
                            }
                        }
                    ) {
                        Text(stringResource(R.string.settings_permission_action_request))
                    }
                } else {
                    Button(
                        onClick = {
                            selectedPermissionForRationale = null
                            context.startActivity(PermissionHelper.createAppSettingsIntent(context))
                        }
                    ) {
                        Text(stringResource(R.string.settings_permission_action_android_settings))
                    }
                }
            },
            dismissButton = {
                if (!perm.isSafAccess && !isGranted && perm.status != AppPermissionStatus.PARTIAL) {
                    OutlinedButton(
                        onClick = {
                            selectedPermissionForRationale = null
                            context.startActivity(PermissionHelper.createAppSettingsIntent(context))
                        }
                    ) {
                        Text(stringResource(R.string.settings_permission_action_system_settings))
                    }
                } else if (!perm.isSafAccess) {
                    OutlinedButton(onClick = { selectedPermissionForRationale = null }) {
                        Text(stringResource(R.string.common_close))
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
    val isGranted = item.status == AppPermissionStatus.GRANTED || item.status == AppPermissionStatus.PARTIAL
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
                        color = when (item.status) {
                            AppPermissionStatus.GRANTED -> MaterialTheme.colorScheme.primaryContainer
                            AppPermissionStatus.PARTIAL -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.errorContainer
                        }
                    ) {
                        Text(
                            text = when {
                                item.isSafAccess -> stringResource(R.string.settings_permission_badge_saf)
                                item.status == AppPermissionStatus.GRANTED -> stringResource(R.string.settings_permission_badge_granted)
                                item.status == AppPermissionStatus.PARTIAL -> stringResource(R.string.settings_permission_badge_partial)
                                else -> stringResource(R.string.settings_permission_badge_denied)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = when (item.status) {
                                AppPermissionStatus.GRANTED -> MaterialTheme.colorScheme.onPrimaryContainer
                                AppPermissionStatus.PARTIAL -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onErrorContainer
                            },
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
                contentDescription = if (isGranted) stringResource(R.string.settings_open_saf_button) else stringResource(R.string.settings_open_saf_warning),
                tint = if (isGranted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
