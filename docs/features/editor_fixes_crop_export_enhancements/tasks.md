# Tareas de Implementacion: Correcciones en Editor, Recorte de Imagenes y Exportacion Unificada

- **Feature vinculada**: `docs/features/editor_fixes_crop_export_enhancements/spec.md`
- **Plan arquitectonico**: `docs/features/editor_fixes_crop_export_enhancements/plan.md`
- **Estado**: Completado

---

## Tareas

- [x] **Tarea 1: Algoritmo Eficiente de Pixelado y Mosaico Proporcional**
  - Archivos: `core/util/BitmapProcessor.kt`, `core/model/MaskingOptions.kt`
  - Descripcion: Reemplazar el bucle pixel a pixel por escalado nearest-neighbor / mosaic sobre sub-regiones rectangulares, mapeando `pixelPercent` proporcionalmente a las dimensiones del bitmap.
  - Verificacion: Test unitario verificando que el bitmap resultante esta pixelado visiblemente y con ejecucion instantanea.

- [x] **Tarea 2: Componente Modal de Recorte de Imagen (ImageCropDialog)**
  - Archivos: `presentation/editor/components/ImageCropDialog.kt` (o en paquete editor), `presentation/editor/EditorScreen.kt`, `presentation/editor/EditorViewModel.kt`, `presentation/editor/EditorUiState.kt`
  - Descripcion: Interceptar la imagen tras captura de camara o seleccion de galeria para mostrar el dialogo de recorte táctil. Si se acepta, procesar el recorte con ML Kit; si se cancela, no cargar nada.
  - Verificacion: Pruebas unitarias de flujo en ViewModel y compilacion.

- [x] **Tarea 3: Unificacion del Flujo de Exportacion y PDF Pro en 1 Cara**
  - Archivos: `core/util/DocumentExporter.kt`, `presentation/editor/EditorUiState.kt`, `presentation/editor/EditorViewModel.kt`, `presentation/editor/EditorScreen.kt`
  - Descripcion:
    - Anadir soporte para exportar PDF de 1 pagina (`exportAndShareSinglePdf`).
    - Anadir switch Pro "Generar como PDF" en modo 1 Cara.
    - Actualizar los textos dinamicos del boton de exportacion ("Exportar imagen de forma segura", "Exportar DNI", "Exportar documento seguro").
    - Eliminar el boton redundante "Descargar PDF en dispositivo".
  - Verificacion: Pruebas unitarias y verificacion de estados de exportacion en ViewModel.

- [x] **Tarea 4: Feedback Visual de Carga en Previsualizacion y Altura Estable**
  - Archivos: `presentation/editor/EditorUiState.kt`, `presentation/editor/EditorViewModel.kt`, `presentation/editor/EditorScreen.kt`
  - Descripcion:
    - Exponer `isMaskingPreviewLoading` cuando se recalculan mascaras y mostrar un indicador sobre la imagen.
    - Asignar una altura minima fija y no compresible al placeholder de seleccion/carga de documentos.
  - Verificacion: Comprobacion visual y pruebas de renderizado en Compose.

- [x] **Tarea 5: Adaptabilidad en Modo Horizontal (Landscape) para Campos Identificados**
  - Archivos: `presentation/editor/EditorScreen.kt`
  - Descripcion: Reorganizar el encabezado y botones "AEPD" y "Pixelar Todo / Desmarcar" para usar layout adaptable (FlowRow o Column segun espacio disponible), evitando distorsion.
  - Verificacion: Test de interfaz y compilacion.

- [x] **Tarea 6: Boton 'Abrir Carpeta' en Ajustes**
  - Archivos: `presentation/settings/SettingsScreen.kt`
  - Descripcion: Anadir boton intermedio "Abrir carpeta" que lance un `Intent.ACTION_VIEW` hacia la carpeta SAF personalizada o a Descargas/Documentos.
  - Verificacion: Verificacion de llamada a intent y ejecucion limpia.

- [x] **Tarea 7: Correccion de Window Insets en Plantillas y Blindaje de Estado PDF**
  - Archivos: `presentation/templates/TemplatesScreen.kt`, `presentation/editor/EditorViewModel.kt`
  - Descripcion:
    - Aplicar insets correctos de Scaffold (`padding(padding)` y `consumeWindowInsets(padding)`) en `TemplatesScreen`.
    - Garantizar que al alternar entre pestañas y Ajustes no se degrade ni resetee el estado del PDF cargado.
  - Verificacion: Compilacion y pruebas unitarias de navegacion.

- [x] **Tarea 8: Bateria de Pruebas Unitarias y Verificacion Final**
  - Archivos: `app/src/test/...`
  - Descripcion: Ejecutar la suite completa de tests unitarios y verificar la compilacion limpia de desarrollo.
  - Verificacion: `.\gradlew.bat testDebugUnitTest` y `.\gradlew.bat assembleDebug` exitosos.
