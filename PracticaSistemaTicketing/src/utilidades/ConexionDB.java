package utilidades;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.SQLException;

public class ConexionDB {

    public static Connection conectar() {
        java.util.Properties config = new java.util.Properties();

        // Busca config.properties en varias ubicaciones posibles
        String[] rutas = {
                "config.properties", // Si se ejecuta desde la raíz del proyecto
                "PracticaSistemaTicketing/config.properties", // Si se ejecuta desde el workspace
                "../config.properties" // Fallback
        };

        boolean cargado = false;
        for (String ruta : rutas) {
            java.io.File archivo = new java.io.File(ruta);
            if (archivo.exists()) {
                try (FileInputStream fis = new FileInputStream(archivo)) {
                    config.load(fis);
                    cargado = true;
                    break;
                } catch (Exception e) {
                    // Intentar siguiente ruta
                }
            }
        }

        if (!cargado) {
            System.out.println("❌ Error: No se encuentra config.properties");
            System.out.println("   Copia config.properties.example como config.properties y rellénalo.");
            return null;
        }

        String rutaWallet = config.getProperty("db.wallet.path");
        String url = config.getProperty("db.url");
        String user = config.getProperty("db.user");
        String password = config.getProperty("db.password");

        System.clearProperty("oracle.net.tns_admin");
        System.clearProperty("oracle.net.wallet_location");
        System.clearProperty("javax.net.ssl.keyStore");
        System.clearProperty("javax.net.ssl.trustStore");

        java.util.Properties props = new java.util.Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);
        props.setProperty("oracle.net.tns_admin", rutaWallet);

        try {
            return java.sql.DriverManager.getConnection(url, props);
        } catch (Exception e) {
            System.out.println("❌ Error de conexión: " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        // 1. Obtenemos la conexión que ya funciona
        Connection conn = conectar();

        if (conn != null) {
            System.out.println("Probando consulta a la tabla EVENTOS");

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
                System.out.println("Error al leer los datos:");
                e.printStackTrace();
            }
        }
    }
}