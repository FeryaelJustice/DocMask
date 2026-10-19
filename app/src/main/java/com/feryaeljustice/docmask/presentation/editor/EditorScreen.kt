package com.feryaeljustice.docmask.presentation.editor

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

/**
 * Adaptive Document Editor Screen for phones, foldables, and tablets.
 * Uses NavigationSuiteScaffold to automatically shift between BottomNavigationBar
 * on compact phones and NavigationRail on tablets/foldables.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocMaskApp(
    viewModel: EditorViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentDestination by remember { mutableStateOf("editor") }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it)) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                viewModel.onEvent(EditorUiEvent.ImageSelected(bitmap))
            } catch (e: Exception) {
                // Handled gracefully in ViewModel
            }
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            item(
                selected = currentDestination == "editor",
                onClick = { currentDestination = "editor" },
                icon = { Icon(Icons.Default.Description, contentDescription = "Editor") },
                label = { Text("Editor") },
                modifier = Modifier.testTag("nav_item_editor")
            )
            item(
                selected = currentDestination == "templates",
                onClick = { currentDestination = "templates" },
                icon = { Icon(Icons.Default.Security, contentDescription = "Plantillas") },
                label = { Text("Plantillas") },
                modifier = Modifier.testTag("nav_item_templates")
            )
            item(
                selected = currentDestination == "settings",
                onClick = { currentDestination = "settings" },
                icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                label = { Text("Ajustes") },
                modifier = Modifier.testTag("nav_item_settings")
            )
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpanded = maxWidth >= 600.dp

            if (isExpanded) {
                // Two-pane layout for tablets and expanded screens
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .padding(16.dp)
                    ) {
                        DocumentViewerPane(
                            uiState = uiState,
                            onEvent = viewModel::onEvent,
                            onPickImage = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(0.8f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(16.dp)
                    ) {
                        EditorControlsPane(
                            uiState = uiState,
                            onEvent = viewModel::onEvent,
                            onPickImage = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        )
                    }
                }
            } else {
                // Single-column layout for phones (single top-level vertical scroll)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    DocumentViewerPane(
                        uiState = uiState,
                        onEvent = viewModel::onEvent,
                        onPickImage = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    EditorControlsPane(
                        uiState = uiState,
                        onEvent = viewModel::onEvent,
                        onPickImage = { imagePickerLauncher.launch("image/*") }
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentViewerPane(
    uiState: EditorUiState,
    onEvent: (EditorUiEvent) -> Unit,
    onPickImage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val bitmap = uiState.maskedBitmap ?: uiState.originalBitmap
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Documento protegido",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onPickImage() },
                    contentScale = ContentScale.Fit
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onPickImage() }
                        .padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Selecciona un documento para proteger",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Procesamiento 100% offline y seguro",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onPickImage,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cargar Documento")
                    }
                }
            }

            if (uiState.isLoading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorControlsPane(
    uiState: EditorUiState,
    onEvent: (EditorUiEvent) -> Unit,
    onPickImage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Document actions (Change / Load)
        if (uiState.hasDocument) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onPickImage,
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cambiar Documento")
                }
            }
        }

        // Sensitive data detection status & action
        Text(
            text = "Detección Local de Datos",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${uiState.redactedCount} de ${uiState.detectedRegions.size} campos protegidos",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = { onEvent(EditorUiEvent.RedactAllSensitive(true)) },
                enabled = uiState.hasDocument,
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pixelar Todo")
            }
        }

        // Region chips
        if (uiState.detectedRegions.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.detectedRegions.forEach { region ->
                    FilterChip(
                        selected = region.isRedacted,
                        onClick = { onEvent(EditorUiEvent.ToggleRegionRedaction(region.id)) },
                        label = { Text(region.type.displayName) },
                        leadingIcon = if (region.isRedacted) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null,
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    )
                }
            }
        }

        // Grayscale switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Conversión a Escala de Grises",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Recomendado por la AEPD contra fraudes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = uiState.maskingOptions.applyGrayscale,
                onCheckedChange = { onEvent(EditorUiEvent.ToggleGrayscale(it)) },
                modifier = Modifier.testTag("switch_grayscale")
            )
        }

        // Watermark section
        Text(
            text = "Marca de Agua Cruzada",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = uiState.maskingOptions.watermarkConfig.purposeText,
            onValueChange = { onEvent(EditorUiEvent.UpdateWatermarkText(it)) },
            label = { Text("Finalidad del documento") },
            placeholder = { Text("Ej. Copia para alquiler de vivienda") },
            modifier = Modifier.fillMaxWidth()
        )

        // Template chips
        if (uiState.availableTemplates.isNotEmpty()) {
            Text(
                text = "Plantillas Rápidas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.availableTemplates.forEach { template ->
                    FilterChip(
                        selected = uiState.selectedTemplateId == template.id,
                        onClick = { onEvent(EditorUiEvent.SelectTemplate(template)) },
                        label = { Text(template.title) },
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    )
                }
            }
        }

        // Export button
        Button(
            onClick = { onEvent(EditorUiEvent.ExportDocument) },
            enabled = uiState.hasDocument,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .testTag("button_export")
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Exportar Documento Seguro")
        }
    }
}
