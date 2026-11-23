package Attizos.Backend.Attizos;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.Map;

public class Producto {
    public static final String ATTR_TAMANO = "tamano";

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

    public Producto() {
        this.tieneReceta = false;
        this.atributosDinamicos = new HashMap<>();
    }

    public Producto(int id, String nombre, double precio, String categoria, double stock, String imagenURL, String estado) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.categoria = categoria;
        this.stock = stock;
        this.estado = estado;
        this.receta = null;
        this.atributosDinamicos = new HashMap<>();

        if (imagenURL == null || imagenURL.isEmpty()) {
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
        if (clave != null && valor != null) {
            this.atributosDinamicos.put(clave, valor);
        }
    }

    public String getAtributo(String clave) {
        if (this.atributosDinamicos == null || clave == null) {
            return "";
        }
        return this.atributosDinamicos.getOrDefault(clave, "");
    }

    @JsonIgnore
    public String getTamano() {
        if (isPromocion() || "Promocion".equalsIgnoreCase(this.categoria)) {
            return "";
        }
        return getAtributo(ATTR_TAMANO);
    }

    @JsonIgnore
    public void setTamano(String tamano) {
        if (this.atributosDinamicos == null) {
            this.atributosDinamicos = new HashMap<>();
        }
        if (isPromocion()
                || "Promocion".equalsIgnoreCase(this.categoria)
                || tamano == null
                || tamano.trim().isEmpty()
                || tamano.equalsIgnoreCase("Sin tamaño")) {
            this.atributosDinamicos.remove(ATTR_TAMANO);
        } else {
            this.atributosDinamicos.put(ATTR_TAMANO, tamano.trim());
        }
    }

    @JsonIgnore
    public String getNombreConTamano() {
        String tamano = getTamano();
        if (tamano != null && !tamano.isEmpty()) {
            return this.nombre + " (" + tamano + ")";
        }
        return this.nombre;
    }

    public Map<String, String> getAtributosDinamicos() {
        if (this.atributosDinamicos == null) {
            this.atributosDinamicos = new HashMap<>();
        }
        return atributosDinamicos;
    }

    public void setAtributosDinamicos(Map<String, String> atributosDinamicos) {
        this.atributosDinamicos = (atributosDinamicos != null) ? atributosDinamicos : new HashMap<>();
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
        if (receta != null) this.tieneReceta = true;
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

    public boolean tieneReceta() {
        return this.receta != null && !this.receta.esVacia();
    }

    public boolean isPromocion() {
        return false;
    }

    @Override
    public String toString() {
        return getNombreConTamano() + " - Bs. " + precio;
    }
}