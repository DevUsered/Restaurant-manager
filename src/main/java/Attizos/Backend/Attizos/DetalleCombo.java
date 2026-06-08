package Attizos.Backend.Attizos;

public class DetalleCombo {
    private Producto producto;
    private int cantidad;

    public DetalleCombo(Producto p, int cant){
        this.producto = p;
        this.cantidad = cant;
    }
    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }
    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }
}
