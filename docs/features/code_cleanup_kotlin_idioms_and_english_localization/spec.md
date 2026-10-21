# [APROBADO] Especificacion Funcional: Limpieza de Codigo no Utilizado, Idiomatica Kotlin y Localizacion al Ingles

- **Fecha**: 2026-10-21
- **Estado**: [APROBADO]
- **Responsable / Autor**: Antigravity & ArisGuimera SDMD Standards

---

## 1. Problema y Objetivo

- **Cual es el problema que resuelve**: 
  - Tras multiples iteraciones de desarrollo, el codigo fuente de DocMask acumula metodos, parametros, eventos y variables sin uso (por ejemplo, funciones redundantes en helpers, eventos no consumidos de exportacion previa o flags residuales).
  - Existen operaciones sobre graficos y Android APIs donde se emplean llamadas directas Java en lugar de funciones de extension idiomaticas de Kotlin o Android KTX (`Bitmap.scale`, `Canvas.withRotation`, `String.toUri`, operadores de indexacion, etc.).
  - Las cadenas de texto de la interfaz de usuario se encuentran directamente incrustadas como literales hardcoded en codigo Kotlin dentro de componentes Compose y clases utilitarias, sin contar con un sistema de localizacion centralizado en `strings.xml`. Actualmente no existe soporte para el idioma ingles, a pesar de que DocMask esta destinado a proteger documentos tanto espanoles como europeos.
- **Objetivo**: 
  - Eliminar de manera segura todo codigo muerto, clases, metodos, argumentos y parametros no referenciados sin romper la compilacion ni las pruebas unitarias.
  - Refactorizar las operaciones aplicables a funciones de extension de Kotlin (KTX y extensiones propias de dominio/utilidades) y modernizar declaraciones obsoletas (como el uso de `data object` en eventos sellados).
  - Externalizar todas las cadenas de texto visibles a `res/values/strings.xml` (teniendo el espanol como idioma por defecto) y proveer una localizacion completa al ingles en `res/values-en/strings.xml`.

---

## 2. Situacion Actual

- `res/values/strings.xml` unicamente contiene el nombre de la aplicacion (`app_name = "DocMask"`).
- Todas las pantallas Compose (`EditorScreen`, `SettingsScreen`, `TemplatesScreen`, `ProScreen`, `LockScreen`, `ImageCropDialog`, `ManualRedactionDialog`) y utilidades (`DocumentExporter`, `BiometricHelper`, `CacheManager`) utilizan textos en espanol codificados en duro.
- Android Lint senala advertencias de tipo `UseKtx` en `BitmapProcessor`, `DocumentExporter`, `PdfDocumentManager` y `SettingsScreen` por no usar funciones de extension KTX disponibles en las dependencias.
- Se detectaron elementos no utilizados como `createCameraTempUri`, `savePdfToStorage`, `EditorUiEvent.DownloadPdf`, `isPdfDownloadAvailable`, recursos de color sin uso en `colors.xml`, etc.
- No existe el directorio de recursos `res/values-en/`.

---

## 3. Alcance

### 3.1. Dentro del Alcance (In Scope)
- **Limpieza de codigo muerto**:
  - Eliminacion de funciones y metodos sin referencias en codigo ni en tests (`createCameraTempUri`, `savePdfToStorage`, etc.).
  - Eliminacion de campos y eventos sin uso en `EditorUiState` y `EditorUiEvent` (`isPdfDownloadAvailable`, `EditorUiEvent.DownloadPdf`, `exportSuccessMessage`, `DismissSuccessMessage`, etc.).
  - Eliminacion de estado no utilizado `AppPermissionStatus.NOT_REQUIRED`.
  - Eliminacion de recursos no referenciados en `res/values/colors.xml` (`purple_200`, `purple_500`, `purple_700`, `teal_200`, `teal_700`, `black`, `white`, `ic_launcher_background`).
  - Limpieza de parametros innecesarios y orden de modificadores en funciones `@Composable` segun convenciones Compose (primer parametro opcional).
- **Modernizacion e idiomatica Kotlin**:
  - Sustitucion de llamadas estaticas por KTX: `Bitmap.scale`, `output[x, y]` para lectura/escritura de pixeles, `Canvas.withRotation`, `String.toUri()`, `createBitmap()`.
  - Migracion de objetos simples dentro de `sealed interface EditorUiEvent` a `data object`.
  - Creacion de funciones de extension reutilizables para conversiones habituales (ej. `Rect.toBoundingBox()`, `BoundingBox.toRectF()`, `Context.findActivity()`, `Context.isCameraPermissionGranted()`, etc.).
  - Extensiones idiomaticas para `DocMaskResult` (`onSuccess`, `onFailure`, `map`).
  - Adaptacion de `SensitiveFieldType` con `@StringRes val titleRes: Int` y extensiones idiomaticas para resolucion de cadenas traducidas.
- **Localizacion completa (Espanol por defecto + Ingles)**:
  - Extraccion sistematica de todos los textos hardcoded en Compose y clases auxiliares hacia `res/values/strings.xml` (idioma espanol por defecto).
  - Creacion de `res/values-en/strings.xml` con traducciones precisas y profesionales al ingles.
  - Soporte para localizacion dinamica de plantillas por defecto a traves de recursos string.
  - Soporte de textos dinamicos con argumentos (`%d`, `%s`) en mensajes de cuota y exportacion.

### 3.2. Fuera del Alcance (Out of Scope)
- Modificacion de la logica de negocio de deteccion de campos sensibles por ML Kit.
- Alteracion de la arquitectura Clean Architecture existente o adicion de dependencias de red.
- Incorporacion de idiomas adicionales fuera de espanol e ingles en esta fase.

---

## 4. Casuisticas y Comportamiento Mobile

- **Cambio de Idioma del Sistema Operativo**:
  - Si el dispositivo esta configurado en ingles (o cualquier variante de ingles), Android cargara automaticamente los textos desde `values-en/strings.xml`.
  - Si el dispositivo esta en espanol o cualquier otro idioma no soportado, Android usara los recursos de `values/strings.xml` (espanol por defecto).
- **Ciclo de Vida y Recomposicion**:
  - La extraccion a `stringResource(...)` en Compose asegura que los cambios de configuracion de idioma del sistema recompondran inmediatamente la interfaz con los textos actualizados.
- **Formateo y Plurales**:
  - Mensajes con cantidades variables (ej. "Has alcanzado el limite diario de X documentos", dias restantes, contadores de plantillas) utilizaran strings con especificadores de formato compatibles con localizacion.
- **Zero-Network y Privacidad**:
  - La localizacion es 100% estatica y empaquetada en el APK. No requiere llamadas a servicios de traduccion en la nube ni conexion a internet.

---

## 5. Criterios de Aceptacion (Given - When - Then)

### Criterio 1: Cero codigo muerto y cero advertencias de KTX
- **Dado que** el proyecto se compila y ejecuta Android Lint,
- **Cuando** se analizan las clases refactorizadas (`BitmapProcessor`, `DocumentExporter`, `CameraCaptureHelper`, `EditorUiState`),
- **Entonces** las funciones muertas han sido eliminadas, las operaciones de bitmap utilizan KTX (`scale`, `withRotation`, `get/set`), y los tests unitarios continuan pasando al 100%.

### Criterio 2: Visualizacion en Espanol por Defecto
- **Dado que** el dispositivo Android esta configurado en idioma Espanol o cualquier idioma no contemplado,
- **Cuando** el usuario navega por las pantallas Editor, Plantillas, Ajustes, Pro y Bloqueo,
- **Entonces** todos los textos se muestran correctamente en espanol desde `values/strings.xml` sin cadenas residuales en duro.

### Criterio 3: Visualizacion en Ingles al cambiar la configuracion regional
- **Dado que** el dispositivo Android esta configurado en idioma Ingles (`en-US`, `en-GB`),
- **Cuando** el usuario abre y utiliza la aplicacion,
- **Entonces** todos los textos de la interfaz, dialogos, botones, descripciones y tipos de campos sensibles se presentan en ingles desde `values-en/strings.xml`.

### Criterio 4: Preservacion de Integridad en Compartir y Exportar
- **Dado que** el usuario exporta un documento con marca de agua y pixelado,
- **Cuando** se genera el dialogo nativo de compartir (`Intent.createChooser`),
- **Entonces** el asunto y cuerpo del mensaje se cargan en el idioma correspondiente a traves de `context.getString(...)`.

---

## 6. Decisiones Resueltas

- [x] Decision 1: Plantillas predefinidas en `TemplateRepositoryImpl`. Se localizan los titulos y textos de las plantillas por defecto mediante recursos string para que se adapten al idioma del sistema.
- [x] Decision 2: Tipos de campos sensibles (`SensitiveFieldType`). Se anade `@StringRes val titleRes: Int` al enum y se mantienen extensiones idiomaticas para obtener el texto traducido de forma limpia y retrocompatible.
- [x] Decision 3: Limpieza de `AppPermissionStatus.NOT_REQUIRED`. Se elimina de forma segura al no poseer uso ni en la UI ni en los tests.
- [x] Decision 4: Eventos y flags de descarga obsoletos (`EditorUiEvent.DownloadPdf`, `isPdfDownloadAvailable`, `exportSuccessMessage`, `DismissSuccessMessage`, `savePdfToStorage`, `createCameraTempUri`, colores de `colors.xml`). Se eliminan completamente.
