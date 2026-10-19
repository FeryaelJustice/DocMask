package com.feryaeljustice.docmask.presentation.editor

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import com.feryaeljustice.docmask.core.model.DocumentSide
import com.feryaeljustice.docmask.core.util.DocumentExporter
import com.feryaeljustice.docmask.presentation.settings.SettingsScreen
import com.feryaeljustice.docmask.presentation.templates.TemplatesScreen
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
                viewModel.onEvent(EditorUiEvent.ImageSelected(bitmap, uiState.activeSide))
            } catch (e: Exception) {
                Toast.makeText(context, "Error al cargar imagen: ${e.message}", Toast.LENGTH_SHORT).show()
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
        when (currentDestination) {
            "templates" -> {
                TemplatesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentDestination = "editor" }
                )
            }
            "settings" -> {
                SettingsScreen(
                    viewModel = viewModel
                )
            }
            else -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "DocMask",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            navigationIcon = {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        )
                    }
                ) { innerPadding ->
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    ) {
                        val isExpanded = maxWidth >= 600.dp

                        if (isExpanded) {
                            // Two-pane layout for tablets and expanded screens
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .imePadding()
                            ) {
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
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.isDualSidedMode) {
                PrimaryTabRow(
                    selectedTabIndex = if (uiState.activeSide == DocumentSide.FRONT) 0 else 1
                ) {
                    Tab(
                        selected = uiState.activeSide == DocumentSide.FRONT,
                        onClick = { onEvent(EditorUiEvent.SelectDocumentSide(DocumentSide.FRONT)) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Anverso")
                                if (uiState.hasFront) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Cargado",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    )
                    Tab(
                        selected = uiState.activeSide == DocumentSide.BACK,
                        onClick = { onEvent(EditorUiEvent.SelectDocumentSide(DocumentSide.BACK)) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Reverso")
                                if (uiState.hasBack) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Cargado",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = uiState.currentMaskedBitmap ?: uiState.currentOriginalBitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = if (uiState.isDualSidedMode) {
                            "Documento protegido (${uiState.activeSide.displayName})"
                        } else "Documento protegido",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
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
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (uiState.isDualSidedMode) {
                                "Cargar ${uiState.activeSide.displayName}"
                            } else "Selecciona un documento para proteger",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.isDualSidedMode) {
                                "Pulsa para adjuntar la imagen del ${uiState.activeSide.displayName.lowercase()}"
                            } else "Procesamiento 100% offline y seguro",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = onPickImage,
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (uiState.isDualSidedMode) "Cargar ${uiState.activeSide.displayName}"
                                else "Cargar Documento"
                            )
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorControlsPane(
    uiState: EditorUiState,
    onEvent: (EditorUiEvent) -> Unit,
    onPickImage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Pro Dual-Sided Mode Card
        if (uiState.isProUser) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DNI Doble Cara (Anverso / Reverso)",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Procesa ambas caras en lote con una sola marca de agua.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isDualSidedMode,
                            onCheckedChange = { onEvent(EditorUiEvent.ToggleDualSidedMode(it)) }
                        )
                    }
                    if (uiState.isDualSidedMode) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Aviso: Diseñado para DNI u otro documento de dos caras. Si adjuntas otro tipo de archivo queda bajo tu responsabilidad.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

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
                    Text(
                        if (uiState.isDualSidedMode) "Cambiar ${uiState.activeSide.displayName}"
                        else "Cambiar Documento"
                    )
                }
            }
        }

        // Sensitive data detection status & action
        Text(
            text = if (uiState.isDualSidedMode) "Detección Local (${uiState.activeSide.displayName})"
                   else "Detección Local de Datos",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${uiState.redactedCount} de ${uiState.currentDetectedRegions.size} campos protegidos",
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
        if (uiState.currentDetectedRegions.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.currentDetectedRegions.forEach { region ->
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

        // Watermark opacity slider
        val currentOpacity = uiState.maskingOptions.watermarkConfig.opacity
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Opacidad de la marca",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${(currentOpacity * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = currentOpacity,
            onValueChange = { onEvent(EditorUiEvent.UpdateWatermarkOpacity(it)) },
            valueRange = 0.1f..0.8f,
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
            onClick = {
                if (uiState.isDualSidedMode) {
                    if (!uiState.hasFront) {
                        Toast.makeText(context, "Falta adjuntar el anverso del documento", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!uiState.hasBack) {
                        Toast.makeText(context, "Falta adjuntar el reverso del documento", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val frontExport = uiState.maskedBitmap ?: uiState.originalBitmap
                    val backExport = uiState.backMaskedBitmap ?: uiState.backOriginalBitmap
                    if (frontExport != null && backExport != null) {
                        val result = DocumentExporter.exportAndShareBitmapsBatch(context, frontExport, backExport)
                        if (result.isSuccess) {
                            Toast.makeText(context, "Documentos exportados. Selecciona dónde compartirlos.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Error al exportar: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    val bitmapToExport = uiState.maskedBitmap ?: uiState.originalBitmap
                    if (bitmapToExport != null) {
                        val result = DocumentExporter.exportAndShareBitmap(context, bitmapToExport)
                        if (result.isSuccess) {
                            Toast.makeText(context, "Documento exportado. Selecciona dónde compartirlo.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Error al exportar: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Primero carga un documento para exportar", Toast.LENGTH_SHORT).show()
                    }
                }
                onEvent(EditorUiEvent.ExportDocument)
            },
            enabled = if (uiState.isDualSidedMode) (uiState.hasFront || uiState.hasBack) else uiState.hasDocument,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .testTag("button_export")
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (uiState.isDualSidedMode) "Exportar DNI Seguro (Lote Anverso y Reverso)"
                else "Exportar Documento Seguro"
            )
        }
    }
}
