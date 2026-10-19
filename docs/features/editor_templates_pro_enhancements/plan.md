# [APROBADO] Plan Tecnico de Arquitectura: Ajustes de Opacidad en Editor, CRUD de Plantillas y Toggle Debug Pro

- **Feature vinculada**: [`spec.md`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/docs/features/editor_templates_pro_enhancements/spec.md)
- **Estado**: [APROBADO]
- **Fecha**: 2026-10-19
- **Directrices aplicadas**: SDMD ([mobile_guidelines.md](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/docs/mobile_guidelines.md))

---

## 1. Hechos Verificados en el Proyecto (Sin Alucinaciones)
- **Lenguaje / JVM**: Kotlin 2.4.21 / Java 11
- **Compilacion y Plugins verificados en `build.gradle.kts`**:
  - Android Gradle Plugin: `9.4.1`
  - Kotlin Compose Plugin: `2.4.21`
  - `minSdk = 24`, `targetSdk = 37`, `compileSdk = 37`
  - Habilitacion requerida: `buildFeatures { compose = true; buildConfig = true }`
- **Librerias verificadas en `libs.versions.toml`**:
  - `composeBom`: `2026.09.00`
  - `material3`: `androidx.compose.material3` (incluye `SwipeToDismissBox`, `rememberSwipeToDismissBoxState`, `SwipeToDismissBoxValue`)
  - `koin`: `4.2.2` (`koin-android`, `koin-androidx-compose`)
  - `billing`: `9.1.0` (`play-billing-ktx`)
  - `test`: JUnit 4.13.2, MockK 1.14.11, Turbine 1.2.1, Coroutines Test 1.11.0

---

## 2. Impacto Arquitectonico y Capas

### 2.1. Capa de Datos (Data Layer)
- **Contratos y Repositorios**:
  - [`TemplateRepository.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/domain/repository/TemplateRepository.kt):
    - Mantiene `getTemplates(): Flow<List<WatermarkTemplate>>`
    - Mantiene `saveTemplate(template: WatermarkTemplate)` (actualiza o inserta segun ID)
    - Mantiene `deleteTemplate(templateId: String)` (borra solo si `!isDefault`)
  - [`BillingRepository.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/domain/repository/BillingRepository.kt):
    - Anadir contrato formal: `fun setDebugProStatus(isPro: Boolean)`
  - [`BillingRepositoryImpl.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/data/repository/BillingRepositoryImpl.kt):
    - Implementar `setDebugProStatus(isPro: Boolean)` actualizando `_isProUser.value = isPro`.

### 2.2. Capa de Dominio (Domain Layer)
- **Casos de Uso (Use Cases)**:
  - Crear [`SaveWatermarkTemplateUseCase.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/domain/usecase/SaveWatermarkTemplateUseCase.kt):
    - `operator suspend fun invoke(template: WatermarkTemplate)` delega en `templateRepository.saveTemplate(template)`.
  - Crear [`DeleteWatermarkTemplateUseCase.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/domain/usecase/DeleteWatermarkTemplateUseCase.kt):
    - `operator suspend fun invoke(templateId: String)` delega en `templateRepository.deleteTemplate(templateId)`.
  - Inyeccion en [`AppModule.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/di/AppModule.kt).

### 2.3. Capa de Presentacion (Presentation / UI Layer)
- **Estado de UI ([`EditorUiState.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/editor/EditorUiState.kt))**:
  - Propiedades computadas de conveniencia:
    - `val defaultTemplates: List<WatermarkTemplate> get() = availableTemplates.filter { it.isDefault }`
    - `val customTemplates: List<WatermarkTemplate> get() = availableTemplates.filter { !it.isDefault }`
- **Eventos de UI ([`EditorUiEvent.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/editor/EditorUiState.kt))**:
  - `data class UpdateWatermarkOpacity(val opacity: Float) : EditorUiEvent`
  - `data class SaveTemplate(val template: WatermarkTemplate) : EditorUiEvent`
  - `data class DeleteTemplate(val templateId: String) : EditorUiEvent`
  - `data class ToggleDebugPro(val enabled: Boolean) : EditorUiEvent`
- **ViewModel ([`EditorViewModel.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/editor/EditorViewModel.kt))**:
  - Procesar `UpdateWatermarkOpacity`: actualiza `maskingOptions.watermarkConfig.copy(opacity = opacity)` y ejecuta `reapplyMasking()`.
  - Procesar `SaveTemplate`: invoca `saveWatermarkTemplateUseCase(template)` y sincroniza la configuracion activa si corresponde.
  - Procesar `DeleteTemplate`: invoca `deleteWatermarkTemplateUseCase(templateId)`.
  - Procesar `ToggleDebugPro`: llama a `billingRepository.setDebugProStatus(enabled)`.
- **UI en Jetpack Compose**:
  - [`EditorScreen.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/editor/EditorScreen.kt):
    - Anadir control deslizante de opacidad (`Slider`) en `EditorControlsPane` con etiqueta `Opacidad de marca: ${(opacity * 100).toInt()}%`.
    - Sincronizacion inmediata con el estado reactivo.
  - [`TemplatesScreen.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/templates/TemplatesScreen.kt):
    - Agrupacion en `LazyColumn`:
      1. Seccion "Plantillas Predeterminadas" con sus items.
      2. Seccion "Plantillas Personalizadas" con sus items.
    - Interaccion tactil con `combinedClickable`:
      - `onClick`: selecciona la plantilla (`SelectTemplate`).
      - `onLongClick`: abre dialogo de edicion con datos de la plantilla precargados.
    - Soporte de borrado con `SwipeToDismissBox`:
      - Habilitado solo para plantillas personalizadas (`!isDefault`).
      - Fondo rojo con icono de basura (`Icons.Default.Delete`).
      - Al superar el umbral hacia la izquierda (`EndToStart`), emite `DeleteTemplate(template.id)`.
    - Dialogo unificado `TemplateFormDialog`:
      - Soporta tanto creacion (`templateToEdit == null`) como edicion (`templateToEdit != null`).
  - [`SettingsScreen.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/settings/SettingsScreen.kt):
    - Dentro de la tarjeta de Pro, verificar `if (BuildConfig.DEBUG)`.
    - Renderizar fila de desarrollo con `Switch` para activar/desactivar `isProUser` inmediatamente.

---

## 3. Estrategia de Testing (Pro Test Battery)
- **Tests Unitarios**:
  - [`EditorViewModelTest.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/test/java/com/feryaeljustice/docmask/presentation/editor/EditorViewModelTest.kt):
    - `givenActiveDocument_whenOpacityChanged_thenReappliesMaskingWithNewOpacity()`
    - `givenTemplateSelected_thenSliderOpacityAdoptsTemplateValue()`
    - `givenCustomTemplate_whenDeleted_thenRepoInvoked()`
    - `givenDebugBuild_whenToggleDebugProCalled_thenProStatusUpdates()`
  - [`TemplateRepositoryTest.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/test/java/com/feryaeljustice/docmask/data/repository/TemplateRepositoryTest.kt):
    - `givenDefaultTemplate_whenDeleteAttempted_thenIsNotDeleted()`
    - `givenCustomTemplate_whenDeleteAttempted_thenIsRemoved()`
    - `givenExistingTemplate_whenSaveCalled_thenUpdatesInPlace()`
- **Compilacion y Validacion**:
  - `./gradlew.bat testDebugUnitTest`
  - `./gradlew.bat assembleDebug`

---

## 4. Riesgos y Plan de Mitigacion
- **Riesgo**: Deslizamiento accidental en la lista de plantillas al hacer scroll vertical.
  - **Mitigacion**: `SwipeToDismissBox` de Material 3 con `SwipeToDismissBoxValue.EndToStart` y umbral estandar de fraccion positional, ademas de restringir el deslizamiento a derecha (`enableDismissFromStartToEnd = false`).
- **Riesgo**: `BuildConfig` no resuelto en compilacion.
  - **Mitigacion**: Asegurar `buildFeatures { buildConfig = true }` en `app/build.gradle.kts` y verificar con `./gradlew.bat assembleDebug`.
