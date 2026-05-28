package Attizos.Backend.Database;

import Attizos.Backend.Attizos.Pedido;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PedidoDAO {

    
    public static ObservableList<Pedido> obtenerPedidosPendientes() {
        ObservableList<Pedido> lista = FXCollections.observableArrayList();
    
        String sql = "SELECT id_pedido, numero_ticket, estado FROM cola_cocina " +
                     "WHERE estado = 'Pendiente' ORDER BY id_pedido ASC";

        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Pedido p = new Pedido();
                p.setIdPedido(rs.getInt("id_pedido"));
                p.setNumeroTicket(rs.getInt("numero_ticket"));
                p.setEstado(rs.getString("estado"));
                
                p.setCliente(""); 

                lista.add(p);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error al obtener pedidos de cocina: " + e.getMessage());
        }
        return lista;
    }
    public static List<String> obtenerDetallesParaCocina(int idPedido) {
        List<String> detalles = new ArrayList<>();
        
        String sql = "SELECT fd.cantidad, p.nombre FROM facturas_detalle fd " +
                     "INNER JOIN productos p ON fd.id_producto = p.id_producto " +
                     "WHERE fd.numero_factura = ? AND p.tiene_receta = true";

        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int cantidad = rs.getInt("cantidad");
                    String nombreProducto = rs.getString("nombre");
                    
                    detalles.add(cantidad + "x  " + nombreProducto);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Error al obtener detalles del pedido: " + e.getMessage());
        }
        return detalles;
    }


    public static boolean eliminarPedidoDespachado(int idPedido) {
        String sql = "DELETE FROM cola_cocina WHERE id_pedido = ?";

        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idPedido);
            int filasAfectadas = ps.executeUpdate();
            
            return filasAfectadas > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error al eliminar pedido despachado: " + e.getMessage());
            return false;
        }
    }
}