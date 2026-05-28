package Attizos.Backend.Database;

import Attizos.Backend.Attizos.*;
import Attizos.Backend.Listas.*;

import java.sql.*;

public class ProductoDAO {
    public static boolean insertarProducto (Producto p){
        String sql = "INSERT INTO productos (nombre, precio, categoria, tipo_clase, stock_directo, tiene_receta, imagen_base64, atributos_extra, estado) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, 'Activo')";
        try(Connection con = ConexionBD.getConexion()){
            PreparedStatement ps = con.prepareStatement(sql,  Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, p.getNombre());
            ps.setDouble(2, p.getPrecio());
            ps.setString(3, p.getCategoria());
            ps.setString(4, p.getClass().getSimpleName()); // "Pizza", "Bebida", etc.
            ps.setInt(5, (int) p.getStock());
            ps.setBoolean(6, p.tieneReceta());
            ps.setString(7, p.getImagenURL());

            String atributosJson = "{}";
            if (p instanceof Pizza) {
                Pizza pizza = (Pizza) p;
                atributosJson = String.format("{\"tamano\": \"%s\", \"extra_queso\": %b}", pizza.getTamano(), pizza.isExtraQueso());
            } else if (p instanceof Bebida) {
                Bebida bebida = (Bebida) p;
                atributosJson = String.format("{\"tamano\": \"%s\", \"tipo\": \"%s\"}", bebida.getTamano().name(), bebida.getTipoBebida());
            } else if (p instanceof Pasta) {
                Pasta pasta = (Pasta) p;
                atributosJson = String.format("{\"salsa\": \"%s\"}", pasta.getTipoSalsa());
            }
            ps.setString(8, atributosJson);

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
    public static ListaDE<Producto> obtenerMenuCompleto(){
        ListaDE<Producto> menu = new ListaDE<>();
        String sql = "SELECT * FROM productos WHERE estado = 'Activo' ORDER BY id_producto";

        try(Connection con = ConexionBD.getConexion()){
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while(rs.next()){
                int id = rs.getInt("id_producto");
                String nombre = rs.getString("nombre");
                double precio = rs.getDouble("precio");
                String categoria = rs.getString("categoria");
                String tipoClase = rs.getString("tipo_clase");
                int stockDirecto = rs.getInt("stock_directo");
                boolean tieneReceta = rs.getBoolean("tiene_receta");
                String imagenBase64 = rs.getString("imagen_base64");
                String jsonStr = rs.getString("atributos_extra");

                if (imagenBase64 == null || imagenBase64.trim().isEmpty()) {
                    imagenBase64 = "\\src\\main\\resources\\images\\default.png"; // URL de imagen genérica
                }

                Producto nuevoProducto = null;

                switch (tipoClase) {
                    case "Pizza":
                        String tamanoPizza = extraerStringJson(jsonStr, "tamano", "Mediana");
                        boolean extraQueso = extraerBooleanoJson(jsonStr, "extra_queso");
                        nuevoProducto = new Pizza(id, nombre, precio, categoria, imagenBase64, tamanoPizza, "", extraQueso);
                        if (tieneReceta) nuevoProducto.setReceta(new Receta()); // Se llenará después con RecetaDAO
                        break;
                    case "Bebida":
                        String tamanoBebidaStr = extraerStringJson(jsonStr, "tamano", "PERSONAL");
                        String tipoBebida = extraerStringJson(jsonStr, "tipo", "Gaseosa");
                        TamanoBebida enumTamano = TamanoBebida.PERSONAL;
                        try {
                            enumTamano = TamanoBebida.valueOf(tamanoBebidaStr);
                        } catch (Exception ignored) {
                        }

                        nuevoProducto = new Bebida(id, nombre, precio, categoria, stockDirecto, imagenBase64, enumTamano, tipoBebida);
                        break;
                    case "Pasta":
                        String salsa = extraerStringJson(jsonStr, "salsa", "Tradicional");
                        nuevoProducto = new Pasta(id, nombre, precio, categoria, imagenBase64, "", salsa);
                        break;
                    case "Calzone":
                        nuevoProducto = new Calzone(id, nombre, precio, categoria, imagenBase64, "");
                        break;
                    case "Postre":
                        nuevoProducto = new Postre(id, nombre, precio, categoria, imagenBase64, "", "", "");
                        break;
                    default:
                        nuevoProducto = new Producto(id, nombre, precio, categoria, stockDirecto, imagenBase64);
                        break;
                }
                if (nuevoProducto != null) {
                    if(tieneReceta){
                        nuevoProducto.setReceta(RecetaDAO.obtenerRecetaPorProducto(id)); // Se llenará después con RecetaDAO
                    }
                    menu.insertarAlFinal(nuevoProducto);
                }
            }
        }catch (SQLException e) {
            System.err.println("❌ Error al cargar el menú desde BD: " + e.getMessage());
        }
        return menu;
    }
    public static boolean actualizarProducto(Producto p){
        String sql = "UPDATE productos SET nombre = ?, precio = ?, categoria = ?, stock_directo = ?, imagen_base64 = ? WHERE id_producto = ?";
        try(Connection con = ConexionBD.getConexion()){
        PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, p.getNombre());
            ps.setDouble(2, p.getPrecio());
            ps.setString(3, p.getCategoria());
            ps.setInt(4, (int) p.getStock());
            ps.setString(5, p.getImagenURL());
            ps.setInt(6, p.getId());

            return ps.executeUpdate() > 0;
        }catch (SQLException e){
            System.err.println("Error al actualizar producto: " + e.getMessage());
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

    private static boolean extraerBooleanoJson(String json, String clave) {
        if (json == null || json.isEmpty()) return false;
        String patron = "\"" + clave + "\": ";
        int inicio = json.indexOf(patron);
        if (inicio != -1) {
            inicio += patron.length();
            return json.substring(inicio).trim().startsWith("true");
        }
        return false;
    }
}
