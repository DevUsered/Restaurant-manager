package Attizos.Backend.Attizos;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Promocion extends Producto{
    private List<DetalleCombo> productosCombo;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    public Promocion(int id, String nombre, double precioFinal, String imagenURL, LocalDate fechaInicio, LocalDate fechaFin){
        super(id, nombre, precioFinal,"Promocion",0, imagenURL, "Activo");
        this.productosCombo = new ArrayList<>();
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }
    public void agregarProducto(Producto p, int cant){
        this.productosCombo.add(new DetalleCombo(p, cant));
    }
    public List<DetalleCombo> getProductosCombo() {
        return productosCombo;
    }

    @Override
    public boolean isPromocion(){
        return true;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }
}
