package Attizos.Backend.Database;

import Attizos.Backend.Attizos.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReportesDAO{
    public static List<Factura> obtenerFacturas(){
        List<Factura> lista = new ArrayList<>();

        String sql = "SELECT numero_factura, fecha_hora, nombre_cliente, total, estado " +
                    "FROM facturas WHERE estado = 'Completada' ORDER BY fecha_hora DESC";

        try(Connection con = ConexionBD.getConexion();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()){

        while(rs.next()){
            Factura f = new Factura(rs.getInt("numero_factura"), rs.getString("nombre_cliente"));
            f.setTotal(rs.getDouble("total"));
            f.setEstado(rs.getString("estado"));
                    
            Timestamp ts = rs.getTimestamp("fecha_hora");
            if (ts != null) {
                f.setFecha(ts.toLocalDateTime());
            }
            lista.add(f);
        }
        }catch (SQLException e) {
            System.err.println("❌ Error al obtener facturas: " + e.getMessage());
        }
        return lista;
    }

    public static List<Egreso> obtenerEgresos() {
        List<Egreso> lista = new ArrayList<>();
        String sql = "SELECT fecha, concepto, monto FROM egresos ORDER BY id_egreso DESC";
        
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
             
            while (rs.next()) {
                Egreso e = new Egreso(rs.getString("concepto"), rs.getDouble("monto"));
                Date sqlDate = rs.getDate("fecha");
                if (sqlDate != null) {
                    e.setDate(sqlDate.toLocalDate());
                }
                lista.add(e);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al obtener egresos: " + e.getMessage());
        }
        return lista;
    }
    public static List<Empleado> obtenerEmpleados() {
        List<Empleado> lista = new ArrayList<>();
        String sql = "SELECT id_empleado, nombre, cargo, sueldo FROM empleados";
        
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
             
            while (rs.next()) {
                Empleado emp = new Empleado();
                emp.setIdEmpleado(rs.getString("id_empleado"));
                emp.setNombre(rs.getString("nombre"));
                emp.setCargo(rs.getString("cargo"));
                emp.setSueldo(rs.getDouble("sueldo"));
                lista.add(emp);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al obtener empleados: " + e.getMessage());
        }
        return lista;
    }
    public static List<RegistroAuditoria> obtenerAuditoria() {
        List<RegistroAuditoria> lista = new ArrayList<>();
        String sql = "SELECT fecha_hora, operador, tipo_area, nombre_item, accion, cantidad, motivo " +
                     "FROM auditoria ORDER BY fecha_hora DESC";
                     
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
             
            while (rs.next()) {
                RegistroAuditoria log = new RegistroAuditoria(
                        rs.getString("operador"),
                        rs.getString("tipo_area"),
                        rs.getString("nombre_item"),
                        rs.getString("accion"),
                        rs.getDouble("cantidad"),
                        rs.getString("motivo")
                );
                
                Timestamp ts = rs.getTimestamp("fecha_hora");
                if (ts != null) {
                    // Formateamos la fecha directamente para la clase RegistroAuditoria
                    log.setDate(ts.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
                }
                lista.add(log);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al obtener auditoría: " + e.getMessage());
        }
        return lista;
    }
    public static boolean registrarEgreso(String concepto, double monto) {
        String sql = "INSERT INTO egresos (fecha ,concepto, monto) VALUES (?, ?, ?)";
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
             
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            ps.setString(2, concepto);
            ps.setDouble(3, monto);
            return ps.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Error al registrar egreso: " + e.getMessage());
            return false;
        }
    }
    public static boolean registrarAuditoria(String operador, String tipoArea, String nombreItem, String accion, double cantidad, String motivo) {
        String sql = "INSERT INTO auditoria (operador, tipo_area, nombre_item, accion, cantidad, motivo) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
             
            ps.setString(1, operador);
            ps.setString(2, tipoArea);
            ps.setString(3, nombreItem);
            ps.setString(4, accion);
            ps.setDouble(5, cantidad);
            ps.setString(6, motivo);
            return ps.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Error al registrar auditoría: " + e.getMessage());
            return false;
        }
    }
    public static Factura obtenerFacturaConDetalles(int numeroFactura) {
        Factura factura = null;
        String sqlFac = "SELECT nombre_cliente, total, fecha_hora, estado FROM facturas WHERE numero_factura = ?";
        // INNER JOIN para traer los nombres y precios desde la tabla de productos
        String sqlDet = "SELECT fd.cantidad, fd.subtotal, p.id_producto, p.nombre, p.precio " +
                        "FROM facturas_detalle fd " +
                        "INNER JOIN productos p ON fd.id_producto = p.id_producto " +
                        "WHERE fd.numero_factura = ?";

        try (Connection con = ConexionBD.getConexion()) {
            
            // A. Obtenemos la Cabecera de la Factura
            try (PreparedStatement psFac = con.prepareStatement(sqlFac)) {
                psFac.setInt(1, numeroFactura);
                try (ResultSet rsFac = psFac.executeQuery()) {
                    if (rsFac.next()) {
                        factura = new Factura(numeroFactura, rsFac.getString("nombre_cliente"));
                        factura.setTotal(rsFac.getDouble("total"));
                        factura.setEstado(rsFac.getString("estado"));
                        Timestamp ts = rsFac.getTimestamp("fecha_hora");
                        if (ts != null) {
                            factura.setFecha(ts.toLocalDateTime());
                        }
                    }
                }
            }
            if (factura != null) {
                try (PreparedStatement psDet = con.prepareStatement(sqlDet)) {
                    psDet.setInt(1, numeroFactura);
                    try (ResultSet rsDet = psDet.executeQuery()) {
                        while (rsDet.next()) {
                            Producto p = new Producto();
                            p.setId(rsDet.getInt("id_producto"));
                            p.setNombre(rsDet.getString("nombre"));
                            p.setPrecio(rsDet.getDouble("precio"));
                            factura.agregarProducto(p, rsDet.getInt("cantidad"));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al obtener detalles de la factura: " + e.getMessage());
        }
        return factura;
    }
    public static boolean existeEgresoPorConcepto(String concepto){
        String sql = "SELECT COUNT(*) FROM egresos WHERE concepto = ?";

        try(Connection con = ConexionBD.getConexion();
            PreparedStatement ps = con.prepareStatement(sql)){

            ps.setString(1, concepto);
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()){
                    return rs.getInt(1) > 0;
                }
            }
        }catch (SQLException e){
            System.err.println("❌ Error al verificar existencia de egreso: " + e.getMessage());
        }
        return false;
    }
}