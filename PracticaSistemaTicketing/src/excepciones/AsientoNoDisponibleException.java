package excepciones;

public class AsientoNoDisponibleException extends TicketingException{
    //constructor heredando de TicketingException
    public AsientoNoDisponibleException(String idAsiento) {
        super("El asiento " + idAsiento + " ya está reservado o no existe en esta sala.");
    }
}
