package Attizos.Backend.Attizos;


import java.util.HashMap;
import java.util.Map;

public class Receta {
    private HashMap<String, Double> ingredientes;

    public Receta() {
        this.ingredientes = new HashMap<>();
    }

    // Método para armar la receta ingrediente por ingrediente
    public void agregarIngrediente(String codigoInsumo, double cantidadNecesaria) {
        if (cantidadNecesaria > 0) {
            ingredientes.put(codigoInsumo, cantidadNecesaria);
        }
    }

    // Devuelve el mapa completo de ingredientes
    public HashMap<String, Double> getIngredientes() {
        return ingredientes;
    }

    // Verifica si la receta tiene ingredientes registrados
    public boolean esVacia() {
        return ingredientes.isEmpty();
    }
}
