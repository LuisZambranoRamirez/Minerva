# Dependencias para exportar movimientos de inventario

Decisión registrada el 24 de octubre de 2026 para el cambio
`inventory-report-export`.

| Biblioteca | Versión | Licencia | Uso |
|---|---:|---|---|
| Apache POI OOXML | 6.0.0 | Apache License 2.0 | Generación de libros XLSX. |
| Apache Commons CSV | 1.14.1 | Apache License 2.0 | Generación de CSV en UTF-8 con escape correcto. |
| Google Guava | 33.7.2-jre | Apache License 2.0 | Matriz producto por origen del resumen XLSX. |

## Compatibilidad

- POI 6.0.0 requiere Java 17 o superior y la documentación oficial incluye
  Java 25 entre las versiones soportadas. Se utiliza `poi-ooxml`, el artefacto
  oficial para XLSX.
- Commons CSV 1.14.1 requiere Java 8 o superior.
- Guava 33.7.2 usa la variante `jre`, apropiada para una aplicación de servidor.
- Minerva compila con Java 25 y Spring Boot 4.0.0, por lo que cumple los mínimos
  de las tres bibliotecas.

## Logging

No se agrega otra implementación de logging. Spring Boot mantiene SLF4J con
Logback como implementación transitiva; los reportes usarán esa fachada para
evitar bindings duplicados.

## Fuentes oficiales

- https://poi.apache.org/
- https://poi.apache.org/components/
- https://commons.apache.org/proper/commons-csv/download_csv.cgi
- https://github.com/google/guava/releases/tag/v33.7.2
