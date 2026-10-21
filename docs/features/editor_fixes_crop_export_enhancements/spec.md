# [APROBADO] Especificacion Funcional: Correcciones en Editor, Recorte de Imagenes y Flujo de Exportacion Unificada

- **Fecha**: 2026-10-21
- **Estado**: [APROBADO]
- **Responsable / Autor**: Antigravity & ArisGuimera Standard

---

## 1. Problema y Objetivo
- **Cual es el problema que resuelve**: 
  1. El procesamiento de pixelado en la exportacion no refleja visualmente las areas sensibles detectadas por IA ni las zonas manuales marcadas por el usuario, debido a una implementacion ineficiente de bloques fijos de pixeles sin escala y la falta de actualizacion del tamano de bloque segun la intensidad configurada.
  2. Al capturar fotos desde la camara o seleccionarlas desde la galeria, no existe una fase intermedia para encuadrar y recortar el documento, provocando que se procesen fondos no deseados o documentos torcidos.
  3. La exportacion tiene opciones redundantes y confusas ("Descargar PDF en dispositivo" separado del boton principal de exportacion segura).
  4. La actualizacion de la vista previa de la imagen al alternar parametros (como escala de grises o intensidad de pixelado) presenta latencia sin feedback visual de carga.
  5. En dispositivos con diferentes alturas de pantalla, el contenedor para adjuntar documentos o imagenes se comprime verticalmente de forma irregular.
  6. En orientacion horizontal (landscape), el encabezado y botones ("AEPD", "Desmarcar / Pixelar Todo") de campos identificados se desbordan o distorsionan.
  7. En la pantalla de Ajustes no existe un acceso directo para abrir la carpeta configurada de almacenamiento en el explorador de archivos del sistema.
  8. Al alternar entre el modo PDF, la pantalla de Ajustes y volver al Editor, se puede disparar un error espurio de inconsistencia de formato o desincronizacion de paginas cacheadas al limpiar la memoria o volver a la vista.
  9. En la pantalla de Plantillas no se respetan correctamente los window insets (barra superior, notch y barra de navegacion inferior).
- **Objetivo**: Proporcionar una experiencia fluida, robusta y 100% offline para la edicion y proteccion de documentos, garantizando pixelado real y de alto rendimiento en la exportacion, una pantalla modal intermedia de recorte previa al procesamiento OCR, unificacion ergonomica de las opciones de exportacion a imagen y PDF (con distincion Pro clara), y correccion integral de UI, insets y responsive en tablets y telefonos.

---

## 2. Situacion Actual
- El boton de exportacion guarda el bitmap procesado mediante `BitmapProcessor`, pero el pixelado manual y automatico utiliza un bucle pixel a pixel con un bloque fijo de 24 px que en fotos de camara de alta resolucion (12+ MP) resulta imperceptible o tarda excesivamente en completarse.
- Al tomar una foto o elegirla de la galeria se inyecta directamente en el pipeline de ML Kit sin permitir recortar sobrantes.
- Existen dos botones en el editor: "Exportar Documento Seguro" y "Descargar PDF", generando duplicidad y confusion sobre el formato resultante.
- El cambio de escala de grises o intensidad re-ejecuta el procesador de bitmap en un hilo de fondo sin mostrar un spinner o indicador de carga localizado en la miniatura previa.
- En la pantalla de Plantillas, `LazyColumn` consume los insets de ventana sin aplicar padding superior e inferior de manera adecuada a las barras del sistema (status bar, navigation bar y notch).
- En Ajustes solo se puede "Cambiar carpeta" o "Restablecer", pero no se puede abrir directamente dicha carpeta con el visor del sistema de archivos.

---

## 3. Alcance

### 3.1. Dentro del Alcance (In Scope)
1. **Correccion y Optimizacion del Pixelado (Exportacion y Previsualizacion)**:
   - Reemplazo del algoritmo lento de bucle pixel a pixel por un algoritmo eficiente de mosaico/pixelacion proporcional a la resolucion de la imagen o factor de escala, vinculando directamente el porcentaje de intensidad con el tamano efectivo de bloque.
   - Garantizar que todas las areas con `isRedacted = true` (tanto deteccion IA como seleccion manual) queden completamente pixeladas en la imagen exportada y compartida.
2. **Modal / Pantalla Intermedia de Recorte (Image Cropper Nativo en Compose)**:
   - Componente nativo ligero 100% en Jetpack Compose, con manipulacion tactil mediante gestos de arrastre para definir el marco de recorte rectangular.
   - Se muestra inmediatamente tras confirmar la foto de la camara o seleccionar una imagen de galeria (solo para imagenes, nunca para importacion de documentos PDF).
   - Compatible tanto con modo 1 cara como con 2 caras (para anverso y reverso independientemente).
   - Opciones claras: "Aceptar / Recortar" (extrae el sub-bitmap recortado e inicia la deteccion IA) y "Cancelar" (cierra la modal sin cargar nada en el editor ni modificar el estado actual).
3. **Flujo de Exportacion Unificado y Botones Adaptativos**:
   - Eliminacion definitiva del boton secundario "Descargar PDF en dispositivo".
   - **Modo 1 Cara**:
     - Boton principal por defecto: "Exportar imagen de forma segura".
     - Switch Pro en controles: "Generar como PDF" (con badge PRO). Si esta activo, el boton principal cambia su texto a "Exportar documento seguro" y genera un PDF de 1 pagina (manteniendo las dimensiones y relacion de aspecto proporcionales de la imagen) guardado en la carpeta de destino y ofrecido para compartir.
   - **Modo 2 Caras**:
     - Switch Pro existente: "Unir anverso y reverso en PDF" (PRO).
     - Si esta desactivado, el boton principal indica: "Exportar DNI" (o "Exportar imagenes (2 caras)") y exporta ambos bitmaps como imagenes.
     - Si esta activado, el boton principal cambia a: "Exportar documento seguro" y compila el PDF de 2 paginas.
   - **Modo PDF**:
     - Boton principal: "Exportar documento seguro".
4. **Feedback Visual de Carga en Previsualizacion**:
   - Al alternar escala de grises, intensidad de pixelado o marca de agua, mostrar un indicador visual sutil (spinner o shimmer overlay) en la tarjeta de previsualizacion mientras se renderiza el resultado en segundo plano.
5. **Altura Estable para Contenedores de Carga**:
   - Establecer `minHeight` adecuado (e.g. 260.dp) y restricciones no compresibles en el contenedor de carga para evitar que se achate o deforme en pantallas de baja altura o al desplegar componentes.
6. **Responsive en Paisaje (Landscape) para Campos Identificados**:
   - Reorganizar el encabezado y botones "AEPD" y "Desmarcar / Pixelar Todo" en `FlowRow` o columna adaptable segun el ancho disponible, evitando compresion y cortes horizontales.
7. **Boton 'Abrir Carpeta' en Ajustes**:
   - Anadir un boton intermedio entre "Cambiar carpeta" y "Restablecer" con el texto "Abrir carpeta" que lance un `Intent.ACTION_VIEW`: si hay carpeta SAF personalizada abre dicha URI con permisos persistidos; si es la predeterminada, abre la carpeta publica de Documentos o Descargas del dispositivo.
8. **Prevencion de Inconsistencia de Formato en PDF**:
   - Preservar adecuadamente el estado de las paginas PDF y el modo de entrada al alternar con Ajustes o limpiar la cache, evitando errores espurios de discrepancia de formato o recarga fallida.
9. **Correccion de Window Insets en Plantillas**:
   - Configurar `Scaffold`, `LazyColumn` y `TopAppBar` en `TemplatesScreen` aplicando `padding(padding)` y `consumeWindowInsets(padding)` correctos para respetar las barras de estado, recortes de pantalla (display cutouts / notch) y barra de navegacion inferior.

### 3.2. Fuera del Alcance (Out of Scope)
- Modificaciones en la biblioteca de facturacion de Google Play.
- Anadir conexiones de red o almacenamiento en la nube (la aplicacion sigue siendo 100% offline sin permiso INTERNET).
- Soporte para recorte en documentos PDF multipagina nativos (el recorte aplica exclusivamente a imagenes individuales de camara y galeria).

---

## 4. Casuisticas y Comportamiento Mobile
- **Comportamiento Offline / Sin Red**:
  - Todo el recorte de imagenes, deteccion ML Kit y transformacion grafica de pixeles y marca de agua ocurre estrictamente en memoria local del dispositivo sin hacer llamadas a red.
- **Ciclo de Vida y Recuperacion de Estado**:
  - Si el usuario rota la pantalla o el sistema recrea la actividad durante el recorte o la edicion, el bitmap original o temporal y el estado de recorte deben preservarse o gestionarse sin fugas de memoria.
  - Las tareas pesadas de procesamiento de imagen se ejecutan en `Dispatchers.Default` vinculadas al `viewModelScope`.
- **Estados Vacios (Empty States) y Errores**:
  - Si el usuario cancela la modal de recorte, la pantalla del editor permanece en su estado anterior sin cargar archivos huerfanos.
  - Si el explorador de archivos no puede abrir la carpeta configurada en Ajustes, se muestra un Toast informativo explicativo.
- **Ergonomia y Accesibilidad**:
  - Todos los botones interactivos del recortador, encabezados y switches mantienen un tamano tactil minimo de 48 x 48 dp.
  - Compatibilidad completa con temas claro (Light) y oscuro (Dark) y soporte adaptativo para pantallas compactas y expandidas (tablets).

---

## 5. Criterios de Aceptacion (Given - When - Then)

### Criterio 1: Pixelado Efectivo en Exportacion
- **Dado que**: El usuario tiene un documento cargado en el editor con areas de riesgo detectadas (o marcadas manualmente) con `isRedacted = true`.
- **Cuando**: El usuario pulsa el boton de exportar.
- **Entonces**: La imagen o PDF resultante contiene las areas seleccionadas completamente pixeladas e ilegibles, respetando la intensidad seleccionada y aplicando el mosaico sobre el bitmap final exportado.

### Criterio 2: Pantalla Intermedia de Recorte al Cargar Imagen
- **Dado que**: El usuario captura una foto con la camara o elige una imagen desde la galeria (en modo 1 cara o 2 caras).
- **Cuando**: La imagen es capturada/seleccionada exitosamente.
- **Entonces**: Se muestra una pantalla o dialogo modal con la herramienta de recorte de la foto en Compose antes de procesar el documento; si el usuario pulsa "Aceptar", se procesa la imagen recortada; si pulsa "Cancelar", no se carga ninguna imagen y se cierra la modal.

### Criterio 3: Unificacion del Boton de Exportacion y Switch Pro de PDF en 1 Cara
- **Dado que**: El usuario esta en el modo de 1 Cara.
- **Cuando**: El switch Pro "Generar como PDF" esta desactivado, el boton muestra "Exportar imagen de forma segura".
- **Cuando**: El usuario activa el switch Pro "Generar como PDF", el boton muestra "Exportar documento seguro" y genera un archivo PDF seguro de 1 pagina con las dimensiones proporcionales del recorte.
- **Entonces**: No se muestra el boton secundario "Descargar PDF en dispositivo".

### Criterio 4: Indicador de Carga al Alterar Parametros Visuales
- **Dado que**: El usuario tiene una imagen cargada en el editor.
- **Cuando**: Modifica la escala de grises, el porcentaje de pixelado o la marca de agua.
- **Entonces**: La vista previa superior muestra un indicador de carga mientras se recalcula el bitmap protegido, actualizandose inmediatamente al terminar.

### Criterio 5: Boton 'Abrir Carpeta' en Ajustes
- **Dado que**: El usuario se encuentra en la pantalla de Ajustes en la seccion de Carpeta de Almacenamiento.
- **Cuando**: Pulsa el boton "Abrir carpeta" situado entre "Cambiar carpeta" y "Restablecer".
- **Entonces**: La aplicacion lanza un `Intent.ACTION_VIEW` para abrir la carpeta SAF o el gestor de archivos en Descargas/Documentos.

### Criterio 6: Respeto de Window Insets en Plantillas
- **Dado que**: El usuario navega a la pestana de Plantillas en un dispositivo con notch superior y barra gestual inferior.
- **Cuando**: La pantalla es renderizada.
- **Entonces**: La barra superior y el contenido de la lista respetan el espaciado seguro del notch y la barra de navegacion del sistema sin solaparse.

---

## 6. Decisiones Resueltas
- [x] **Image Cropper**: Se implementa un componente nativo ligero en Jetpack Compose, garantizando cero dependencias externas y funcionamiento 100% offline.
- [x] **Dimensiones de PDF en 1 Cara**: Se preservan las dimensiones y relacion de aspecto proporcionales del recorte de la foto en el PDF generado.
- [x] **Comportamiento 'Abrir Carpeta' en Ajustes**: Habilitado siempre: si hay carpeta SAF personalizada se abre directamente; si es la predeterminada de la app, se abre el directorio publico de Descargas/Documentos.
- [x] **Inconsistencia de Formato PDF**: Sanitizacion y proteccion del estado en ViewModel al alternar pestanas para evitar que operaciones de cache o navegacion contaminen las paginas del PDF.

