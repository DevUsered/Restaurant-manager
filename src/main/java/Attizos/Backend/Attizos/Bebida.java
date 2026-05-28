package Attizos.Backend.Attizos;


public class Bebida extends Producto
{
    private TamanoBebida tamano;
    private String tipo;
    public Bebida(int id, String nombre, double precio, String categoria, int stock,String imagenURL,
                  TamanoBebida tamano, String tipo){
        super(id, nombre, precio, categoria, stock, imagenURL);
        this.tamano = tamano;
        this.tipo = tipo;
    }

    public TamanoBebida getTamano() {
        return tamano;
    }
    public void setTamano(TamanoBebida tamano){
        this.tamano = tamano;
    }
    public String getTipo() {
        return tipo;
    }
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    @Override
    public String toString() {
        return super.toString() + String.format(" | Tam: %-10s | Tipo: %-10s",
                    tamano.getDescripcion(), tipo);
    }

    public String getTipoBebida() {
        return tipo;
    }
}
