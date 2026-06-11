package Attizos.Backend.Database;

import Attizos.Backend.Attizos.App;
import Attizos.Backend.Attizos.Producto;
import Attizos.Backend.Attizos.Receta;
import Attizos.Backend.Listas.NodoDE;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RecetaDAO {
    public static void cargarRecetas(){
        String sql = "SELECT id_producto, codigo_insumo, cantidad_necesaria FROM recetas_detalle";

        try(Connection con = ConexionBD.getConexion();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()){

            while(rs.next()){
                int idProducto = rs.getInt("id_producto");
                String codInsumo = rs.getString("codigo_insumo");
                double cantidad = rs.getDouble("cantidad_necesaria");

                Producto p = buscarProductoActivo(idProducto);
                if(p != null){
                    if(p.getReceta() == null){
                        p.setReceta(new Receta());
                    }
                    p.getReceta().agregarIngrediente(codInsumo, cantidad);
                }
            }
            System.out.println("Recetas vinculadas correctamente a los productos del menú.");
        }catch (SQLException e){
            System.err.println("❌ Error al cargar recetas desde BD: " + e.getMessage());
        }
    }
    public static boolean guardarReceta(int idProducto, Receta receta){
        String sqlDelete = "DELETE FROM recetas_detalle WHERE id_producto = ?";
        String sqlInsert = "INSERT INTO recetas_detalle (id_producto, codigo_insumo, cantidad_necesaria) VALUES (?, ?, ?)";

        Connection con = null;
        try{
            con = ConexionBD.getConexion();
            con.setAutoCommit(false);

            try(PreparedStatement psDelete = con.prepareStatement(sqlDelete)){
                psDelete.setInt(1, idProducto);
                psDelete.executeUpdate();
            }
            if(receta != null && !receta.getIngredientes().isEmpty()){
                try(PreparedStatement psInsert = con.prepareStatement(sqlInsert)){
                    for(String codInsumo : receta.getIngredientes().keySet()){
                        psInsert.setInt(1, idProducto);
                        psInsert.setString(2, codInsumo);
                        psInsert.setDouble(3, receta.getIngredientes().get(codInsumo));
                        psInsert.addBatch(); // Empaquetamos para mayor velocidad
                    }
                    psInsert.executeBatch();
                }
            }
            con.commit();
            return true;
        }catch(SQLException e){
            System.err.println("❌ Error al guardar/actualizar receta: " + e.getMessage());
            if(con != null){
                try{ 
                    con.rollback(); 
                } catch(SQLException ex){ 
                    ex.printStackTrace(); 
                }
            }
            return false;
        } finally {
            if(con != null){
                try{
                    con.setAutoCommit(true);
                    con.close();
                }catch(SQLException ex){
                    ex.printStackTrace();
                }
            }
        }
    }
    private static Producto buscarProductoActivo(int idProducto){
        if(App.attizos == null || App.attizos.getMenu() == null) return null;

        for(Producto p : App.attizos.getMenu()){
            if(p.getId() == idProducto) return p;
        }
        return null;
    }

    // ==================================================================================
    public static Receta obtenerRecetaPorProducto(int idProducto) {
        Receta receta = new Receta();
        String sql = "SELECT codigo_insumo, cantidad_necesaria FROM recetas_detalle WHERE id_producto = ?";

        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idProducto);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String codInsumo = rs.getString("codigo_insumo");
                    double cantidad = rs.getDouble("cantidad_necesaria");

                    receta.agregarIngrediente(codInsumo, cantidad);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al obtener receta del producto " + idProducto + ": " + e.getMessage());
        }
        return receta;
    }
}
