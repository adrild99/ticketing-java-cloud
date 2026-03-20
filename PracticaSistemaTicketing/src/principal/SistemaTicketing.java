package principal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.Stack;

import excepciones.AsientoNoDisponibleException;

import modelo.Asiento;
import modelo.Categoria;
import modelo.Cine;
import modelo.Concierto;
import modelo.Evento;
import modelo.ModoAforo;
import modelo.Sesion;
import modelo.Teatro;

import pagos.Pago;
import pagos.PagoBizum;
import pagos.PagoPayPal;
import pagos.PagoTarjeta;

import pedidos.Carrito;
import pedidos.Entrada;
import pedidos.EstadoPedido;
import pedidos.Operacion;
import pedidos.Pedido;
import pedidos.TipoOperacion;
import utilidades.ConexionDB;
import utilidades.Validador;

import java.time.format.DateTimeFormatter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.io.BufferedReader;
import java.io.FileReader;

public class SistemaTicketing {

    public static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm:ss");

    private ArrayList<Evento> catalogo = new ArrayList<>();
    private Stack<Operacion> historial = new Stack<>();
    private Queue<Pedido> colaPedidos = new LinkedList<>();
    private Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        SistemaTicketing sistema = new SistemaTicketing();
        sistema.cargarDatosDesdeNube();
        sistema.menu();
    }

    public void inicializarDatos() {
        System.out.println("Cargando catálogo de eventos...");

        Concierto c1 = new Concierto("Festival Rock", "Wizink Center", Categoria.CONCIERTO, true, false);
        Concierto c2 = new Concierto("Concierto Indie", "Sala Riviera", Categoria.CONCIERTO, false, false);

        Sesion s1 = new Sesion(LocalDateTime.now().plusDays(10), 500, 500, ModoAforo.GENERAL);
        Sesion s2 = new Sesion(LocalDateTime.now().plusDays(10), 500, 500, ModoAforo.GENERAL);

        c1.addSesion(s1);
        c2.addSesion(s2);

        Teatro t1 = new Teatro("El Rey León", "Teatro Lope de Vega", Categoria.TEATRO, false, true);
        Sesion s3 = new Sesion(LocalDateTime.now().plusDays(5), 100, 100, ModoAforo.NUMERADO);
        Sesion s4 = new Sesion(LocalDateTime.now().plusDays(6), 100, 100, ModoAforo.NUMERADO);
        Cine p1 = new Cine("Interstellar", "Mk2 Cinesur", Categoria.CINE, false, false);
        Sesion s5 = new Sesion(LocalDateTime.now().plusDays(6), 60, 60, ModoAforo.NUMERADO);

        t1.addSesion(s3);
        t1.addSesion(s4);
        p1.addSesion(s5);

        this.catalogo.add(c1);
        this.catalogo.add(c2);
        this.catalogo.add(t1);
        this.catalogo.add(p1);
    }

    public void menu() {
        boolean salir = false;

        while (!salir) {
            System.out.println("\n--> SISTEMA DE VENTA DE ENTRADAS <--");
            System.out.println("1. Ver catálogo");
            System.out.println("2. Comprar entradas");
            System.out.println("3. Deshacer última operación");
            System.out.println("4. Procesar cola de pedidos");
            System.out.println("5. Historial global de pedidos");
            System.out.println("6. Ver estadísticas de ventas");
            System.out.println("7. SALIR");

            System.out.print("Elige una opción: ");

            try {
                int seleccion = sc.nextInt();
                sc.nextLine(); // Limpiar el buffer

                switch (seleccion) {
                    case 1:
                        verCatalogo();
                        break;
                    case 2:
                        iniciarCompra();
                        break;
                    case 3:
                        deshacerUltimaOperacion();
                        break;
                    case 4:
                        procesarColaPedidos();
                        break;
                    case 5:
                        leerHistorialVentas();
                        break;
                    case 6:
                        mostrarEstadisticas();
                        break;
                    case 7:
                        salir = true;
                        System.out.println("Hasta prontoooooo");
                        break;
                    default:
                        System.out.println("Opción incorrecta, elige un número del 1 al 7.");
                        break;
                }

            } catch (java.util.InputMismatchException e) {
                System.out.println("Error: Debes introducir un NÚMERO, no letras.\n");
                sc.nextLine(); // Limpiar el buffer tras el error
            }
        }
    }

    public void verCatalogo() {
        System.out.println("\n--- CATÁLOGO DE EVENTOS ---");

        if (this.catalogo.isEmpty()) {
            System.out.println("Lo sentimos, no hay eventos programados en este momento.");
            return;
        }
        for (Evento e : this.catalogo) {
            System.out.println(e.toString());

            if (e.getSesiones().isEmpty()) {
                System.out.println("  -> (Sin sesiones programadas todavía)");
            } else {
                for (Sesion s : e.getSesiones()) {
                    // decimos a la fecha de la sesión que se formatee usando nuestro molde
                    String fechaTexto = s.getFechaHora().format(FORMATO_FECHA);

                    // Imprimimos el texto ya formateado
                    System.out.println("  -> Sesión: " + s.getIdSesion() + " | Fecha: " + fechaTexto
                            + " | Entradas libres: " + s.getAforoDisponible());
                }
            }
        }
    }

    public void iniciarCompra() {
        System.out.println("INICIANDO COMPRA: ");

        verCatalogo();

        // --- 1. PEDIR Y VALIDAR EL EVENTO ---
        Evento eventoElegido = null;
        while (true) {
            System.out.print("\nEscribe el ID del evento que quieres (ej. 1, 01, EV-01): ");
            String inputEvento = sc.nextLine();

            if (utilidades.Validador.esIdEventoValido(inputEvento)) {
                // Limpiamos y formateamos la entrada
                String soloNumeros = inputEvento.replaceAll("[^0-9]", "");
                int numeroEvento = Integer.parseInt(soloNumeros);
                String idOficialEvento = String.format("EV-%02d", numeroEvento);

                // Buscamos en el catálogo
                for (Evento e : this.catalogo) {
                    if (e.getId().equalsIgnoreCase(idOficialEvento)) {
                        eventoElegido = e;
                        break;
                    }
                }

                if (eventoElegido != null) {
                    break; // Evento encontrado, salimos del bucle
                } else {
                    System.out.println(
                            "El evento " + idOficialEvento + " no existe en el catálogo. Inténtalo de nuevo.");
                }
            } else {
                System.out.println("Formato incorrecto. Escribe solo el número o usa EV-XX.");
            }
        }

        // --- 2. PEDIR Y VALIDAR LA SESIÓN ---
        Sesion sesionElegida = null;
        while (true) {
            System.out.print("Escribe el ID de la sesión (ej. 1, 01, SES-01): ");
            String inputSesion = sc.nextLine();

            if (utilidades.Validador.esIdSesionValido(inputSesion)) {
                // Limpiamos y formateamos la entrada
                String soloNumeros = inputSesion.replaceAll("[^0-9]", "");
                int numeroSesion = Integer.parseInt(soloNumeros);
                String idOficialSesion = String.format("SES-%02d", numeroSesion);

                sesionElegida = eventoElegido.getSesionById(idOficialSesion);

                if (sesionElegida != null) {
                    break; // Sesión encontrada, salimos del bucle
                } else {
                    System.out.println(
                            "La sesión " + idOficialSesion + " no existe para este evento. Inténtalo de nuevo.");
                }
            } else {
                System.out.println("Formato incorrecto. Escribe solo el número o usa SES-XX.");
            }
        }

        System.out.print("¿Cuántas entradas quieres comprar?: ");
        int cantidad = 0;
        // para coger el error de letras y de numeros negativos
        try {
            cantidad = sc.nextInt();
            sc.nextLine();
        } catch (java.util.InputMismatchException e) {
            System.out.println("Error: Debes introducir un número entero. Compra cancelada.");
            sc.nextLine(); // Limpiar el buffer
            return; // Salimos del método directamente, no hay nada que devolver a la nube aún
        }

        if (cantidad <= 0) {
            System.out.println("Error: Debes comprar al menos 1 entrada. Compra cancelada.");
            return;
        }

        if (sesionElegida.hayDisponibilidad(cantidad) == false) {
            System.out.println("No hay suficientes asientos disponibles para esta sesion.");
            return;
        }

        System.out.println("Hay disponibilidad, preparando carrito...");

        Carrito miCarrito = new Carrito();
        ArrayList<Entrada> entradasCompradas = new ArrayList<>();
        double precioBase = 20.0; // POR EJEMPLO, 20 EUROS

        // Declaramos la lista aquí fuera para poder usarla luego si el pago falla y hay
        // que devolverlos
        ArrayList<Asiento> asientosReservados = null;

        if (sesionElegida.getModo() == ModoAforo.GENERAL) {
            sesionElegida.reservarGeneral(cantidad);
            actualizarAforoEnNube(sesionElegida, eventoElegido.getId());

            for (int i = 0; i < cantidad; i++) {
                double precioFinal = precioBase * eventoElegido.getRecargoBase();
                Entrada e = new Entrada(eventoElegido.getId(), sesionElegida.getIdSesion(), null, precioFinal);
                miCarrito.addEntrada(e);
                entradasCompradas.add(e);
            }
        } else {
            // Preguntamos cuántas quiere comprar (por seguridad)
            System.out.println("Asientos disponibles en esta sesión:");
            sesionElegida.mostrarAsientosLibres(); // Este método lo creamos ahora en Sesion

            asientosReservados = new ArrayList<>(); // Inicializamos la lista de los que elija

            for (int i = 0; i < cantidad; i++) {
                System.out.print("Elige el ID del asiento para la entrada " + (i + 1) + ": ");
                String idAsiento = sc.nextLine();

                try {
                    // Intentamos buscar el asiento (Si no existe, salta directo al catch)
                    Asiento a = sesionElegida.buscarAsientoPorId(idAsiento);

                    // Comprobamos si el asiento existe pero ya estaba comprado
                    if (a.getReservado()) {
                        throw new AsientoNoDisponibleException(idAsiento); // Forzamos el error
                    }

                    a.setReservado(true);
                    asientosReservados.add(a);

                    double precioFinal = precioBase * eventoElegido.getRecargoBase() * a.getMultiplicadorZona();

                    Entrada e = new Entrada(eventoElegido.getId(), sesionElegida.getIdSesion(), a, precioFinal);
                    miCarrito.addEntrada(e);
                    entradasCompradas.add(e);

                    System.out.println("Asiento " + idAsiento + " añadido. Precio: " + precioFinal + " euros");

                } catch (AsientoNoDisponibleException ex) {
                    // Si falla cualquier cosa arriba, el programa cae aquí y no se cuelga
                    System.out.println("Error " + ex.getMessage());
                    i--; // disminuye 1 al contador para no perder la entrada y que el usuario vuelva a
                         // intentarlo
                }
            }
            sesionElegida.reservarGeneral(cantidad); // disminuye el número total del aforo en Java
            actualizarAforoEnNube(sesionElegida, eventoElegido.getId());
        }

        System.out.println("Total a pagar: " + miCarrito.calcularTotal() + " EUROS");

        String emailCliente;
        boolean emailCorrecto = false;

        do {
            System.out.print("Introduce tu email para enviarte los tickets: ");
            emailCliente = sc.nextLine();

            if (utilidades.Validador.esEmailValido(emailCliente)) {
                emailCorrecto = true;
            } else {
                System.out.println("Formato de email incorrecto. Ejemplo: usuario@dominio.com");
            }
        } while (!emailCorrecto);

        // Una vez sale del bucle, ya puedes seguir con el pago...

        System.out.println("MÉTODO DE PAGO: ");
        System.out.println("1. Bizum");
        System.out.println("2. Tarjeta de Crédito");
        System.out.println("3. PayPal");
        System.out.print("Elige cómo quieres pagar: ");

        int opcionPago = 0;
        // otro intento de error, en este caso es un fallo al introducir letras en el
        // pago
        try {
            opcionPago = sc.nextInt();
            sc.nextLine();

        } catch (java.util.InputMismatchException e) {
            System.out.println("Error: Debes introducir un número. Compra cancelada.");
            sc.nextLine();

            // Devolvemos las entradas porque el usuario falla al pagar
            if (sesionElegida.getModo() == ModoAforo.GENERAL) {
                sesionElegida.liberarGeneral(cantidad);
            } else {
                sesionElegida.liberarAsientos(asientosReservados);
            }

            // Para que la baseddtos sepa que las entradas vuelven a estar libres
            actualizarAforoEnNube(sesionElegida, eventoElegido.getId());

            return;
        }

        Pago pago = null;

        if (opcionPago == 1) { // BIZUM
            String telefono;
            while (true) {
                System.out.print("Escribe tu número de teléfono: ");
                telefono = sc.nextLine();

                if (Validador.esTelefonoValido(telefono)) {
                    break; // Si es válido, rompemos el bucle
                }
                // Si no es válido, sale el error y el bucle vuelve a empezar
                System.out.println(
                        "El teléfono debe tener 9 cifras y empezar por 6, 7 o 9. Inténtalo de nuevo.");
            }
            pago = new PagoBizum("PAGO-1", telefono);

        } else if (opcionPago == 2) { // TARJETA
            String numTarjeta;
            String titular;
            String caducidad;
            String cvv;

            while (true) {
                System.out.println("\n--- PASARELA DE PAGO SEGURO ---");
                System.out.print("Número de la tarjeta (16 dígitos): ");
                numTarjeta = sc.nextLine();

                System.out.print("Titular de la tarjeta: ");
                titular = sc.nextLine();

                System.out.print("Fecha de caducidad (MM/YY): ");
                caducidad = sc.nextLine();

                System.out.print("CVV (3 dígitos): ");
                cvv = sc.nextLine();

                if (Validador.esTarjetaValida(numTarjeta) &&
                        Validador.esTitularValido(titular) &&
                        Validador.esFechaCaducidadValida(caducidad) &&
                        Validador.esCvvValido(cvv)) {
                    break; // Todo está perfecto, salimos del bucle
                }
                System.out.println(" Algún dato de la tarjeta es incorrecto. Por favor, revisa e inténtalo de nuevo.");
            }
            pago = new PagoTarjeta("PAGO-" + miCarrito.hashCode(), numTarjeta, titular, caducidad, cvv);

        } else if (opcionPago == 3) { // PAYPAL
            String email;
            while (true) {
                System.out.print("Escribe tu email de PayPal: ");
                email = sc.nextLine();

                if (Validador.esEmailValido(email)) {
                    break;
                }
                System.out.println("El formato del email es incorrecto. Inténtalo de nuevo.");
            }
            pago = new PagoPayPal("PAGO-1", email, 1.50);

        } else {
            // Este bloque se queda igual, por si mete un número raro en el menú de pago
            System.out.println("Opción no válida. Se cancela la compra.");
            if (sesionElegida.getModo() == ModoAforo.GENERAL) {
                sesionElegida.liberarGeneral(cantidad);
            } else {
                sesionElegida.liberarAsientos(asientosReservados);
            }
            actualizarAforoEnNube(sesionElegida, eventoElegido.getId());
            return;
        }

        Pedido miPedido = new Pedido(miCarrito, pago, eventoElegido.getNombre());

        Operacion op = new Operacion(TipoOperacion.COMPRA,
                "Compra de " + cantidad + " entradas para " + " (Ref: " + miPedido.getIdPedido() + ") "
                        + eventoElegido.getNombre(),
                entradasCompradas);
        this.historial.push(op);

        this.colaPedidos.add(miPedido);

        // Preparamos el ID del pedido (Cogemos el que ya genera tu clase Pedido)
        String idPedidoNube = miPedido.getIdPedido();

        // Preparamos la lista de asientos separada por comas
        StringBuilder asientosStr = new StringBuilder();
        for (Entrada e : entradasCompradas) {
            if (e.getAsiento() != null) {
                asientosStr.append(e.getAsiento().getIdAsiento()).append(", ");
            } else {
                asientosStr.append("General, ");
            }
        }

        String asientosFinal = asientosStr.length() > 0 ? asientosStr.substring(0, asientosStr.length() - 2) : "";

        guardarVentaEnNube(
                idPedidoNube,
                eventoElegido.getId(),
                sesionElegida.getIdSesion(),
                cantidad,
                miCarrito.calcularTotal(),
                pago.getClass().getSimpleName(),
                emailCliente,
                asientosFinal);

        System.out.println("COMPRA FINALIZADA");
        System.out.println(miPedido.toString());
    }

    public void deshacerUltimaOperacion() {
        System.out.println("\n--- DESHACER ÚLTIMA OPERACIÓN ---");

        if (this.historial.isEmpty()) {
            System.out.println("No hay ninguna operación en el historial.");
            return;
        }

        Operacion ultima = this.historial.pop();
        System.out.println("Deshaciendo: " + ultima.getDetalle());

        ArrayList<Entrada> entradasDevueltas = ultima.getEntradasAfectadas();

        // Comprobar si la lista viene vacía
        if (entradasDevueltas == null || entradasDevueltas.isEmpty()) {
            System.out.println("La lista de entradas de la operación está vacía o es nula.");
        } else {
            System.out.println("Hay " + entradasDevueltas.size() + " entradas para devolver");

            Entrada primeraEntrada = entradasDevueltas.get(0);
            System.out.println("El ID del Evento guardado en la entrada es: " + primeraEntrada.getIdEvento());

            Evento evento = null;
            for (Evento e : this.catalogo) {
                if (e.getId().equals(primeraEntrada.getIdEvento())) {
                    evento = e;
                    break;
                }
            }

            // Comprobar si encontró el evento
            if (evento == null) {
                System.out.println("¡No se encontró el evento en el catálogo!");
            } else {
                System.out.println("Evento encontrado. El ID de la Sesión es: " + primeraEntrada.getIdSesion());
                Sesion sesion = evento.getSesionById(primeraEntrada.getIdSesion());

                // Comprobar si encontró la sesión
                if (sesion == null) {
                    System.out.println("¡No se encontró la sesión dentro del evento!");
                } else {
                    if (sesion.getModo() == ModoAforo.GENERAL) {
                        sesion.liberarGeneral(entradasDevueltas.size());
                        actualizarAforoEnNube(sesion, evento.getId()); // actualiza en la nube de la db
                    } else {
                        ArrayList<Asiento> asientosDevueltos = new ArrayList<>();
                        for (Entrada e : entradasDevueltas) {
                            asientosDevueltos.add(e.getAsiento());
                        }
                        sesion.liberarAsientos(asientosDevueltos);
                        actualizarAforoEnNube(sesion, evento.getId()); // actualiza en la nube de la db
                    }
                    System.out
                            .println("Se ha restaurado el aforo: " + entradasDevueltas.size() + " asientos devueltos.");
                }
            }
        }

        boolean estabaEnCola = false;

        // Comprobamos si la cola no está vacía
        if (!this.colaPedidos.isEmpty() && this.colaPedidos instanceof java.util.LinkedList) {
            // Lo borramos de la cola de pendientes
            ((java.util.LinkedList<pedidos.Pedido>) this.colaPedidos).removeLast();
            estabaEnCola = true;
            System.out.println("El pedido se ha borrado de la cola antes de ser procesado");
        }

        // Por tanto, el dinero ya sumó en el .txt y hay que hacer un ticket negativo.
        if (estabaEnCola == false) {
            guardarDevolucionEnNube(ultima);
            System.out.println(
                    "El pedido ya estaba procesado. Se ha generado un comprobante de DEVOLUCIÓN en el archivo físico.");
        }
    }

    public void procesarColaPedidos() {
        System.out.println("\n--- PROCESANDO COLA DE PEDIDOS ---");

        if (this.colaPedidos.isEmpty()) {
            System.out.println("No hay ningún pedido pendiente");
            return;
        }

        int contador = 0; // Para llevar la cuenta

        // Mientras la cola NO esté vacía, sigue sacando pedidos
        while (!this.colaPedidos.isEmpty()) {

            // Saca el pedido de la fila
            Pedido pedidoProcesado = this.colaPedidos.poll();

            // cambia estado a procesado
            pedidoProcesado.setEstado(EstadoPedido.PROCESADO);

            System.out.println("Procesado: " + pedidoProcesado.toString());

            // Lo escribimos en el disco duro
            guardarPedidoEnFichero(pedidoProcesado);

            contador++;
        }

        System.out.println("Se han procesado " + contador + " pedidos en total.");
    }

    public void guardarPedidoEnFichero(Pedido pedido) {
        try {
            // carpeta que queremos usar
            File directorio = new File("PracticaSistemaTicketing/src/registroEntradas/RegistroVentas.txt");
            // Si la carpeta NO existe, le decimos a Java que la fabrique
            if (!directorio.exists()) {
                directorio.mkdirs();
            }

            // 3. Ahora guardamos el archivo indicando la ruta: "carpeta/archivo.txt"
            FileWriter writer = new FileWriter("PracticaSistemaTicketing/src/registroEntradas/RegistroVentas.txt",
                    true);

            String tipoPago = pedido.getPago().getClass().getSimpleName().replace("Pago", "");

            // Redactamos el documento
            writer.write("\n === NUEVO TICKET DE VENTA === \n");

            String fechaFormateada = LocalDateTime.now().format(FORMATO_FECHA);
            writer.write("Fecha de proceso: " + fechaFormateada + "\n");

            writer.write("Método de pago: " + tipoPago + "\n");

            writer.write("Datos del pedido: \n");
            writer.write(pedido.toString() + "\n");
            writer.write("=============================\n");

            // Cerramos el archivo
            writer.close();

            System.out.println("Pedido guardado físicamente en 'registroEntradas/RegistroVentas.txt'");

        } catch (IOException e) {
            System.out.println("Error al escribir el archivo de ventas");
            e.printStackTrace();
        }
    }

    public void mostrarEstadisticas() {
        System.out.println("\n--- ESTADÍSTICAS DE VENTAS ---");
        double totalRecaudado = 0;
        int pedidosProcesados = 0;

        // Apuntamos al archivo que tu Opción 4 genera
        File archivo = new File("PracticaSistemaTicketing/src/registroEntradas/RegistroVentas.txt");

        if (!archivo.exists()) {
            System.out.println("Todavía no hay ventas registradas o el archivo no existe.");
            return;
        }

        try {

            BufferedReader reader = new BufferedReader(new FileReader(archivo));
            String linea;

            while ((linea = reader.readLine()) != null) {

                // Buscamos la línea que tiene el resumen del pedido
                if (linea.contains("Total: ")) {
                    try {
                        int inicio = linea.indexOf("Total: ") + 7;
                        int fin = linea.indexOf("euros");

                        String cifraTexto = linea.substring(inicio, fin).trim();

                        totalRecaudado += Double.parseDouble(cifraTexto);
                        pedidosProcesados++;
                    } catch (Exception e) {

                    }
                }
            }
            reader.close();
            System.out.println("Dinero recaudado: " + totalRecaudado + " euros");
            System.out.println("Total de pedidos procesados: " + pedidosProcesados);
            System.out.println("---------------------------------");

        } catch (IOException e) {
            System.out.println("Error al abrir el archivo de ventas.");
            e.printStackTrace();
        }
    }

    public static void leerHistorialVentas() {
        System.out.println("\n--- HISTORIAL COMPLETO DE VENTAS ---");

        // instanciamos el log
        File archivo = new File("PracticaSistemaTicketing/src/registroEntradas/RegistroVentas.txt");

        // Si nadie ha comprado nada aún, avisamos y salimos
        if (!archivo.exists()) {
            System.out.println("Todavía no hay ventas registradas.");
            return;
        }

        try {
            // usamos buffer reader para leer el archivo
            BufferedReader reader = new BufferedReader(new FileReader(archivo));
            String linea;

            // Leemos línea a línea hasta que se acabe el texto y devuelva null
            while ((linea = reader.readLine()) != null) {
                System.out.println(linea); // Imprimimos cada línea tal cual está en el .txt
            }

            // Cerramos el grifo de lectura
            reader.close();
            System.out.println("--- FIN DEL HISTORIAL ---\n");

        } catch (IOException e) {
            System.out.println("Error al leer el archivo de ventas.");
            e.printStackTrace();
        }
    }

    public void guardarVentaEnNube(String idPedido, String idEvento, String idSesion, int cantidad, double precioTotal,
            String metodoPago, String emailUsuario, String asientos) {
        String sql = "INSERT INTO HISTORIAL_VENTAS (ID_PEDIDO, ID_EVENTO, ID_SESION, CANTIDAD_ENTRADAS, PRECIO_TOTAL, METODO_PAGO, EMAIL_USUARIO, ASIENTOS_COMPRADOS) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (java.sql.Connection conn = utilidades.ConexionDB.conectar();
                java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, idPedido);
            pstmt.setString(2, idEvento);
            pstmt.setString(3, idSesion);
            pstmt.setInt(4, cantidad);
            pstmt.setDouble(5, precioTotal);
            pstmt.setString(6, metodoPago);
            pstmt.setString(7, emailUsuario);
            pstmt.setString(8, asientos);

            pstmt.executeUpdate();
            System.out.println("Venta registrada en Oracle Cloud.");

        } catch (java.sql.SQLException e) {
            System.out.println("Error de Oracle: " + e.getMessage());
        }
    }

    public void guardarDevolucionEnNube(Operacion op) {
        // Si no hay entradas, no hay nada que devolver
        if (op.getEntradasAfectadas() == null || op.getEntradasAfectadas().isEmpty()) {
            return;
        }

        // Sacamos los datos básicos de la primera entrada devuelta
        pedidos.Entrada primera = op.getEntradasAfectadas().get(0);
        String idEvento = primera.getIdEvento();
        String idSesion = primera.getIdSesion();

        // Calculamos el total a devolver y generamos un ID de Devolución único
        // Calculamos el total a devolver y sacamos los asientos (con cuidado por si son
        // nulos)
        double totalDevuelto = 0;
        StringBuilder asientosDevueltos = new StringBuilder();

        for (pedidos.Entrada e : op.getEntradasAfectadas()) {
            totalDevuelto += e.getPrecioFinal();

            // ESCUDO: Comprobamos si la entrada tiene un asiento físico asignado
            if (e.getAsiento() != null) {
                asientosDevueltos.append(e.getAsiento().getIdAsiento()).append(", ");
            } else {
                asientosDevueltos.append("General, ");
            }
        }

        // Quitamos la última coma y espacio de los asientos
        String asientosStr = asientosDevueltos.length() > 0
                ? asientosDevueltos.substring(0, asientosDevueltos.length() - 2)
                : "";

        // Generamos un ID especial para que sepas que es una devolución (ej.
        // DEV-8f4b2a)
        String idDevolucion = "DEV-" + java.util.UUID.randomUUID().toString().substring(0, 6);
        int cantidadDevuelta = op.getEntradasAfectadas().size();

        // Hacemos el INSERT. Nota que ponemos la cantidad y el precio en NEGATIVO.
        String sql = "INSERT INTO HISTORIAL_VENTAS (ID_PEDIDO, ID_EVENTO, ID_SESION, CANTIDAD_ENTRADAS, PRECIO_TOTAL, METODO_PAGO, EMAIL_USUARIO, ASIENTOS_COMPRADOS) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (java.sql.Connection conn = ConexionDB.conectar();
                java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, idDevolucion);
            pstmt.setString(2, idEvento);
            pstmt.setString(3, idSesion);
            pstmt.setInt(4, -cantidadDevuelta); // Cantidad en negativo
            pstmt.setDouble(5, -totalDevuelto); // Dinero en negativo
            pstmt.setString(6, "REEMBOLSO"); // Método de pago especial
            pstmt.setString(7, "usuario@sistema"); // O saca el email si lo tienes guardado en la operación
            pstmt.setString(8, "DEVOLUCIÓN: " + asientosStr);

            pstmt.executeUpdate();
            System.out.println("Justificante de devolución " + idDevolucion + " guardado en la nube.");

        } catch (java.sql.SQLException e) {
            System.out.println("Error al guardar la devolución en la nube: " + e.getMessage());
        }
    }

    public void cargarDatosDesdeNube() {
        System.out.println("Conectando a Oracle Cloud para cargar el catálogo...");
        this.catalogo.clear();

        String sqlEventos = "SELECT * FROM EVENTOS";

        try (java.sql.Connection conn = utilidades.ConexionDB.conectar();
                java.sql.Statement stmt = conn.createStatement();
                java.sql.ResultSet rsEv = stmt.executeQuery(sqlEventos)) {

            while (rsEv.next()) {
                String id = rsEv.getString("id_evento");
                String nombre = rsEv.getString("nombre");
                String lugar = rsEv.getString("lugar");
                String tipo = rsEv.getString("tipo");

                // Creamos el objeto según el tipo (como tu inicializarDatos)
                Evento ev;
                if ("MUSICA".equalsIgnoreCase(tipo) || "CONCIERTO".equalsIgnoreCase(tipo)) {
                    ev = new Concierto(nombre, lugar, Categoria.CONCIERTO, true, false);
                } else if ("TEATRO".equalsIgnoreCase(tipo)) {
                    ev = new Teatro(nombre, lugar, Categoria.TEATRO, false, true);
                } else {
                    ev = new Cine(nombre, lugar, Categoria.CINE, false, false);
                }

                ev.setId(id); // Importante: mantenemos el ID de la base de datos

                // CARGAMOS LAS SESIONES DE ESTE EVENTO
                cargarSesionesDeEvento(ev, conn);

                this.catalogo.add(ev);
            }
            System.out.println("Catálogo cargado: " + this.catalogo.size() + " eventos recuperados.");

        } catch (java.sql.SQLException e) {
            System.out.println("Error al cargar desde la nube. Usando datos de respaldo...");
            e.printStackTrace();
            this.inicializarDatos(); // Si falla la nube, cargamos los de prueba
        }
    }

    private void cargarSesionesDeEvento(Evento ev, java.sql.Connection conn) throws java.sql.SQLException {
        String sqlSes = "SELECT * FROM SESIONES WHERE id_evento = ?";
        try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sqlSes)) {
            pstmt.setString(1, ev.getId());
            java.sql.ResultSet rsSes = pstmt.executeQuery();

            while (rsSes.next()) {
                // Convertimos el Timestamp de Oracle a LocalDateTime de Java
                java.time.LocalDateTime fecha = rsSes.getTimestamp("fecha_hora").toLocalDateTime();
                int aforoMax = rsSes.getInt("aforo_maximo");
                int aforoDisp = rsSes.getInt("aforo_disponible");

                // Si el evento es un Concierto, es General. Si es Teatro o Cine, es Numerado.
                ModoAforo modo = (ev instanceof Concierto) ? ModoAforo.GENERAL : ModoAforo.NUMERADO;
                Sesion s = new Sesion(fecha, aforoMax, aforoDisp, modo);
                s.setIdSesion(rsSes.getString("id_sesion"));

                ev.addSesion(s);
            }
        }
    }

    // Le añadimos el String idEvento al paréntesis
    private void actualizarAforoEnNube(Sesion s, String idEvento) {
        // Añadimos "AND id_evento = ?" para que sea un tiro de precisión
        String sql = "UPDATE SESIONES SET aforo_disponible = ? WHERE id_sesion = ? AND id_evento = ?";

        try (java.sql.Connection conn = utilidades.ConexionDB.conectar();
                java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, s.getAforoDisponible());
            pstmt.setString(2, s.getIdSesion());
            pstmt.setString(3, idEvento); // <--- Usamos el ID que pasamos por parámetro

            pstmt.executeUpdate();
            System.out.println("Base de datos actualizada con éxito.");

        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

}