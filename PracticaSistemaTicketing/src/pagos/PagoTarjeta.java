package pagos;

public class PagoTarjeta extends Pago {

    private String numTarjeta;
    private String titular;
    private String caducidad;
    private String cvv;

    public PagoTarjeta(String idPago, String numTarjeta, String titular, String caducidad, String cvv) {
        super(idPago);
        this.numTarjeta = numTarjeta;
        this.titular = titular;
        this.caducidad = caducidad;
        this.cvv = cvv;
    }

    @Override
    public void procesarPago(double importe) throws excepciones.PagoRechazadoException {
        System.out.println("Cobrando " + importe + "€ a la tarjeta de " + titular);
    }

    public String getNumTarjeta() {
        return numTarjeta;
    }

    public void setNumTarjeta(String numTarjeta) {
        this.numTarjeta = numTarjeta;
    }

    public String getTitular() {
        return titular;
    }

    public void setTitular(String titular) {
        this.titular = titular;
    }

    public void setCVV(String cvv) {
        this.cvv = cvv;
    }

    public String getCVV() {
        return cvv;
    }

    public String getCaducidad() {
        return caducidad;
    }

    public void setCaducidad(String caducidad) {
        this.caducidad = caducidad;
    }

}
