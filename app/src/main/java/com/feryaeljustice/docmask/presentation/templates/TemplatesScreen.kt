package com.feryaeljustice.docmask.presentation.templates

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.feryaeljustice.docmask.R
import com.feryaeljustice.docmask.core.model.WatermarkConfig
import com.feryaeljustice.docmask.domain.model.WatermarkTemplate
import com.feryaeljustice.docmask.presentation.editor.EditorUiEvent
import com.feryaeljustice.docmask.presentation.editor.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LocalContextGetResourceValueCall")
@Composable
fun TemplatesScreen(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<WatermarkTemplate?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.templates_title)) },
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
                    if (uiState.availableTemplates.size >= uiState.maxTemplatesLimit) {
                        val max = uiState.maxTemplatesLimit
                        val msg = context.getString(R.string.templates_pro_limit_reached, uiState.availableTemplates.size, max)
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    } else {
                        editingTemplate = null
                        showDialog = true
                    }
                },
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 16.dp)
                    .defaultMinSize(minWidth = 56.dp, minHeight = 56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.templates_create_new))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.templates_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Section 1: Default System Templates
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.templates_filter_all),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${uiState.availableTemplates.size}/${uiState.maxTemplatesLimit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(uiState.defaultTemplates, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    isSelected = uiState.selectedTemplateId == template.id,
                    onClick = { viewModel.onEvent(EditorUiEvent.SelectTemplate(template)) },
                    onLongClick = {
                        editingTemplate = template
                        showDialog = true
                    },
                    onToggleFavorite = { viewModel.onEvent(EditorUiEvent.ToggleFavoriteTemplate(template.id)) }
                )
            }

            // Section 2: Custom User Templates
            item {
                Text(
                    text = stringResource(R.string.templates_filter_custom),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                )
            }

            if (uiState.customTemplates.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.templates_empty_desc),
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
                                contentDescription = stringResource(R.string.common_delete),
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
                        },
                        onToggleFavorite = { viewModel.onEvent(EditorUiEvent.ToggleFavoriteTemplate(template.id)) }
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
    modifier: Modifier = Modifier,
    onToggleFavorite: () -> Unit = {}
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Title + Purpose & Favorite Star Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "\"${template.config.purposeText}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (template.isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                        contentDescription = stringResource(R.string.templates_filter_favorites),
                        tint = if (template.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Row: Badges (Opacity, Include Date) & Selection Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.templates_badge_opacity, (template.config.opacity * 100).toInt()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (template.config.includeDate) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.templates_badge_with_date),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (isSelected) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.templates_default_badge),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onClick,
                        modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.common_apply),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateFormDialog(
    modifier: Modifier = Modifier,
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
        modifier = modifier,
        title = {
            Text(
                text = if (isEditing) stringResource(R.string.templates_edit_title) else stringResource(R.string.editor_save_template_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.editor_template_name_label)) },
                    placeholder = { Text(stringResource(R.string.editor_template_name_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = purposeText,
                    onValueChange = { purposeText = it },
                    label = { Text(stringResource(R.string.editor_watermark_purpose_label)) },
                    placeholder = { Text(stringResource(R.string.editor_watermark_purpose_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.editor_watermark_include_date),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = includeDate,
                        onCheckedChange = { includeDate = it }
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.editor_watermark_opacity_label),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${(opacity * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = opacity,
                        onValueChange = { opacity = it },
                        valueRange = 0.1f..0.8f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}
