package pagos;

public class PagoPayPal extends Pago {

    private String email;
    private double comision;

    public PagoPayPal(String idPago, String email, double comision) {
        super(idPago);
        this.email = email;
        this.comision = comision;
    }

    @Override
    public void procesarPago(double importe) throws excepciones.PagoRechazadoException {
        double importeFinal = importe + comision;
        System.out.println("Cobrando " + importeFinal + "€ (incluye " + comision + "€ de comisión) de: " + email);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public double getComision() {
        return comision;
    }

    public void setComision(double comision) {
        this.comision = comision;
    }
}