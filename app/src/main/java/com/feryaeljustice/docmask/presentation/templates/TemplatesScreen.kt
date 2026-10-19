package com.feryaeljustice.docmask.presentation.templates

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.feryaeljustice.docmask.core.model.WatermarkConfig
import com.feryaeljustice.docmask.domain.model.WatermarkTemplate
import com.feryaeljustice.docmask.presentation.editor.EditorUiEvent
import com.feryaeljustice.docmask.presentation.editor.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<WatermarkTemplate?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plantillas de Marca de Agua") },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTemplate = null
                    showDialog = true
                },
                modifier = Modifier.defaultMinSize(minWidth = 56.dp, minHeight = 56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva plantilla")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(padding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Pulsa una plantilla para activarla, o mantenla pulsada para editarla. Desliza hacia la izquierda para eliminar plantillas personalizadas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Section 1: Default System Templates
            item {
                Text(
                    text = "Plantillas Predeterminadas",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }

            items(uiState.defaultTemplates, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    isSelected = uiState.selectedTemplateId == template.id,
                    onClick = { viewModel.onEvent(EditorUiEvent.SelectTemplate(template)) },
                    onLongClick = {
                        editingTemplate = template
                        showDialog = true
                    }
                )
            }

            // Section 2: Custom User Templates
            item {
                Text(
                    text = "Plantillas Personalizadas",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                )
            }

            if (uiState.customTemplates.isEmpty()) {
                item {
                    Text(
                        text = "Aún no tienes plantillas personalizadas. Pulsa '+' para crear una.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            items(uiState.customTemplates, key = { it.id }) { template ->
                @Suppress("DEPRECATION")
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { dismissValue ->
                        if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                            viewModel.onEvent(EditorUiEvent.DeleteTemplate(template.id))
                            true
                        } else {
                            false
                        }
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    enableDismissFromEndToStart = true,
                    backgroundContent = {
                        val color = when (dismissState.targetValue) {
                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(color)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar plantilla",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                ) {
                    TemplateCard(
                        template = template,
                        isSelected = uiState.selectedTemplateId == template.id,
                        onClick = { viewModel.onEvent(EditorUiEvent.SelectTemplate(template)) },
                        onLongClick = {
                            editingTemplate = template
                            showDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showDialog) {
        TemplateFormDialog(
            templateToEdit = editingTemplate,
            onDismiss = {
                showDialog = false
                editingTemplate = null
            },
            onConfirm = { savedTemplate ->
                viewModel.onEvent(EditorUiEvent.SaveTemplate(savedTemplate))
                if (editingTemplate == null) {
                    viewModel.onEvent(EditorUiEvent.SelectTemplate(savedTemplate))
                }
                showDialog = false
                editingTemplate = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TemplateCard(
    template: WatermarkTemplate,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
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
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\"${template.config.purposeText}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Opacidad: ${(template.config.opacity * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (template.config.includeDate) {
                        Text(
                            text = "• Con fecha",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (isSelected) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Activa",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Activa",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) {
                    Text("Usar")
                }
            }
        }
    }
}

@Composable
fun TemplateFormDialog(
    templateToEdit: WatermarkTemplate? = null,
    onDismiss: () -> Unit,
    onConfirm: (WatermarkTemplate) -> Unit
) {
    val isEditing = templateToEdit != null
    var title by remember { mutableStateOf(templateToEdit?.title ?: "") }
    var purposeText by remember { mutableStateOf(templateToEdit?.config?.purposeText ?: "") }
    var includeDate by remember { mutableStateOf(templateToEdit?.config?.includeDate ?: true) }
    var opacity by remember { mutableFloatStateOf(templateToEdit?.config?.opacity ?: 0.35f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Editar Plantilla" else "Nueva Plantilla") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nombre de la plantilla") },
                    placeholder = { Text("Ej. Alquiler de coche") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = purposeText,
                    onValueChange = { purposeText = it },
                    label = { Text("Texto de la marca de agua") },
                    placeholder = { Text("Copia para alquiler de vehículo") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Incluir fecha automática")
                    Switch(
                        checked = includeDate,
                        onCheckedChange = { includeDate = it }
                    )
                }

                Text(
                    text = "Opacidad: ${(opacity * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = opacity,
                    onValueChange = { opacity = it },
                    valueRange = 0.1f..0.8f
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && purposeText.isNotBlank()) {
                        val template = if (templateToEdit != null) {
                            templateToEdit.copy(
                                title = title,
                                config = templateToEdit.config.copy(
                                    purposeText = purposeText,
                                    includeDate = includeDate,
                                    opacity = opacity
                                )
                            )
                        } else {
                            WatermarkTemplate(
                                title = title,
                                config = WatermarkConfig(
                                    purposeText = purposeText,
                                    includeDate = includeDate,
                                    opacity = opacity
                                ),
                                isDefault = false
                            )
                        }
                        onConfirm(template)
                    }
                },
                enabled = title.isNotBlank() && purposeText.isNotBlank()
            ) {
                Text(if (isEditing) "Guardar Cambios" else "Guardar y Usar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
