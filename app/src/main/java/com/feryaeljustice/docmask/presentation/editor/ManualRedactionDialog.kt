package com.feryaeljustice.docmask.presentation.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.feryaeljustice.docmask.R
import com.feryaeljustice.docmask.core.model.BoundingBox
import com.feryaeljustice.docmask.core.model.DocumentSide
import com.feryaeljustice.docmask.core.model.RedactionArea
import com.feryaeljustice.docmask.core.model.SensitiveFieldType
import com.feryaeljustice.docmask.core.util.AndroidBitmapProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import androidx.core.graphics.scale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualRedactionDialog(
    originalBitmap: Bitmap,
    existingRegions: List<RedactionArea>,
    side: DocumentSide,
    onDismiss: () -> Unit,
    onSave: (List<RedactionArea>) -> Unit,
    modifier: Modifier = Modifier
) {
    var manualRegions by remember {
        mutableStateOf(existingRegions.filter { it.type == SensitiveFieldType.CUSTOM_SELECTION })
    }
    val aiRegions = remember {
        existingRegions.filter { it.type != SensitiveFieldType.CUSTOM_SELECTION }
    }

    var showFullScreenPreview by remember { mutableStateOf(false) }

    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragCurrent by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    // Real-time small downscaled preview without watermark or grayscale
    val previewThumbnail by produceState<Bitmap?>(initialValue = null, manualRegions, originalBitmap) {
        value = withContext(Dispatchers.Default) {
            val maxDim = 240f
            val scaleFactor = min(maxDim / originalBitmap.width, maxDim / originalBitmap.height).coerceAtMost(1f)
            val thumbW = (originalBitmap.width * scaleFactor).toInt().coerceAtLeast(1)
            val thumbH = (originalBitmap.height * scaleFactor).toInt().coerceAtLeast(1)
            val thumb = originalBitmap.scale(thumbW, thumbH)

            val activeRegions = (aiRegions.filter { it.isRedacted } + manualRegions)
            val scaledBoxes = activeRegions.map { region ->
                BoundingBox(
                    left = (region.boundingBox.left * scaleFactor).coerceIn(0f, thumbW.toFloat()),
                    top = (region.boundingBox.top * scaleFactor).coerceIn(0f, thumbH.toFloat()),
                    right = (region.boundingBox.right * scaleFactor).coerceIn(0f, thumbW.toFloat()),
                    bottom = (region.boundingBox.bottom * scaleFactor).coerceIn(0f, thumbH.toFloat())
                )
            }
            AndroidBitmapProcessor().applyPixelation(thumb, scaledBoxes, blockSize = 10)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Draw,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.manual_dialog_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = stringResource(R.string.manual_dialog_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close))
                    }
                }

                // Interactive Canvas Container
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val density = LocalDensity.current.density
                        val imgW = originalBitmap.width.toFloat()
                        val imgH = originalBitmap.height.toFloat()
                        val aspect = imgW / imgH

                        val maxAvailableW = maxWidth.value
                        val maxAvailableH = 360f

                        val (canvasW, canvasH) = if (maxAvailableW / aspect <= maxAvailableH) {
                            maxAvailableW to (maxAvailableW / aspect)
                        } else {
                            (maxAvailableH * aspect) to maxAvailableH
                        }

                        val canvasWidthPx = canvasW * density
                        val canvasHeightPx = canvasH * density

                        val imageBitmap = remember(originalBitmap) { originalBitmap.asImageBitmap() }

                        Canvas(
                            modifier = Modifier
                                .size(canvasW.dp, canvasH.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .pointerInput(canvasWidthPx, canvasHeightPx, originalBitmap) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val cx = offset.x.coerceIn(0f, canvasWidthPx)
                                            val cy = offset.y.coerceIn(0f, canvasHeightPx)
                                            dragStart = Offset(cx, cy)
                                            dragCurrent = Offset(cx, cy)
                                            isDragging = true
                                        },
                                        onDrag = { change, _ ->
                                            val cx = change.position.x.coerceIn(0f, canvasWidthPx)
                                            val cy = change.position.y.coerceIn(0f, canvasHeightPx)
                                            dragCurrent = Offset(cx, cy)
                                        },
                                        onDragEnd = {
                                            val start = dragStart
                                            val current = dragCurrent
                                            if (start != null && current != null) {
                                                val leftPx = min(start.x, current.x)
                                                val topPx = min(start.y, current.y)
                                                val rightPx = max(start.x, current.x)
                                                val bottomPx = max(start.y, current.y)

                                                val scaleX = originalBitmap.width.toFloat() / canvasWidthPx
                                                val scaleY = originalBitmap.height.toFloat() / canvasHeightPx

                                                val bmpLeft = (leftPx * scaleX).coerceIn(0f, originalBitmap.width.toFloat())
                                                val bmpTop = (topPx * scaleY).coerceIn(0f, originalBitmap.height.toFloat())
                                                val bmpRight = (rightPx * scaleX).coerceIn(0f, originalBitmap.width.toFloat())
                                                val bmpBottom = (bottomPx * scaleY).coerceIn(0f, originalBitmap.height.toFloat())

                                                if ((bmpRight - bmpLeft) >= 12f && (bmpBottom - bmpTop) >= 12f) {
                                                    val newArea = RedactionArea(
                                                        type = SensitiveFieldType.CUSTOM_SELECTION,
                                                        boundingBox = BoundingBox(bmpLeft, bmpTop, bmpRight, bmpBottom),
                                                        isRedacted = true
                                                    )
                                                    manualRegions = manualRegions + newArea
                                                }
                                            }
                                            dragStart = null
                                            dragCurrent = null
                                            isDragging = false
                                        },
                                        onDragCancel = {
                                            dragStart = null
                                            dragCurrent = null
                                            isDragging = false
                                        }
                                    )
                                }
                        ) {
                            // 1. Draw source photo
                            drawImage(
                                image = imageBitmap,
                                dstOffset = IntOffset.Zero,
                                dstSize = IntSize(size.width.toInt(), size.height.toInt())
                            )

                            val scaleToCanvasX = size.width / originalBitmap.width.toFloat()
                            val scaleToCanvasY = size.height / originalBitmap.height.toFloat()

                            // 2. Draw existing AI detected areas (blue dashed outline)
                            for (ai in aiRegions) {
                                val left = ai.boundingBox.left * scaleToCanvasX
                                val top = ai.boundingBox.top * scaleToCanvasY
                                val w = (ai.boundingBox.right - ai.boundingBox.left) * scaleToCanvasX
                                val h = (ai.boundingBox.bottom - ai.boundingBox.top) * scaleToCanvasY

                                drawRect(
                                    color = Color(0x332196F3),
                                    topLeft = Offset(left, top),
                                    size = Size(w, h)
                                )
                                drawRect(
                                    color = Color(0xFF1976D2),
                                    topLeft = Offset(left, top),
                                    size = Size(w, h),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }

                            // 3. Draw manual regions (orange/tertiary fill and border)
                            for (man in manualRegions) {
                                val left = man.boundingBox.left * scaleToCanvasX
                                val top = man.boundingBox.top * scaleToCanvasY
                                val w = (man.boundingBox.right - man.boundingBox.left) * scaleToCanvasX
                                val h = (man.boundingBox.bottom - man.boundingBox.top) * scaleToCanvasY

                                drawRect(
                                    color = Color(0x66FF9800),
                                    topLeft = Offset(left, top),
                                    size = Size(w, h)
                                )
                                drawRect(
                                    color = Color(0xFFF57C00),
                                    topLeft = Offset(left, top),
                                    size = Size(w, h),
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }

                            // 4. Draw actively dragging rectangle
                            val start = dragStart
                            val current = dragCurrent
                            if (isDragging && start != null && current != null) {
                                val left = min(start.x, current.x)
                                val top = min(start.y, current.y)
                                val w = max(start.x, current.x) - left
                                val h = max(start.y, current.y) - top

                                drawRect(
                                    color = Color(0x44FF5722),
                                    topLeft = Offset(left, top),
                                    size = Size(w, h)
                                )
                                drawRect(
                                    color = Color(0xFFFF5722),
                                    topLeft = Offset(left, top),
                                    size = Size(w, h),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }
                }

                // Controls row: Undo, Clear, Counter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                if (manualRegions.isNotEmpty()) {
                                    manualRegions = manualRegions.dropLast(1)
                                }
                            },
                            enabled = manualRegions.isNotEmpty(),
                            modifier = Modifier.testTag("button_undo_manual_region")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.manual_dialog_undo))
                        }

                        OutlinedButton(
                            onClick = { manualRegions = emptyList() },
                            enabled = manualRegions.isNotEmpty(),
                            modifier = Modifier.testTag("button_clear_manual_regions")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.manual_dialog_clear))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = stringResource(R.string.manual_dialog_regions_count, manualRegions.size),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Small Preview Box (Downscaled, with applied manual pixelation)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    @OptIn(ExperimentalFoundationApi::class)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = {
                                    // Hint to user
                                },
                                onLongClick = {
                                    if (previewThumbnail != null) {
                                        showFullScreenPreview = true
                                    }
                                }
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (previewThumbnail != null) {
                            Image(
                                bitmap = previewThumbnail!!.asImageBitmap(),
                                contentDescription = stringResource(R.string.editor_manual_quick_preview_content_description),
                                modifier = Modifier
                                    .size(width = 110.dp, height = 75.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(width = 110.dp, height = 75.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.editor_manual_quick_preview_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.editor_manual_quick_preview_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (showFullScreenPreview && previewThumbnail != null) {
                    FullScreenImagePreviewDialog(
                        bitmap = previewThumbnail!!,
                        onDismiss = { showFullScreenPreview = false }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Confirm and Cancel buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(stringResource(R.string.common_cancel))
                    }

                    Button(
                        onClick = { onSave(manualRegions) },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("button_save_manual_regions")
                    ) {
                        Text(stringResource(R.string.manual_dialog_save))
                    }
                }
            }
        }
    }
}
