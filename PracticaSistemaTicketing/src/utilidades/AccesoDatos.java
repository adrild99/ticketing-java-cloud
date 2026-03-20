package utilidades;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;

import modelo.*;
import pedidos.*;

public class AccesoDatos {

    // 1. Mueve aquí el método de cargar sesiones
    public void cargarSesionesDeEvento(Evento ev, java.sql.Connection conn) throws java.sql.SQLException {
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
                Sesion s = new Sesion(fecha, aforoMax, aforoDisp, modo, rsSes.getString("NOMBRE_ZONA"));
                s.setIdSesion(rsSes.getString("id_sesion"));

                ev.addSesion(s);
            }
        }
    }

    // Le añadimos el String idEvento al paréntesis
    public void actualizarAforoEnNube(Sesion s, String idEvento) {
        // Añadimos "AND id_evento = ?" para que sea un tiro de precisión
        String sql = "UPDATE SESIONES SET aforo_disponible = ? WHERE id_sesion = ? AND id_evento = ? AND nombre_zona = ?";

        try (java.sql.Connection conn = utilidades.ConexionDB.conectar();
                java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, s.getAforoDisponible());
            pstmt.setString(2, s.getIdSesion());
            pstmt.setString(3, idEvento);
            pstmt.setString(4, s.getNombreZona());

            pstmt.executeUpdate();
            System.out.println("Base de datos actualizada con éxito.");

        } catch (java.sql.SQLException e) {
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

    public ArrayList<Evento> cargarDatosDesdeNube() {
        System.out.println("Conectando a Oracle Cloud para cargar el catálogo...");
        ArrayList<Evento> listaTemporal = new ArrayList<>();

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

                listaTemporal.add(ev);
            }
            System.out.println("Catálogo cargado: " + listaTemporal.size() + " eventos recuperados.");

        } catch (java.sql.SQLException e) {
            System.out.println("Error al cargar desde la nube. Usando datos de respaldo...");
            e.printStackTrace();
            // this.inicializarDatos(); // Si falla la nube, cargamos los de prueba
        }
        return listaTemporal;
    }
}