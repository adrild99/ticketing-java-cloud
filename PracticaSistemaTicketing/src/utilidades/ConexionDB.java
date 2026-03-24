package utilidades;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.SQLException;

public class ConexionDB {

    public static Connection conectar() {
        try {
            // 1. Cargamos el archivo de configuración secreto
            java.util.Properties config = new java.util.Properties();
            try (java.io.FileInputStream fis = new FileInputStream(
                    "PracticaSistemaTicketing/config.properties");) {// Le indicamos la ruta exacta desde la
                                                                              // carpeta donde se ejecuta VS Code
                config.load(fis);
            } catch (Exception e) {
                System.out.println("Error: No se encuentra el archivo config.properties");
                return null;
            }

            // 2. Extraemos los datos a variables
            String rutaWallet = config.getProperty("db.wallet.path");
            String url = config.getProperty("db.url");
            String user = config.getProperty("db.user");
            String password = config.getProperty("db.password");

            // Limpieza total de memoria de intentos previos
            System.clearProperty("oracle.net.tns_admin");
            System.clearProperty("oracle.net.wallet_location");
            System.clearProperty("javax.net.ssl.keyStore");
            System.clearProperty("javax.net.ssl.trustStore");

            // Propiedades mínimas
            java.util.Properties props = new java.util.Properties();
            props.setProperty("user", user);
            props.setProperty("password", password);
            props.setProperty("oracle.net.tns_admin", rutaWallet);

            Connection conexion = java.sql.DriverManager.getConnection(url, props);
            return conexion;

        } catch (Exception e) {
            System.out.println("Error de conexión: " + e.getMessage());
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