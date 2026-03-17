# Advanced Ticketing System (Java Console)

Management and sale of event tickets (Concerts and Theater).

## Key Implemented Features

### Architecture and Data Structures
* **Object-Oriented Programming (OOP):** Use of inheritance (different payment gateways such as Bizum, Credit Card, and PayPal), constructors, and Enums (`OrderState`, `CapacityMode`).
* **Design Patterns & Formatting:** Use of global constants (`public static final`) for consistent date and time formatting across the application using `DateTimeFormatter`. Classes implement multiple interfaces simultaneously (e.g., `Vendible` and `Serializable`) to separate concerns.
* **Java Collections:** * `ArrayList` for managing the event catalog.
  * `Queue` for queuing and processing pending orders (FIFO).
  * `Stack` for operation history, allowing the "Undo" option for purchases and automatic capacity restoration.

### Security and Error Handling
* **Exception Management:** Main menu and numerical input control through `try-catch` blocks to prevent unexpected crashes due to `InputMismatchException`.
* **Data Validation:** Independent utility class (`Validator`) that verifies data before processing payments:
  * **Bizum:** Valid 9-digit Spanish phone format.
  * **PayPal:** Standard email format.
  * **Credit Card:** 16-digit card number, character control for the cardholder name, expiration date (MM/YY), and 3-digit CVV code.

### Data Persistence
* **Writing (Logs):** Automatic generation of sales tickets in plain text format within the path `src/registroEntradas/RegistroVentas.txt`, with automatic directory creation if they do not exist.
* **Reading and Statistics:** Analytical engine that reads the historical file in real-time, parses amounts while ignoring corrupted lines (`FileNotFoundException`, `NumberFormatException`), and returns the total revenue balance and processed orders.
* **Binary Serialization (State Management):** Preservation of the entire application state (catalog, modified capacities, and reservations) across different sessions. The system automatically loads and saves the objects using `ObjectOutputStream` and `ObjectInputStream` into a `datos.dat` file, curing volatile memory loss upon program restart.

### User Experience (UX)
* Implementation of `while(true)` loops to allow infinite retries in payment gateways without losing shopping cart data.
* Search for events and sessions tolerant of typographical errors using `.equalsIgnoreCase()`.
