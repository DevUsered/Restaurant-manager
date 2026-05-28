package Attizos.Backend.Attizos;



public class Pasta extends Producto
{
    private String descripcionIngredientes;
    private String salsa;
    public Pasta(int id, String nombre, double precio, String categoria,String img,String descripcionIngredientes, String salsa){
        super(id, nombre, precio, categoria, 0.0,img);
        this.descripcionIngredientes = descripcionIngredientes;
        this.salsa = salsa;
    }
    @Override
    public void mostrarDetalles() {
        System.out.println("\n🍝 INFO DEL PLATO: " + getNombre());
        System.out.println("   Salsa: " + salsa);
        System.out.println("   Ingredientes: " + descripcionIngredientes);
        System.out.println("   Precio: Bs. " + getPrecio());
    }

    public String getIngredientes(){
        return descripcionIngredientes;
    }
    public String getSalsa(){
        return salsa;
    }

    public String getTipoSalsa() {
        return salsa;
    }
}

