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
    private String modalidadActual;

    public Restaurante(String nombre, String modalidadActual){
        this.modalidadActual = modalidadActual;
        this.nombre = nombre;
        this.inventario = new Inventario(nombre);
        this.menu = new ArrayList<>();
        this.empleados = new ArrayList<Empleado>();
        this.reservas = new ArrayList<>();
        this.colaPedidos = new LinkedList<>();
        this.lotesConsumidosPorPedido = new HashMap<>();
        this.promocionesActivas = new ArrayList<>();
    }
    public void registrarVentaFinalizada(Factura f) {
        HashMap<String, Double> consumoExactoDeEstaFactura = new HashMap<>();

        for (DetalleFactura df : f.getDetalles()) {
            Producto p = df.getProducto();
            int cantVendida = df.getCantidad();

            if (p.tieneReceta() && p.getReceta() != null) {
                for (Map.Entry<String, Double> entry : p.getReceta().getIngredientes().entrySet()) {
                    String codigoInsumo = entry.getKey();
                    double cantidadNecesaria = entry.getValue() * cantVendida;

                    HashMap<String, Double> stockAntes = new HashMap<>();
                    for (Insumo i : inventario.getInventarioInsumos().values()) {
                        if (i.getCodigo().equals(codigoInsumo) || i.getCodigo().startsWith(codigoInsumo + "-L")) {
                            stockAntes.put(i.getCodigo(), i.getStockActual());
                        }
                    }

                    inventario.consumirInsumoFEFO(codigoInsumo, cantidadNecesaria);

                    for (String codLote : stockAntes.keySet()) {
                        Insumo i = inventario.buscarInsumo(codLote);
                        double stockDespues = (i != null) ? i.getStockActual() : 0.0;
                        double diferencia = stockAntes.get(codLote) - stockDespues;

                        if (diferencia > 0) {
                            consumoExactoDeEstaFactura.put(codLote, consumoExactoDeEstaFactura.getOrDefault(codLote, 0.0) + diferencia);
                        }
                    }
                }
            } else {
                p.reducirStock(cantVendida);
            }
        }
        lotesConsumidosPorPedido.put(f.getNumeroFactura(), consumoExactoDeEstaFactura);
    }
    public boolean cancelarPedido(int idPedido) {
        Iterator<Pedido> iterador = colaPedidos.iterator();
        while (iterador.hasNext()) {
            Pedido pedidoCancelado = iterador.next();
            if (pedidoCancelado.getIdPedido() == idPedido) {
                HashMap<String, Double> lotesUsados = lotesConsumidosPorPedido.get(idPedido);
                if (lotesUsados != null) {
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
                for (DetalleFactura df : pedidoCancelado.getProductos()) {
                    Producto p = df.getProducto();
                    if (!p.tieneReceta()) {
                        p.aumentarStock(df.getCantidad());
                    }
                }
                iterador.remove();
                return true;
            }
        }
        return false;
    }
    public void agregarEmpleado(Empleado e) { empleados.add(e); }
    public void agregarPedido(Pedido p) { colaPedidos.addLast(p); }
    public Pedido atenderSiguientePedido() { return colaPedidos.pollFirst(); }
    public String getNombre() { return nombre; }
    public String getModalidadActual() { return modalidadActual; }
    public void setModalidadActual(String modalidad) { this.modalidadActual = modalidad; }
    public Inventario getInventario() { return inventario; }

    public ArrayList<Producto> getMenu() { return menu; }
    public void setMenu(ArrayList<Producto> menuActualizado) { this.menu = menuActualizado; }

    public ArrayList<Empleado> getEmpleados() { return empleados; }
    public ArrayList<Reserva> getReservas() { return reservas; }
    public LinkedList<Pedido> getPedidos() { return colaPedidos; }

    public ArrayList<Promocion> getPromocionesActivas() { return promocionesActivas; }
    public void setPromocionesActivas(ArrayList<Promocion> promos) { this.promocionesActivas = promos; }

    public String generarIdReserva(LocalDateTime fechaReserva) {
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
}
