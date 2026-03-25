package excepciones;

public class PagoRechazadoException extends TicketingException {
    public PagoRechazadoException(String metodoPago) {
        super("El pago con " + metodoPago + " ha sido rechazado.");
    }
}