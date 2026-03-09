package pedidos;
public enum EstadoPedido {
    PENDIENTE("Está pendiente"),
    PROCESADO("Está procesado"),
    PAGADO("Está pagado"),
    CANCELADO("Está cancelado"),
    REEMBOLSADO("Está reembolsado");

    private String descripcion;

    private EstadoPedido(String descripcion){
        this.descripcion = descripcion;
    }

    public String getDescripcion(){
        return descripcion;
    }

}
