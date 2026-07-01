package Attizos.Backend.Attizos;



import java.util.ArrayList;
import java.util.HashMap;

public class Inventario {
    private HashMap<String, Insumo> inventarioInsumos;
    private String nombreNegocio;
    public Inventario(){}

    public Inventario(String nombre) {
        this.inventarioInsumos = new HashMap<>();
        this.nombreNegocio = nombre;
    }
    public void agregarInsumo(Insumo insumo) {
        if (insumo != null) {
            inventarioInsumos.put(insumo.getCodigo(), insumo);
        }
    }

    public Insumo buscarInsumo(String codigo) {
        return inventarioInsumos.get(codigo);
    }

    public boolean registrarCompra(String codigo, double cantidad) {
        Insumo insumo = inventarioInsumos.get(codigo);
        if (insumo != null && cantidad > 0) {
            insumo.setStockActual(insumo.getStockActual() + cantidad);
            return true;
        }
        return false;
    }

    public boolean consumirInsumo(String codigo, double cantidad) {
        Insumo insumo = inventarioInsumos.get(codigo);
        if (insumo != null && insumo.getStockActual() >= cantidad && cantidad > 0) {
            insumo.setStockActual(insumo.getStockActual() - cantidad);
            return true;
        }
        return false;
    }
    public boolean consumirInsumoFEFO(String codigoBase, double cantidadRequerida) {
        ArrayList<Insumo> lotesDisponibles = new ArrayList<>();
        double stockTotalValido = 0;

        for (Insumo ins : inventarioInsumos.values()) {
            if (ins.getCodigo().equals(codigoBase) || ins.getCodigo().startsWith(codigoBase + "-L")) {
                if (ins.getStockActual() > 0 && !ins.isVencido()) {
                    lotesDisponibles.add(ins);
                    stockTotalValido += ins.getStockActual();
                }
            }
        }

        if (stockTotalValido < cantidadRequerida) {
            return false;
        }

        lotesDisponibles.sort((i1, i2) -> {
            if (i1.getFechaVencimiento() == null && i2.getFechaVencimiento() == null) return 0;
            if (i1.getFechaVencimiento() == null) return 1;
            if (i2.getFechaVencimiento() == null) return -1;
            return i1.getFechaVencimiento().compareTo(i2.getFechaVencimiento());
        });

        double cantidadRestante = cantidadRequerida;
        for (Insumo lote : lotesDisponibles) {
            if (cantidadRestante <= 0) break;

            if (lote.getStockActual() >= cantidadRestante) {
                lote.setStockActual(lote.getStockActual() - cantidadRestante);
                cantidadRestante = 0;
            } else {
                cantidadRestante -= lote.getStockActual();
                lote.setStockActual(0);
            }
        }
        return true;
    }

    public boolean hayStockSuficiente(String codigoBase, double cantidadRequerida) {
        double stockTotal = 0;
        for (Insumo ins : inventarioInsumos.values()) {
            if ((ins.getCodigo().equals(codigoBase) || ins.getCodigo().startsWith(codigoBase + "-L")) && !ins.isVencido()) {
                stockTotal += ins.getStockActual();
            }
        }
        return stockTotal >= cantidadRequerida;
    }
    public HashMap<String, Insumo> getStockBajo() {
        HashMap<String, Insumo> stockBajo = new HashMap<>();
        for (Insumo i : inventarioInsumos.values()) {
            if (i.getStockActual() < i.getStockMinimo()) {
                stockBajo.put(i.getCodigo(), i);
            }
        }
        return stockBajo;
    }

    public HashMap<String, Insumo> getInventarioInsumos() {
        return inventarioInsumos;
    }

    public String getNombreNegocio() {
        return nombreNegocio;
    }
}
