package Attizos.Backend.Attizos;



import java.util.ArrayList;
import java.util.HashMap;

public class Inventario {
    private HashMap<String, Insumo> inventarioInsumos;
    private String nombrePizzeria;

    // Constructor limpio: Inicia el HashMap vacío
    public Inventario(String nombre) {
        this.inventarioInsumos = new HashMap<>();
        this.nombrePizzeria = nombre;
    }

    public void agregarInsumo(Insumo insumo) {
        if (insumo != null) {
            inventarioInsumos.put(insumo.getCodigo(), insumo);
        }
    }

    public Insumo buscarInsumo(String codigo) {
        return inventarioInsumos.get(codigo);
    }

    // Para cuando el proveedor trae más ingredientes
    public boolean registrarCompra(String codigo, double cantidad) {
        Insumo insumo = inventarioInsumos.get(codigo);
        if (insumo != null && cantidad > 0) {
            insumo.setStockActual(insumo.getStockActual() + cantidad);
            return true;
        }
        return false;
    }

    // Para cuando la cocina prepara un pedido
    public boolean consumirInsumo(String codigo, double cantidad) {
        Insumo insumo = inventarioInsumos.get(codigo);
        if (insumo != null && insumo.getStockActual() >= cantidad && cantidad > 0) {
            insumo.setStockActual(insumo.getStockActual() - cantidad);
            return true;
        }
        return false;
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

    public void mostrarInventario() {
        System.out.println("===================================================================================");
        System.out.println("                   INVENTARIO INSUMOS - " + nombrePizzeria);
        System.out.println("===================================================================================");
        // Ajustamos los espacios e incluimos la columna VENCE
        System.out.printf("%-10s | %-22s | %-10s | %6s | %6s | %4s | %-10s%n",
                "CÓDIGO", "INSUMO", "CATEGORÍA", "ACTUAL", "MÍNIMO", "UNID", "VENCE");
        System.out.println("-----------------------------------------------------------------------------------");

        if (inventarioInsumos.isEmpty()) {
            System.out.println("                   (El inventario de insumos está vacío)                           ");
        } else {
            for (Insumo i : inventarioInsumos.values()) {
                String alerta = i.getStockActual() < i.getStockMinimo() ? " ⚠️" : "";

                // Si la fecha no es nula, la mostramos, si es nula ponemos "N/A" (No Aplica)
                String fechaStr = (i.getFechaVencimiento() != null) ? i.getFechaVencimiento().toString() : "N/A";

                System.out.printf("%-10s | %-22.22s | %-10.10s | %6.2f | %6.2f | %4s | %-10s%s%n",
                        i.getCodigo(), i.getNombre(), i.getCategoria(), i.getStockActual(),
                        i.getStockMinimo(), i.getUnidad(), fechaStr, alerta);
            }
        }
        System.out.println("===================================================================================");
    }
    public void mostrarCompraSugerida() {
        System.out.println("\n========================================");
        System.out.println("       COMPRA SUGERIDA DEL DÍA");
        System.out.println("========================================");
        HashMap<String, Insumo> bajos = getStockBajo();
        if (bajos.isEmpty()) {
            System.out.println("✅ No se requieren compras urgentes en el almacén.");
        } else {
            System.out.printf("%-20s %10s %10s%n", "INSUMO", "ACTUAL", "A COMPRAR");
            System.out.println("----------------------------------------");
            for (Insumo i : bajos.values()) {
                // Sugiere comprar lo necesario para llegar al stock máximo
                double aComprar = i.getStockMaximo() - i.getStockActual();
                System.out.printf("%-20.20s %10.2f %10.2f %s%n",
                        i.getNombre(), i.getStockActual(), aComprar, i.getUnidad());
            }
        }
        System.out.println("========================================");
    }
    public void mostrarAlertasVencimiento(){
        System.out.println("\n========================================");
        System.out.println("       🚨 ALERTAS DE CADUCIDAD 🚨");
        System.out.println("========================================");

        boolean hayAlertas = false;

        for(Insumo i : inventarioInsumos.values()){
            if(i.getFechaVencimiento() != null){
                if(i.isVencido()){
                    System.out.println("❌ PELIGRO: [" + i.getNombre() + "] está VENCIDO desde el " + i.getFechaVencimiento());
                    hayAlertas = true;
                }else if(i.isPorVencer()){
                    long dias = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), i.getFechaVencimiento());
                    System.out.println("⚠️ ATENCIÓN: [" + i.getNombre() + "] vence en " + dias + " días (" + i.getFechaVencimiento() + ")");
                    hayAlertas = true;
                }
            }
        }
        if(!hayAlertas){
            System.out.println("✅ Todos los insumos están en buen estado.");
        }
        System.out.println("========================================");
    }
    public boolean consumirInsumoFEFO(String codigoBase, double cantidadRequerida){
        ArrayList<Insumo> lotesDisponibles = new ArrayList<>();
        double stockTotalValido = 0;
        boolean hayVencidos = false;
        for(Insumo ins : inventarioInsumos.values()){
            if(ins.getCodigo().equals(codigoBase) || ins.getCodigo().startsWith(codigoBase+ "-L")){
                if(ins.getStockActual() > 0){
                    if(ins.isVencido()){
                        hayVencidos = true;
                    }else{
                        lotesDisponibles.add(ins);
                        stockTotalValido += ins.getStockActual();
                    }
                }
            }
        }
        if(stockTotalValido < cantidadRequerida){
            System.out.println("❌ No hay stock válido suficiente para el insumo " + codigoBase + ".");
            System.out.println("   Faltan " + (cantidadRequerida - stockTotalValido) + " unidades.");
            if(hayVencidos){
                System.out.println("   💡 Nota: Hay más stock en almacén, pero el sistema lo bloqueó por estar caducado.");
            }
            return false;
        }
        lotesDisponibles.sort((i1,i2) ->{
            if (i1.getFechaVencimiento() == null && i2.getFechaVencimiento() == null) return 0;
            if (i1.getFechaVencimiento() == null) return 1;  // Sin fecha (no perecederos) van al final
            if (i2.getFechaVencimiento() == null) return -1;
            return i1.getFechaVencimiento().compareTo(i2.getFechaVencimiento());
        });
        double cantidadRestante = cantidadRequerida;
        for(Insumo lote : lotesDisponibles){
            if(cantidadRestante <= 0) break;;

            if(lote.getStockActual() >= cantidadRestante){
                lote.setStockActual(lote.getStockActual() - cantidadRestante);
                cantidadRestante = 0;
            }else{
                cantidadRestante -= lote.getStockActual();
                lote.setStockActual(0);
            }
        }
        return true;
    }
    public HashMap<String, Insumo> getInventarioInsumos(){
        return inventarioInsumos;
    }

}
