package Attizos.Backend.Attizos;

import Attizos.Backend.Listas.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.time.LocalDate;

public class Restaurante
{
    private String nombre;
    private ListaDE<Producto> menu;
    private Inventario inventario;
    private ArrayList<Empleado> empleados;
    private ListaDE<Reserva> reservas = new ListaDE<>();
    private ListaDE<Pedido> colaPedidos = new ListaDE<>();
    private ListaDE<Factura> historialVentas;
    private LocalDate fechaFacturacion;
    private int contadorFacturasDiarias;
    private ListaDE<Egreso> expenseHistory;

    public Restaurante(String nombre){
        this.nombre = nombre;
        inventario = new Inventario(nombre);
        menu = new ListaDE<Producto>();
        empleados = new ArrayList<Empleado>();
        this.reservas = new ListaDE<>();
        this.colaPedidos = new ListaDE<>();
        this.historialVentas = new ListaDE<>();
        this.fechaFacturacion = LocalDate.now();
        this.expenseHistory = new ListaDE<>();
        this.contadorFacturasDiarias = 0;
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
    public Producto buscarPorNombre(String nombre) {
        NodoDE<Producto> aux = menu.getCabeza();
        while (aux != null) {
            if (aux.getDato().getNombre().equalsIgnoreCase(nombre)) {
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
    public Usuario autenticarEmpleado(String username, String password){
        for(Empleado emp : empleados){
            if(emp instanceof Usuario){
                Usuario user = (Usuario) emp;
                if(user.login(username,password)){
                    return user;
                }
            }
        }
        return null;
    }
    public void mostrarPlanillaEmpleados(){
        System.out.println("\n---PLANILLA DE EMPLEADOS---");
        double totalSueldos = 0;
        for(Empleado emp : empleados){
            System.out.println(emp);
            totalSueldos += emp.getSueldo();
        }
        System.out.println("----------------------------");
        System.out.println("Total en sueldos: Bs." + totalSueldos);
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
        //3. Obtener el día del mes(ZZ)
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
    public boolean cancelarPedido(int idPedido) {
        NodoDE<Pedido> actual = colaPedidos.getCabeza();

        while (actual != null) {
            if (actual.getDato().getIdPedido() == idPedido) {
                // 1. Guardamos el pedido que vamos a cancelar
                Pedido pedidoCancelado = actual.getDato();

                NodoDE<DetalleFactura> detActual = pedidoCancelado.getProductos().getCabeza();
                while (detActual != null) {
                    DetalleFactura df = detActual.getDato();
                    Producto p = df.getProducto();
                    int cantDevolver = df.getCantidad();

                    if (p.tieneReceta()) {
                        for (Map.Entry<String, Double> entry : p.getReceta().getIngredientes().entrySet()) {
                            devolverIngredienteInteligente(entry.getKey(), entry.getValue() * cantDevolver);
                        }
                    }
                    else {
                        p.aumentarStock(cantDevolver);
                    }
                    detActual = detActual.getSiguiente();
                }

                colaPedidos.eliminarPorValor(pedidoCancelado);
                return true;
            }
            actual = actual.getSiguiente();
        }
        return false;
    }
    private void devolverIngredienteInteligente(String codBase, double cantidad) {
        Insumo insumoDestino = null;

        for (Insumo i : inventario.getInventarioInsumos().values()) {
            if (i.getCodigo().equals(codBase) || i.getCodigo().startsWith(codBase + "-L")) {

                if (!i.isVencido()) {
                    if (insumoDestino == null) {
                        insumoDestino = i;
                    } else if (i.getFechaVencimiento() != null && insumoDestino.getFechaVencimiento() != null) {
                        if (i.getFechaVencimiento().isBefore(insumoDestino.getFechaVencimiento())) {
                            insumoDestino = i;
                        }
                    }
                }
            }
        }

        if (insumoDestino == null) {
            insumoDestino = inventario.buscarInsumo(codBase);
        }

        if (insumoDestino != null) {
            insumoDestino.setStockActual(insumoDestino.getStockActual() + cantidad);
        }
    }
    public void retrocederCorrelativoFactura() {
        if (contadorFacturasDiarias > 0) {
            contadorFacturasDiarias--;
        }
    }
    public void mostrarMenuCompleto() {
        System.out.println("\n===============================================================");
        System.out.println("                   MENÚ COMPLETO - " + nombre);
        System.out.println("===============================================================");
        System.out.printf("%-5s | %-20s | %-12s | %-8s | %-10s\n",
                "ID", "PRODUCTO", "CATEGORÍA", "PRECIO", "STOCK/TIPO");
        System.out.println("---------------------------------------------------------------");

        NodoDE<Producto> aux = menu.getCabeza();
        if (aux == null) {
            System.out.println("                  (El menú está vacío)                         ");
        } else {
            while (aux != null) {
                Producto p = aux.getDato();

                // Verificamos si usa inventario o stock directo
                String infoStock = p.tieneReceta() ? "Con Receta" : String.format("%.2f und", p.getStock());

                System.out.printf("%-5d | %-20.20s | %-12.12s | Bs.%-5.2f | %-10s\n",
                        p.getId(), p.getNombre(), p.getCategoria(), p.getPrecio(), infoStock);
                aux = aux.getSiguiente();
            }
        }
        System.out.println("===============================================================");
    }
    public int generarNumeroFactura(){
        LocalDate hoy = LocalDate.now();
        if(!hoy.isEqual(fechaFacturacion)){
            fechaFacturacion = hoy;
            contadorFacturasDiarias = 0;
        }
        return ++ contadorFacturasDiarias ;
    }
    public void registrarVentaFinalizada(Factura f){
        historialVentas.insertarAlFinal(f);
        NodoDE<DetalleFactura> ac = f.getDetalles().getCabeza();
        while(ac != null){
            DetalleFactura df = ac.getDato();
            Producto p = df.getProducto();
            int cantVendida = df.getCantidad();
            if(p.tieneReceta()){
                for(Map.Entry<String, Double> entry : p.getReceta().getIngredientes().entrySet()){
                    String codigoInsumo = entry.getKey();
                    double cantidadNecesaria = entry.getValue() * cantVendida;
                    boolean exito = inventario.consumirInsumoFEFO(codigoInsumo, cantidadNecesaria);
                    if(!exito){
                        System.out.println("⚠️ ALERTA CRÍTICA: Faltó stock en almacén para el insumo " + codigoInsumo);
                    }
                }
            }else{
                p.reducirStock(cantVendida);
            }
            ac = ac.getSiguiente();
        }
    }
    public boolean anularFacturaFinanciera(int numeroFactura){
        NodoDE<Factura> actual = historialVentas.getCabeza();
        while(actual != null) {
            if (actual.getDato().getNumeroFactura() == numeroFactura) {
                historialVentas.eliminarPorValor(actual.getDato());
                return true;
            }
            actual = actual.getSiguiente();
        }
        return false;
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
    public ListaDE<Factura> getHistorialVentas() {
        return historialVentas;
    }
    public ArrayList<Empleado> getEmpleados() {
        return empleados;
    }

    public Empleado buscarEmpleado(String id){
        for(Empleado emp : empleados){
            if(emp.getId().equalsIgnoreCase(id)){
                return emp;
            }
        }
        return null;
    }
    public boolean eliminarEmpleado(String id) {
        Iterator<Empleado> iterador = empleados.iterator();
        while (iterador.hasNext()) {
            Empleado emp = iterador.next();
            if (emp.getId().equalsIgnoreCase(id)) {
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
    public void registerExpense(Egreso egreso){
        expenseHistory.insertarAlFinal(egreso);
    }
    public ListaDE<Egreso> getExpenseHistory() {
        return expenseHistory;
    }
}
