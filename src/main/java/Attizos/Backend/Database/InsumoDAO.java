package Attizos.Backend.Database;

import Attizos.Backend.Attizos.Insumo;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.time.LocalDate;

public class InsumoDAO {
    public static String generarSiguienteCodigo(){
        String sql = "SELECT MAX(CAST(SUBSTRING(codigo, 5) AS INTEGER)) FROM insumos_catalogo WHERE codigo LIKE 'INS-%'";
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                int maxNumero = rs.getInt(1);
                return String.format("INS-%03d", maxNumero + 1);
            }
        } catch (SQLException e) {
            System.err.println("Error al generar código: " + e.getMessage());
        }
        return "INS-001";
    }
    public static boolean insertarInsumoNuevo(Insumo insumo, double costoInicial) {
        String sqlCatalogo = " INSERT INTO insumos_catalogo (codigo, nombre, categoria, unidad_medida, stock_minimo, stock_maximo, estado)" +
                "VALUES (?, ?, ?, ?, ?, ?, 'Activo')";
        String sqlLote = "INSERT INTO insumos_lotes (codigo_insumo, stock_actual, fecha_ingreso, fecha_vencimiento, costo_compra, estado)" +
                "VALUES (?, ?, CURRENT_DATE, ?, ?, 'Activo')";

        Connection con = null;
        try {
            con = ConexionBD.getConexion();
            con.setAutoCommit(false);
            try(PreparedStatement psCat = con.prepareStatement(sqlCatalogo)){
                psCat.setString(1, insumo.getCodigo());
                psCat.setString(2, insumo.getNombre());
                psCat.setString(3, insumo.getCategoria());
                psCat.setString(4, insumo.getUnidad());
                psCat.setDouble(5, insumo.getStockMinimo());
                psCat.setDouble(6, insumo.getStockMaximo());
                psCat.executeUpdate();
            }

            try (PreparedStatement psLote = con.prepareStatement(sqlLote)) {
                psLote.setString(1, insumo.getCodigo());
                psLote.setDouble(2, insumo.getStockActual());

                if (insumo.getFechaVencimiento() != null) {
                    psLote.setDate(3, Date.valueOf(insumo.getFechaVencimiento()));
                } else {
                    psLote.setDate(3, Date.valueOf(LocalDate.now().plusYears(10)));
                }
                psLote.setDouble(4, costoInicial);
                psLote.executeUpdate();
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Error en Transacción de Insumo. Haciendo Rollback... " + e.getMessage());
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public static HashMap<String, Insumo> obtenerInventarioActivo() {
        HashMap<String, Insumo> inventario = new HashMap<>();

        String sql = "SELECT c.codigo, c.nombre, c.categoria, c.unidad_medida, c.stock_minimo, c.stock_maximo, " +
                "COALESCE (SUM(l.stock_actual), 0) AS stock_total, " +
                "MIN(l.fecha_vencimiento) AS proximo_vencimiento " +
                "FROM insumos_catalogo c " +
                "LEFT JOIN insumos_lotes l ON c.codigo = l.codigo_insumo AND l.estado = 'Activo' " +
                "WHERE c.estado = 'Activo' " +
                "GROUP BY  c.codigo, c.nombre, c.categoria, c.unidad_medida, c.stock_minimo, c.stock_maximo";

        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String codigo = rs.getString("codigo");
                String nombre = rs.getString("nombre");
                String categoria = rs.getString("categoria");
                String unidad = rs.getString("unidad_medida");
                double stockMinimo = rs.getDouble("stock_minimo");
                double stockMaximo = rs.getDouble("stock_maximo");
                double stockTotal = rs.getDouble("stock_total");

                Date dbDate = rs.getDate("proximo_vencimiento");
                LocalDate proximoVencimiento = (dbDate != null) ? dbDate.toLocalDate() : LocalDate.now().plusYears(6);

                Insumo i = new Insumo(codigo, nombre, categoria, unidad, stockTotal, stockMinimo, stockMaximo, proximoVencimiento);
                inventario.put(codigo, i);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return inventario;
    }

    public static boolean registrarNuevaCompraLote(String codigoInsumo, double cantidadComprada, double costo, LocalDate vencimiento) {
        String sqlLote = "INSERT INTO insumos_lotes (codigo_insumo, stock_actual, fecha_ingreso, fecha_vencimiento, costo_compra, estado) " +
                "VALUES (?, ?, CURRENT_DATE, ?, ?, 'Activo')";

        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sqlLote)) {

            ps.setString(1, codigoInsumo);
            ps.setDouble(2, cantidadComprada);
            ps.setDate(3, Date.valueOf(vencimiento));
            ps.setDouble(4, costo);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error al registrar nuevo lote de compra: " + e.getMessage());
            return false;
        }
    }

    public static boolean darDeBajaInsumo(String codigo) {
        String sqlCatalogo = "UPDATE insumos_catalogo SET estado = 'Inactivo' WHERE codigo = ?";
        String sqlLotes = "UPDATE insumos_lotes SET estado = 'Inactivo' WHERE codigo_insumo = ?";

        Connection con = null;
        try {
            con = ConexionBD.getConexion();
            con.setAutoCommit(false);

            try (PreparedStatement psLotes = con.prepareStatement(sqlLotes)) {
                psLotes.setString(1, codigo);
                psLotes.executeUpdate();
            }
            try (PreparedStatement psCat = con.prepareStatement(sqlCatalogo)) {
                psCat.setString(1, codigo);
                psCat.executeUpdate();
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            System.out.println("Error al dar de baja al insumo: " + e.getMessage());
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public static boolean descontarStockFEFO(String codigoInsumo, double cantidadRequerida) {
        String sqlSelect = "SELECT id_lote, stock_actual FROM insumos_lotes " +
                "WHERE codigo_insumo = ? AND estado = 'Activo' AND stock_actual > 0 " +
                "AND fecha_vencimiento >= CURRENT_DATE " +
                "ORDER BY fecha_vencimiento ASC, id_lote ASC";

        String sqlUpdateLote = "UPDATE insumos_lotes SET stock_actual = ?, estado = ? WHERE id_lote = ?";

        Connection con = null;
        try {
            con = ConexionBD.getConexion();
            con.setAutoCommit(false);

            double cantidadFaltante = cantidadRequerida;
            try (PreparedStatement psSelect = con.prepareStatement(sqlSelect)) {
                psSelect.setString(1, codigoInsumo);
                try (ResultSet rs = psSelect.executeQuery()) {
                    while (rs.next() && cantidadFaltante > 0) {
                        int idLote = rs.getInt("id_lote");
                        double stockLote = rs.getDouble("stock_actual");

                        double nuevoStockLote;
                        String nuevoEstado = "Activo";

                        if (stockLote <= cantidadFaltante) {
                            cantidadFaltante -= stockLote;
                            nuevoStockLote = 0;
                            nuevoEstado = "Inactivo";
                        } else {
                            nuevoStockLote = stockLote - cantidadFaltante;
                            cantidadFaltante = 0.0;
                        }
                        try (PreparedStatement psUpdate = con.prepareStatement(sqlUpdateLote)) {
                            psUpdate.setDouble(1, nuevoStockLote);
                            psUpdate.setString(2, nuevoEstado);
                            psUpdate.setInt(3, idLote);
                            psUpdate.executeUpdate();
                        }
                    }
                }
            }
            if (cantidadFaltante > 0) {
                System.err.println("❌ Stock insuficiente en almacén para: " + codigoInsumo);
                con.rollback(); // Cancelamos los descuentos parciales
                return false;
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Error en el proceso FEFO: " + e.getMessage());
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
    public static double darDeBajaLotesVencidos(String codigoInsumo) {
        String sqlSelect = "SELECT id_lote, stock_actual FROM insumos_lotes " +
                            "WHERE codigo_insumo = ? AND fecha_vencimiento < CURRENT_DATE AND estado = 'Activo'";
        String sqlUpdateLote = "UPDATE insumos_lotes SET estado = 'Inactivo', stock_actual = 0 WHERE id_lote = ?";

        Connection con = null;
        double totalDescontado = 0;
        try {
            con = ConexionBD.getConexion();
            con.setAutoCommit(false);

            try (PreparedStatement psSelect = con.prepareStatement(sqlSelect)) {
                psSelect.setString(1, codigoInsumo);
                try (ResultSet rs = psSelect.executeQuery()) {
                    try (PreparedStatement psUpdate = con.prepareStatement(sqlUpdateLote)) {
                        while (rs.next()) {
                            int idLote = rs.getInt("id_lote");
                            double stock = rs.getDouble("stock_actual");
                            totalDescontado += stock;

                            psUpdate.setInt(1, idLote);
                            psUpdate.executeUpdate();
                        }
                    }
                }
            }
            con.commit();
            return totalDescontado;
        }catch (SQLException e){
            System.err.println("❌ Error retirando lotes vencidos: " + e.getMessage());
            if (con != null) {
                try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return -1;
        } finally {
            if (con != null) {
                try { con.setAutoCommit(true); con.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }

    }
}
