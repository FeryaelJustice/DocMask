# DocMask: Protector de Documentos de Identidad (100% Offline)

[![Offline First](https://img.shields.io/badge/Privacy-100%25%20Offline%20(Zero--Network)-brightgreen)](#garantia-de-privacidad-radical)
[![Android](https://img.shields.io/badge/Platform-Android%20(Phone%20%26%20Tablet)-blue)](#arquitectura-y-adaptabilidad)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-purple)](#stack-tecnologico)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3%20Adaptive-green)](#stack-tecnologico)

**DocMask** es una aplicacion Android disenada para proteger la identidad de los ciudadanos cuando comparten copias de su DNI, Pasaporte o NIE para tramites habituales (alquiler de vivienda, reservas hoteleras, procesos de seleccion o tramites administrativos).

---

## El Problema Real

Tanto la **Agencia Espanola de Proteccion de Datos (AEPD)** como la **Policia Nacional** alertan reiteradamente del grave peligro de enviar fotografias o escaneos del documento de identidad sin editar. Con una copia sin proteger, los ciberdelincuentes pueden:
- Solicitar microcreditos a tu nombre.
- Abrir cuentas bancarias online o lineas telefonicas para fraudes.
- Suplantar tu identidad en plataformas de comercio electronico.

La recomendacion oficial es tajante:
1. **Pixelar o tachar los datos no requeridos** (como el numero de soporte, codigo CAN o firma).
2. **Convertir la imagen a escala de grises** para dificultar su reutilizacion en tramites oficiales.
3. **Cruzar el documento con una marca de agua visible** que especifique claramente su finalidad ("Copia exclusiva para alquiler en fecha DD/MM/AAAA").

### Por que las soluciones actuales fallan
Las webs de marcas de agua y aplicaciones genericas existentes solicitan subir las fotos de tus documentos mas sensibles a servidores remotos o inundan la app con anuncios con acceso a internet. **Nadie deberia confiar la foto de su DNI a un servidor web desconocido.**

---

## La Propuesta de Valor de DocMask: Privacidad Radical

- **Cero Permisos de Red**: En el `AndroidManifest.xml` de DocMask **NO existe** el permiso `android.permission.INTERNET`. Es imposible que la aplicacion envie tus fotografias a ningun servidor externo, porque el propio sistema operativo Android le niega el acceso al stack de red.
- **Inteligencia Artificial 100% Local**: Usamos **Google ML Kit Bundled** (empaquetado directamente en el binario). La deteccion de texto y rostros se realiza integramente en la CPU/GPU del movil, incluso en modo avion.
- **Rapidez Extrema**: Selecciona la foto, pulsa un boton para pixelar los datos criticos detectados por la IA, aplica la marca de agua cruzada y exporta en cuestion de segundos.

---

## Caracteristicas Principales

### Version Gratuita (Free)
- Carga y captura de fotografias de documentos.
- Marca de agua cruzada configurable con texto personalizado y fecha automatica.
- Conversion opcional a escala de grises.
- Exportacion manual de imagen protegida.
- 100% offline y libre de anuncios.

### Version Pro (Pago Unico In-App)
- **Deteccion Automatica por IA**: Localizacion inteligente de numero de soporte, codigo CAN, firma y rostro.
- **Pixelado Instantaneo con un Toque**: Enmascaramiento automatico de todas las areas sensibles o seleccion manual con pincel.
- **Plantillas Frecuentes**: Guarda tus plantillas favoritas de marcas de agua ("Alquiler de piso", "Check-in hotel", "Tramite laboral").
- **Procesamiento por Lotes y PDF Seguro**: Une anverso y reverso del documento en un unico documento PDF cifrado y protegido.
- **Bloqueo Biometrico**: Acceso a la aplicacion y a tus plantillas mediante huella dactilar o desbloqueo facial local (`BiometricPrompt`).

---

## Arquitectura y Adaptabilidad

La aplicacion sigue los principios de **Clean Architecture**, **SOLID** y el patron **MVVM/MVI** con Unidirectional Data Flow (UDF):
- **core/**: Modulos de utilidades de renderizado en Canvas, extensiones graficas y modelos transversales.
- **data/**: Motores locales de ML Kit, repositorios locales, persistencia Room y DataStore.
- **domain/**: Casos de uso de negocio y entidades inmutables puras.
- **presentation/**: Interfaces construidas con Jetpack Compose Material 3 Adaptive.

### Experiencia Adaptativa (Multi-Dispositivo)
- **Telefonos**: Layout vertical optimizado para manejo con una mano y navegacion inferior (`NavigationBar`).
- **Tabletas y Plegables**: Layout de dos columnas (`ListDetailPaneScaffold`) con barra de navegacion lateral (`NavigationRail`), permitiendo visualizar el documento en alta resolucion en un panel mientras se ajustan las herramientas en el panel contiguo.

---

## Stack Tecnologico

| Tecnologia | Descripcion |
| :--- | :--- |
| **Kotlin** | Lenguaje principal (version 2.4.21) |
| **Android Gradle Plugin** | AGP 9.4.1 con Gradle 9.6.0 |
| **Jetpack Compose** | Compose BOM 2026.09.00 y Material 3 |
| **Compose Adaptive** | `material3-adaptive-navigation-suite` y `adaptive-layout` |
| **Inyeccion de Dependencias** | Koin 4.x (100% Kotlin, sin transformacion de bytecode) |
| **Machine Learning** | Google ML Kit Text Recognition & Face Detection (Bundled) |
| **Persistencia** | Room Database y DataStore Preferences |
| **Seguridad** | AndroidX Biometric y Android Security Crypto |
| **Facturacion** | Google Play Billing Library (mediante IPC local) |
| **Testing** | JUnit, MockK, Turbine, Coroutines Test, Compose Test Rule |

---

## Metodologia SDMD

Este repositorio aplica formalmente **Spec-Driven Mobile Development (SDMD)**:
- Toda funcionalidad nace de una especificacion (`docs/features/<feature>/spec.md`).
- Se planifica la arquitectura con hechos verificados (`docs/features/<feature>/plan.md`).
- Se ejecuta mediante tareas atómicas y tests unitarios / instrumentados (`docs/features/<feature>/tasks.md`).
