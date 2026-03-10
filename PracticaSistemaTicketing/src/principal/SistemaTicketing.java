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
import utilidades.Validador;

import java.time.format.DateTimeFormatter;

import java.time.LocalDateTime;

public class SistemaTicketing {

    private ArrayList<Evento> catalogo = new ArrayList<>();
    private Stack<Operacion> historial = new Stack<>();
    private Queue<Pedido> colaPedidos = new LinkedList<>();
    private Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        SistemaTicketing sistema = new SistemaTicketing();
        sistema.inicializarDatos();
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
            System.out.println("5. Salir");
            System.out.println("6. Ver estadísticas de ventas");
            System.out.print("Elige una opción: ");

            try {
                int seleccion = sc.nextInt();
                sc.nextLine();

                if (seleccion == 1) {
                    verCatalogo();
                } else if (seleccion == 2) {
                    iniciarCompra();
                } else if (seleccion == 3) {
                    deshacerUltimaOperacion();
                } else if (seleccion == 4) {
                    procesarColaPedidos();
                } else if (seleccion == 5) {
                    salir = true;
                    System.out.println("Hasta prontooooo");
                } else if (seleccion == 6) {
                    mostrarEstadisticas();
                } else {
                    System.out.println("Opción incorrecta, elige un número del 1 al 6.");
                }

            } catch (java.util.InputMismatchException e) {
                // Si salta el error de que no es un número, caemos aquí
                System.out.println("Error: Debes introducir un NÚMERO, no letras.\n");
                sc.nextLine();
            }
        }
    }

    public void verCatalogo() {
        System.out.println("\n--- CATÁLOGO DE EVENTOS ---");

        if (this.catalogo.isEmpty()) {
            System.out.println("Lo sentimos, no hay eventos programados en este momento.");
            return;
        }
        // formateo de fecha para ponerlo más estético
        DateTimeFormatter formatoBonito = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");

        for (Evento e : this.catalogo) {
            System.out.println(e.toString());

            if (e.getSesiones().isEmpty()) {
                System.out.println("  -> (Sin sesiones programadas todavía)");
            } else {
                for (Sesion s : e.getSesiones()) {
                    // decimos a la fecha de la sesión que se formatee usando nuestro molde
                    String fechaTexto = s.getFechaHora().format(formatoBonito);

                    // Imprimimos el texto ya formateado
                    System.out.println("  -> Sesión: " + s.getIdSesion() + " | Fecha: " + fechaTexto);
                }
            }
        }
    }

    public void iniciarCompra() {
        System.out.println("INICIANDO COMPRA: ");

        verCatalogo();

        System.out.print("\nEscribe el ID del evento que quieres (ej. EV-01): ");
        String idEvento = sc.nextLine();

        Evento eventoElegido = null;
        for (Evento e : this.catalogo) {
            if (e.getId().equalsIgnoreCase(idEvento)) {
                eventoElegido = e;
                break;
            }
        }

        if (eventoElegido == null) {
            System.out.println("Error, el evento no se encuentra.");
            return;
        }

        System.out.print("Escribe el ID de la sesión (ej. SES-01): ");
        String idSesion = sc.nextLine();

        Sesion sesionElegida = eventoElegido.getSesionById(idSesion);

        if (sesionElegida == null) {
            System.out.println("Error, la sesión no se encuentra.");
            return;
        }

        System.out.print("¿Cuántas entradas quieres comprar?: ");
        int cantidad = 0;
        // para coger el error de letras y de numeros negativos
        try {
            cantidad = sc.nextInt();
            sc.nextLine();
        } catch (java.util.InputMismatchException e) {
            System.out.println("Error: Debes introducir un número. Compra cancelada.");
            sc.nextLine();
            return;
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
                    //Si falla cualquier cosa arriba, el programa cae aquí y no se cuelga
                    System.out.println("Error " + ex.getMessage());
                    i--; // Restamos 1 al contador para no perder la entrada y que el usuario vuelva a intentarlo
                }
            }
        }

        System.out.println("Total a pagar: " + miCarrito.calcularTotal() + " EUROS");

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
            return;
        }

        Pedido miPedido = new Pedido(miCarrito, pago);

        Operacion op = new Operacion(TipoOperacion.COMPRA,
                "Compra de " + cantidad + " entradas para " + eventoElegido.getNombre(), entradasCompradas);
        this.historial.push(op);

        this.colaPedidos.add(miPedido);

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

        // CHIVATO 1: Comprobar si la lista viene vacía
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

            // CHIVATO 2: Comprobar si encontró el evento
            if (evento == null) {
                System.out.println("¡No se encontró el evento en el catálogo!");
            } else {
                System.out.println("Evento encontrado. El ID de la Sesión es: " + primeraEntrada.getIdSesion());
                Sesion sesion = evento.getSesionById(primeraEntrada.getIdSesion());

                // CHIVATO 3: Comprobar si encontró la sesión
                if (sesion == null) {
                    System.out.println("¡No se encontró la sesión dentro del evento!");
                } else {
                    if (sesion.getModo() == ModoAforo.GENERAL) {
                        sesion.liberarGeneral(entradasDevueltas.size());
                    } else {
                        ArrayList<Asiento> asientosDevueltos = new ArrayList<>();
                        for (Entrada e : entradasDevueltas) {
                            asientosDevueltos.add(e.getAsiento());
                        }
                        sesion.liberarAsientos(asientosDevueltos);
                    }
                    System.out
                            .println("Se ha restaurado el aforo: " + entradasDevueltas.size() + " asientos devueltos.");
                }
            }
        }

        if (this.colaPedidos instanceof java.util.LinkedList) {
            ((java.util.LinkedList<pedidos.Pedido>) this.colaPedidos).removeLast();
            System.out.println("El pedido ha sido cancelado y eliminado de la cola.");
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
            java.io.File directorio = new java.io.File("src/registroEntradas");
            // Si la carpeta NO existe, le decimos a Java que la fabrique
            if (!directorio.exists()) {
                directorio.mkdirs();
            }

            // 3. Ahora guardamos el archivo indicando la ruta: "carpeta/archivo.txt"
            java.io.FileWriter fw = new java.io.FileWriter("src/registroEntradas/RegistroVentas.txt", true);
            java.io.PrintWriter pw = new java.io.PrintWriter(fw);

            // Redactamos el documento
            pw.println("=== NUEVO TICKET DE VENTA ===");
            pw.println("Fecha de proceso: " + java.time.LocalDateTime.now());
            pw.println("Datos del pedido:");
            pw.println(pedido.toString());
            pw.println("=============================\n");

            // Cerramos el archivo
            pw.close();

            System.out.println("Pedido guardado físicamente en 'registroEntradas/RegistroVentas.txt'");

        } catch (java.io.IOException e) {
            System.out.println("No se ha podido escribir en el disco duro.");
            System.out.println("Motivo: " + e.getMessage());
        }
    }

    public void mostrarEstadisticas() {
        System.out.println("\n--- ESTADÍSTICAS DE VENTAS ---");
        double totalRecaudado = 0;
        int pedidosProcesados = 0;

        // Apuntamos al archivo que tu Opción 4 genera
        java.io.File archivo = new java.io.File("src/registroEntradas/RegistroVentas.txt");

        if (!archivo.exists()) {
            System.out.println("Todavía no hay ventas registradas o el archivo no existe.");
            return;
        }

        // Usamos Scanner para leer el archivo
        try (java.util.Scanner lector = new java.util.Scanner(archivo)) {
            while (lector.hasNextLine()) {
                String linea = lector.nextLine();

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
            System.out.println("Dinero recaudado: " + totalRecaudado + " euros");
            System.out.println("Total de pedidos procesados: " + pedidosProcesados);
            System.out.println("---------------------------------");

        } catch (java.io.FileNotFoundException e) {
            System.out.println("Error al abrir el archivo de ventas.");
        }
    }
}