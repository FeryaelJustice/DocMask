# Politica de Privacidad de DocMask

> **Garantia Zero-Network:** DocMask esta disenada bajo el principio de Privacidad Radical. La aplicacion no dispone del permiso de red (`android.permission.INTERNET`), por lo que tecnicamente es imposible que tus fotos, documentos o datos personales salgan de tu dispositivo.

- **Ultima actualizacion:** 19 de octubre de 2026
- **Aplicacion:** DocMask (Android)
- **Desarrollador:** Equipo DocMask / Feryael Justice
- **Modelo:** 100% Offline / Privacidad Radical

---

## 1. Compromiso Fundamental y Mision

DocMask es una utilidad Android disenada para proteger copias de documentos de identidad (DNI, Pasaporte, NIE, carnet de conducir) antes de compartirlos con terceros (arrendadores, reservas hoteleras, tramites laborales, agencias o administraciones publicas).

A diferencia de servicios web y aplicaciones convencionales que suben imagenes confidenciales a servidores en la nube, DocMask opera de manera **estrictamente local y offline**. Toda la manipulacion, analisis por inteligencia artificial, pixelado, insercion de marcas de agua y generacion de documentos PDF se ejecuta de principio a fin en el procesador y memoria de tu propio dispositivo movil.

---

## 2. Arquitectura de Red Cero (Zero-Network Policy)

Para garantizar una privacidad verificable por cualquier usuario o auditor de seguridad:

- **Sin permiso de Internet:** El archivo de manifiesto (`AndroidManifest.xml`) elimina explicitamente cualquier permiso de red mediante `tools:node="remove"` para `android.permission.INTERNET` y `android.permission.ACCESS_NETWORK_STATE`. El sistema operativo Android rechaza cualquier intento de comunicacion exterior.
- **Sin servidores propios ni remotos:** No existen servidores de DocMask ni bases de datos externas.
- **Sin telemetria ni analiticas:** No se incluye Google Analytics, Firebase Analytics, Crashlytics ni ningun SDK de seguimiento de comportamiento o uso.
- **Sin publicidad:** La aplicacion no incluye anuncios ni bibliotecas de rastreo publicitario.

---

## 3. Tratamiento de Documentos e Inteligencia Artificial Local

Todas las funciones de DocMask operan de forma exclusiva en el almacenamiento y memoria local del dispositivo:

- **Lectura y edicion de fotos:** Las imagenes seleccionadas de tu galeria o capturadas con la camara se cargan temporalmente en la memoria de la aplicacion y nunca salen del dispositivo.
- **Deteccion inteligente de campos (Google ML Kit Bundled):** Los modelos de reconocimiento optico de caracteres (OCR) y deteccion facial estan empaquetados directamente dentro del binario de la app. Funcionan incluso en modo avion y sin conexion a Internet, detectando de forma autonoma numero de soporte, codigo CAN, firma, rostro o fecha de nacimiento sin enviar datos a Google ni a terceros.
- **Pixelado y censura:** El oscurecimiento o pixelado de informacion sensible se aplica directamente sobre los mapas de bits mediante el motor Canvas nativo.
- **Marcas de agua y escala de grises:** La superposicion de textos cruzados y la transformacion a escala de grises son calculadas en el hardware local.
- **Union a PDF seguro y exportacion:** La combinacion de anverso y reverso en un documento PDF protegido o imagenes procesadas se almacena en el directorio temporal interno de la aplicacion y solo se comparte si el usuario decide pulsar explicitamente el boton de compartir del sistema.

---

## 4. Permisos del Sistema Operativo y su Uso

DocMask unicamente solicita o emplea los permisos indispensables para su funcionamiento local:

| Permiso / Componente | Finalidad Exclusiva | Almacenamiento / Transmision |
| :--- | :--- | :--- |
| **Acceso a Archivos / Photo Picker** | Permitir al usuario escoger la foto del documento que desea editar o guardar el resultado. | Local. Solo se accede al archivo seleccionado expresamente. |
| **Camara (opcional)** | Tomar una fotografia en vivo de un documento para su edicion. | Local. No se almacena ninguna captura sin accion voluntaria del usuario. |
| **Biometria (`BiometricPrompt`)** | Bloqueo y desbloqueo local opcional de la aplicacion y de las plantillas. | Gestionado exclusivamente por el hardware seguro del sistema (Android Keystore). DocMask nunca tiene acceso ni almacena huellas dactilares o datos biometricos. |
| **FileProvider** | Permitir compartir el documento procesado con aplicaciones externas elegidas por el usuario. | Rutas privadas temporales con permisos de lectura estrictamente delegados. |

---

## 5. Persistencia y Almacenamiento Local

DocMask minimiza la conservacion de datos en reposo:

- **Plantillas personalizadas:** Los textos predefinidos para marcas de agua guardados por el usuario se conservan en la base de datos local Room y DataStore en el almacenamiento privado de la app.
- **Copias de seguridad en la nube deshabilitadas:** La directiva `android:allowBackup="false"` esta configurada para impedir que las copias de seguridad automaticas de Android extraigan imagenes o plantillas privadas hacia la nube.
- **Control total:** Al desinstalar la aplicacion o limpiar sus datos desde los Ajustes de Android, se eliminan permanentemente todas las configuraciones y plantillas locales.

---

## 6. Facturacion y Version Pro (Google Play Billing)

La modalidad Pro se adquiere mediante pago unico a traves de la pasarela oficial de **Google Play Billing** por IPC del sistema operativo:

- DocMask no recopila, procesa ni almacena datos bancarios, numeros de tarjeta ni direcciones de facturacion.
- Toda la transaccion es gestionada directamente por Google Play con arreglo a sus propios terminos y politicas de seguridad.

---

## 7. Cumplimiento con RGPD / GDPR y Derechos del Usuario

Conforme al Reglamento General de Proteccion de Datos (RGPD) de la Union Europea y a la LOPDGDD:

- Al no existir cuentas de usuario, telemetria ni almacenamiento en servidores remotos, no se recopila ningun dato de caracter personal susceptible de tratamiento centralizado.
- El usuario mantiene el control absoluto y exclusivo sobre sus archivos.
- El ejercicio de los derechos de acceso, rectificacion, supresion y oposicion se realiza de forma directa e instantanea borrando los datos de la aplicacion o desinstalando la misma.

---

## 8. Menores de Edad

DocMask no solicita ni recopila datos de ningun individuo, incluidos menores de edad. Su proposito es servir como herramienta de utilidad local para salvaguardar la privacidad documental.

---

## 9. Modificaciones a esta Politica

Cualquier actualizacion o ajuste en la presente politica sera reflejado en este documento con su respectiva fecha de revision. La garantia fundacional de funcionamiento 100% offline y ausencia de conexion de red permanecera inalterada.

---

## 10. Contacto y Soporte

Para dudas o consultas respecto a esta politica o la seguridad de la aplicacion:

- **Proyecto:** DocMask para Android
- **Desarrollador:** Equipo DocMask / Feryael Justice
- **Canal:** Repositorio oficial del proyecto
