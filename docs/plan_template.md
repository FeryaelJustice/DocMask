# [BORRADOR | APROBADO] Plan Tecnico de Arquitectura: [Nombre de la Feature]

- **Feature vinculada**: `docs/features/<nombre_feature>/spec.md`
- **Estado**: [BORRADOR | APROBADO]
- **Fecha**: YYYY-MM-DD

---

## 1. Hechos Verificados en el Proyecto (Sin Alucinaciones)
- **Lenguaje / JVM**: Kotlin / Java 11 (target) / Java 25 launcher
- **Librerias y Versiones comprobadas en libs.versions.toml**:
  - [Libreria 1]: [Version exacta]
  - [Libreria 2]: [Version exacta]

---

## 2. Impacto Arquitectonico y Capas

### 2.1. Capa de Datos (Data Layer)
- **Entidades / Tablas**: [Definicion de Entity Room, DataStore o ficheros]
- **DAOs / Data Sources**: [Contratos de DAOs y procesadores locales]
- **Mapeadores (Mappers)**: [DTO <-> Entity <-> Domain Model]
- **Contratos de Repositorio**: [Interfaces reactivas exponiendo Flow<T>]

### 2.2. Capa de Dominio (Domain Layer)
- **Modelos de Dominio**: [Modelos inmutables libres de dependencias de framework]
- **Casos de Uso (Use Cases)**: [Clases individuales con operador invoke]

### 2.3. Capa de Presentacion (Presentation / UI Layer)
- **ViewModel**: [Gestion de estado con StateFlow<UiState> y manejo de eventos]
- **Pantallas / Componentes UI**: [Jetpack Compose respetando mobile_guidelines.md y NavigationSuiteScaffold para adaptabilidad tablet/telefono]

---

## 3. Estrategia de Testing (Pro Test Battery)
- **Tests Unitarios**:
  - Casos de uso y logica de negocio (JUnit, MockK)
  - ViewModels y reduccion de estados (TestDispatcher, Turbine)
- **Tests Instrumentados**:
  - Room DAOs en emulador/dispositivo
  - Tests de integracion de UI con Compose Test Rule
  - Pruebas de adaptabilidad (pantalla compacta vs pantalla expandida)

---

## 4. Riesgos y Plan de Mitigacion
- [Rendimiento en bitmaps de alta resolucion, consumo de memoria en rotacion o fallos de deteccion]
