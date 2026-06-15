package Attizos.Backend.Attizos;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Reserva {
    private String id;
    private String nombreCliente;
    private String telefono;
    private int cantidadPersonas;
    private LocalDateTime fecha;
    private String observaciones;
    private int numeroMesa;
    private String estado;

    public Reserva() {}

    public Reserva(String id, String nombreCliente, String telefono, int cantidadPersonas, LocalDateTime fechaHora, String observaciones) {
        this.id = id;
        this.nombreCliente = nombreCliente;
        this.telefono = telefono;
        this.cantidadPersonas = cantidadPersonas;
        this.fecha = fechaHora;
        this.observaciones = observaciones;
        this.numeroMesa = 0;
        this.estado = "Pendiente";
    }
    public boolean toleranciaExcedida() {
        if (estado.equals("Pendiente") || estado.equals("Confirmada")) {
            LocalDateTime limite = fecha.plusMinutes(15);
            return LocalDateTime.now().isAfter(limite);
        }
        return false;
    }
    public String getId() { return id; }
    public String getNombreCliente() { return nombreCliente; }
    public String getTelefono() { return telefono; }
    public int getCantidadPersonas() { return cantidadPersonas; }
    public LocalDateTime getFecha() { return fecha; }
    public String getObservaciones() { return observaciones; }
    public int getNumeroMesa() { return numeroMesa; }
    public String getEstado() { return estado; }

    public void setEstado(String estado) { this.estado = estado; }
    public void setNumeroMesa(int numeroMesa) { this.numeroMesa = numeroMesa; }
    @Override
    public String toString() {

        String alerta = toleranciaExcedida() ? " ⏰ (RETRASADO)" : "";
        String mesaAsignada = numeroMesa > 0 ? String.valueOf(numeroMesa) : "Sin asignar";

        return String.format("ID: %-7s | Fecha: %02d/%02d %02d:%02d | Cliente: %-15s | Tel: %-8s | Pax: %d | Mesa: %-10s | Estado: %s%s\n   📌 Obs: %s",
                id, fecha.getDayOfMonth(), fecha.getMonthValue(), fecha.getHour(), fecha.getMinute(),
                nombreCliente, telefono, cantidadPersonas, mesaAsignada, estado, alerta, observaciones);
    }
}

