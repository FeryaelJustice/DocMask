package com.feryaeljustice.docmask.data.repository

import android.graphics.Bitmap
import android.graphics.Rect
import com.feryaeljustice.docmask.core.model.BoundingBox
import com.feryaeljustice.docmask.core.model.toBoundingBox
import com.feryaeljustice.docmask.core.model.DocMaskError
import com.feryaeljustice.docmask.core.model.DocMaskResult
import com.feryaeljustice.docmask.core.model.MaskingOptions
import com.feryaeljustice.docmask.core.model.RedactionArea
import com.feryaeljustice.docmask.core.model.SensitiveFieldType
import com.feryaeljustice.docmask.core.util.BitmapProcessor
import com.feryaeljustice.docmask.data.ml.SensitiveFieldDetector
import com.feryaeljustice.docmask.domain.repository.DocumentProcessorRepository
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DocumentProcessorRepositoryImpl(
    private val bitmapProcessor: BitmapProcessor,
    private val fieldDetector: SensitiveFieldDetector = SensitiveFieldDetector()
) : DocumentProcessorRepository {

    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val faceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
        FaceDetection.getClient(options)
    }

    override suspend fun detectSensitiveRegions(bitmap: Bitmap): DocMaskResult<List<RedactionArea>> =
        withContext(Dispatchers.Default) {
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val detectedAreas = mutableListOf<RedactionArea>()

                // 1. Text Recognition
                val visionText: Text = textRecognizer.process(inputImage).awaitResult()
                val textAreas = fieldDetector.analyzeTextBlocks(visionText)
                detectedAreas.addAll(textAreas)

                // 2. Face Detection
                val faces: List<Face> = faceDetector.process(inputImage).awaitResult()
                for (face in faces) {
                    val box = face.boundingBox.toBoundingBox()
                    detectedAreas.add(
                        RedactionArea(
                            type = SensitiveFieldType.FACE,
                            boundingBox = box,
                            detectedText = "Rostro detectado",
                            isRedacted = false // By default per AEPD, users can toggle redaction on the face
                        )
                    )
                }

                DocMaskResult.Success(detectedAreas)
            } catch (e: Exception) {
                DocMaskResult.Failure(
                    DocMaskError.DetectionFailed(
                        message = "Error en la detección local de campos sensibles: ${e.message}",
                        cause = e
                    )
                )
            }
        }

    override suspend fun applyMasking(
        source: Bitmap,
        areas: List<RedactionArea>,
        options: MaskingOptions
    ): DocMaskResult<Bitmap> = withContext(Dispatchers.Default) {
        try {
            val processed = bitmapProcessor.processDocument(source, areas, options)
            DocMaskResult.Success(processed)
        } catch (e: Exception) {
            DocMaskResult.Failure(
                DocMaskError.ImageProcessingFailed(
                    message = "Error procesando el documento: ${e.message}",
                    cause = e
                )
            )
        }
    }

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }
        addOnFailureListener { exception ->
            if (continuation.isActive) {
                continuation.resumeWithException(exception)
            }
        }
    }
}
