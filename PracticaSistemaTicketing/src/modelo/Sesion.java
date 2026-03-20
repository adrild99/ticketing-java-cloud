package modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;

import excepciones.AsientoNoDisponibleException;

public class Sesion {

    private String idSesion;
    private LocalDateTime fechaHora;
    private int aforoTotal;
    private int aforoDisponible;
    private ModoAforo modo;
    private String nombreZona;

    private ArrayList<Asiento> asientos = new ArrayList<>();

    public Sesion(LocalDateTime fechaHora, int aforoTotal, int aforoDisponible, ModoAforo modo, String nombreZona) {
        this.fechaHora = fechaHora;
        this.aforoTotal = aforoTotal;
        this.aforoDisponible = aforoDisponible;
        this.modo = modo;
        this.nombreZona = nombreZona;

        // Inicializamos los asientos solo si es numerado
        if (modo == ModoAforo.NUMERADO) {
            for (int i = 1; i <= aforoTotal; i++) {
                Zona zonaAsiento = (i <= 10) ? Zona.VIP : Zona.NORMAL;
                this.asientos.add(new Asiento(i + "", zonaAsiento));
            }
        }
    }

    public int getAforoDisponible() {
        return this.aforoDisponible;
    }

    public void setAforoDisponible(int aforoDisponible) {
        this.aforoDisponible = aforoDisponible;
    }

    public boolean hayDisponibilidad(int cantidad) {
        return this.aforoDisponible >= cantidad;
    }

    public void reservarGeneral(int cantidad) {
        this.aforoDisponible = this.aforoDisponible - cantidad;
    }

    public void liberarGeneral(int cantidad) {
        this.aforoDisponible = this.aforoDisponible + cantidad;
        if (this.aforoDisponible > this.aforoTotal) { // estoe s para que el disponible nunca sea mayor que el total
            this.aforoDisponible = this.aforoTotal;
        }
    }

    public ArrayList<Asiento> reservarAsientos(int cantidad) {
        if (this.aforoDisponible < cantidad) {
            return null;
        }

        ArrayList<Asiento> asientosCarrito = new ArrayList<>();

        for (Asiento asiento : this.asientos) {
            if (asiento.getReservado() == false) {
                asiento.setReservado(true);
                asientosCarrito.add(asiento);
                if (asientosCarrito.size() == cantidad)
                    break;
            }
        }

        this.aforoDisponible = this.aforoDisponible - cantidad;
        return asientosCarrito;
    }

    public void liberarAsientos(ArrayList<Asiento> asientosLiberar) {
        for (Asiento asiento : asientosLiberar) {
            asiento.setReservado(false);
        }
        this.aforoDisponible = this.aforoDisponible + asientosLiberar.size();

        // Si al devolver nos pasamos del máximo, lo topamos al máximo
        if (this.aforoDisponible > this.aforoTotal) {
            this.aforoDisponible = this.aforoTotal;
        }
    }

    public void mostrarAsientosLibres() {
        System.out.println("\n--- PLANO DE LA SALA (Leyenda: [V]=VIP, [N]=Normal, [XX]=Ocupado) ---");
        int contador = 0;

        for (Asiento a : this.asientos) {
            String leyenda;

            if (a.getReservado()) {
                leyenda = "[ XX ]";
            } else {
                String letraZona = (a.getZona() == Zona.VIP) ? "V" : "N";
                leyenda = String.format("[%s-%2s]", letraZona, a.getIdAsiento());
            }

            System.out.print(leyenda + " ");
            contador++;

            if (contador % 10 == 0) {
                System.out.println();
            }
        }
        System.out.println("-------------------------------------------------------------------\n");
    }

    public Asiento buscarAsientoPorId(String id) throws AsientoNoDisponibleException {
        for (Asiento a : this.asientos) {
            if (a.getIdAsiento().equalsIgnoreCase(id)) {
                return a;
            }
        }
        throw new AsientoNoDisponibleException(id);
    }

    public String getIdSesion() {
        return idSesion;
    }

    public void setIdSesion(String idSesion) {
        this.idSesion = idSesion;
    }

    public ModoAforo getModo() {
        return modo;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setAforoTotal(int aforoTotal) {
        this.aforoTotal = aforoTotal;
    }

    public int getAforoTotal() {
        return aforoTotal;
    }

    public String getNombreZona() {
        return nombreZona;
    }
}