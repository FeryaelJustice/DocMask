package com.feryaeljustice.docmask.presentation.editor

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.feryaeljustice.docmask.core.model.AppPermissionStatus
import com.feryaeljustice.docmask.core.util.PermissionHelper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.feryaeljustice.docmask.core.model.DocumentSide
import com.feryaeljustice.docmask.core.model.RedactionArea
import com.feryaeljustice.docmask.core.util.CameraCaptureHelper
import com.feryaeljustice.docmask.core.util.DocumentExporter
import com.feryaeljustice.docmask.core.util.PdfDocumentManager
import com.feryaeljustice.docmask.data.ml.SensitiveFieldDetector
import com.feryaeljustice.docmask.presentation.pro.ProScreen
import com.feryaeljustice.docmask.presentation.settings.SettingsScreen
import com.feryaeljustice.docmask.presentation.templates.TemplatesScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

fun getFileNameFromUri(context: Context, uri: Uri): String {
    var name = "documento.pdf"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && cursor.moveToFirst()) {
            name = cursor.getString(nameIndex)
        }
    }
    return name
}

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
    var currentDestination by rememberSaveable { mutableStateOf("editor") }
    var showManualEditorDialog by rememberSaveable { mutableStateOf(false) }
    var showPhotoSourceSheet by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showCameraRationaleDialog by remember { mutableStateOf(false) }
    var showCameraPermanentDenialDialog by remember { mutableStateOf(false) }
    var showMediaRationaleDialog by remember { mutableStateOf(false) }
    var showMediaPermanentDenialDialog by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            tempCameraUri?.let { uri ->
                viewModel.onEvent(EditorUiEvent.SetLoading(true, "Procesando foto de cámara..."))
                coroutineScope.launch {
                    try {
                        val bitmap: Bitmap = withContext(Dispatchers.IO) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                                    decoder.isMutableRequired = true
                                }
                            } else {
                                @Suppress("DEPRECATION")
                                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                            }
                        }
                        viewModel.onEvent(EditorUiEvent.ImageSelected(bitmap, uiState.activeSide))
                    } catch (e: Exception) {
                        viewModel.onEvent(EditorUiEvent.ShowError("Error al procesar foto de cámara: ${e.message}"))
                    }
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onEvent(EditorUiEvent.RefreshPermissions(context))
        if (isGranted) {
            try {
                val uri = CameraCaptureHelper.createCameraTempUri(context)
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "No se pudo iniciar la cámara: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            val shouldShowRationale = PermissionHelper.shouldShowCameraRationale(context)
            if (!shouldShowRationale) {
                showCameraPermanentDenialDialog = true
            } else {
                Toast.makeText(context, "Permiso de cámara no concedido.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.onEvent(EditorUiEvent.SetLoading(true, "Cargando imagen..."))
            coroutineScope.launch {
                try {
                    val bitmap: Bitmap = withContext(Dispatchers.IO) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it)) { decoder, _, _ ->
                                decoder.isMutableRequired = true
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                        }
                    }
                    viewModel.onEvent(EditorUiEvent.ImageSelected(bitmap, uiState.activeSide))
                } catch (e: Exception) {
                    viewModel.onEvent(EditorUiEvent.ShowError("Error al cargar imagen: ${e.message}"))
                }
            }
        }
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onEvent(EditorUiEvent.RefreshPermissions(context))
        val status = PermissionHelper.checkMediaPermissionStatus(context)
        if (status == AppPermissionStatus.GRANTED || status == AppPermissionStatus.PARTIAL) {
            imagePickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            val shouldShowRationale = PermissionHelper.shouldShowMediaRationale(context)
            if (!shouldShowRationale) {
                showMediaPermanentDenialDialog = true
            } else {
                Toast.makeText(context, "Permiso de galería no concedido.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val requestCameraAction: () -> Unit = {
        if (PermissionHelper.isCameraPermissionGranted(context)) {
            try {
                val uri = CameraCaptureHelper.createCameraTempUri(context)
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "No se pudo iniciar la cámara: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else if (PermissionHelper.shouldShowCameraRationale(context)) {
            showCameraRationaleDialog = true
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    val requestMediaAction: () -> Unit = {
        if (PermissionHelper.isMediaPermissionGranted(context)) {
            imagePickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else if (PermissionHelper.shouldShowMediaRationale(context)) {
            showMediaRationaleDialog = true
        } else {
            mediaPermissionLauncher.launch(
                PermissionHelper.getRequiredMediaPermissions().toTypedArray()
            )
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { pdfUri ->
            viewModel.onEvent(EditorUiEvent.SetLoading(true, "Cargando documento PDF..."))
            coroutineScope.launch {
                try {
                    val fileName = getFileNameFromUri(context, pdfUri)
                    val pagesResult = withContext(Dispatchers.IO) {
                        PdfDocumentManager.renderPdfPagesToBitmaps(context, pdfUri)
                    }
                    val pages = pagesResult.getOrThrow()
                    if (pages.isEmpty()) {
                        viewModel.onEvent(EditorUiEvent.ShowError("El documento PDF no contiene páginas válidas."))
                        return@launch
                    }

                    viewModel.onEvent(EditorUiEvent.PdfSelected(fileName, pages, emptyList()))
                } catch (e: Exception) {
                    viewModel.onEvent(EditorUiEvent.ShowError("Error al importar PDF: ${e.message}"))
                }
            }
        }
    }

    val onPickDocumentAction = {
        if (uiState.isPdfMode) {
            pdfPickerLauncher.launch(arrayOf("application/pdf"))
        } else {
            showPhotoSourceSheet = true
        }
    }

    if (showPhotoSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoSourceSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Seleccionar origen de la imagen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        showPhotoSourceSheet = false
                        requestCameraAction()
                    },
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hacer foto con la cámara")
                }
                OutlinedButton(
                    onClick = {
                        showPhotoSourceSheet = false
                        requestMediaAction()
                    },
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Elegir de la galería")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showMediaRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showMediaRationaleDialog = false },
            icon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null) },
            title = {
                Text(
                    text = "Acceso a Fotos y Galería",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "DocMask necesita acceder a tus imágenes para cargar el documento a anonimizar. Puedes conceder acceso a ciertas fotos seleccionadas o a toda tu galería. El procesamiento es 100% local."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMediaRationaleDialog = false
                        mediaPermissionLauncher.launch(
                            PermissionHelper.getRequiredMediaPermissions().toTypedArray()
                        )
                    }
                ) {
                    Text("Conceder Permiso")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showMediaRationaleDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showMediaPermanentDenialDialog) {
        AlertDialog(
            onDismissRequest = { showMediaPermanentDenialDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = {
                Text(
                    text = "Permiso de Fotos Bloqueado",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "El acceso a la galería está bloqueado en Android. Para seleccionar documentos desde tus fotos, concede el permiso en Ajustes de la aplicación."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMediaPermanentDenialDialog = false
                        context.startActivity(PermissionHelper.createAppSettingsIntent(context))
                    }
                ) {
                    Text("Abrir Ajustes")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showMediaPermanentDenialDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showCameraRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showCameraRationaleDialog = false },
            icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
            title = {
                Text(
                    text = "Permiso de Cámara Requerido",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "DocMask necesita acceder a la cámara únicamente para fotografiar tu documento de identidad y anonimizarlo en tu dispositivo. La imagen no se sube a internet ni se comparte con nadie."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCameraRationaleDialog = false
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                ) {
                    Text("Conceder Permiso")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCameraRationaleDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showCameraPermanentDenialDialog) {
        AlertDialog(
            onDismissRequest = { showCameraPermanentDenialDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = {
                Text(
                    text = "Permiso Bloqueado",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "El permiso de cámara está deshabilitado en Android. Para tomar fotos directamente desde la app, habilítalo en la sección de Permisos de la configuración del sistema."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCameraPermanentDenialDialog = false
                        context.startActivity(PermissionHelper.createAppSettingsIntent(context))
                    }
                ) {
                    Text("Abrir Ajustes")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCameraPermanentDenialDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
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
                selected = currentDestination == "settings" || currentDestination == "pro",
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
                    viewModel = viewModel,
                    onNavigateToPro = { currentDestination = "pro" }
                )
            }
            "pro" -> {
                ProScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentDestination = "settings" }
                )
            }
            else -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("DocMask") },
                            actions = {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "100% Offline",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
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
                                        onPickDocument = onPickDocumentAction,
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
                                        onPickDocument = onPickDocumentAction,
                                        onNavigateToPro = { currentDestination = "pro" },
                                        onEditManualRegions = { showManualEditorDialog = true },
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
                                    onPickDocument = onPickDocumentAction,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(280.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                EditorControlsPane(
                                    uiState = uiState,
                                    onEvent = viewModel::onEvent,
                                    onPickDocument = onPickDocumentAction,
                                    onNavigateToPro = { currentDestination = "pro" },
                                    onEditManualRegions = { showManualEditorDialog = true }
                                )
                            }
                        }

                        // Modal Overlay for Loading / Exporting Operations
                        if (uiState.isLoading || uiState.isExporting) {
                            Dialog(onDismissRequest = { /* Prevent dismissing while running */ }) {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(48.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = uiState.loadingMessage ?: "Procesando documento...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        // Error Dialog
                        uiState.errorMessage?.let { error ->
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { viewModel.onEvent(EditorUiEvent.DismissError) },
                                title = { Text("Aviso de Error") },
                                text = { Text(error) },
                                confirmButton = {
                                    androidx.compose.material3.TextButton(
                                        onClick = { viewModel.onEvent(EditorUiEvent.DismissError) }
                                    ) {
                                        Text("Entendido")
                                    }
                                }
                            )
                        }

                        // Success message Dialog
                        uiState.exportSuccessMessage?.let { successMsg ->
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { viewModel.onEvent(EditorUiEvent.DismissSuccessMessage) },
                                title = { Text("Operación Exitosa") },
                                text = { Text(successMsg) },
                                confirmButton = {
                                    androidx.compose.material3.TextButton(
                                        onClick = { viewModel.onEvent(EditorUiEvent.DismissSuccessMessage) }
                                    ) {
                                        Text("Aceptar")
                                    }
                                }
                            )
                        }

                        // Manual Redaction Pro Dialog
                        if (showManualEditorDialog && uiState.currentOriginalBitmap != null) {
                            ManualRedactionDialog(
                                originalBitmap = uiState.currentOriginalBitmap!!,
                                existingRegions = uiState.currentDetectedRegions,
                                side = uiState.activeSide,
                                onDismiss = { showManualEditorDialog = false },
                                onSave = { manualRegions ->
                                    viewModel.onEvent(EditorUiEvent.SaveManualRegions(uiState.activeSide, manualRegions))
                                    showManualEditorDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerPane(
    uiState: EditorUiState,
    onEvent: (EditorUiEvent) -> Unit,
    onPickDocument: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Input Mode Segmented Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = uiState.inputMode == InputMode.SINGLE_PHOTO,
                        onClick = { onEvent(EditorUiEvent.SelectInputMode(InputMode.SINGLE_PHOTO)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = uiState.inputMode == InputMode.SINGLE_PHOTO) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = "1 Cara",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                    SegmentedButton(
                        selected = uiState.inputMode == InputMode.DNI_DUAL,
                        onClick = { onEvent(EditorUiEvent.SelectInputMode(InputMode.DNI_DUAL)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = uiState.inputMode == InputMode.DNI_DUAL) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = "2 Caras",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                    SegmentedButton(
                        selected = uiState.inputMode == InputMode.PDF_DOCUMENT,
                        onClick = { onEvent(EditorUiEvent.SelectInputMode(InputMode.PDF_DOCUMENT)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = uiState.inputMode == InputMode.PDF_DOCUMENT) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = "PDF",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }

            // Dual-sided tabs when in DNI dual mode
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

            // Main Viewer Canvas / Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isPdfMode) {
                    // PDF Document Executive Card View
                    if (uiState.hasDocument) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onPickDocument() }
                                .padding(20.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = uiState.pdfDocumentName ?: "documento.pdf",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${uiState.pdfPageCount} páginas • ${uiState.pdfDetectedRegions.size} campos de riesgo identificados",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(onClick = onPickDocument) {
                                Text("Cambiar Documento PDF")
                            }
                        }
                    } else {
                        // Empty PDF placeholder
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onPickDocument() }
                                .padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Selecciona un documento PDF para proteger",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Procesamiento 100% offline página por página",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = onPickDocument,
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cargar Documento PDF")
                            }
                        }
                    }
                } else {
                    // Bitmap Photo / DNI Viewer
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
                                .clickable { onPickDocument() },
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onPickDocument() }
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
                                    "Pulsa para adjuntar la foto del ${uiState.activeSide.displayName.lowercase()}"
                                } else "Procesamiento 100% offline y seguro",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = onPickDocument,
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
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun EditorControlsPane(
    uiState: EditorUiState,
    onEvent: (EditorUiEvent) -> Unit,
    onPickDocument: () -> Unit = {},
    onNavigateToPro: () -> Unit = {},
    onEditManualRegions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showFullScreenPreview by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Thumbnail & Quick Actions Row
        if (!uiState.isPdfMode && uiState.hasDocument) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val originalThumb = uiState.currentOriginalBitmap
                if (originalThumb != null) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .combinedClickable(
                                onClick = {
                                    showFullScreenPreview = true
                                },
                                onLongClick = {
                                    showFullScreenPreview = true
                                }
                            )
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = originalThumb.asImageBitmap(),
                            contentDescription = "Miniatura original",
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Original sin efecto",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Toca para ampliar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (showFullScreenPreview) {
                        FullScreenImagePreviewDialog(
                            bitmap = originalThumb,
                            onDismiss = { showFullScreenPreview = false }
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                val configuration = LocalConfiguration.current
                val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

                val actionButtons: @Composable () -> Unit = {
                    Button(
                        onClick = {
                            if (uiState.isProUser) {
                                onEditManualRegions()
                            } else {
                                onNavigateToPro()
                            }
                        },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 44.dp)
                            .testTag("button_manual_redaction")
                    ) {
                        Icon(Icons.Default.Draw, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Zonas manuales PRO")
                    }

                    OutlinedButton(
                        onClick = onPickDocument,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 44.dp)
                            .testTag("button_change_document")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (uiState.isDualSidedMode) "Cambiar ${uiState.activeSide.displayName}"
                            else "Cambiar Documento"
                        )
                    }
                }

                if (isLandscape) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        actionButtons()
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        actionButtons()
                    }
                }
            }
        }

        // Sensitive data detection header & actions
        val regions = uiState.currentDetectedRegions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (uiState.isPdfMode) "Campos Identificados en PDF"
                           else if (uiState.isDualSidedMode) "Datos en ${uiState.activeSide.displayName}"
                           else "Datos Sensibles Detectados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${uiState.redactedCount} de ${regions.size} campos seleccionados para pixelar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { onEvent(EditorUiEvent.ApplyAepdPreset) },
                    modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AEPD")
                }
                OutlinedButton(
                    onClick = {
                        val allRedacted = regions.all { it.isRedacted }
                        onEvent(EditorUiEvent.RedactAllSensitive(!allRedacted))
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                ) {
                    val allRedacted = regions.all { it.isRedacted }
                    Text(if (allRedacted) "Desmarcar" else "Pixelar Todo")
                }
            }
        }

        // Sensitive fields chips
        if (regions.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                regions.forEach { area ->
                    FilterChip(
                        selected = area.isRedacted,
                        onClick = { onEvent(EditorUiEvent.ToggleRegionRedaction(area.id)) },
                        label = {
                            val label = if (uiState.isPdfMode && area.pageIndex > 0) {
                                "Pág ${area.pageIndex}: ${area.type.displayName}"
                            } else {
                                area.type.displayName
                            }
                            Text(label)
                        },
                        leadingIcon = if (area.isRedacted) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    )
                }
            }
        }

        // Pixelation intensity slider
        val currentPixelPercent = uiState.maskingOptions.pixelPercent
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Intensidad de Pixelado / Blur",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Ajuste uniforme para todas las áreas sensibles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "$currentPixelPercent%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = currentPixelPercent.toFloat(),
                onValueChange = { onEvent(EditorUiEvent.UpdatePixelIntensity(it.toInt())) },
                valueRange = 10f..100f,
                steps = 17,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("slider_pixel_intensity")
            )
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

        // Watermark with Date (PRO feature switch)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Incluir fecha de hoy en la marca",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "PRO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Añade automáticamente el sufijo de fecha (- dd/MM/yyyy)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = uiState.maskingOptions.watermarkConfig.includeDate,
                onCheckedChange = { onEvent(EditorUiEvent.ToggleWatermarkDate(it)) }
            )
        }

        OutlinedTextField(
            value = uiState.maskingOptions.watermarkConfig.purposeText,
            onValueChange = { onEvent(EditorUiEvent.UpdateWatermarkText(it)) },
            label = { Text("Finalidad del documento") },
            placeholder = { Text("Ej. Copia para alquiler de vivienda") },
            modifier = Modifier.fillMaxWidth()
        )

        // Watermark opacity slider (with debounced/responsive steps to avoid ANR)
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
            steps = 13,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("slider_watermark_opacity")
        )

        // Quick template chips (Favorite templates prioritized)
        val quickTemplates = uiState.favoriteTemplates.ifEmpty { uiState.defaultTemplates }
        if (quickTemplates.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Plantillas Rápidas (Favoritas)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (uiState.favoriteTemplates.isEmpty()) {
                    Text(
                        text = "Predeterminadas (Marca ⭐ en Plantillas)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickTemplates.forEach { template ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = uiState.selectedTemplateId == template.id,
                            onClick = { onEvent(EditorUiEvent.SelectTemplate(template)) },
                            label = { Text(template.title) },
                            leadingIcon = if (template.isFavorite) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Favorita",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        )
                        IconButton(
                            onClick = { onEvent(EditorUiEvent.ToggleFavoriteTemplate(template.id)) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (template.isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                                contentDescription = "Alternar favorito",
                                tint = if (template.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Dual-sided DNI PDF compilation toggle
        if (uiState.isDualSidedMode) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Unir Anverso y Reverso en PDF",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "PRO",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Genera un único archivo PDF unificado de 2 páginas con ambas caras protegidas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.generatePdfForDni,
                        onCheckedChange = { onEvent(EditorUiEvent.ToggleGeneratePdf(it)) }
                    )
                }
            }
        }

        // Daily quota badge
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cuota diaria de exportación:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${uiState.dailyExportsRemaining} de ${uiState.dailyExportsLimit} restantes hoy",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.dailyExportsRemaining > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }

        // Export button (Primary)
        Button(
            onClick = {
                if (uiState.dailyExportsRemaining <= 0) {
                    val limitMsg = if (uiState.isProUser) {
                        "Has alcanzado el límite diario de ${uiState.dailyExportsLimit} documentos protegidos."
                    } else {
                        "Has alcanzado tu límite gratuito de ${uiState.dailyExportsLimit} documentos al día. Actualiza a Pro en Ajustes para exportar hasta 40 documentos diarios."
                    }
                    Toast.makeText(context, limitMsg, Toast.LENGTH_LONG).show()
                    return@Button
                }

                coroutineScope.launch {
                    onEvent(EditorUiEvent.SetLoading(true, "Exportando y preparando documento seguro..."))
                    try {
                        if (uiState.isPdfMode) {
                            val pages = uiState.pdfMaskedPagesBitmaps.ifEmpty { uiState.pdfPagesBitmaps }
                            if (pages.isNotEmpty()) {
                                val result = withContext(Dispatchers.IO) {
                                    DocumentExporter.savePdfToStorage(
                                        context = context,
                                        bitmaps = pages,
                                        title = uiState.pdfDocumentName?.removeSuffix(".pdf") ?: "docmask_documento",
                                        customStorageUri = uiState.customStorageUri
                                    )
                                }
                                onEvent(EditorUiEvent.SetLoading(false))
                                if (result.isSuccess) {
                                    val uri = result.getOrNull()
                                    if (uri != null) {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            putExtra(Intent.EXTRA_SUBJECT, "Documento PDF protegido con DocMask")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Compartir PDF protegido").apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        })
                                    }
                                    onEvent(EditorUiEvent.ExportDocument)
                                } else {
                                    Toast.makeText(context, "Error al procesar PDF: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                onEvent(EditorUiEvent.SetLoading(false))
                            }
                        } else if (uiState.isDualSidedMode) {
                            if (!uiState.hasFront) {
                                onEvent(EditorUiEvent.SetLoading(false))
                                Toast.makeText(context, "Falta adjuntar el anverso del documento", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            if (!uiState.hasBack) {
                                onEvent(EditorUiEvent.SetLoading(false))
                                Toast.makeText(context, "Falta adjuntar el reverso del documento", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            val frontExport = uiState.maskedBitmap ?: uiState.originalBitmap
                            val backExport = uiState.backMaskedBitmap ?: uiState.backOriginalBitmap
                            if (frontExport != null && backExport != null) {
                                if (uiState.generatePdfForDni) {
                                    val pdfResult = withContext(Dispatchers.IO) {
                                        DocumentExporter.exportAndSharePdfBatch(
                                            context = context,
                                            frontBitmap = frontExport,
                                            backBitmap = backExport,
                                            customStorageUri = uiState.customStorageUri
                                        )
                                    }
                                    onEvent(EditorUiEvent.SetLoading(false))
                                    if (pdfResult.isSuccess) {
                                        onEvent(EditorUiEvent.ExportDocument)
                                    } else {
                                        Toast.makeText(context, "Error al exportar PDF: ${pdfResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    val result = withContext(Dispatchers.IO) {
                                        DocumentExporter.exportAndShareBitmapsBatch(
                                            context = context,
                                            frontBitmap = frontExport,
                                            backBitmap = backExport,
                                            customStorageUri = uiState.customStorageUri
                                        )
                                    }
                                    onEvent(EditorUiEvent.SetLoading(false))
                                    if (result.isSuccess) {
                                        onEvent(EditorUiEvent.ExportDocument)
                                    } else {
                                        Toast.makeText(context, "Error al exportar: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                onEvent(EditorUiEvent.SetLoading(false))
                            }
                        } else {
                            val bitmapToExport = uiState.maskedBitmap ?: uiState.originalBitmap
                            if (bitmapToExport != null) {
                                val result = withContext(Dispatchers.IO) {
                                    DocumentExporter.exportAndShareBitmap(
                                        context = context,
                                        bitmap = bitmapToExport,
                                        customStorageUri = uiState.customStorageUri
                                    )
                                }
                                onEvent(EditorUiEvent.SetLoading(false))
                                if (result.isSuccess) {
                                    onEvent(EditorUiEvent.ExportDocument)
                                } else {
                                    Toast.makeText(context, "Error al exportar: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                onEvent(EditorUiEvent.SetLoading(false))
                                Toast.makeText(context, "Primero carga un documento para exportar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        onEvent(EditorUiEvent.SetLoading(false))
                        Toast.makeText(context, "Error inesperado al exportar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            enabled = if (uiState.isPdfMode) uiState.hasDocument
                      else if (uiState.isDualSidedMode) (uiState.hasFront && uiState.hasBack)
                      else uiState.hasDocument,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .testTag("button_export")
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (uiState.isPdfMode) "Exportar y Compartir PDF Protegido"
                else if (uiState.isDualSidedMode && (!uiState.hasFront || !uiState.hasBack)) "Carga Anverso y Reverso para Exportar"
                else if (uiState.isDualSidedMode && uiState.generatePdfForDni) "Exportar DNI en PDF Combinado"
                else if (uiState.isDualSidedMode) "Exportar DNI (2 Caras)"
                else "Exportar Documento Seguro"
            )
        }

        // Dedicated "Descargar PDF" button (Enabled when PDF or combined DNI ready)
        val canDownloadPdf = uiState.isPdfDownloadAvailable || (uiState.isPdfMode && uiState.hasDocument) || (uiState.isDualSidedMode && uiState.hasFront && uiState.hasBack)
        if (canDownloadPdf) {
            OutlinedButton(
                onClick = {
                    if (uiState.dailyExportsRemaining <= 0) {
                        onEvent(EditorUiEvent.DownloadPdf)
                        return@OutlinedButton
                    }

                    val bitmapsToPdf = if (uiState.isPdfMode) {
                        uiState.pdfMaskedPagesBitmaps.ifEmpty { uiState.pdfPagesBitmaps }
                    } else if (uiState.isDualSidedMode) {
                        listOfNotNull(uiState.maskedBitmap ?: uiState.originalBitmap, uiState.backMaskedBitmap ?: uiState.backOriginalBitmap)
                    } else {
                        listOfNotNull(uiState.maskedBitmap ?: uiState.originalBitmap)
                    }

                    if (bitmapsToPdf.isNotEmpty()) {
                        coroutineScope.launch {
                            onEvent(EditorUiEvent.SetLoading(true, "Guardando documento PDF en almacenamiento..."))
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    DocumentExporter.savePdfToStorage(
                                        context = context,
                                        bitmaps = bitmapsToPdf,
                                        title = if (uiState.isDualSidedMode) "docmask_dni_completo" else uiState.pdfDocumentName?.removeSuffix(".pdf") ?: "docmask_descarga",
                                        customStorageUri = uiState.customStorageUri
                                    )
                                }
                                onEvent(EditorUiEvent.SetLoading(false))
                                if (result.isSuccess) {
                                    Toast.makeText(context, "PDF guardado correctamente en la carpeta de destino.", Toast.LENGTH_LONG).show()
                                    onEvent(EditorUiEvent.DownloadPdf)
                                } else {
                                    Toast.makeText(context, "Error al guardar PDF: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                onEvent(EditorUiEvent.SetLoading(false))
                                Toast.makeText(context, "Error al guardar PDF: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Primero genera o procesa un documento para descargar su PDF.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 50.dp)
                    .testTag("button_download_pdf")
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Descargar PDF en Dispositivo")
            }
        }
    }
}

/**
 * Fullscreen original preview dialog with spring bounce animation, dark background, rounded corners,
 * accessible 'X' button and back handler support.
 */
@Composable
fun FullScreenImagePreviewDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        isVisible = true
    }

    val coroutineScope = rememberCoroutineScope()
    val closeAnimated: () -> Unit = {
        coroutineScope.launch {
            isVisible = false
            kotlinx.coroutines.delay(220)
            onDismiss()
        }
    }

    BackHandler(enabled = true) {
        closeAnimated()
    }

    Dialog(
        onDismissRequest = { closeAnimated() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .clickable { closeAnimated() },
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                        scaleIn(
                            initialScale = 0.85f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ),
                exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                        scaleOut(
                            targetScale = 0.85f,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                        )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Vista previa original en pantalla completa",
                        modifier = Modifier
                            .fillMaxSize(0.92f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = false) {},
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Accessible Close Button top right
            IconButton(
                onClick = { closeAnimated() },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 20.dp)
                    .size(48.dp)
                    .testTag("button_close_fullscreen_preview")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar vista previa",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
