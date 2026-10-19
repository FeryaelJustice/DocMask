# [APROBADO] Especificacion Funcional: Ajustes de Opacidad en Editor, CRUD de Plantillas y Toggle Debug Pro

- **Fecha**: 2026-10-19
- **Estado**: [APROBADO]
- **Responsable / Autor**: Antigravity & Usuario
- **Directrices aplicadas**: SDMD ([mobile_guidelines.md](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/docs/mobile_guidelines.md))

---

## 1. Problema y Objetivo
- **Cual es el problema que resuelve**: 
  1. En el editor no existe un control visual para regular la opacidad de la marca de agua en tiempo real ni se refleja el valor de la plantilla de forma interactiva.
  2. Las plantillas guardadas no se diferencian entre predeterminadas y personalizadas; no pueden editarse manteniendo pulsado ni eliminarse mediante gestos tactiles ergonomicos modernos de Android (swipe to dismiss).
  3. No existe un control en la pantalla de Ajustes exclusivo de depuracion (`BuildConfig.DEBUG`) para alternar el estado de usuario Pro y probar de forma inmediata las funciones avanzadas.
- **Objetivo**: 
  - Incorporar en el editor un control deslizante de opacidad sincronizado con [`WatermarkConfig`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/core/model/WatermarkConfig.kt).
  - Categorizar en [`TemplatesScreen.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/templates/TemplatesScreen.kt) las plantillas en dos secciones: "Plantillas Predeterminadas" y "Plantillas Personalizadas".
  - Diferenciar la interaccion tactil: pulsacion corta para seleccionar plantilla activa; pulsacion larga (mantener pulsado) para abrir el dialogo de edicion con los campos precargados.
  - Permitir la eliminacion de plantillas personalizadas mediante deslizamiento a la izquierda (`SwipeToDismissBox`) con fondo rojo e icono de papelera. Prohibir el borrado de las plantillas predeterminadas (solo editables).
  - Anadir un control de alternancia de Modo Pro visible unicamente en [`SettingsScreen.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/settings/SettingsScreen.kt) cuando la app este compilada en modo debug (`BuildConfig.DEBUG`).

---

## 2. Situacion Actual
- En [`EditorScreen.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/editor/EditorScreen.kt), la seccion de marca de agua solo expone el campo de texto y los chips de plantillas, a pesar de que [`WatermarkConfig`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/core/model/WatermarkConfig.kt) almacena `opacity: Float`.
- En [`TemplatesScreen.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/templates/TemplatesScreen.kt), las plantillas se muestran en una sola lista plana sin categorizar. El dialogo actual solo crea nuevas plantillas y no admite modo edicion. No hay deteccion de toque prolongado ni soporte de `SwipeToDismissBox`.
- En [`TemplateRepositoryImpl.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/data/repository/TemplateRepositoryImpl.kt), `saveTemplate` ya reemplaza por ID si existe, pero falta exponer el caso de uso y eventos para eliminar o actualizar plantillas desde el ViewModel.
- En [`SettingsScreen.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/settings/SettingsScreen.kt), la tarjeta de Pro solo muestra un boton con un Toast informativo; no existe un switch o boton para alternar el estado Pro en modo depuracion.
- En `app/build.gradle.kts`, `buildFeatures.buildConfig` no esta activado explicitamente, impidiendo el uso de `BuildConfig.DEBUG`.

---

## 3. Alcance

### 3.1. Dentro del Alcance (In Scope)
- **Editor**:
  - Slider de opacidad en [`EditorScreen.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/editor/EditorScreen.kt) con porcentaje visible (10% a 80%), actualizando la previsualizacion en vivo.
  - Sincronizacion bidireccional: al pulsar un chip de plantilla en el editor, el Slider adopta la opacidad de la plantilla; al mover el slider se actualiza la configuracion activa de la marca de agua.
- **Plantillas**:
  - Categorizacion visual en [`TemplatesScreen.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/templates/TemplatesScreen.kt) con cabeceras de seccion:
    1. "Plantillas Predeterminadas" (`isDefault == true`)
    2. "Plantillas Personalizadas" (`isDefault == false`)
  - Interaccion tactil dual:
    - Toque normal: selecciona la plantilla como activa para el editor.
    - Mantener pulsado (long press): abre el dialogo en modo edicion con titulo "Editar Plantilla", campos precargados (nombre, texto, fecha, opacidad) y persistencia de cambios.
  - Swipe to dismiss a la izquierda:
    - Habilitado exclusivamente para "Plantillas Personalizadas" (`!isDefault`).
    - Al deslizar hacia la izquierda, se revela fondo de advertencia con icono de papelera. Al superar el umbral, se elimina la plantilla.
    - Para las "Plantillas Predeterminadas", el gesto de borrado esta inhabilitado (`enableDismissFromEndToStart = false`), pero pueden editarse mediante pulsacion prolongada.
- **Modo Pro Debug**:
  - Habilitar `buildFeatures { buildConfig = true }` en `app/build.gradle.kts`.
  - En [`SettingsScreen.kt`](file:///c:/Users/nano9/AndroidStudioProjects/DocMask/app/src/main/java/com/feryaeljustice/docmask/presentation/settings/SettingsScreen.kt), dentro de la tarjeta de Pro, mostrar un control (Switch o Button) de depuracion visible exclusivamente cuando `BuildConfig.DEBUG == true`, permitiendo activar/desactivar `isProUser` inmediatamente.

### 3.2. Fuera del Alcance (Out of Scope)
- Botones de depuracion en la pantalla del Editor (el usuario especifico explicitamente que debe estar "Solo en settings").
- Borrado de plantillas predeterminadas del sistema (solo son editables).
- Integracion con servidores externos o AdMob (preservacion radical de la politica Zero-Network).

---

## 4. Casuisticas y Comportamiento Mobile
- **Politica Zero-Network (Offline-First)**: Persistencia de cambios en memoria local/DataStore sin requerir internet.
- **Ergonomia Gestual**: El umbral de swipe to dismiss debe respetar el estandar de Material 3 para evitar borrados accidentales por toques oblicuos; el gesto requiere deslizamiento intencional con retroalimentacion haptica/visual.
- **Accesibilidad y Areas Tactiles**:
  - `combinedClickable` en tarjetas de plantilla garantizando un objetivo tactil minimo de 48 x 48 dp.
  - Soporte `imePadding` en los dialogos de creacion y edicion de plantillas.
  - Soporte completo para temas claro y oscuro.

---

## 5. Criterios de Aceptacion (Given - When - Then)

### Criterio 1: Regulacion interactiva de opacidad en el Editor
- **Dado que** el usuario tiene un documento cargado en el Editor,
- **Cuando** desplaza el control deslizante de opacidad,
- **Entonces** el valor numerico porcentual se actualiza visualmente y la marca de agua del documento se redibuja en tiempo real con el nuevo nivel de transparencia.

### Criterio 2: Sincronizacion de opacidad con seleccion de plantilla
- **Dado que** existen plantillas con distintas configuraciones de opacidad (ej. 35% y 60%),
- **Cuando** el usuario pulsa sobre un chip de plantilla en el Editor,
- **Entonces** el Slider de opacidad adopta automaticamente el valor exacto de dicha plantilla y se refleja en la imagen.

### Criterio 3: Categorizacion y pulsacion normal para seleccionar plantilla
- **Dado que** el usuario accede a la pantalla de Plantillas,
- **Cuando** observa la lista,
- **Entonces** ve claramente diferenciadas las secciones "Plantillas Predeterminadas" y "Plantillas Personalizadas"; y al realizar una pulsacion simple sobre cualquiera de ellas, queda seleccionada como la plantilla activa.

### Criterio 4: Mantener pulsado para editar plantilla (predeterminada o personalizada)
- **Dado que** el usuario esta en la pantalla de Plantillas,
- **Cuando** mantiene pulsada una tarjeta de plantilla (predeterminada o personalizada),
- **Entonces** se despliega el modal en modo edicion con el titulo "Editar Plantilla", los campos precargados con los valores actuales y la opcion de guardar los cambios sin alterar su identificador.

### Criterio 5: Swipe to dismiss solo en plantillas personalizadas
- **Dado que** el usuario visualiza la seccion de "Plantillas Personalizadas",
- **Cuando** desliza una tarjeta hacia la izquierda superando el umbral de descarte,
- **Entonces** la tarjeta se descarta visualmente y la plantilla se elimina del repositorio local.
- **Dado que** el usuario visualiza la seccion de "Plantillas Predeterminadas",
- **Cuando** intenta deslizar la tarjeta hacia la izquierda,
- **Entonces** el deslizamiento esta bloqueado y la tarjeta no puede eliminarse.

### Criterio 6: Control de alternancia de Modo Pro exclusivo en Settings en modo DEBUG
- **Dado que** la aplicacion se ejecuta en una compilacion `debug` (`BuildConfig.DEBUG == true`),
- **Cuando** el desarrollador visualiza la pantalla de Ajustes,
- **Entonces** dentro de la tarjeta de Pro aparece un control de depuracion para activar o desactivar el estado Pro; al alternarlo, se refleja inmediatamente en el estado global.
- **Dado que** la aplicacion se compila en variante `release`,
- **Cuando** se accede a la pantalla de Ajustes,
- **Entonces** dicho control de depuracion no existe ni se dibuja.

---

## 6. Decisiones Resueltas
- [x] Control Debug Pro: Exclusivo en la pantalla de Ajustes (`SettingsScreen.kt`).
- [x] Interaccion en Plantillas: Toque simple selecciona activa; mantener pulsado abre el modal de edicion.
- [x] Borrado y Categorizacion: Seccion separada para "Plantillas Predeterminadas" (solo editables, no borrables) y "Plantillas Personalizadas" (editables y borrables con swipe to dismiss).
