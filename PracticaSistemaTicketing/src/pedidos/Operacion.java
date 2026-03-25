package pedidos;

import java.util.ArrayList;

public class Operacion {
    private TipoOperacion tipo;
    private String detalle;
    private ArrayList<Entrada> entradasAfectadas = new ArrayList<>();
    private String emailUsuario;

    public Operacion(TipoOperacion tipo, String detalle, ArrayList<Entrada> entradasAfectadas, String emailUsuario) {
        this.tipo = tipo;
        this.detalle = detalle;

        this.entradasAfectadas = new ArrayList<>(entradasAfectadas);
        this.emailUsuario = emailUsuario;

    }

    @Override
    public String toString() {
        return "Operación [" + tipo + "] - " + detalle + " (" + entradasAfectadas.size() + " entradas)";
    }

    public TipoOperacion getTipo() {
        return tipo;
    }

    public void setTipo(TipoOperacion tipo) {
        this.tipo = tipo;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public ArrayList<Entrada> getEntradasAfectadas() {
        return entradasAfectadas;
    }

    public void setEntradasAfectadas(ArrayList<Entrada> entradasAfectadas) {
        this.entradasAfectadas = entradasAfectadas;
    }

    public String getEmailUsuario() {
        return emailUsuario;
    }
    public void setEmailUsuario(){
        this.emailUsuario=emailUsuario;
    }
}
