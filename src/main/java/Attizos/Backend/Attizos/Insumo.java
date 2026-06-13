package Attizos.Backend.Attizos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
public class Insumo {
    private String codigo;
    private String nombre;
    private String categoria;
    private double stockActual;
    private double stockMinimo;
    private double stockMaximo;
    private LocalDate fechaVencimiento;

    public String estado;
    private String unidad;
    
    public Insumo(){}

    public Insumo(String codigo, String nombre, String categoria, String unidad,
                  double stockActual, double stockMinimo, double stockMaximo,
                  LocalDate fechaVencimiento) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.categoria = categoria;
        this.unidad = unidad;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.stockMaximo = stockMaximo;
        this.fechaVencimiento = fechaVencimiento;
    }

    // Getters
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
    public String getUnidad() { return unidad; }
    public double getStockActual() { return stockActual; }
    public double getStockMinimo() { return stockMinimo; }
    public double getStockMaximo() { return stockMaximo; }
    public LocalDate getFechaVencimiento(){return fechaVencimiento;}

    //Control de vencimiento
    public boolean isVencido(){
        return fechaVencimiento != null && fechaVencimiento.isBefore(LocalDate.now());
    }
    //Retorna los días restantes para el vencimiento (negativo si ya venció)
    public boolean isPorVencer(){
        if(fechaVencimiento == null || isVencido()) return false;
        long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), fechaVencimiento);
        return diasRestantes >= 0 && diasRestantes <= 7; // Consideramos "por vencer" si quedan 7 días o menos
    }

    // Setters principales
    public void setStockActual(double stockActual) { this.stockActual = stockActual; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public void mostrarInfo() {
        System.out.println("Insumo: " + nombre + " (" + categoria + ")");
        System.out.println("Código: " + codigo);
        System.out.println("Stock actual: " + stockActual + " " + unidad);
        System.out.println("Stock mínimo: " + stockMinimo + " " + unidad);
        System.out.println("Stock máximo: " + stockMaximo + " " + unidad);

        if(fechaVencimiento != null){
            System.out.println("Vencimiento: " + fechaVencimiento);
            if (isVencido()) {
                System.out.println("⚠️ ESTADO: ¡VENCIDO!");
            } else if (isPorVencer()) {
                System.out.println("⚠️ ESTADO: ¡Próximo a vencer!");
            }
        } else {
            System.out.println("Vencimiento: No perecedero");
        }
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public void setStockMinimo(double stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public void setStockMaximo(double stockMaximo) {
        this.stockMaximo = stockMaximo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    @Override
    public String toString(){
        return nombre;
    }
}
