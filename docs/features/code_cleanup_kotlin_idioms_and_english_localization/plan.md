# [APROBADO] Plan Tecnico de Arquitectura: Limpieza de Codigo, Idiomatica Kotlin y Localizacion al Ingles

- **Feature vinculada**: `docs/features/code_cleanup_kotlin_idioms_and_english_localization/spec.md`
- **Estado**: [APROBADO]
- **Fecha**: 2026-10-21

---

## 1. Hechos Verificados en el Proyecto (Sin Alucinaciones)

- **Lenguaje / JVM**: Kotlin 2.4.21, AGP 9.4.1, compileSdk/targetSdk 37, minSdk 24.
- **Librerias verificadas en libs.versions.toml**:
  - `androidx-core-ktx`: 1.19.1
  - `compose-bom`: 2026.09.00
  - `koin`: 4.2.2
  - `mlkit-text-recognition`: 16.0.1
  - `mlkit-face-detection`: 16.1.7
  - `navigation-compose`: 2.10.2
  - `material3-adaptive-nav-suite`: 1.4.0
  - `datastore`: 1.2.1
  - `biometric`: 1.1.0
  - `billing`: 9.1.0

---

## 2. Impacto Arquitectonico y Capas

### 2.1. Capa Core y Utilidades (`core/`)
- **Limpieza de codigo muerto**:
  - En `CameraCaptureHelper.kt`: eliminar `createCameraTempUri` no utilizado.
  - En `DocumentExporter.kt`: eliminar `savePdfToStorage` no utilizado.
  - En `core/model/PermissionModels.kt`: eliminar `AppPermissionStatus.NOT_REQUIRED`.
  - En `res/values/colors.xml`: purgar colores no referenciados (`purple_200`, `purple_500`, etc.).
- **Idiomatica Kotlin y KTX**:
  - En `BitmapProcessor.kt`:
    - Sustituir `Bitmap.createScaledBitmap` por extension KTX `subBitmap.scale(width, height, filter = false)`.
    - Usar operadores de indexacion KTX `output[x, y]` y `output[x, y] = value` en lugar de `getPixel`/`setPixel`.
    - Usar `Canvas.withRotation(degrees, px, py) { ... }` para la rotacion de marca de agua.
    - Usar `createBitmap(width, height)` en lugar de `Bitmap.createBitmap`.
  - En `PdfDocumentManager.kt`:
    - Reemplazar instanciacion de bitmap por funcion KTX `createBitmap(width, height)`.
  - En `DocumentExporter.kt`, `SettingsScreen.kt`, `ValidateCustomStorageUriUseCase.kt`:
    - Usar `uriString.toUri()` de KTX en lugar de `Uri.parse(uriString)`.
  - En `core/model/BoundingBox.kt`:
    - Definir funciones de extension e interop: `Rect.toBoundingBox()`, `BoundingBox.toRectF()`, `BoundingBox.toRect()`, `operator fun contains(point: Offset)`.
  - En `core/model/DocMaskResult.kt`:
    - Anadir funciones de orden superior idiomaticas: `inline fun <T> DocMaskResult<T>.onSuccess(...)`, `inline fun <T> DocMaskResult<T>.onFailure(...)`.
  - En `core/model/SensitiveFieldType.kt`:
    - Anadir `@StringRes val titleRes: Int`.
    - Definir extension `fun SensitiveFieldType.getLabel(context: Context): String` y `@Composable fun SensitiveFieldType.getLabel(): String`.

### 2.2. Capa de Dominio y Datos (`domain/` y `data/`)
- En `SensitiveFieldDetector.kt` y `DocumentProcessorRepositoryImpl.kt`:
  - Reutilizar la funcion de extension centralizada `Rect.toBoundingBox()` y eliminar las funciones privadas duplicadas.
- En `TemplateRepositoryImpl.kt`:
  - Los titulos y textos de proposito de plantillas por defecto se inicializan o exponen vinculados a claves de recursos string para permitir que se adapten al idioma activo del dispositivo.

### 2.3. Capa de Presentacion (`presentation/`)
- En `EditorUiState.kt` y `EditorUiEvent.kt`:
  - Eliminar propiedades obsoletas no leidas: `isPdfDownloadAvailable`, `exportSuccessMessage`.
  - Eliminar eventos obsoletos: `EditorUiEvent.DownloadPdf`, `EditorUiEvent.DismissSuccessMessage`.
  - Migrar `object` sellados a `data object`: `data object ApplyAepdPreset`, `data object RestorePurchases`, `data object ExportDocument`, `data object DismissError`.
- En `EditorViewModel.kt`:
  - Eliminar metodos muertos correspondientes: `onDownloadPdf()`.
- En componentes Compose (`EditorScreen.kt`, `TemplatesScreen.kt`, `SettingsScreen.kt`, `ProScreen.kt`, `LockScreen.kt`, `ImageCropDialog.kt`, `ManualRedactionDialog.kt`):
  - Corregir advertencias de Compose Lint: asegurar que `modifier: Modifier = Modifier` sea el primer parametro opcional.
  - Reemplazar todas las cadenas de texto en duro por `stringResource(R.string.<id>)`.
  - Manejar cadenas dinamicas con formato (`%d`, `%s`, etc.).

### 2.4. Recursos y Localizacion (`res/values/`)
- `res/values/strings.xml`: Fuente de verdad en **Espanol** (idioma por defecto). Centraliza todos los textos de la app estructurados por modulos (Comun, Editor, Mascaras, Plantillas, Ajustes, Pro, Bloqueo, Errores).
- `res/values-en/strings.xml`: Archivo creado con todas las claves traducidas fielmente al **Ingles**.

---

## 3. Estrategia de Testing (Pro Test Battery)

- **Compilacion y Lint**:
  - Ejecutar `./gradlew.bat compileDebugKotlin` para asegurar ausencia de errores de tipos.
  - Ejecutar `./gradlew.bat lintDebug` para verificar la resolucion de todas las advertencias `UseKtx`, `UnusedResources` y `ModifierParameter`.
- **Tests Unitarios**:
  - Ejecutar `./gradlew.bat testDebugUnitTest` validando que todos los tests existentes pasen sin regresiones.
  - Asegurar que `EditorViewModelTest` no dependa de campos eliminados y cubra los nuevos flujos.

---

## 4. Riesgos y Plan de Mitigacion

- **Riesgo**: Claves de strings faltantes o no coincidentes entre `values/strings.xml` y `values-en/strings.xml`.
  - **Mitigacion**: Verificacion con Android Lint (`MissingTranslation` check) que garantiza paridad 1:1 de claves entre ambos idiomas.
- **Riesgo**: Regresion en renderizado de bitmaps al usar KTX `scale` o `withRotation`.
  - **Mitigacion**: Los algoritmos matematicos y de escalado se mantienen intactos, delegando a la implementacion optimizada de KTX de Google (`androidx.core.graphics`).
