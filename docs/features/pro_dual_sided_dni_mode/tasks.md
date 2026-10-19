# Checklist de Tareas: Modo Pro Doble Cara DNI (Anverso y Reverso con Exportacion en Lote)

- **Feature vinculada**: [`spec.md`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/docs/features/pro_dual_sided_dni_mode/spec.md)
- **Plan vinculado**: [`plan.md`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/docs/features/pro_dual_sided_dni_mode/plan.md)

---

### Fase 1: Modelo y Utilidades Core
- [x] Tarea 1.1: Crear enum [`DocumentSide.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/core/model/DocumentSide.kt) (`FRONT`, `BACK`).
- [x] Tarea 1.2: Implementar `exportAndShareBitmapsBatch` en [`DocumentExporter.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/core/util/DocumentExporter.kt) con `Intent.ACTION_SEND_MULTIPLE`.

---

### Fase 2: Capa de Presentacion (Estado y ViewModel)
- [x] Tarea 2.1: Extender [`EditorUiState.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/editor/EditorUiState.kt) con soporte para caras anverso/reverso, estado activo y eventos `ToggleDualSidedMode` y `SelectDocumentSide`.
- [x] Tarea 2.2: Implementar en [`EditorViewModel.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/editor/EditorViewModel.kt) la gestión multicara (carga individual, preservación del anverso al desactivar y re-procesamiento conjunto de marca de agua).
- [x] Tarea 2.3: Ampliar las pruebas unitarias en [`EditorViewModelTest.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/test/java/com/feryaeljustice/docmask/presentation/editor/EditorViewModelTest.kt).

---

### Fase 3: Capa de Interfaz de Usuario (Jetpack Compose)
- [x] Tarea 3.1: En [`EditorScreen.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/editor/EditorScreen.kt), incorporar la tarjeta de conmutación "Documento Doble Cara" (visible solo si `isProUser == true`) con la advertencia de uso para DNI.
- [x] Tarea 3.2: En `DocumentViewerPane`, añadir la barra de pestañas moderna Material 3 (`PrimaryTabRow`) para alternar entre "Anverso" y "Reverso" con distintivos de estado.
- [x] Tarea 3.3: Conectar el lanzador de selección de imagen para asociar la foto a la cara activa en pantalla.
- [x] Tarea 3.4: Adaptar el botón de exportación para verificar ambas caras en modo doble cara (bloqueando con mensaje si falta una) y emitir la exportación en lote.

---

### Fase 4: Verificacion y Validacion Final
- [x] Tarea 4.1: Ejecutar `./gradlew.bat testDebugUnitTest` y verificar éxito del 100%.
- [x] Tarea 4.2: Ejecutar `./gradlew.bat assembleDebug` garantizando compilación limpia.
