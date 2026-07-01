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
}

