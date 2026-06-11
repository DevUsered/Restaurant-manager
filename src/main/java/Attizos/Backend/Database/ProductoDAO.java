package Attizos.Backend.Database;

import Attizos.Backend.Attizos.*;
import Attizos.Backend.Listas.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.Map;

public class ProductoDAO {
    public static boolean insertarProducto (Producto p){
        String sql = "INSERT INTO productos (nombre, precio, categoria, tipo_clase, stock_directo, tiene_receta, imagen_base64, atributos_extra, estado) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, 'Activo')";
        try(Connection con = ConexionBD.getConexion()){
            PreparedStatement ps = con.prepareStatement(sql,  Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, p.getNombre());
            ps.setDouble(2, p.getPrecio());
            ps.setString(3, p.getCategoria());
            ps.setString(4, "Producto"); // "Pizza", "Bebida", etc.
            ps.setInt(5, (int) p.getStock());
            ps.setBoolean(6, p.tieneReceta());
            ps.setString(7, p.getImagenURL());

            StringBuilder jsonBuilder = new StringBuilder("{");
            boolean primero = true;
            for (Map.Entry<String, String> entry : p.getAtributosDinamicos().entrySet()) {
                if (!primero) jsonBuilder.append(", ");
                jsonBuilder.append("\"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
                primero = false;
            }
            jsonBuilder.append("}");

            ps.setString(8, jsonBuilder.toString());

            int filasAfectadas = ps.executeUpdate();

            if (filasAfectadas > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        p.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        }catch (SQLException e){
            System.err.println("Error al insertar producto: " + e.getMessage());
        }
        return false;
    }
    public static ArrayList<Producto> obtenerMenuCompleto() {
        ArrayList<Producto> menu = new ArrayList<>();
        String sql = "SELECT * FROM productos WHERE estado = 'Activo' ORDER BY id_producto";

        try (Connection con = ConexionBD.getConexion()) {
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id_producto");
                String nombre = rs.getString("nombre");
                double precio = rs.getDouble("precio");
                String categoria = rs.getString("categoria");
                int stockDirecto = rs.getInt("stock_directo");
                boolean tieneReceta = rs.getBoolean("tiene_receta");
                String imagenBase64 = rs.getString("imagen_base64");
                String jsonStr = rs.getString("atributos_extra");

                if (imagenBase64 == null || imagenBase64.trim().isEmpty()) {
                    imagenBase64 = "\\src\\main\\resources\\images\\default.png"; // URL de imagen genérica
                }
                Producto nuevoProducto = new Producto(id, nombre, precio, categoria, stockDirecto, imagenBase64, "Activo");

                if (jsonStr != null && jsonStr.length() > 2) {
                    String contenido = jsonStr.substring(1, jsonStr.length() - 1); // Quitar llaves { }
                    String[] pares = contenido.split(",");

                    for (String par : pares) {
                        String[] claveValor = par.split(":");
                        if (claveValor.length == 2) {
                            String clave = claveValor[0].replace("\"", "").trim();
                            String valor = claveValor[1].replace("\"", "").trim();
                            nuevoProducto.agregarAtributo(clave, valor);
                        }
                    }
                }

                if (tieneReceta) {
                    nuevoProducto.setReceta(RecetaDAO.obtenerRecetaPorProducto(id));
                }

                menu.add(nuevoProducto);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al cargar el menú desde BD: " + e.getMessage());
        }
        return menu;
    }
    public static boolean actualizarProducto(Producto p) {
        String sql = "UPDATE productos SET nombre = ?, precio = ?, categoria = ?, stock_directo = ?, imagen_base64 = ?, atributos_extra = ?::jsonb WHERE id_producto = ?";

        try (Connection con = ConexionBD.getConexion()) {
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, p.getNombre());
            ps.setDouble(2, p.getPrecio());
            ps.setString(3, p.getCategoria());
            ps.setInt(4, (int) p.getStock());
            ps.setString(5, p.getImagenURL());

            // Reconstruimos el JSON para la actualización
            StringBuilder jsonBuilder = new StringBuilder("{");
            boolean primero = true;
            for (Map.Entry<String, String> entry : p.getAtributosDinamicos().entrySet()) {
                if (!primero) jsonBuilder.append(", ");
                jsonBuilder.append("\"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
                primero = false;
            }
            jsonBuilder.append("}");
            ps.setString(6, jsonBuilder.toString());

            ps.setInt(7, p.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Error al actualizar producto: " + e.getMessage());
        }
        return false;
    }

    public static boolean eliminarProducto(int idProducto) {
        String sql = "UPDATE productos SET estado = 'Inactivo' WHERE id_producto = ?";
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idProducto);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Error al dar de baja el producto: " + e.getMessage());
            return false;
        }
    }
    private static String extraerStringJson(String json, String clave, String valorPorDefecto) {
        if (json == null || json.isEmpty()) return valorPorDefecto;
        String patron = "\"" + clave + "\": \"";
        int inicio = json.indexOf(patron);
        if (inicio != -1) {
            inicio += patron.length();
            int fin = json.indexOf("\"", inicio);
            if (fin != -1) return json.substring(inicio, fin);
        }
        return valorPorDefecto;
    }
    public static boolean actualizarImagenProducto(int idProducto, String nuevaURL){
        String sql = "UPDATE productos SET imagen_base64 = ? WHERE id_producto = ?";
        try(Connection con = ConexionBD.getConexion();
            PreparedStatement ps = con.prepareStatement(sql)){
            ps.setString(1, nuevaURL);
            ps.setInt(2, idProducto);

            return ps.executeUpdate() > 0;
        }catch (SQLException e){
            System.out.println("Error al actualizar la imagen. "+ e.getMessage());
            return false;
        }
    }
}
