package Attizos.Backend.Attizos;

import Attizos.Backend.Listas.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.time.LocalDate;

public class Restaurante
{
    private String nombre;
    private ArrayList<Producto> menu;
    private Inventario inventario;
    private ArrayList<Empleado> empleados;
    private ListaDE<Reserva> reservas;
    private ListaDE<Pedido> colaPedidos;
    private ArrayList<Promocion> promocionesActivas;

    private HashMap<Integer, HashMap<String, Double>> lotesConsumidosPorPedido;

    public Restaurante(String nombre){
        this.nombre = nombre;
        this.inventario = new Inventario(nombre);
        this.menu = new ArrayList<>();
        this.empleados = new ArrayList<Empleado>();
        this.reservas = new ListaDE<>();
        this.colaPedidos = new ListaDE<>();
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
        reservas.insertarAlFinal(r);
    }

    public ListaDE<Reserva> getReservas() {
        return reservas;
    }

    public String generarIdReserva(java.time.LocalDateTime fechaReserva) {
        String[] inicialesDias = {"L", "M", "X", "J", "V", "S", "D"};
        String letraDia = inicialesDias[fechaReserva.getDayOfWeek().getValue() - 1];

        int correlativoMes = 1;
        NodoDE<Reserva> aux = reservas.getCabeza();
        while (aux != null) {
            java.time.LocalDateTime f = aux.getDato().getFecha();
            if (f.getMonthValue() == fechaReserva.getMonthValue() && f.getYear() == fechaReserva.getYear()) {
                correlativoMes++;
            }
            aux = aux.getSiguiente();
        }
        int diaMes = fechaReserva.getDayOfMonth();
        return String.format("%s%03d%02d",letraDia,correlativoMes,diaMes);
    }

    public Reserva buscarReserva(String id) {
        NodoDE<Reserva> aux = reservas.getCabeza();
        while (aux != null) {
            if (aux.getDato().getId().equalsIgnoreCase(id)) return aux.getDato();
            aux = aux.getSiguiente();
        }
        return null;
    }

    public ListaDE<Pedido> getPedidos(){ return colaPedidos; }
    public void agregarPedido(Pedido p){
        colaPedidos.insertarAlFinal(p);
    }
    public Pedido buscarPedido(int id){
        NodoDE<Pedido> aux = colaPedidos.getCabeza();
        while (aux != null) {
            if (aux.getDato().getIdPedido() == id) return aux.getDato();
            aux = aux.getSiguiente();
        }
        return null;
    }
    public Pedido atenderSiguientePedido(){
        return colaPedidos.eliminarElInicio();
    }
    public boolean cancelarPedido(int idPedido){
        NodoDE<Pedido> ac = colaPedidos.getCabeza();
        while(ac != null){
            if(ac.getDato().getIdPedido() == idPedido){
                Pedido pedidoCancelado = ac.getDato();

                HashMap<String, Double> lotesUsados = lotesConsumidosPorPedido.get(idPedido);
                if(lotesUsados != null){
                    for(Map.Entry<String, Double> entry : lotesUsados.entrySet()){
                        String codLoteExacto = entry.getKey();
                        double cantidadADevolver = entry.getValue();

                        Insumo loteReal = inventario.buscarInsumo(codLoteExacto);
                        if(loteReal != null){
                            loteReal.setStockActual(loteReal.getStockActual() + cantidadADevolver);
                        }else{
                            String base = codLoteExacto.split("-L")[0];
                            Insumo insumoBase = inventario.buscarInsumo(base);
                            if(insumoBase != null) insumoBase.setStockActual(insumoBase.getStockActual() + cantidadADevolver);
                        }
                    }
                    lotesConsumidosPorPedido.remove(idPedido);
                }
                NodoDE<DetalleFactura> detAc = pedidoCancelado.getProductos().getCabeza();
                while(detAc != null){
                    Producto p = detAc.getDato().getProducto();
                    if(!p.tieneReceta()){
                        p.aumentarStock(detAc.getDato().getCantidad());
                    }
                    detAc = detAc.getSiguiente();
                }
                colaPedidos.eliminarPorValor(pedidoCancelado);
                return  true;
            }
            ac = ac.getSiguiente();
        }
        return false;
    }
    public void registrarVentaFinalizada(Factura f){
        NodoDE<DetalleFactura> ac = f.getDetalles().getCabeza();

        HashMap<String, Double> consumoExactoDeEstaFactura = new HashMap<>();
        while(ac != null){
            DetalleFactura df = ac.getDato();
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
            ac = ac.getSiguiente();
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
