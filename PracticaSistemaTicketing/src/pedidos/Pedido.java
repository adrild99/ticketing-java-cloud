package pedidos;

import java.util.ArrayList;

import pagos.Pago;

public class Pedido {

    private String idPedido;
    private EstadoPedido estado;
    private ArrayList<Entrada> entradas = new ArrayList<>();

    private String nombreEvento;

    private Pago pago;
    private double total;

    public Pedido(Carrito carrito, Pago pago, String nombreEvento) {

        String codigoCorto = java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        this.idPedido = "PED-" + codigoCorto;

        for (Entrada e : carrito.getEntradas()) {
            this.entradas.add(e);
        }
        this.pago = pago;
        this.estado = EstadoPedido.PENDIENTE;
        this.total = calcularTotal();

        this.nombreEvento =nombreEvento;
    }

    public double calcularTotal() {
        double suma = 0.0;

        for (Entrada e : this.entradas) {
            suma = suma + e.getPrecioFinal();
        }
        return suma;
    }

    @Override
    public String toString() {
        // el texto del evento y sesión por si acaso la lista está vacía
        String infoEvento = "Desconocido";
        String infoSesion = "Desconocida";

        // Si el pedido tiene entradas dentro miramos la primera
        if (this.entradas != null && !this.entradas.isEmpty()) {
            infoEvento = this.entradas.get(0).getIdEvento();
            infoSesion = this.entradas.get(0).getIdSesion();
        }

        return "PEDIDO: " + idPedido + ", Estado: " + estado +
                ", Total: " + total + " euros, Nº Entradas: " + entradas.size() + "\n" + 
                "Evento: " + this.nombreEvento + " Evento ID: " + infoEvento + " | Sesión ID: " + infoSesion;
    }

    public String getIdPedido() {
        return idPedido;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public ArrayList<Entrada> getEntradas() {
        return entradas;
    }

    public Pago getPago() {
        return pago;
    }

    public double getTotal() {
        return total;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }
}
