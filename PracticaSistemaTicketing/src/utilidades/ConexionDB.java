package utilidades;

import java.sql.Connection;
import java.sql.SQLException;


public class ConexionDB {

    public static Connection conectar() {
        try {
            String rutaWallet = "C:/PlataformaTicketing/Ticketing/PracticaSistemaTicketing/wallet";

            // Limpieza total de memoria de intentos previos
            System.clearProperty("oracle.net.tns_admin");
            System.clearProperty("oracle.net.wallet_location");
            System.clearProperty("javax.net.ssl.keyStore");
            System.clearProperty("javax.net.ssl.trustStore");

            // URL 
            String url = "jdbc:oracle:thin:@ydokx1ilqvdntkzr_high";

            // Propiedades mínimas
            java.util.Properties props = new java.util.Properties();
            props.setProperty("user", "TICKETINGCLOUD");
            props.setProperty("password", "Estudiante_2026_Db");
            props.setProperty("oracle.net.tns_admin", rutaWallet);
            

            Connection conexion = java.sql.DriverManager.getConnection(url, props);

            //System.out.println("CONEXIÓN TOTAL CON LA NUBE ESTABLECIDA!"); es una prueba por qué no sé por qué narices falla
            return conexion;

        } catch (Exception e) {
            System.out.println("Error final:");
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        // 1. Obtenemos la conexión que ya funciona
        Connection conn = conectar();

        if (conn != null) {
            System.out.println("🔎 Probando consulta a la tabla EVENTOS...");

            // 2. Preparamos la sentencia SQL
            String sql = "SELECT id_evento, nombre, tipo FROM EVENTOS";

            try (java.sql.Statement stmt = conn.createStatement();
                    java.sql.ResultSet rs = stmt.executeQuery(sql)) {

                // 3. Recorremos los resultados
                while (rs.next()) {
                    String id = rs.getString("id_evento");
                    String nombre = rs.getString("nombre");
                    String tipo = rs.getString("tipo");

                    System.out.println("Evento encontrado: [" + id + "] " + nombre + " (" + tipo + ")");
                }

                // Cerramos la conexión al terminar la prueba
                conn.close();

            } catch (SQLException e) {
                System.out.println("❌ Error al leer los datos:");
                e.printStackTrace();
            }
        }
    }
}