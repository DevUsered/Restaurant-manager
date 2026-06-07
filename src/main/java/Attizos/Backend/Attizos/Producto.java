package Attizos.Backend.Attizos;


import java.util.Map;
import java.util.HashMap;

public  class Producto
{
    private int id;
    private String nombre;
    private double precio;
    private String categoria;
    private double stock;
    private Receta receta;
    private String imagenURL;
    private String estado;
    private Map<String, String> atributosDinamicos;

    public Producto(){
        this.atributosDinamicos = new HashMap<>();
    }
    public Producto(int id, String nombre, double precio, String categoria, double stock, String imagenURL, String estado){
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.categoria = categoria;
        this.stock = stock;
        this.estado = estado;
        this.receta = null;
        this.atributosDinamicos = new HashMap<>();

        if(imagenURL == null || imagenURL.isEmpty()){
            this.imagenURL = "/images/default.png";
        } else {
            this.imagenURL = imagenURL;
        }
    }
    public String getAtributo(String clave){
        return this.atributosDinamicos.getOrDefault(clave, "");
    }
    public  Map<String, String> getAtributosDinamicos() {
        return atributosDinamicos;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public double getStock() { return stock; }
    public void setStock(double stock) { this.stock = stock; }

    public String getImagenURL() { return imagenURL; }
    public void setImagenURL(String imagenURL) { this.imagenURL = imagenURL; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Receta getReceta() { return receta; }
    public void setReceta(Receta receta) { this.receta = receta; }

    public boolean tieneReceta() {
        return this.receta != null && !this.receta.esVacia();
    }

    public void aumentarStock(int cantidad){
        this.stock += cantidad;
    }
    public boolean reducirStock(int cantidad){
        if(this.stock >= cantidad){
            this.stock -= cantidad;
            return true;
        }
        return false;
    }
    public int calcularDisponibilidad(Inventario inventario) {
        if (!tieneReceta()) {
            return (int) this.stock;
        }
        Receta r = this.getReceta();
        int maxPlatosPosibles = Integer.MAX_VALUE;

        for (Map.Entry<String, Double> entry : r.getIngredientes().entrySet()) {
            String codInsumoBase = entry.getKey();
            double cantNecesariaPorPlato = entry.getValue();
            double stockValido = 0;

            for (Insumo i : inventario.getInventarioInsumos().values()) {
                if ((i.getCodigo().equals(codInsumoBase) || i.getCodigo().startsWith(codInsumoBase + "-L")) && !i.isVencido()) {
                    stockValido += i.getStockActual();
                }
            }
            int porciones = (int) (stockValido / cantNecesariaPorPlato);

            if (porciones < maxPlatosPosibles) {
                maxPlatosPosibles = porciones;
            }
        }
        return maxPlatosPosibles == Integer.MAX_VALUE ? 0 : maxPlatosPosibles;
    }
    public void agregarAtributo(String clave, String valor) {
        if (this.atributosDinamicos == null) {
            this.atributosDinamicos = new HashMap<>();
        }
        // Ahora sí guardamos el dato de forma segura
        this.atributosDinamicos.put(clave, valor);
    }
    public boolean isPromocion(){
        return false;
    }
    @Override
    public String toString(){
        return nombre + "- Bs. "+precio;
    }
}
