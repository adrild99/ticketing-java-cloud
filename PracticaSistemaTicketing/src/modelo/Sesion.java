package modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class Sesion {

    private String idSesion;
    private LocalDateTime fechaHora;
    private int aforoTotal;
    private int aforoDisponible;
    private ModoAforo modo; // es como Categoria en evento

    private ArrayList<Asiento> asientos = new ArrayList<>();

    public Sesion(LocalDateTime fechaHora, int aforoTotal, int aforoDisponible, ModoAforo modo) {

        this.fechaHora = fechaHora;
        this.aforoTotal = aforoTotal;
        this.aforoDisponible = aforoDisponible;
        this.modo = modo;
        this.asientos = new ArrayList<>();
        if (modo == ModoAforo.NUMERADO) {
            for (int i = 1; i <= aforoTotal; i++) {
                Zona zonaAsiento = (i <= 10) ? Zona.VIP : Zona.NORMAL; // Los 10 primeros son VIP
                this.asientos.add(new Asiento(i + "", zonaAsiento));
            }
        }
    }

    public boolean hayDisponibilidad(int cantidad) {
        if (this.aforoDisponible >= cantidad) {
            return true;
        } else {
            return false;
        }
        // Sería igual que poner:
        // return this.aforoDisponible >= cantidad;
    }

    public void reservarGeneral(int cantidad) {
        this.aforoDisponible = this.aforoDisponible - cantidad; // quitamos la cantidad de entradas que se hayan
                                                                // comprado
    }

    public void liberarGeneral(int cantidad) {
        this.aforoDisponible = this.aforoDisponible + cantidad; // Devolvemos la cantidad de entradas que se devuelven
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
        for (Asiento asiento : asientosLiberar) {// Recorremos los asientos que el cliente quiere devolver
            asiento.setReservado(false); // Los volvemos a poner como libres
        }
        this.aforoDisponible = this.aforoDisponible + asientosLiberar.size();
    }

    // mostrar qué hay libre
    public void mostrarAsientosLibres() {
        System.out.println("\n--- PLANO DE LA SALA (Leyenda: [V]=VIP, [N]=Normal, [XX]=Ocupado) ---");
        int contador = 0;

        for (Asiento a : this.asientos) {
            String leyenda;

            if (a.getReservado()) {
                leyenda = "[ XX ]"; // Asiento ocupado
            } else {
                // Si está libre, mostramos el ID y una letra para la zona
                String letraZona = (a.getZona() == Zona.VIP) ? "V" : "N";
                // Formateamos para que el ID siempre ocupe 2 espacios (ej: "01", "10")
                leyenda = String.format("[%s-%2s]", letraZona, a.getIdAsiento());
            }

            System.out.print(leyenda + " ");
            contador++;

            // Salto de línea cada 10 asientos para formar la matriz
            if (contador % 10 == 0) {
                System.out.println();
            }
        }
        System.out.println("-------------------------------------------------------------------\n");
    }

    // Método para buscar el objeto Asiento por su nombre (ID)
    public Asiento buscarAsientoPorId(String id) {
        for (Asiento a : this.asientos) {
            if (a.getIdAsiento().equalsIgnoreCase(id)) {
                return a;
            }
        }
        return null; // Si no lo encuentra
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
}