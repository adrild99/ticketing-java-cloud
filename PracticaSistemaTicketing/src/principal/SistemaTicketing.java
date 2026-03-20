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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

public class SistemaTicketing {

    public static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm:ss");

    private ArrayList<Evento> catalogo = new ArrayList<>();
    private Stack<Operacion> historial = new Stack<>();
    private Queue<Pedido> colaPedidos = new LinkedList<>();
    private Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        SistemaTicketing sistema = new SistemaTicketing();
        sistema.cargarDatosBinarios();
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
                    leerHistorialVentas();
                } else if (seleccion == 6) {
                    mostrarEstadisticas();
                } else if (seleccion == 7) {
                    salir = true;
                    guardarDatosBinarios();
                    System.out.println("Hasta prontooooo");
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
                    String fechaTexto = s.getFechaHora().format(FORMATO_FECHA);

                    // Imprimimos el texto ya formateado
                    System.out.println("  -> Sesión: " + s.getIdSesion() + " | Fecha: " + fechaTexto);
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
                    // Si falla cualquier cosa arriba, el programa cae aquí y no se cuelga
                    System.out.println("Error " + ex.getMessage());
                    i--; // Restamos 1 al contador para no perder la entrada y que el usuario vuelva a
                         // intentarlo
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

        Pedido miPedido = new Pedido(miCarrito, pago, eventoElegido.getNombre());

        Operacion op = new Operacion(TipoOperacion.COMPRA,
                "Compra de " + cantidad + " entradas para " + " (Ref: " + miPedido.getIdPedido() + ") "
                        + eventoElegido.getNombre(),
                entradasCompradas);
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
            guardarDevolucionEnFichero(ultima);
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

    public void guardarDatosBinarios() {
        try {
            // el archivo "datos.dat" en la carpeta src
            FileOutputStream fos = new FileOutputStream("src/datos.dat");
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            // la lista entera de eventos dentro de oos
            oos.writeObject(this.catalogo);

            oos.close();
            System.out.println("Estado del programa guardado correctamente en 'datos.dat'.");

        } catch (Exception e) {
            System.out.println("Error al guardar los datos binarios.");
            e.printStackTrace();
        }
    }

    public void cargarDatosBinarios() {
        File archivo = new File("src/datos.dat");

        // Si el archivo no existe, significa que es la primera vez que abres el
        // programa
        if (!archivo.exists()) {
            System.out.println("No hay datos guardados. Se creará un catálogo vacío.");
            this.inicializarDatos();
            return;
        }

        try {
            // Si el archivo SÍ existe, lo lee
            FileInputStream fis = new FileInputStream(archivo);
            ObjectInputStream ois = new ObjectInputStream(fis);

            // Carga la lista y machaca el catálogo vacío con los datos reales
            this.catalogo = (ArrayList<Evento>) ois.readObject();

            ois.close();
            System.out.println("Datos cargados con éxito. Eventos recuperados: " + this.catalogo.size());

        } catch (Exception e) {
            System.out.println("Error al cargar los datos binarios.");
            e.printStackTrace();
        }
    }

    public void guardarDevolucionEnFichero(Operacion op) {
        try {
            File directorio = new File("PracticaSistemaTicketing/src/registroEntradas/RegistroVentas.txt");
            if (!directorio.exists()) {
                directorio.mkdirs();
            }

            // Abrimos en modo "true"
            FileWriter writer = new FileWriter("PracticaSistemaTicketing/src/registroEntradas/RegistroVentas.txt",
                    true);

            writer.write("\n === TICKET DE DEVOLUCIÓN === \n");
            String fechaFormateada = LocalDateTime.now().format(FORMATO_FECHA);
            writer.write("Fecha de proceso: " + fechaFormateada + "\n");
            writer.write("Operación deshecha: " + op.getDetalle() + "\n");

            if (op.getEntradasAfectadas() != null && !op.getEntradasAfectadas().isEmpty()) {
                pedidos.Entrada primera = op.getEntradasAfectadas().get(0);
                writer.write("Evento ID: " + primera.getIdEvento() + " | Sesión ID: " + primera.getIdSesion() + "\n");
            }

            // Calculamos cuánto dinero hay que devolver sumando las entradas afectadas
            double totalDevuelto = 0;
            for (pedidos.Entrada e : op.getEntradasAfectadas()) {
                totalDevuelto += e.getPrecioFinal();
            }

            writer.write("Estado: CANCELADO / REEMBOLSADO\n");

            writer.write("Total: -" + totalDevuelto + " euros\n");
            writer.write("=============================\n");

            writer.close();

        } catch (IOException e) {
            System.out.println("Error al escribir el ticket de devolución en el historial físico.");
            e.printStackTrace();
        }
    }

}