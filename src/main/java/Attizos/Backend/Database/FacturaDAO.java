package Attizos.Backend.Database;

import Attizos.Backend.Attizos.Producto;
import Attizos.Frontend.AlertaPersonalizada;
import javafx.scene.control.Alert;

import java.sql.*;
import java.time.LocalDate;
import java.util.Map;

public class FacturaDAO {
    public static int registrarVenta(String nombreCliente, double total, Map<Producto, Integer> carrito ){
        String sqlFactura = "INSERT INTO facturas (nombre_cliente, total, estado, numero_ticket) VALUES (?, ?, 'Completada', ?)";
        String sqlDetalle = "INSERT INTO facturas_detalle (numero_factura, id_producto, cantidad, subtotal) VALUES (?, ?, ?, ?)";
        String sqlRestarVitrina = "UPDATE productos SET stock_directo = stock_directo - ? WHERE id_producto = ?";

        String sqlSelectLotes = "SELECT id_lote, stock_actual FROM insumos_lotes " +
                "WHERE codigo_insumo = ? AND estado = 'Activo' AND stock_actual > 0 " +
                "ORDER BY fecha_vencimiento ASC, id_lote ASC FOR UPDATE";

        String sqlUpdateLote = "UPDATE insumos_lotes SET stock_actual = ?, estado = ?  WHERE id_lote = ?";
        String sqlHistorialLote = "INSERT INTO lotes_consumidos_venta (numero_factura, id_lote, cantidad_descontada) VALUES (? ,?, ?)";
        String sqlCocina = "INSERT INTO cola_cocina (id_pedido, numero_ticket) VALUES (?, ?)";

        Connection con = null;
        try{
            con = ConexionBD.getConexion();
            con.setAutoCommit(false);

            int numeroFactura = -1;
            int numeroTicketDiario = 1;
            
            String sqlDiario = "SELECT COALESCE(MAX(numero_ticket), 0) + 1 FROM facturas WHERE CAST(fecha_hora AS DATE) = ?";
            try (PreparedStatement psDiario = con.prepareStatement(sqlDiario)){
                psDiario.setDate(1, Date.valueOf(LocalDate.now()));
                 try(ResultSet rsDiario = psDiario.executeQuery()) {
                     if (rsDiario.next()) {
                         numeroTicketDiario = rsDiario.getInt(1);
                     }
                 }
            }

            try(PreparedStatement psFac = con.prepareStatement(sqlFactura, Statement.RETURN_GENERATED_KEYS)){
                psFac.setString(1, nombreCliente);
                psFac.setDouble(2, total);
                psFac.setInt(3, numeroTicketDiario);
                psFac.executeUpdate();

                try(ResultSet rs = psFac.getGeneratedKeys()){
                    if(rs.next()){
                        numeroFactura = rs.getInt(1);
                    }else{
                        throw new SQLException("No se pudo obtener el número de factura generado.");
                    }
                }
            }
            boolean requiereCocina = false;

            for(Map.Entry<Producto, Integer> item : carrito.entrySet()){
                Producto producto = item.getKey();
                int cantidadComprada = item.getValue();
                double subtotal = producto.getPrecio() * cantidadComprada;

                try(PreparedStatement psDet = con.prepareStatement(sqlDetalle)){
                    psDet.setInt(1, numeroFactura);
                    psDet.setInt(2, producto.getId());
                    psDet.setInt(3, cantidadComprada);
                    psDet.setDouble(4, subtotal);
                    psDet.executeUpdate();
                }

                if(!producto.tieneReceta()){
                    try(PreparedStatement psVitrina = con.prepareStatement(sqlRestarVitrina)){
                        psVitrina.setInt(1, cantidadComprada);
                        psVitrina.setInt(2, producto.getId());
                        psVitrina.executeUpdate();
                    }
                }else{
                    requiereCocina = true;

                    if(producto.getReceta() != null){
                        for(Map.Entry<String, Double> ingrediente : producto.getReceta().getIngredientes().entrySet()){
                            String codInsumo = ingrediente.getKey();
                            double totalNecesario = ingrediente.getValue() * cantidadComprada;

                            double cantidadFaltante = totalNecesario;

                            try(PreparedStatement psSelLote = con.prepareStatement(sqlSelectLotes)){
                                psSelLote.setString(1, codInsumo);
                                try(ResultSet rsLotes = psSelLote.executeQuery()){
                                    while(rsLotes.next() && cantidadFaltante > 0){
                                        int idLote = rsLotes.getInt("id_lote");
                                        double stockLote = rsLotes.getDouble("stock_actual");

                                        double descuentoDeEsteLote;
                                        double nuevoStockLote;
                                        String nuevoEstado = "Activo";

                                        if(stockLote <= cantidadFaltante){
                                            descuentoDeEsteLote = stockLote;
                                            cantidadFaltante -= stockLote;
                                            nuevoStockLote = 0.0;
                                            nuevoEstado = "Inactivo";
                                        }else{
                                            descuentoDeEsteLote = cantidadFaltante;
                                            nuevoStockLote = stockLote - cantidadFaltante;
                                            cantidadFaltante = 0.0;
                                        }

                                        try(PreparedStatement psUpdLote = con.prepareStatement(sqlUpdateLote)){
                                            psUpdLote.setDouble(1, nuevoStockLote);
                                            psUpdLote.setString(2, nuevoEstado);
                                            psUpdLote.setInt(3, idLote);
                                            psUpdLote.executeUpdate();
                                        }

                                        try(PreparedStatement psHistorial = con.prepareStatement(sqlHistorialLote)){
                                            psHistorial.setInt(1, numeroFactura);
                                            psHistorial.setInt(2, idLote);
                                            psHistorial.setDouble(3, descuentoDeEsteLote);
                                            psHistorial.executeUpdate();
                                        }
                                    }
                                }
                            }
                            if(cantidadFaltante > 0){
                                throw new SQLException("Stock insuficiente para el insumo: " + codInsumo);
                            }
                        }
                    }
                }
            }
            if(requiereCocina){
                try(PreparedStatement psCocina = con.prepareStatement(sqlCocina)){
                    psCocina.setInt(1, numeroFactura);
                    psCocina.setInt(2, numeroTicketDiario);
                    psCocina.executeUpdate();
                }
            }
            con.commit();
            return numeroTicketDiario;
        }catch (SQLException e){
            System.out.println("❌ Error al registrar la venta: " + e.getMessage());
            if(con != null){
                try{
                    con.rollback();
                }catch (SQLException ex){
                    ex.printStackTrace();
                }
            }
            javafx.application.Platform.runLater(() -> {
                AlertaPersonalizada.mostrarAlerta("Error", "No se pudo realizar la venta. Verifica el stock.", Alert.AlertType.WARNING);
            });
            return -1;
        } finally {
            if(con != null){
                try{
                    con.setAutoCommit(true);
                    con.close();
                }catch (SQLException ex){
                    ex.printStackTrace();
                }
            }
        }

    }
    public static boolean anularVenta(int numeroFactura){
        String sqlEstado = "UPDATE facturas SET estado = 'Anulada' WHERE numero_factura = ?";
        String sqlBorrarCocina = "DELETE FROM cola_cocina WHERE id_pedido = ?";

        String sqlSelectVitrina = "SELECT fd.id_producto, fd.cantidad FROM facturas_detalle fd " +
                "INNER JOIN productos p ON fd.id_producto = p.id_producto " +
                "WHERE fd.numero_factura = ? AND p.tiene_receta = false";
        String sqlSumarVitrina = "UPDATE productos SET stock_directo = stock_directo + ? WHERE id_producto = ?";

        String sqlSelectLotesUsados = "SELECT id_lote, cantidad_descontada FROM lotes_consumidos_venta WHERE numero_factura = ?";
        String sqlSumarLote = "UPDATE insumos_lotes SET stock_actual = stock_actual + ?, estado = 'Activo' WHERE id_lote = ?";

        Connection con = null;
        try{
            con = ConexionBD.getConexion();
            con.setAutoCommit(false);

            try(PreparedStatement psEst = con.prepareStatement(sqlEstado)){
                psEst.setInt(1, numeroFactura);
                psEst.executeUpdate();
            }
            try (PreparedStatement psCocina = con.prepareStatement(sqlBorrarCocina)) {
                psCocina.setInt(1, numeroFactura);
                psCocina.executeUpdate();
            }
            try (PreparedStatement psSelV = con.prepareStatement(sqlSelectVitrina)) {
                psSelV.setInt(1, numeroFactura);
                try (ResultSet rsV = psSelV.executeQuery()) {
                    while (rsV.next()) {
                        int idProducto = rsV.getInt("id_producto");
                        int cantidadRestablecer = rsV.getInt("cantidad");

                        try (PreparedStatement psSumV = con.prepareStatement(sqlSumarVitrina)) {
                            psSumV.setInt(1, cantidadRestablecer);
                            psSumV.setInt(2, idProducto);
                            psSumV.executeUpdate();
                        }
                    }
                }
            }
            try (PreparedStatement psSelLotes = con.prepareStatement(sqlSelectLotesUsados)) {
                psSelLotes.setInt(1, numeroFactura);
                try (ResultSet rsLotes = psSelLotes.executeQuery()) {
                    while (rsLotes.next()) {
                        int idLote = rsLotes.getInt("id_lote");
                        double cantidadRestablecer = rsLotes.getDouble("cantidad_descontada");

                        try (PreparedStatement psSumLote = con.prepareStatement(sqlSumarLote)) {
                            psSumLote.setDouble(1, cantidadRestablecer);
                            psSumLote.setInt(2, idLote);
                            psSumLote.executeUpdate();
                        }
                    }
                }
            }
            con.commit();
            return true;
        }catch(SQLException e){
            System.err.println("❌ Error al anular la venta: " + e.getMessage());
            if (con != null) try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            if (con != null) try { con.setAutoCommit(true); con.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }
}
