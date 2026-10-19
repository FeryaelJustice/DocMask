# Directrices Criticas de Mobile (Mobile Guidelines - SDMD)

Este documento define las restricciones fisicas y de sistema operativo que la IA debe auditar obligatoriamente antes de cerrar cualquier especificacion o plan en DocMask.

---

## 1. Ciclo de Vida (Lifecycle) y Muerte de Procesos
- **Destruccion de Procesos**: El sistema operativo puede destruir el proceso en segundo plano en cualquier momento por escasez de memoria (Low Memory Killer).
- **Sobrevivencia a Cambios de Configuracion**: Gestionar rotacion de pantalla, cambio de idioma o modo multi-ventana mediante `SavedStateHandle` o retencion adecuada en ViewModel.
- **Cancelacion de Corrutinas / Tareas Asincronas**: Asegurar que los trabajos asincronos (renderizado de canvas, deteccion ML Kit) esten vinculados a `viewModelScope` para prevenir memory leaks.

---

## 2. Conectividad y Privacidad Radical (Zero-Network)
- **100% Offline por Diseno**: DocMask no incluye `android.permission.INTERNET` en el `AndroidManifest.xml`.
- **Modelos de IA Empaquetados (Bundled)**: Ninguna funcion de reconocimiento depende de descargas dinamicas de Google Play Services.
- **Sin Telemetria Externa**: Todo diagnostico, historial o registro de error permanece local y bajo control exclusivo del usuario.

---

## 3. Ergonomia de UI y Accesibilidad
- **Objetivos Tactiles Minimos**: Todos los botones y componentes interactivos deben tener un tamano minimo de 48 x 48 dp.
- **Gestion del Teclado**: Utilizar modificadores de padding de teclado (`imePadding`) para evitar que el teclado cubra campos de entrada de texto (por ejemplo el texto de la marca de agua).
- **Adaptabilidad Multi-Dispositivo**:
  - Telefonos: Layout vertical de una columna con barra inferior de navegacion (`NavigationBar`).
  - Tabletas y Plegables: Layout adaptativo de dos columnas (`ListDetailPaneScaffold` / Supporting Pane) con riel de navegacion lateral (`NavigationRail`).
- **Temas**: Soporte obligatorio para tema claro (Light Mode) y tema oscuro (Dark Mode).

---

## 4. Persistencia, Seguridad y Respaldos
- **Seguridad en Reposo**: Proteccion de plantillas y documentos mediante almacenamiento interno privado de la aplicacion (`context.filesDir` o base de datos local).
- **Bloqueo Biometrico**: Acceso a documentos y configuraciones restringido opcionalmente mediante `BiometricPrompt`.
- **Limpieza de Temporales**: Eliminacion inmediata de archivos intermedios de renderizado de imagen al salir o finalizar la exportacion.
