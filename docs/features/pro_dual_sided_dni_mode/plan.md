# [APROBADO] Plan Tecnico de Arquitectura: Modo Pro Doble Cara DNI (Anverso y Reverso con Exportacion en Lote)

- **Feature vinculada**: [`spec.md`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/docs/features/pro_dual_sided_dni_mode/spec.md)
- **Estado**: [APROBADO]
- **Fecha**: 2026-10-19
- **Directrices aplicadas**: SDMD ([mobile_guidelines.md](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/docs/mobile_guidelines.md))

---

## 1. Hechos Verificados en el Proyecto (Sin Alucinaciones)
- **Lenguaje / JVM**: Kotlin 2.4.21 / Java 11 (target)
- **Framework de UI**: Jetpack Compose con Compose BOM `2026.09.00` y Material 3 (incluye `PrimaryTabRow`, `Tab`, `Icon`, `Badge`, `Card`, `Switch`)
- **Procesamiento de Archivos y Compartir**:
  - `androidx.core.content.FileProvider` ya configurado en AndroidManifest.xml (`${applicationId}.fileprovider`)
  - `Intent.ACTION_SEND_MULTIPLE` disponible en Android SDK estándar desde API 1 para compartir múltiples URIs de imagen en lote
- **Dependencias probadas en libs.versions.toml**:
  - Google ML Kit Text Recognition (`16.0.1`) y Face Detection (`16.1.7`)
  - Koin `4.2.2`, Turbine `1.2.1`, MockK `1.14.11`, Coroutines Test `1.11.0`

---

## 2. Impacto Arquitectonico y Capas

### 2.1. Capa Core y Utilidades
- **Modelo de Lado de Documento**:
  - Crear [`DocumentSide.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/core/model/DocumentSide.kt):
    ```kotlin
    enum class DocumentSide(val displayName: String) {
        FRONT("Anverso"),
        BACK("Reverso")
    }
    ```
- **Exportador por Lote ([`DocumentExporter.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/core/util/DocumentExporter.kt))**:
  - Incorporar método `exportAndShareBitmapsBatch(context: Context, frontBitmap: Bitmap, backBitmap: Bitmap): Result<List<Uri>>`:
    - Guarda `${baseTitle}_anverso_${timestamp}.png` y `${baseTitle}_reverso_${timestamp}.png` en el directorio de caché de exportación.
    - Genera ambas URIs seguras con FileProvider.
    - Lanza `Intent(Intent.ACTION_SEND_MULTIPLE)` con `putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(uriFront, uriBack))` y tipo `"image/png"`.

### 2.2. Capa de Presentacion (Presentation / UI Layer)
- **Estado de UI ([`EditorUiState.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/editor/EditorUiState.kt))**:
  - `val isDualSidedMode: Boolean = false`
  - `val activeSide: DocumentSide = DocumentSide.FRONT`
  - `val frontOriginalBitmap: Bitmap? = null`
  - `val frontMaskedBitmap: Bitmap? = null`
  - `val frontDetectedRegions: List<RedactionArea> = emptyList()`
  - `val backOriginalBitmap: Bitmap? = null`
  - `val backMaskedBitmap: Bitmap? = null`
  - `val backDetectedRegions: List<RedactionArea> = emptyList()`
  - Propiedades computadas:
    - `val currentOriginalBitmap: Bitmap? get() = if (activeSide == DocumentSide.FRONT) frontOriginalBitmap else backOriginalBitmap`
    - `val currentMaskedBitmap: Bitmap? get() = if (activeSide == DocumentSide.FRONT) frontMaskedBitmap else backMaskedBitmap`
    - `val currentDetectedRegions: List<RedactionArea> get() = if (activeSide == DocumentSide.FRONT) frontDetectedRegions else backDetectedRegions`
    - `val hasFront: Boolean get() = frontOriginalBitmap != null`
    - `val hasBack: Boolean get() = backOriginalBitmap != null`
    - `val isMissingSideInDualMode: Boolean get() = isDualSidedMode && (!hasFront || !hasBack)`
- **Eventos de UI ([`EditorUiEvent.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/editor/EditorUiState.kt))**:
  - `data class ToggleDualSidedMode(val enabled: Boolean) : EditorUiEvent`
  - `data class SelectDocumentSide(val side: DocumentSide) : EditorUiEvent`
  - `data class ImageSelected(val bitmap: Bitmap, val targetSide: DocumentSide? = null) : EditorUiEvent`
- **ViewModel ([`EditorViewModel.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/editor/EditorViewModel.kt))**:
  - Al cambiar `ToggleDualSidedMode(false)`: sincroniza `frontOriginalBitmap` en el documento principal y conserva la imagen del Anverso.
  - Al cambiar `SelectDocumentSide(side)`: actualiza `activeSide`.
  - Al recibir `ImageSelected`: detecta regiones con ML Kit y renderiza la máscara para la cara indicada (`activeSide`).
  - Al modificar parámetros de marca de agua o escala de grises: re-renderiza simultáneamente ambas caras si están cargadas.
- **Pantalla Compose ([`EditorScreen.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/editor/EditorScreen.kt))**:
  - Tarjeta de activación Pro: Visible en `EditorControlsPane` cuando `uiState.isProUser == true`. Incluye Switch con aclaración de responsabilidad para DNI.
  - Visor multicara: En `DocumentViewerPane`, si `isDualSidedMode == true`, incluye `PrimaryTabRow` superior con pestañas "Anverso" y "Reverso" y distintivos visuales (indicando si la cara ya tiene imagen o está vacía).
  - Botón de Exportar:
    - Si `isDualSidedMode` y falta alguna cara: dispara un Toast o mensaje informativo de advertencia ("Falta adjuntar el [reverso/anverso]").
    - Si ambas caras están presentes: ejecuta `exportAndShareBitmapsBatch`.

---

## 3. Estrategia de Testing (Pro Test Battery)
- **Tests Unitarios**:
  - [`EditorViewModelTest.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/test/java/com/feryaeljustice/docmask/presentation/editor/EditorViewModelTest.kt):
    - `givenDualSidedModeEnabled_whenBackSelected_thenActiveSideIsBack()`
    - `givenImagesLoadedOnBothSides_whenDualModeDisabled_thenPreservesFrontImage()`
    - `givenWatermarkConfigChanged_thenReappliesMaskingToBothFrontAndBack()`
    - `givenMissingBackImage_whenExportAttempted_thenBlocksExport()`
  - [`DocumentExporterTest.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/test/java/com/feryaeljustice/docmask/core/util/DocumentExporterTest.kt):
    - Comprobación de generación de archivos PNG diferenciados (`_anverso` y `_reverso`).
- **Validacion de Build**:
  - `./gradlew.bat testDebugUnitTest`
  - `./gradlew.bat assembleDebug`

---

## 4. Riesgos y Plan de Mitigacion
- **Riesgo**: Consumo excesivo de memoria RAM al almacenar 4 bitmaps simultáneos (2 originales + 2 procesados).
  - **Mitigacion**: Reescalado preventivo en el procesamiento si las imágenes superan 2048px, o reutilización eficiente de canvas en `BitmapProcessor`.
