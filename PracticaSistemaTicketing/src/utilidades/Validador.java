package utilidades;

import java.util.regex.Pattern;

public class Validador {

    //regex para bizum 
    public static boolean esTelefonoValido(String telefono) {
        return Pattern.matches("^[679][0-9]{8}$", telefono);
    }

    // Formato email estándar
    public static boolean esEmailValido(String email) {
        return Pattern.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$", email);
    }

    // Exactamente 16 números para la tarjeta
    public static boolean esTarjetaValida(String tarjeta) {
        return Pattern.matches("^[0-9]{16}$", tarjeta);
    }

    // Solo letras y espacios (mínimo 3 caracteres, sin números)
    public static boolean esTitularValido(String nombre) {
        // [a-zA-ZÁÉÍÓÚáéíóúñÑ\s] permite letras con tildes, la ñ y espacios
        return Pattern.matches("^[a-zA-ZÁÉÍÓÚáéíóúñÑ\\s]{3,50}$", nombre);
    }
    // 🔒 Validación de CVV (Exactamente 3 números)
    public static boolean esCvvValido(String cvv) {
        return java.util.regex.Pattern.matches("^[0-9]{3}$", cvv);
    }

    // 📅 Validación de Fecha de Tarjeta (Formato MM/YY, ej: 12/25)
    public static boolean esFechaCaducidadValida(String fecha) {
        // Explicación: (0[1-9]|1[0-2]) obliga a que el mes sea entre 01 y 12.
        // Luego exige una barra "/", y termina con 2 números para el año [0-9]{2}
        return java.util.regex.Pattern.matches("^(0[1-9]|1[0-2])/[0-9]{2}$", fecha);
    }
    public static boolean esDniValido(String dni) {
        // (?i) ignora mayúsculas/minúsculas.
        // Luego pide 8 números y una letra válida de DNI, O empieza por X, Y, Z (NIE)
        return java.util.regex.Pattern.matches("(?i)^[0-9]{8}[TRWAGMYFPDXBNJZSQVHLCKE]$|^[XYZ][0-9]{7}[TRWAGMYFPDXBNJZSQVHLCKE]$", dni);
    }
}