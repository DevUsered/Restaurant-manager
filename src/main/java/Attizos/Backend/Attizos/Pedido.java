package Attizos.Backend.Attizos;

import Attizos.Backend.Listas.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Pedido {
    private int idPedido;
    private int numeroTicket;
    private String descripcionBreve;
    
    private String fechaHora;
    private ArrayList<DetalleFactura> productos;
    private double total;
    private String estado;
    private String cliente;

    public Pedido() {
        this.estado = "Pendiente";
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("HH:mm");
        this.fechaHora = LocalDateTime.now().format(formato);
    }

    public Pedido(int idPedido, String cliente, ArrayList<DetalleFactura> productos, double total) {
        this.idPedido = idPedido;
        this.cliente = cliente;
        this.productos = productos;
        this.total = total;
        this.estado = "En Espera";

        DateTimeFormatter formato = DateTimeFormatter.ofPattern("HH:mm");
        this.fechaHora = LocalDateTime.now().format(formato);
    }

    public int getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(int idPedido) {
        this.idPedido = idPedido;
    }

    public int getNumeroTicket() {
        return numeroTicket;
    }

    public void setNumeroTicket(int numeroTicket) {
        this.numeroTicket = numeroTicket;
    }

    public String getDescripcionBreve() {
        return descripcionBreve;
    }

    public void setDescripcionBreve(String descripcionBreve) {
        this.descripcionBreve = descripcionBreve;
    }

    public String getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(String fechaHora) {
        this.fechaHora = fechaHora;
    }

    public ArrayList<DetalleFactura> getProductos() {
        return productos;
    }

    public void setProductos(ArrayList<DetalleFactura> productos) {
        this.productos = productos;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }
}