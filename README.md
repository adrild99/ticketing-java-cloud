# Sistema de Ticketing Avanzado (Consola Java)

Gestión y venta de entradas de eventos (Conciertos y Teatro).

## Características Principales Implementadas

### Arquitectura y Estructura de Datos
* **Programación Orientada a Objetos (POO):** Uso de herencia (diferentes pasarelas de pago como Bizum, Tarjeta y PayPal), constructores y Enums (`EstadoPedido`, `ModoAforo`).
* **Colecciones de Java:** * `ArrayList` para la gestión del catálogo de los eventos.
  * `Queue` para el encolado y procesamiento de pedidos pendientes (FIFO).
  * `Stack` para el historial de operaciones, permitiendo la opción de "Deshacer" compras y restaurar aforos automáticamente.

### Seguridad y Control de Errores
* **Gestión de Excepciones:** Control del menú principal y entradas numéricas mediante bloques `try-catch` para evitar cierres inesperados por `InputMismatchException`.
* **Validación de Datos:** Clase de utilidad independiente (`Validador`) que verifica los datos antes de procesar pagos:
  * **Bizum:** Formato de teléfono español válido com los 9 dígitos.
  * **PayPal:** Formato estándar de correo electrónico.
  * **Tarjeta:** 16 dígitos para el número de la tarjeta, control de caracteres en el titular, fecha de caducidad (MM/YY) y código CVV de 3 dígitos.

### Persistencia de Datos
* **Escritura:** Generación automática de tickets de venta en formato texto plano dentro de la ruta `src/registroEntradas/RegistroVentas.txt`, con creación automática de directorios si no existen.
* **Lectura y Estadísticas:** Motor analítico que lee el fichero histórico en tiempo real, parsea los importes ignorando líneas corruptas (`FileNotFoundException`, `NumberFormatException`) y devuelve el balance total de ingresos y pedidos procesados.

### Experiencia de Usuario (UX)
* Implementación de bucles `while(true)` para permitir reintentos infinitos en las pasarelas de pago sin perder los datos de la cesta.
* Búsqueda de eventos y sesiones tolerante a fallos tipográficos mediante `.equalsIgnoreCase()`.
