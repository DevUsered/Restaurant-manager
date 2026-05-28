package Attizos.Backend.Attizos;

import Attizos.Backend.Listas.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Factura {
    private int numeroFactura;
    private LocalDateTime fecha;
    private String nombreCliente;
    private ListaDE<DetalleFactura> detalles;
    private double total;
    private String estado;

    public Factura(int numeroFactura, String nombreCliente) {
        this.numeroFactura = numeroFactura;
        this.nombreCliente = nombreCliente;
        this.fecha = LocalDateTime.now();
        this.estado = "Completada";
        this.detalles = new ListaDE<>();
        this.total = 0.0;
    }

    // Agrega el producto al carrito sin tocar el almacén
    public boolean agregarProducto(Producto producto, int cantidad) {
        if (producto != null && cantidad > 0) {
            DetalleFactura nuevoDetalle = new DetalleFactura(producto, cantidad);
            detalles.insertarAlFinal(nuevoDetalle);
            calcularTotal();
            return true;
        }
        return false;
    }

    // Elimina el producto de la lista del carrito sin devolver nada al almacén
    public void eliminarProducto(Producto producto) {
        if (producto != null) {
            NodoDE<DetalleFactura> ac = detalles.getCabeza();
            while (ac != null) {
                if (ac.getDato().getProducto().getId() == producto.getId()) {
                    detalles.eliminarPorValor(ac.getDato());
                    calcularTotal();
                    return;
                }
                ac = ac.getSiguiente();
            }
        }
    }

    // Modifica la cantidad en la lista del carrito sin alterar el almacén
    public boolean modificarCantidad(Producto producto, int nuevaCantidad) {
        if (producto == null || nuevaCantidad < 0) return false;

        NodoDE<DetalleFactura> ac = detalles.getCabeza();
        while (ac != null) {
            DetalleFactura detalle = ac.getDato();
            if (detalle.getProducto().getId() == producto.getId()) {
                if (nuevaCantidad == 0) {
                    eliminarProducto(producto);
                } else {
                    detalle.setCantidad(nuevaCantidad);
                    calcularTotal();
                }
                return true;
            }
            ac = ac.getSiguiente();
        }
        return false;
    }

    private void calcularTotal() {
        this.total = 0.0;
        NodoDE<DetalleFactura> actual = detalles.getCabeza();
        while (actual != null) {
            this.total += actual.getDato().getSubtotal();
            actual = actual.getSiguiente();
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

        NodoDE<DetalleFactura> actual = detalles.getCabeza();
        while (actual != null) {
            DetalleFactura det = actual.getDato();
            sb.append(String.format("%-5d | %-20.20s | Bs.%8.2f\n",
                    det.getCantidad(), det.getProducto().getNombre(), det.getSubtotal()));
            actual = actual.getSiguiente();
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
    public ListaDE<DetalleFactura> getDetalles() { return detalles; }

    public void setTotal(double total) {
        this.total = total;
    }

    public void setDetalles(ListaDE<DetalleFactura> detalles) {
        this.detalles = detalles;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public double getTotal() { return total; }

    public void setEstado(String estado) {
    }
}