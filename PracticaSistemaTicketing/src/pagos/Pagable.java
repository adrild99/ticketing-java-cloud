package pagos;

public interface Pagable {
    void procesarPago(double importe) throws excepciones.PagoRechazadoException;
}
