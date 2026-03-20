#  Advanced Ticketing System (Java Cloud Edition)

Professional ticket management and sales system for events (Concerts, Cinema, and Theater) featuring real-time cloud persistence.

---

## Recent Cloud Updates

### Hybrid Data Architecture (Cloud + Local)
* **Oracle Cloud Persistence:** Total migration from legacy binary files (`.dat`) to a relational **Oracle Autonomous Database**.
* **Secure Connectivity (mTLS):** Implementation of JDBC connection via **Oracle Wallet**, ensuring end-to-end data encryption between the Java application and the cloud.
* **Real-time Synchronization:** The system performs automatic inventory updates using SQL `UPDATE` and `SELECT` queries, ensuring capacity accuracy across multiple concurrent sessions.

### 🏛️ Relational Database Modeling
* **Composite Primary Keys:** Implementation of referential integrity through composite keys (`id_event` + `id_session`), allowing for an intuitive session numbering (1, 2, 3...) that is independent for each specific event.
* **Capacity Shield Logic:** A security algorithm within the `Session` class that prevents "available capacity" from exceeding the "total capacity," protecting the database from data corruption during multi-step refunds.

### 🛠️ Code Refactoring & Logic
* **High-Performance Menu:** Transitioned from nested `if-else` structures to a `switch-case` control engine, significantly improving code readability and maintainability.
* **Advanced Business Validation:** * **Credit Cards:** Logical expiration control (comparing MM/YY input against the system's `YearMonth` clock).
    * **Emails:** Strict validation using Regular Expressions (Regex) to filter out invalid domain structures.

---

##  Core Features

### OOP & Data Structures
* **Polymorphism:** Inheritance-based payment gateways (`Bizum`, `CreditCard`, `PayPal`) with specific validation logic for each account type.
* **Dynamic Collections:** * `ArrayList` for the event catalog retrieved from the cloud.
    * `Queue` for FIFO (First-In-First-Out) processing of pending orders.
    * `Stack` for operation history, enabling the **"Undo"** feature with automatic capacity restoration in both local memory and the Oracle database.

### Security & Robustness
* **Custom Exception Handling:** Implementation of specific exceptions like `AsientoNoDisponibleException` (SeatNotAvailable) for fine-grained flow control.
* **Defensive Programming:** Numerical input validation via `try-catch` blocks and `Scanner.nextLine()` buffer clearing to prevent infinite loops on user input errors.

### Logs & Auditing
* **Local Ticketer:** Automatic generation of sales receipts in plain text at `src/registroEntradas/RegistroVentas.txt` as a physical backup for cloud transactions.
* **Statistics Engine:** A real-time analyzer that parses the sales history to return total revenue balances and transaction volume.

---

## User Experience (UX)
* **Interactive Seat Map:** Dynamic generation of seating grids for numbered events, visually distinguishing between **Normal [N]** and **VIP [V]** seats.
* **Dynamic Pricing:** Automatic calculation of final prices based on event type (Cinema, Theater, Concert) and specific zone multipliers.

---

## Requirements
* **Java SDK:** 17 or higher.
* **Database:** Oracle Autonomous DB (Cloud).
* **Driver:** Oracle JDBC (ojdbc11.jar).
