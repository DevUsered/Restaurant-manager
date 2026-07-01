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
    private boolean tieneReceta;

    public Producto(){
        this.tieneReceta = false;
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
    public void aumentarStock(int cantidad) {
        this.stock += cantidad;
    }

    public boolean reducirStock(int cantidad) {
        if (this.stock >= cantidad) {
            this.stock -= cantidad;
            return true;
        }
        return false;
    }
    public void agregarAtributo(String clave, String valor) {
        if (this.atributosDinamicos == null) {
            this.atributosDinamicos = new HashMap<>();
        }
        this.atributosDinamicos.put(clave, valor);
    }

    public String getAtributo(String clave) {
        return this.atributosDinamicos.getOrDefault(clave, "");
    }

    public Map<String, String> getAtributosDinamicos() {
        return atributosDinamicos;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public double getStock() {
        return stock;
    }

    public void setStock(double stock) {
        this.stock = stock;
    }

    public Receta getReceta() {
        return receta;
    }

    public void setReceta(Receta receta) {
        this.receta = receta;
        if(receta != null) this.tieneReceta = true;
    }

    public boolean isTieneReceta() {
        return tieneReceta;
    }

    public void setTieneReceta(boolean tieneReceta) {
        this.tieneReceta = tieneReceta;
    }

    public String getImagenURL() {
        return imagenURL;
    }

    public void setImagenURL(String imagenURL) {
        this.imagenURL = imagenURL;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setAtributosDinamicos(Map<String, String> atributosDinamicos) {
        this.atributosDinamicos = atributosDinamicos;
    }
    public boolean tieneReceta() {
        return this.receta != null && !this.receta.esVacia();
    }

    public boolean isPromocion() {
        return false;
    }

    @Override
    public String toString() {
        return nombre + " - Bs. " + precio;
    }
}
