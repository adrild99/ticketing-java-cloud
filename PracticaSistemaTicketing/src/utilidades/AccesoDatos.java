package utilidades;

import java.sql.*;
import java.time.LocalDateTime;
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

    // 3. Mueve aquí también guardarVentaEnNube y guardarDevolucionEnNube...
}