package Attizos.Backend.Attizos;

import Attizos.Backend.Listas.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Factura {
    private int numeroFactura;
    private int numeroTicket;
    private LocalDateTime fecha;
    private String nombreCliente;
    private ArrayList<DetalleFactura> detalles;
    private double total;
    private String estado;

    public Factura() {}

    public Factura(int numeroFactura, String nombreCliente) {
        this.numeroFactura = numeroFactura;
        this.nombreCliente = nombreCliente;
        this.fecha = LocalDateTime.now();
        this.estado = "Pendiente";
        this.detalles = new ArrayList<>();
        this.total = 0.0;
    }

    public boolean agregarProducto(Producto producto, int cantidad) {
        if (producto != null && cantidad > 0) {
            DetalleFactura nuevoDetalle = new DetalleFactura(producto, cantidad);
            detalles.add(nuevoDetalle);
            calcularTotal();
            return true;
        }
        return false;
    }
    public void eliminarProducto(Producto producto) {
        if (producto != null && detalles != null) {
            detalles.removeIf(df -> df.getProducto().getId() == producto.getId());
            calcularTotal();
        }
    }

    public boolean modificarCantidad(Producto producto, int nuevaCantidad) {
        if (producto == null || nuevaCantidad < 0) return false;
        for(DetalleFactura df : detalles){
            if(df.getProducto().getId() == producto.getId()){
                df.setCantidad(nuevaCantidad);
                calcularTotal();
                return true;
            }
        }
        return false;
    }

    private void calcularTotal() {
        this.total = 0.0;
        if (detalles == null) return;
        for(DetalleFactura df : detalles){
            this.total += df.getSubtotal();
        }
    }

    public boolean requierePreparacion() {
        if (detalles == null || detalles.isEmpty()) return false;
        for (DetalleFactura df : detalles) {
            Producto p = df.getProducto();
            if (p.tieneReceta() || p.isPromocion()) {
                return true;
            }
        }
        return false;
    }
    public int getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(int numeroFactura) { this.numeroFactura = numeroFactura; }

    public int getNumeroTicket() { return numeroTicket; }
    public void setNumeroTicket(int numeroTicket) { this.numeroTicket = numeroTicket; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public ArrayList<DetalleFactura> getDetalles() { return detalles; }
    public void setDetalles(ArrayList<DetalleFactura> detalles) { this.detalles = detalles; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}