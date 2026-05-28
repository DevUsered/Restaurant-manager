package Attizos.Backend.Attizos;

import Attizos.Backend.Listas.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Pedido {
    private int idPedido;
    private int numeroTicket;         // 🔥 NUEVO: Para el número diario que ve el cocinero
    private String descripcionBreve;  // 🔥 NUEVO: Para la vista rápida en la tabla
    
    private String fechaHora;
    private ListaDE<DetalleFactura> productos;
    private double total;
    private String estado;
    private String cliente;

    public Pedido() {
        this.estado = "Pendiente";
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("HH:mm");
        this.fechaHora = LocalDateTime.now().format(formato);
    }

    public Pedido(int idPedido, String cliente, ListaDE<DetalleFactura> productos, double total) {
        this.idPedido = idPedido;
        this.cliente = cliente;
        this.productos = productos;
        this.total = total;
        this.estado = "En Espera";

        DateTimeFormatter formato = DateTimeFormatter.ofPattern("HH:mm");
        this.fechaHora = LocalDateTime.now().format(formato);
    }

    // ============================================================================
    // GETTERS
    // ============================================================================
    public int getIdPedido() { return idPedido; }
    public int getNumeroTicket() { return numeroTicket; }
    public String getDescripcionBreve() { return descripcionBreve; }
    public String getFechaHora() { return fechaHora; }
    public ListaDE<DetalleFactura> getProductos() { return productos; }
    public double getTotal() { return total; }
    public String getEstado() { return estado; }
    public String getCliente() { return cliente; }

    // ============================================================================
    // SETTERS 
    // ============================================================================
    public void setIdPedido(int idPedido) { this.idPedido = idPedido; }
    public void setNumeroTicket(int numeroTicket) { this.numeroTicket = numeroTicket; }
    public void setDescripcionBreve(String descripcionBreve) { this.descripcionBreve = descripcionBreve; }
    public void setEstado(String estado) { this.estado = estado; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public void setProductos(ListaDE<DetalleFactura> productos) { this.productos = productos; }
    public void setTotal(double total) { this.total = total; }
    public void setFechaHora(String fechaHora) { this.fechaHora = fechaHora; }

    public void mostrarPedido() {
        System.out.println(this.toString());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📋 Ticket Nro: %-3d (ID BD: %d) | Hora: %s | Cliente: %-15s | Estado: [%s]\n",
                numeroTicket, idPedido, fechaHora, cliente, estado));
        sb.append("   🍳 A PREPARAR:\n");
        
        // Si viene de la BD con descripción breve, la mostramos
        if (descripcionBreve != null && !descripcionBreve.isEmpty()) {
            sb.append("      👉 ").append(descripcionBreve).append("\n");
        } 
        // Si viene de la memoria RAM con la lista, iteramos (compatibilidad antigua)
        else if (productos != null) {
            NodoDE<DetalleFactura> actual = productos.getCabeza();
            while (actual != null) {
                DetalleFactura det = actual.getDato();
                sb.append(String.format("      👉 %d x %s\n", det.getCantidad(), det.getProducto().getNombre()));
                actual = actual.getSiguiente();
            }
        }
        sb.append("----------------------------------------------------------------");

        return sb.toString();
    }
}