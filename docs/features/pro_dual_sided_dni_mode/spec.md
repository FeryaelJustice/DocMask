# [APROBADO] Especificacion Funcional: Modo Pro Doble Cara DNI (Anverso y Reverso con Exportacion en Lote)

- **Fecha**: 2026-10-19
- **Estado**: [APROBADO]
- **Responsable / Autor**: Antigravity & Usuario
- **Directrices aplicadas**: SDMD ([mobile_guidelines.md](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/docs/mobile_guidelines.md))

---

## 1. Problema y Objetivo
- **Cual es el problema que resuelve**: 
  Los documentos de identidad oficiales (DNI, NIE, carnet de conducir) constan obligatoriamente de dos caras (anverso con foto/CAN/soporte y reverso con equipo emisor/domicilio/MRZ). Actualmente, el editor de DocMask solo permite procesar una sola imagen a la vez, obligando al usuario Pro a realizar todo el proceso dos veces manualmente.
- **Objetivo**: 
  - Ofrecer a los usuarios con suscripción/licencia Pro activa (o en depuración con el toggle Debug Pro) la opción de activar el "Modo Doble Cara (DNI)".
  - Disponer de una barra de pestañas moderna de dos opciones (*[ Anverso ]* y *[ Reverso ]*) en la parte superior del visor de documento.
  - Bloquear la exportación si falta alguna de las dos caras por subir, avisando al usuario explícitamente de qué cara falta.
  - Si el usuario desmarca el modo doble cara, se conserva intacta la imagen del Anverso como el documento principal para el modo estándar de una sola cara.
  - Aplicar la configuración de marca de agua (finalidad, plantilla, fecha, escala de grises y opacidad) de manera unificada a ambas caras.
  - Al pulsar "Exportar Documento Seguro", generar ambas imágenes protegidas por separado (`_anverso.png` y `_reverso.png`) y compartirlas simultáneamente en lote a través de `Intent.ACTION_SEND_MULTIPLE`.
  - Aclarar de forma visible que el modo está diseñado para DNI/identidad doble cara, y que el uso con otros documentos queda bajo la responsabilidad del usuario.

---

## 2. Situacion Actual
- En [`EditorUiState.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/editor/EditorUiState.kt), el estado actual solo modela un único par de bitmaps (`originalBitmap` y `maskedBitmap`) y una lista única de regiones (`detectedRegions`).
- En [`EditorScreen.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/editor/EditorScreen.kt), [`DocumentViewerPane`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/editor/EditorScreen.kt#L214) muestra un solo visor sin selector de caras ni noción de anverso/reverso.
- En [`DocumentExporter.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/core/util/DocumentExporter.kt), solo existe `exportAndShareBitmap` que emite `Intent.ACTION_SEND` para un solo archivo PNG.

---

## 3. Alcance

### 3.1. Dentro del Alcance (In Scope)
- **Activacion y Desactivacion del Modo Doble Cara**:
  - Control de selección tipo Switch o Card interactiva visible exclusivamente si `isProUser == true`.
  - Etiqueta explicativa: *"Documento Doble Cara (Anverso / Reverso)"* con texto aclaratorio: *"Optimizado para DNI y documentos de identidad de dos caras. Si adjuntas otro tipo de archivo queda bajo tu responsabilidad."*
  - Comportamiento de preservación: Si se desactiva el check, la imagen asignada al Anverso se conserva automáticamente como el documento único activo en el editor.
- **Visor Interactivo y Navegacion de Caras**:
  - Barra de pestañas moderna Material 3 (`PrimaryTabRow` o `SecondaryTabRow`) sobre el visor con pestañas "Anverso" y "Reverso" y distintivos de estado (indica visualmente si la cara ya tiene imagen cargada o está pendiente).
  - Cada cara dispone de su propio estado: imagen original, imagen con máscara aplicada y lista de regiones detectadas localmente por ML Kit.
  - Al alternar de pestaña, los chips de datos detectados en la barra de herramientas se actualizan para reflejar las regiones de la cara activa.
  - Al cargar o cambiar imagen desde el selector del sistema, se asocia específicamente a la cara que esté enfocada en ese instante.
- **Marca de Agua Unificada**:
  - Un único conjunto de controles (texto, fecha, plantilla rápida, opacidad, escala de grises) que afecta de forma idéntica a ambas caras.
- **Exportacion en Lote con Validacion Bloqueante**:
  - Si el modo doble cara está activado y el usuario intenta exportar faltando alguna cara (ej. anverso o reverso vacío), se bloquea la exportación y se despliega un mensaje informativo: *"Falta adjuntar el [reverso/anverso] del documento"*.
  - Si ambas caras están cargadas y procesadas, la función `exportAndShareBitmapsBatch` en [`DocumentExporter.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/core/util/DocumentExporter.kt) genera los dos archivos temporales protegidos (`_anverso.png` y `_reverso.png`) y abre la hoja de compartir nativa de Android con ambas imágenes adjuntas a la vez (`Intent.ACTION_SEND_MULTIPLE`).

### 3.2. Fuera del Alcance (Out of Scope)
- Generación de un PDF único combinado (el requerimiento del usuario pide explícitamente exportar las dos imágenes por separado en lote en la misma acción de compartir).
- Configuración de marcas de agua independientes por cara (se unifican en un solo ajuste global).
- Conexiones de red externas (preservación absoluta de la política Zero-Network).

---

## 4. Casuisticas y Comportamiento Mobile
- **Gestion de Memoria (RAM)**:
  - Manejo cuidadoso de ambos bitmaps en memoria para evitar saturar el heap en dispositivos de gama de entrada.
- **Retencion de Estado y Ciclo de Vida**:
  - Los dos bitmaps y sus listas de regiones detectadas se retienen íntegramente en el `EditorViewModel` ante rotaciones de pantalla o cambios de configuración.
- **Ergonomia Tactil y Accesibilidad**:
  - Altura mínima táctil de 48 dp en las pestañas del visor de documento.
  - Soporte completo para temas claro y oscuro.

---

## 5. Criterios de Aceptacion (Given - When - Then)

### Criterio 1: Visibilidad del Modo Doble Cara exclusiva para usuarios Pro
- **Dado que** la aplicación se encuentra en ejecución en el Editor,
- **Cuando** el usuario no tiene activo el modo Pro (`isProUser == false`),
- **Entonces** no se muestra el control de doble cara;
- **Y cuando** el usuario tiene modo Pro activo (`isProUser == true`),
- **Entonces** aparece el control de conmutación para habilitar el "Modo Doble Cara (DNI)".

### Criterio 2: Navegacion entre Anverso y Reverso mediante TabRow
- **Dado que** el usuario activa el modo doble cara,
- **Cuando** pulsa en la pestaña "Reverso" (o "Anverso"),
- **Entonces** el visor cambia inmediatamente mostrando la imagen y los campos detectados de la cara seleccionada, con un indicador visual de si dicha cara tiene imagen cargada.

### Criterio 3: Preservacion de imagen al desactivar el modo doble cara
- **Dado que** el usuario tiene imágenes cargadas en el modo doble cara,
- **Cuando** desactiva el switch de doble cara,
- **Entonces** la aplicación vuelve al modo estándar de una cara conservando la imagen del Anverso como el documento activo sin que el usuario tenga que volver a seleccionarla.

### Criterio 4: Bloqueo de exportacion si falta una cara
- **Dado que** el modo doble cara está activado y solo se ha cargado el anverso (o solo el reverso),
- **Cuando** el usuario pulsa "Exportar Documento Seguro",
- **Entonces** la exportación no se ejecuta y se muestra un mensaje de alerta indicando que falta adjuntar la cara restante.

### Criterio 5: Exportacion y comparticion en lote con ambas caras completas
- **Dado que** el anverso y el reverso están cargados y procesados,
- **Cuando** el usuario pulsa "Exportar Documento Seguro",
- **Entonces** se generan los dos archivos PNG protegidos con la marca de agua común y se abre la hoja de compartir de Android con ambas imágenes adjuntas simultáneamente (`Intent.ACTION_SEND_MULTIPLE`).

---

## 6. Decisiones Resueltas
- [x] 1. Bloqueo de exportación si falta alguna cara: Bloquear y notificar al usuario.
- [x] 2. Preservación al desactivar: Conservar la imagen del Anverso para el modo simple.
- [x] 3. Alcance de la marca de agua: Única y global para ambas caras.
- [x] 4. Interfaz del visor: Barra de pestañas moderna Material 3 (`PrimaryTabRow` / `SecondaryTabRow`) con indicadores de carga.
