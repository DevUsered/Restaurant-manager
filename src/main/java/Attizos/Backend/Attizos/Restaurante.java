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
    private ListaDE<Producto> menu;
    private Inventario inventario;
    private ArrayList<Empleado> empleados;
    private ListaDE<Reserva> reservas;
    private ListaDE<Pedido> colaPedidos;

    private HashMap<Integer, HashMap<String, Double>> lotesConsumidosPorPedido;

    public Restaurante(String nombre){
        this.nombre = nombre;
        this.inventario = new Inventario(nombre);
        this.menu = new ListaDE<Producto>();
        this.empleados = new ArrayList<Empleado>();
        this.reservas = new ListaDE<>();
        this.colaPedidos = new ListaDE<>();
        this.lotesConsumidosPorPedido = new HashMap<>();
    }
    public void agregarProducto(Producto nuevo) {
        if (nuevo != null) {
            menu.insertarAlFinal(nuevo);
        }
    }
    public Producto buscarPorId(int id) {
        NodoDE<Producto> aux = menu.getCabeza();
        while (aux != null) {
            if (aux.getDato().getId() == id) {
                return aux.getDato();
            }
            aux = aux.getSiguiente();
        }
        return null;
    }
    public boolean eliminarProducto(int id) {
        Producto prod = buscarPorId(id);
        if (prod != null) {
            menu.eliminarPorValor(prod);
            return true;
        }
        return false;
    }
    public ListaDE<Producto> obtenerProductosDisponibles() {
        ListaDE<Producto> disponibles = new ListaDE<>();
        NodoDE<Producto> aux = menu.getCabeza();
        while (aux != null) {
            Producto p = aux.getDato();

            if (p.tieneReceta() || p.getStock() > 0) {
                disponibles.insertarAlFinal(p);
            }
            aux = aux.getSiguiente();
        }
        return disponibles;
    }
    public ListaDE<Producto> buscarPorCategoria(String cat) {
        ListaDE<Producto> filtrados = new ListaDE<>();
        NodoDE<Producto> aux = menu.getCabeza();
        while (aux != null) {
            if (aux.getDato().getCategoria().equalsIgnoreCase(cat)) {
                filtrados.insertarAlFinal(aux.getDato());
            }
            aux = aux.getSiguiente();
        }
        return filtrados;
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
    public ListaDE<Producto> getMenu() {
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
        NodoDE<Producto> auxId = menu.getCabeza();
        while(auxId != null){
            if(auxId.getDato().getId() >= idGenerado){
                idGenerado = auxId.getDato().getId() + 1;
            }
            auxId = auxId.getSiguiente();
        }
        return idGenerado;
    }

    public void setMenu(ListaDE<Producto> menuActualizado) {
        if(menuActualizado != null){
            this.menu = menuActualizado;
        }
    }
}
