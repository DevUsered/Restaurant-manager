package Attizos.Backend.Attizos;

import Attizos.Backend.Listas.*;

import java.time.LocalDateTime;
import java.util.*;
import java.time.LocalDate;

public class Restaurante
{
    private String nombre;
    private ArrayList<Producto> menu;
    private Inventario inventario;
    private ArrayList<Empleado> empleados;
    private ArrayList<Reserva> reservas;
    private LinkedList<Pedido> colaPedidos;
    private ArrayList<Promocion> promocionesActivas;

    private HashMap<Integer, HashMap<String, Double>> lotesConsumidosPorPedido;

    public Restaurante(String nombre){
        this.nombre = nombre;
        this.inventario = new Inventario(nombre);
        this.menu = new ArrayList<>();
        this.empleados = new ArrayList<Empleado>();
        this.reservas = new ArrayList<>();
        this.colaPedidos = new LinkedList<>();
        this.lotesConsumidosPorPedido = new HashMap<>();
        this.promocionesActivas = new ArrayList<>();
    }
    public void agregarProducto(Producto nuevo) {
        if (nuevo != null) {
            menu.add(nuevo);
        }
    }
    public Producto buscarPorId(int id) {
        for(Producto p : menu){
            if(p.getId() == id) return p;
        }
        return null;
    }
    public boolean eliminarProducto(int id) {
        for(Producto p : menu){
            if(p.getId() == id){
                menu.remove(p);
                return true;
            }
        }
        return false;
    }
    public ArrayList<Producto> obtenerProductosDisponibles() {
        ArrayList<Producto> disponibles = new ArrayList<>();
        for(Producto p : menu){
            if(p.getEstado() != null && p.getEstado().equalsIgnoreCase("Activo")){
                disponibles.add(p);
            }
        }
        return disponibles;
    }
    public ArrayList<Producto> buscarPorCategoria(String cat) {
        ArrayList<Producto> productos = new ArrayList<>();
        for(Producto p : menu){
            if(p.getCategoria().equalsIgnoreCase(cat)){
                productos.add(p);
            }
        }
        return productos;
    }
    public void agregarEmpleado(Empleado nuevoEmpleado) {
        if(nuevoEmpleado != null){
            empleados.add(nuevoEmpleado);
        }
    }
    public Empleado autenticarEmpleado(String username, String password){
        for(Empleado emp : empleados){
            if(emp.getUsername() != null && emp.getUsername().equals(username)){
                if(emp.getPasswordHash() != null && emp.getPasswordHash().equals(password)){
                    return emp;
                }
            }
        }
        return null;
    }
    public void agregarReserva(Reserva r) {
        reservas.add(r);
    }

    public ArrayList<Reserva> getReservas() {
        return reservas;
    }

    public String generarIdReserva(java.time.LocalDateTime fechaReserva) {
        String[] inicialesDias = {"L", "M", "X", "J", "V", "S", "D"};
        String letraDia = inicialesDias[fechaReserva.getDayOfWeek().getValue() - 1];

        int correlativoMes = 1;
        for( Reserva r : reservas){
            LocalDateTime f = r.getFecha();
            if(f.getMonthValue() == fechaReserva.getMonthValue() && f.getYear() == fechaReserva.getYear()){
                correlativoMes++;
            }
        }
        int diaMes = fechaReserva.getDayOfMonth();
        return String.format("%s%03d%02d",letraDia,correlativoMes,diaMes);
    }

    public Reserva buscarReserva(String id) {
        for(Reserva r : reservas){
            if(r.getId().equalsIgnoreCase(id)) return r;
        }
        return null;
    }

    public LinkedList<Pedido> getPedidos(){ return colaPedidos; }
    public void agregarPedido(Pedido p){
        colaPedidos.addLast(p);
    }
    public Pedido buscarPedido(int id){
        for(Pedido p : colaPedidos){
            if(p.getIdPedido() == id) return p;
        }
        return null;
    }
    public Pedido atenderSiguientePedido(){
        return colaPedidos.pollFirst();
    }
    public boolean cancelarPedido(int idPedido){
        Iterator<Pedido> iterador = colaPedidos.iterator();
        while (iterador.hasNext()){
            Pedido pedidoCancelado = iterador.next();
            if(pedidoCancelado.getIdPedido() == idPedido){
                HashMap<String, Double> lotesUsados = lotesConsumidosPorPedido.get(idPedido);
                if(lotesUsados != null) {
                    for (Map.Entry<String, Double> entry : lotesUsados.entrySet()) {
                        String codLoteExacto = entry.getKey();
                        double cantidadADevolver = entry.getValue();

                        Insumo loteReal = inventario.buscarInsumo(codLoteExacto);
                        if (loteReal != null) {
                            loteReal.setStockActual(loteReal.getStockActual() + cantidadADevolver);
                        } else {
                            String base = codLoteExacto.split("-L")[0];
                            Insumo insumoBase = inventario.buscarInsumo(base);
                            if (insumoBase != null)
                                insumoBase.setStockActual(insumoBase.getStockActual() + cantidadADevolver);
                        }
                    }
                    lotesConsumidosPorPedido.remove(idPedido);
                }
                for(DetalleFactura df : pedidoCancelado.getProductos()){
                    Producto p = df.getProducto();
                    if(!p.tieneReceta()){
                        p.aumentarStock(df.getCantidad());
                    }
                }
                iterador.remove();
                return true;
            }
        }
        return false;
    }
    public void registrarVentaFinalizada(Factura f){
        HashMap<String, Double> consumoExactoDeEstaFactura = new HashMap<>();

        for (DetalleFactura df : f.getDetalles()) {
            Producto p = df.getProducto();
            int cantVendida = df.getCantidad();
            if(p.tieneReceta() && p.getReceta() != null){
                for(Map.Entry<String, Double> entry : p.getReceta().getIngredientes().entrySet()){
                    String codigoInsumo = entry.getKey();
                    double cantidadNecesaria = entry.getValue() * cantVendida;
                    HashMap<String, Double> stockAntes = new HashMap<>();
                    for(Insumo i : inventario.getInventarioInsumos().values()){
                        if(i.getCodigo().equals(codigoInsumo) || i.getCodigo().startsWith(codigoInsumo + "-L")){
                            stockAntes.put(i.getCodigo(), i.getStockActual());
                        }
                    }
                    inventario.consumirInsumoFEFO(codigoInsumo, cantidadNecesaria);
                    for(String codLote : stockAntes.keySet()){
                        Insumo i = inventario.buscarInsumo(codLote);
                        double scockDespues = (i != null) ? i.getStockActual() : 0.0;
                        double diferencia = stockAntes.get(codLote) - scockDespues;

                        if(diferencia > 0){
                            consumoExactoDeEstaFactura.put(codLote, consumoExactoDeEstaFactura.getOrDefault(codLote, 0.0) + diferencia);
                        }
                    }
                }
            }else{
                p.reducirStock(cantVendida);
            }
        }
        lotesConsumidosPorPedido.put(f.getNumeroFactura(), consumoExactoDeEstaFactura);
    }
    public Inventario getInventario() {
        return inventario;
    }
    public ArrayList<Producto> getMenu() {
        return menu;
    }
    public String getNombre() {
        return nombre;
    }
    public ArrayList<Empleado> getEmpleados() {
        return empleados;
    }

    public Empleado buscarEmpleado(String id){
        for(Empleado emp : empleados){
            if(emp.getIdEmpleado().equalsIgnoreCase(id)){
                return emp;
            }
        }
        return null;
    }
    public boolean eliminarEmpleado(String id) {
        Iterator<Empleado> iterador = empleados.iterator();
        while (iterador.hasNext()) {
            Empleado emp = iterador.next();
            if (emp.getIdEmpleado().equalsIgnoreCase(id)) {
                iterador.remove();
                return true;
            }
        }
        return false;
    }
    public int generarIdNuevoProducto(){
        int idGenerado = 1;
        for(Producto p : menu){
            if(p.getId() >= idGenerado){
                idGenerado = p.getId() + 1;
            }
        }
        return idGenerado;
    }

    public void setMenu(ArrayList<Producto> menuActualizado) {
        if(menuActualizado != null){
            this.menu = menuActualizado;
        }
    }
    public ArrayList<Promocion> getPromocionesActivas(){
        return promocionesActivas;
    }

    public void setPromocionesActivas(ArrayList<Promocion> promocionesActivas) {
        this.promocionesActivas = promocionesActivas;
    }
}
