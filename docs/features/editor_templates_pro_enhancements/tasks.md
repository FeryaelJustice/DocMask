# Checklist de Tareas: Ajustes de Opacidad en Editor, CRUD de Plantillas y Toggle Debug Pro

- **Feature vinculada**: [`spec.md`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/docs/features/editor_templates_pro_enhancements/spec.md)
- **Plan vinculado**: [`plan.md`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/docs/features/editor_templates_pro_enhancements/plan.md)

---

### Fase 1: Configuracion del Entorno y Build
- [x] Tarea 1.1: Activar `buildFeatures { buildConfig = true }` en [`app/build.gradle.kts`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/build.gradle.kts).
- [x] Tarea 1.2: Verificar compilacion limpia con `./gradlew.bat assembleDebug`.

---

### Fase 2: Capa de Dominio y Datos (Casos de Uso y Repositorios)
- [x] Tarea 2.1: Anadir metodo `setDebugProStatus(isPro: Boolean)` en [`BillingRepository.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/domain/repository/BillingRepository.kt) e implementarlo en [`BillingRepositoryImpl.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/data/repository/BillingRepositoryImpl.kt).
- [x] Tarea 2.2: Crear [`SaveWatermarkTemplateUseCase.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/domain/usecase/SaveWatermarkTemplateUseCase.kt) y [`DeleteWatermarkTemplateUseCase.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/domain/usecase/DeleteWatermarkTemplateUseCase.kt).
- [x] Tarea 2.3: Registrar los nuevos casos de uso en el grafo de dependencias de Koin en [`AppModule.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/di/AppModule.kt).
- [x] Tarea 2.4: Crear pruebas unitarias para `TemplateRepositoryTest` verificando la proteccion de borrado de plantillas por defecto.

---

### Fase 3: Capa de Presentacion (ViewModel y Maquina de Estados)
- [x] Tarea 3.1: Extender [`EditorUiState.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/editor/EditorUiState.kt) con eventos `UpdateWatermarkOpacity`, `SaveTemplate`, `DeleteTemplate` y `ToggleDebugPro`, y propiedades calculadas `defaultTemplates` y `customTemplates`.
- [x] Tarea 3.2: Implementar la gestion de los nuevos eventos en [`EditorViewModel.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/editor/EditorViewModel.kt).
- [x] Tarea 3.3: Actualizar y ampliar las pruebas unitarias en [`EditorViewModelTest.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/test/java/com/feryaeljustice/docmask/presentation/editor/EditorViewModelTest.kt).

---

### Fase 4: Capa de Interfaz de Usuario (Jetpack Compose)
- [x] Tarea 4.1: Incorporar control deslizante (`Slider`) de opacidad interactivo en el panel de controles de [`EditorScreen.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/editor/EditorScreen.kt) y sincronizarlo con la seleccion de plantillas.
- [x] Tarea 4.2: En [`TemplatesScreen.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/templates/TemplatesScreen.kt), categorizar las plantillas en dos listas con encabezados ("Plantillas Predeterminadas" y "Plantillas Personalizadas").
- [x] Tarea 4.3: Implementar `combinedClickable` en las tarjetas de plantilla: pulsacion simple para activar y pulsacion prolongada (long press) para editar.
- [x] Tarea 4.4: Implementar gesto de descarte hacia la izquierda con `SwipeToDismissBox` e icono de papelera, activo exclusivamente para plantillas personalizadas.
- [x] Tarea 4.5: Unificar el dialogo de formulario de plantillas en `TemplatesScreen.kt` para soportar tanto modo creacion como modo edicion.
- [x] Tarea 4.6: En [`SettingsScreen.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/settings/SettingsScreen.kt), anadir switch de depuracion condicionado a `if (BuildConfig.DEBUG)` dentro de la tarjeta de Pro.

---

### Fase 5: Verificacion y Validacion Final
- [x] Tarea 5.1: Ejecutar `./gradlew.bat testDebugUnitTest` y verificar que todos los tests pasen exitosamente.
- [x] Tarea 5.2: Ejecutar `./gradlew.bat assembleDebug` garantizando compilacion limpia.
