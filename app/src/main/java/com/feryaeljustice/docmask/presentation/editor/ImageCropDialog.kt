package com.feryaeljustice.docmask.presentation.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.feryaeljustice.docmask.R

private data class CropSelection(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val viewW: Float,
    val viewH: Float
)

/**
 * 100% Offline native Compose image cropping dialog.
 * Allows the user to frame their document before OCR and redaction.
 */
@Composable
fun ImageCropDialog(
    sourceBitmap: Bitmap,
    onDismiss: () -> Unit,
    onCropConfirmed: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    var cropSelection by remember { mutableStateOf<CropSelection?>(null) }

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
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Crop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.crop_dialog_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.crop_dialog_instructions),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cropper Area
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val density = LocalDensity.current.density
                        val imgW = sourceBitmap.width.toFloat()
                        val imgH = sourceBitmap.height.toFloat()
                        val imgAspect = imgW / imgH

                        val maxBoxW = maxWidth.value
                        val maxBoxH = maxHeight.value

                        val (viewW, viewH) = if (maxBoxW / imgAspect <= maxBoxH) {
                            maxBoxW to (maxBoxW / imgAspect)
                        } else {
                            (maxBoxH * imgAspect) to maxBoxH
                        }

                        val viewWidthPx = viewW * density
                        val viewHeightPx = viewH * density

                        // Crop rect normalized coordinates in view space (px)
                        var cropLeft by remember { mutableFloatStateOf(viewWidthPx * 0.05f) }
                        var cropTop by remember { mutableFloatStateOf(viewHeightPx * 0.05f) }
                        var cropRight by remember { mutableFloatStateOf(viewWidthPx * 0.95f) }
                        var cropBottom by remember { mutableFloatStateOf(viewHeightPx * 0.95f) }

                        SideEffect {
                            cropSelection = CropSelection(cropLeft, cropTop, cropRight, cropBottom, viewWidthPx, viewHeightPx)
                        }

                        val minCropSize = 48f * density
                        val handleRadius = 14f * density

                        val imageBitmap = remember(sourceBitmap) { sourceBitmap.asImageBitmap() }

                        // State for tracking which handle or region is being dragged
                        var activeDragMode by remember { mutableFloatStateOf(0f) }
                        // 1: Top-Left, 2: Top-Right, 3: Bottom-Left, 4: Bottom-Right, 5: Move Inside

                        Canvas(
                            modifier = Modifier
                                .size(viewW.dp, viewH.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .pointerInput(viewWidthPx, viewHeightPx) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val x = offset.x
                                            val y = offset.y
                                            val hitThreshold = handleRadius * 2.5f

                                            val distTL = kotlin.math.hypot(x - cropLeft, y - cropTop)
                                            val distTR = kotlin.math.hypot(x - cropRight, y - cropTop)
                                            val distBL = kotlin.math.hypot(x - cropLeft, y - cropBottom)
                                            val distBR = kotlin.math.hypot(x - cropRight, y - cropBottom)

                                            activeDragMode = when {
                                                distTL <= hitThreshold -> 1f
                                                distTR <= hitThreshold -> 2f
                                                distBL <= hitThreshold -> 3f
                                                distBR <= hitThreshold -> 4f
                                                x in cropLeft..cropRight && y in cropTop..cropBottom -> 5f
                                                else -> 0f
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val dx = dragAmount.x
                                            val dy = dragAmount.y

                                            when (activeDragMode) {
                                                1f -> { // Top-Left
                                                    cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - minCropSize)
                                                    cropTop = (cropTop + dy).coerceIn(0f, cropBottom - minCropSize)
                                                }
                                                2f -> { // Top-Right
                                                    cropRight = (cropRight + dx).coerceIn(cropLeft + minCropSize, viewWidthPx)
                                                    cropTop = (cropTop + dy).coerceIn(0f, cropBottom - minCropSize)
                                                }
                                                3f -> { // Bottom-Left
                                                    cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - minCropSize)
                                                    cropBottom = (cropBottom + dy).coerceIn(cropTop + minCropSize, viewHeightPx)
                                                }
                                                4f -> { // Bottom-Right
                                                    cropRight = (cropRight + dx).coerceIn(cropLeft + minCropSize, viewWidthPx)
                                                    cropBottom = (cropBottom + dy).coerceIn(cropTop + minCropSize, viewHeightPx)
                                                }
                                                5f -> { // Move entire box
                                                    val width = cropRight - cropLeft
                                                    val height = cropBottom - cropTop
                                                    val newLeft = (cropLeft + dx).coerceIn(0f, viewWidthPx - width)
                                                    val newTop = (cropTop + dy).coerceIn(0f, viewHeightPx - height)
                                                    cropLeft = newLeft
                                                    cropTop = newTop
                                                    cropRight = newLeft + width
                                                    cropBottom = newTop + height
                                                }
                                            }
                                        },
                                        onDragEnd = { activeDragMode = 0f },
                                        onDragCancel = { activeDragMode = 0f }
                                    )
                                }
                        ) {
                            // 1. Draw source document image
                            drawImage(
                                image = imageBitmap,
                                dstOffset = IntOffset.Zero,
                                dstSize = IntSize(size.width.toInt(), size.height.toInt())
                            )

                            // 2. Draw darkened outer mask
                            val cropRect = Rect(cropLeft, cropTop, cropRight, cropBottom)
                            val path = Path().apply {
                                addRect(Rect(0f, 0f, size.width, size.height))
                            }
                            clipPath(path) {
                                clipPath(Path().apply { addRect(cropRect) }, clipOp = ClipOp.Difference) {
                                    drawRect(Color.Black.copy(alpha = 0.55f))
                                }
                            }

                            // 3. Draw crop border
                            drawRect(
                                color = Color.White,
                                topLeft = Offset(cropLeft, cropTop),
                                size = Size(cropRight - cropLeft, cropBottom - cropTop),
                                style = Stroke(width = 3.dp.toPx())
                            )

                            // 4. Draw corner handles
                            val handleColor = Color(0xFF2196F3)
                            drawCircle(color = handleColor, radius = handleRadius * 0.7f, center = Offset(cropLeft, cropTop))
                            drawCircle(color = handleColor, radius = handleRadius * 0.7f, center = Offset(cropRight, cropTop))
                            drawCircle(color = handleColor, radius = handleRadius * 0.7f, center = Offset(cropLeft, cropBottom))
                            drawCircle(color = handleColor, radius = handleRadius * 0.7f, center = Offset(cropRight, cropBottom))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons
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
                        Text(stringResource(R.string.crop_dialog_cancel))
                    }

                    Button(
                        onClick = {
                            val sel = cropSelection
                            if (sel != null && sel.viewW > 0 && sel.viewH > 0) {
                                try {
                                    val scaleX = sourceBitmap.width.toFloat() / sel.viewW
                                    val scaleY = sourceBitmap.height.toFloat() / sel.viewH

                                    val leftPx = (sel.left * scaleX).toInt().coerceIn(0, sourceBitmap.width - 1)
                                    val topPx = (sel.top * scaleY).toInt().coerceIn(0, sourceBitmap.height - 1)
                                    val rightPx = (sel.right * scaleX).toInt().coerceIn(leftPx + 1, sourceBitmap.width)
                                    val bottomPx = (sel.bottom * scaleY).toInt().coerceIn(topPx + 1, sourceBitmap.height)

                                    val cropW = rightPx - leftPx
                                    val cropH = bottomPx - topPx

                                    val cropped = Bitmap.createBitmap(sourceBitmap, leftPx, topPx, cropW, cropH)
                                    onCropConfirmed(cropped)
                                } catch (e: Exception) {
                                    onCropConfirmed(sourceBitmap)
                                }
                            } else {
                                onCropConfirmed(sourceBitmap)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("button_confirm_crop")
                    ) {
                        Text(stringResource(R.string.crop_dialog_apply))
                    }
                }
            }
        }
    }
}
