# Reglas Generales de Ingenieria y Calidad de Codigo

---

## 1. Principios de Diseno
- **Clean Architecture**: Separacion estricta en capas (Data, Domain, Presentation). Las dependencias siempre apuntan hacia adentro.
- **Flujo de Datos Unidireccional (UDF)**: La UI emite eventos hacia el ViewModel; el ViewModel expone un unico flujo inmutable de estado hacia la UI.
- **Inmutabilidad**: Todos los modelos de datos y estados deben ser inmutables (`data class` con propiedades `val`). Las modificaciones se realizan mediante `.copy()`.
- **SOLID**: Clases con responsabilidad unica, abstracciones e interfaces para desacoplar implementaciones y facilitar testing exhaustivo con dobles de prueba (test doubles).

---

## 2. Manejo de Errores y Seguridad
- No tragar excepciones silenciosamente.
- Modelar resultados y errores explicitamente mediante tipos sellados (`Result<T>` o `sealed interface`).
- Nunca mostrar errores tecnicos crudos al usuario final; mapear a mensajes claros y accionables.
- Politica Zero-Network: Garantizar ausencia total de transmisiones de datos no autorizadas fuera del dispositivo.

---

## 3. Verificacion y Pruebas (Pro Testing Standards)
- Cada caso de uso debe contar con pruebas unitarias que cubran tanto el camino feliz como casos de borde o error.
- Nomenclatura autodescriptiva en ingles siguiendo la convencion *Given-When-Then*.
- Cada cambio debe compilar limpiamente y pasar la suite completa de pruebas antes de considerarse terminado.
