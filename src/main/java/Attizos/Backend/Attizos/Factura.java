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

    // Agrega el producto al carrito sin tocar el almacén
    public boolean agregarProducto(Producto producto, int cantidad) {
        if (producto != null && cantidad > 0) {
            DetalleFactura nuevoDetalle = new DetalleFactura(producto, cantidad);
            detalles.add(nuevoDetalle);
            calcularTotal();
            return true;
        }
        return false;
    }

    // Elimina el producto de la lista del carrito sin devolver nada al almacén
    public void eliminarProducto(Producto producto) {
        if (producto != null) {
            for(DetalleFactura df : detalles){
                if(df.getProducto().getId() == producto.getId()){
                    detalles.remove(df);
                    calcularTotal();
                    return;
                }
            }
        }
    }

    // Modifica la cantidad en la lista del carrito sin alterar el almacén
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

    public String generarTicket() {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        sb.append("\n=========================================\n");
        sb.append("             RESTAURANTE ATTIZOS         \n");
        sb.append("=========================================\n");
        sb.append(String.format("                  %03d\n", numeroFactura));
        sb.append("-----------------------------------------\n");
        sb.append("Fecha:       ").append(fecha.format(formato)).append("\n");
        sb.append("Cliente:     ").append(nombreCliente).append("\n");
        sb.append("-----------------------------------------\n");
        sb.append(String.format("%-5s | %-20s | %-10s\n", "CANT", "PRODUCTO", "SUBTOTAL"));
        sb.append("-----------------------------------------\n");

        for(DetalleFactura df : detalles){
            sb.append(String.format("%-5d | %-20.20s | Bs.%8.2f\n",
                    df.getCantidad(), df.getProducto().getNombre(), df.getSubtotal()));

        }
        sb.append("-----------------------------------------\n");
        sb.append(String.format("TOTAL A PAGAR:               Bs.%8.2f\n", total));
        sb.append("=========================================\n");
        return sb.toString();
    }

    // --- Getters y Setters ---
    public int getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(int numeroFactura) { this.numeroFactura = numeroFactura; }
    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }
    public LocalDateTime getFecha() { return fecha; }
    public ArrayList<DetalleFactura> getDetalles() { return detalles; }

    public void setTotal(double total) {
        this.total = total;
    }
    public int getNumeroTicket() { return numeroTicket; }
    public void setNumeroTicket(int numeroTicket) { this.numeroTicket = numeroTicket; }
    public void setDetalles(ArrayList<DetalleFactura> detalles) {
        this.detalles = detalles;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public double getTotal() { return total; }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getEstado() {
        return estado;
    }
    public boolean requiereCocina(){
        if(detalles == null || detalles.isEmpty()) return false;

        for(DetalleFactura df : detalles){
            Producto p = df.getProducto();
            if(p.tieneReceta() || p.isPromocion()){
                return true;
            }
        }
        return false;
    }
}