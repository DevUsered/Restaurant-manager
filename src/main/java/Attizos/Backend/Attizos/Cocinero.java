package Attizos.Backend.Attizos;

public class Cocinero extends Usuario{
    public Cocinero(String id, String nombre, double sueldo, String username, String password) {
        super(id, nombre, "Cocinero", sueldo, username, password);
    }
    @Override
    public void mostrarMenu(){
       //No hay nada
    }
}
