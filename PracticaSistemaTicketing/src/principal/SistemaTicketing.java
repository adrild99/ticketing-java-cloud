package principal;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.Stack;

import excepciones.AsientoNoDisponibleException;
import modelo.Asiento;
import modelo.Evento;
import modelo.ModoAforo;
import modelo.Sesion;
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
import utilidades.AccesoDatos;
import utilidades.Validador;

public class SistemaTicketing {

    public static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm:ss");

    private AccesoDatos db = new AccesoDatos(); // acceso a la clase AccesoDatos para conectar con la base de datos

    private ArrayList<Evento> catalogo = new ArrayList<>();
    private Stack<Operacion> historial = new Stack<>();
    private Queue<Pedido> colaPedidos = new LinkedList<>();
    private Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        SistemaTicketing sistema = new SistemaTicketing();
        sistema.catalogo = sistema.db.cargarDatosDesdeNube();
        sistema.menu();
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
                        db.leerHistorialDeVentas();
                        break;
                    case 6:
                        db.mostrarEstadisticas();
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
            db.actualizarAforoEnNube(sesionElegida, eventoElegido.getId());

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
            db.actualizarAforoEnNube(sesionElegida, eventoElegido.getId());
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
            db.actualizarAforoEnNube(sesionElegida, eventoElegido.getId());

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
            db.actualizarAforoEnNube(sesionElegida, eventoElegido.getId());
            return;
        }

        try {
            pago.procesarPago(miCarrito.calcularTotal());
        } catch (excepciones.PagoRechazadoException e) {
            System.out.println(e.getMessage());
            if (sesionElegida.getModo() == ModoAforo.GENERAL) {
                sesionElegida.liberarGeneral(cantidad);
            } else {
                sesionElegida.liberarAsientos(asientosReservados);
            }
            db.actualizarAforoEnNube(sesionElegida, eventoElegido.getId());
            return;
        }

        Pedido miPedido = new Pedido(miCarrito, pago, eventoElegido.getNombre());

        Operacion op = new Operacion(TipoOperacion.COMPRA,
                "Compra de " + cantidad + " entradas para " + " (Ref: " + miPedido.getIdPedido() + ") "
                        + eventoElegido.getNombre(),
                entradasCompradas, emailCliente);
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

        db.guardarVentaEnNube(
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
                        db.actualizarAforoEnNube(sesion, evento.getId()); // actualiza en la nube de la db
                    } else {
                        ArrayList<Asiento> asientosDevueltos = new ArrayList<>();
                        for (Entrada e : entradasDevueltas) {
                            asientosDevueltos.add(e.getAsiento());
                        }
                        sesion.liberarAsientos(asientosDevueltos);
                        db.actualizarAforoEnNube(sesion, evento.getId()); // actualiza en la nube de la db
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
        if (!estabaEnCola) {
            db.guardarDevolucionEnNube(ultima);
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
            File archivo = new File("PracticaSistemaTicketing/src/registroEntradas/RegistroVentas.txt");
            archivo.getParentFile().mkdirs(); // Crea la carpeta padre si no existe

            // 3. Ahora guardamos el archivo indicando la ruta: "carpeta/archivo.txt"
            try (FileWriter writer = new FileWriter("PracticaSistemaTicketing/src/registroEntradas/RegistroVentas.txt",
                    true)) {

                String tipoPago = pedido.getPago().getClass().getSimpleName().replace("Pago", "");

                // Redactamos el documento
                writer.write("\n === NUEVO TICKET DE VENTA === \n");

                String fechaFormateada = LocalDateTime.now().format(FORMATO_FECHA);
                writer.write("Fecha de proceso: " + fechaFormateada + "\n");

                writer.write("Método de pago: " + tipoPago + "\n");

                writer.write("Datos del pedido: \n");
                writer.write(pedido.toString() + "\n");
                writer.write("=============================\n");
            }

            System.out.println("Pedido guardado físicamente en 'registroEntradas/RegistroVentas.txt'");

        } catch (IOException e) {
            System.out.println("Error al escribir el archivo de ventas");
            e.printStackTrace();
        }
    }

}