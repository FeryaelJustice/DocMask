package com.feryaeljustice.docmask.presentation.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var showCreateDialog by remember { mutableStateOf(false) }

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
                onClick = { showCreateDialog = true },
                modifier = Modifier.defaultMinSize(minWidth = 56.dp, minHeight = 56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva plantilla")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Selecciona o crea una plantilla frecuente para aplicar la marca de agua recomendada por la AEPD.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(uiState.availableTemplates, key = { it.id }) { template ->
                val isSelected = uiState.selectedTemplateId == template.id
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                            if (template.config.includeDate) {
                                Text(
                                    text = "+ Fecha actual automática",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.onEvent(EditorUiEvent.SelectTemplate(template)) },
                            enabled = !isSelected,
                            modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Activa")
                            } else {
                                Text("Usar")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateTemplateDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { newTemplate ->
                viewModel.onEvent(EditorUiEvent.SelectTemplate(newTemplate))
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun CreateTemplateDialog(
    onDismiss: () -> Unit,
    onConfirm: (WatermarkTemplate) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var purposeText by remember { mutableStateOf("") }
    var includeDate by remember { mutableStateOf(true) }
    var opacity by remember { mutableFloatStateOf(0.35f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Plantilla") },
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
                        val template = WatermarkTemplate(
                            title = title,
                            config = WatermarkConfig(
                                purposeText = purposeText,
                                includeDate = includeDate,
                                opacity = opacity
                            )
                        )
                        onConfirm(template)
                    }
                },
                enabled = title.isNotBlank() && purposeText.isNotBlank()
            ) {
                Text("Guardar y Usar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
