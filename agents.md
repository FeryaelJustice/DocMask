# Guia del Proyecto: DocMask (Protector de Documentos de Identidad 100% Offline)

Este documento es la guia de navegacion y contexto tecnico para cualquier agente de IA o desarrollador trabajando en DocMask.

---

## 1. Mision y Propuesta de Valor

DocMask es una utilidad Android disenada para proteger documentos de identidad espanoles y europeos (DNI, Pasaporte, NIE, carnet de conducir) al enviarlos a terceros (alquiler de vivienda, reservas hoteleras, tramites laborales).
- **Politica Zero-Network (Privacidad Radical)**: La aplicacion **NO TIENE** el permiso `android.permission.INTERNET` en el `AndroidManifest.xml`. No se realizan conexiones de red, no hay telemetria de terceros y no se sube ningun archivo a servidores externos.
- **Procesamiento 100% Local**:
  - Deteccion automatica de datos sensibles mediante Google ML Kit empaquetado (*Bundled Text Recognition y Face Detection*): numero de soporte, codigo CAN, firma, rostro y fecha de nacimiento.
  - Pixelado interactivo con un toque o pincel manual.
  - Conversion opcional a escala de grises para evitar copias fraudulentas a color.
  - Superposicion de marca de agua cruzada ("Copia exclusiva para alquiler en fecha X").
  - Union de anverso y reverso en un unico archivo PDF seguro.
  - Bloqueo biometrico local para acceso protegido a la app y plantillas.

---

## 2. Stack Tecnologico

- **Lenguaje**: Kotlin 2.4.21
- **Gradle & Plugin**: Gradle 9.6.0 con Android Gradle Plugin 9.4.1
- **SDK**: `minSdk = 24`, `compileSdk = release(37)`, `targetSdk = 37`
- **Interfaz de Usuario**:
  - Jetpack Compose con Compose BOM 2026.09.00
  - Material 3 Design
  - Adaptabilidad para Telefonos, Tabletas y Plegables: `material3-adaptive-navigation-suite`, `adaptive-layout`, `adaptive-navigation`
  - Navegacion: Jetpack Navigation Compose (Type-safe)
- **Inyeccion de Dependencias**: Koin (`koin-android`, `koin-androidx-compose`)
- **Inteligencia Artificial Local**:
  - Google ML Kit Text Recognition (*Bundled*)
  - Google ML Kit Face Detection (*Bundled*)
- **Persistencia & Seguridad**:
  - Room Database
  - DataStore Preferences
  - AndroidX Biometric (`BiometricPrompt`)
  - Google Play Billing Library (`billing-ktx` mediante IPC local de Play Store)
- **Testing**:
  - JUnit 4 / JUnit 5
  - MockK
  - Turbine (para testeo de StateFlow / Flow)
  - Kotlinx Coroutines Test
  - AndroidX Compose UI Test (`createComposeRule`)
  - Espresso Core

---

## 3. Arquitectura y Reglas de Codigo (Clean Architecture + SOLID)

Estructura de paquetes:
```text
com.feryaeljustice.docmask/
├── core/             # Helpers, extensiones de bitmap/canvas, design system, errores sellados
├── data/             # DAOs, implementaciones de ML Kit, DataStore, repositorios locales
├── domain/           # Entidades puras inmutables, interfaces de repositorios, casos de uso
├── presentation/     # ViewModels (StateFlow), pantallas Compose, componentes adaptativos
└── di/               # Modulos Koin
```

Reglas estrictas:
1. **Unidirectional Data Flow (UDF)**: La UI emite eventos; el ViewModel expone un unico `StateFlow<UiState>`.
2. **Inmutabilidad**: Todos los modelos son `data class` inmutables con propiedades `val`.
3. **Manejo de Errores**: Emplear tipos sellados y `Result<T>`. Prohibido tragar excepciones.
4. **Zero-Network**: Queda terminantemente prohibido anadir `android.permission.INTERNET` al manifiesto o incorporar librerias que requieran conexion de red obligatoria (Firebase Analytics, Crashlytics, etc.).

---

## 4. Estandar de Pruebas Profesionales (Pro Test Standards)

- Toda la logica de negocio en `domain` (Casos de Uso) debe tener pruebas unitarias con cobertura exhaustiva.
- Los ViewModels se testean con `MainDispatcherRule`, `StandardTestDispatcher` y `Turbine`.
- Las pantallas Compose se auditan mediante pruebas instrumentadas verificando adaptabilidad en pantallas compactas (telefono) y expandidas (tablet).
- **Convencion de Nombres de Test**: Obligatoriamente en ingles, autodescriptivos, siguiendo el patron *Given-When-Then*.
  - Ejemplo: `givenSpanishDniImage_whenSensitiveRegionsDetected_thenSupportNumberAndCanAreReturned()`
  - Ejemplo: `givenCompactDeviceWidth_whenScreenRendered_thenShowsBottomNavigationBar()`
  - Ejemplo: `givenExpandedTabletDevice_whenScreenRendered_thenShowsNavigationRailAndTwoPanes()`

---

## 5. Comandos de Terminal

- **Compilacion de desarrollo**:
  ```powershell
  .\gradlew.bat assembleDebug
  ```
- **Pruebas unitarias**:
  ```powershell
  .\gradlew.bat testDebugUnitTest
  ```
- **Pruebas instrumentadas (con dispositivo/emulador conectado)**:
  ```powershell
  .\gradlew.bat connectedAndroidTest
  ```
- **Verificacion de grafo de dependencias**:
  ```powershell
  .\gradlew.bat dependencies --configuration debugRuntimeClasspath
  ```

---

## 6. Metodologia de Trabajo

Seguimos **SDMD** (*Spec-Driven Mobile Development*):
- No se genera codigo de produccion sin antes contar con `spec.md` en estado `[APROBADO]`.
- Se planifica la arquitectura en `plan.md` con hechos verificados.
- Se implementa tarea a tarea segun `tasks.md`, compilando y testeando tras cada cambio.
