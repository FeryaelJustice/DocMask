# [APROBADO] Plan Tecnico de Arquitectura: Correcciones en Editor, Recorte de Imagenes y Exportacion Unificada

- **Feature vinculada**: `docs/features/editor_fixes_crop_export_enhancements/spec.md`
- **Estado**: [APROBADO]
- **Fecha**: 2026-10-21

---

## 1. Hechos Verificados en el Proyecto (Sin Alucinaciones)
- **Lenguaje / JVM**: Kotlin 2.4.21 / AGP 9.4.1 / compileSdk 37 / targetSdk 37 / minSdk 24
- **Librerias y Versiones comprobadas en libs.versions.toml**:
  - `androidx-compose-bom`: 2026.09.00
  - `androidx-compose-material3`: Integrado en BOM
  - `material3-adaptive-navigation-suite`: 1.4.0
  - `koin-android` / `koin-androidx-compose`: 4.2.2
  - `mlkit-text-recognition`: 16.0.1
  - `mlkit-face-detection`: 16.1.7
  - `kotlinx-coroutines-test`: 1.11.0
  - `turbine`: 1.2.1
  - `junit`: 4.13.2
  - `mockk`: 1.14.11

---

## 2. Impacto Arquitectonico y Capas

### 2.1. Capa Core y de Utilidades (Core / Util Layer)
- **`BitmapProcessor` / `AndroidBitmapProcessor`**:
  - **Optimizacion del algoritmo de pixelado (`applyPixelation`)**:
    - Reemplazar el bucle pixel a pixel `getPixel`/`setPixel` (que tarda segundos y produce bloques microscopicos en fotos de 12+ MP) por un algoritmo rapido y visualmente nitido de mosaico basado en escalado bicúbico/nearest-neighbor sobre los rectángulos seleccionados (`Bitmap.createBitmap` de la sub-region, `Bitmap.createScaledBitmap` a tamano reducido segun `pixelPercent` / resolucion de imagen, y redibujado con `Paint(Paint.FILTER_BITMAP_FLAG = false)`).
    - Mapear dinamicamente `pixelPercent` (10%-100%) a un tamano de bloque efectivo proporcional a las dimensiones del documento (e.g. entre 1.5% y 8% del ancho de la imagen), garantizando que las areas queden 100% ofuscadas y el procesamiento sea casi instantaneo (< 30 ms).
- **`DocumentExporter`**:
  - Unificar las funciones de exportacion:
    - `exportAndShareBitmap`: Exporta 1 imagen (PNG) a cache y directorio SAF si existe.
    - `exportAndShareBitmapsBatch`: Exporta anverso y reverso (2 PNGs).
    - `exportAndShareSinglePdf`: Nueva funcion para exportar una unica imagen como documento PDF de 1 pagina conservando su relacion de aspecto y dimensiones reales.
    - `exportAndSharePdfBatch`: Ya compila anverso y reverso en un PDF de 2 paginas.
  - Abrir carpeta en Ajustes: Helper o Intent para abrir la URI SAF o el explorador publico (`ACTION_VIEW`).

### 2.2. Capa de Dominio (Domain Layer)
- **`MaskingOptions`**:
  - Mantener propiedades inmutables y asegurar que el calculo de pixelado sea coherente con `pixelPercent`.
- **Casos de Uso**:
  - `ApplyMaskingUseCase`: Permanece como contrato reactivo puro para procesar documentos de forma local y offline.

### 2.3. Capa de Presentacion (Presentation / UI Layer)
- **`ImageCropDialog` (Nuevo componente en `presentation/editor/components/ImageCropDialog.kt`)**:
  - Dialog modal en Jetpack Compose para recortar imagenes recien capturadas o seleccionadas.
  - Dibuja la imagen original y un overlay con esquinas/bordes interactivos de arrastre (`detectDragGestures`).
  - Botones "Cancelar" (cierra el dialogo sin cargar nada) y "Aceptar" (extrae el `Bitmap.createBitmap` del area seleccionada y lo emite como `ImageSelected`).
- **`EditorUiState` y `EditorViewModel`**:
  - Estado `pendingCropBitmap: Bitmap?` y `pendingCropSide: DocumentSide?`.
  - Estado `isMaskingPreviewLoading: Boolean`: feedback visual instantaneo en la tarjeta de preview mientras se calcula el bitmap procesado.
  - En `InputMode.SINGLE_PHOTO`: switch "Generar como PDF (PRO)" (`generatePdfForSingleSide: Boolean`).
  - Textos dinamicos del boton de exportacion:
    - 1 cara sin PDF: "Exportar imagen de forma segura".
    - 1 cara con PDF: "Exportar documento seguro".
    - 2 caras sin PDF: "Exportar DNI" (o "Exportar imagenes (2 caras)").
    - 2 caras con PDF: "Exportar documento seguro".
    - PDF: "Exportar documento seguro".
  - Eliminacion del boton secundario `button_download_pdf`.
- **`DocumentViewerPane`**:
  - Spinner/indicador de carga sobre la imagen cuando `isMaskingPreviewLoading == true`.
  - Asignar una altura minima estable (`defaultMinSize(minHeight = 260.dp)`) al contenedor vacio de carga para evitar que se comprima o achate.
- **`EditorControlsPane`**:
  - Adaptabilidad de la fila de campos detectados ("AEPD" y "Desmarcar") usando un layout responsive (`FlowRow` o `Column` en landscape) para evitar desbordamiento.
- **`SettingsScreen`**:
  - Anadir boton "Abrir carpeta" en la fila de acciones de almacenamiento, entre "Cambiar carpeta" y "Restablecer".
- **`TemplatesScreen`**:
  - Corregir insets del sistema pasando `Modifier.padding(padding).consumeWindowInsets(padding)` a la estructura principal.

---

## 3. Estrategia de Testing (Pro Test Battery)
- **Tests Unitarios (`app/src/test/...`)**:
  - `AndroidBitmapProcessorTest`: Verificar que `applyPixelation` produce bitmaps modificados en las zonas de riesgo con rendimiento rapido y que los pixeles de las zonas sensibles difieren del original.
  - `EditorViewModelTest`:
    - Verificar que en modo 1 cara el toggle de PDF altera el estado del boton y genera la exportacion correspondiente.
    - Verificar que el flujo de cancelacion de recorte no modifica los bitmaps existentes.
    - Verificar que al alternar modo PDF con navegacion a Ajustes no se corrompe el estado del documento.
- **Verificacion de Compilacion**:
  - `.\gradlew.bat testDebugUnitTest`
  - `.\gradlew.bat assembleDebug`

---

## 4. Riesgos y Plan de Mitigacion
- **Consumo de memoria con bitmaps grandes en el recortador**: El recortador muestra un bitmap ajustado a escala de pantalla y aplica el recorte en proporciones relativas para recortar el bitmap original o una copia eficiente sin agotar la memoria heap (OOM).
- **Compatibilidad de intents SAF para abrir carpetas**: En Android 11+ (API 30+), abrir una URI de carpeta puede no ser soportado por todos los fabricantes; se implementara captura segura con fallback a abrir la aplicacion de archivos o Descargas.
