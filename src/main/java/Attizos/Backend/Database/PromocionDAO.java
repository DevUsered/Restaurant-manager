package Attizos.Backend.Database;

import Attizos.Backend.Attizos.DetalleCombo;
import Attizos.Backend.Attizos.Producto;
import Attizos.Backend.Attizos.Promocion;
import Attizos.Backend.Listas.ListaDE;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;

public class PromocionDAO {
    public static ListaDE<Promocion> cargarPromocionesActivas(ListaDE<Producto> menu){
        ListaDE<Promocion> listaPromo = new ListaDE<>();
        String sqlPromo = "SELECT * FROM productos WHERE categoria = 'Promocion' AND estado = 'Activo'";
        String sqlDetalle = "SELECT id_producto, cantidad FROM detalle_combo WHERE id_promocion = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement psPromo = conn.prepareStatement(sqlPromo);
             ResultSet rsPromo = psPromo.executeQuery()) {

            while (rsPromo.next()) {
                int id = rsPromo.getInt("id_producto");
                String nombre = rsPromo.getString("nombre");
                double precio = rsPromo.getDouble("precio");
                String img = rsPromo.getString("imagen_base64");

                String fInicioStr = rsPromo.getString("fecha_inicio");
                String fFinStr = rsPromo.getString("fecha_fin");

                LocalDate fInicio = (fInicioStr != null) ? LocalDate.parse(fInicioStr) : null;
                LocalDate fFin = (fFinStr != null) ? LocalDate.parse(fFinStr) : null;

                Promocion promo = new Promocion(id, nombre, precio, img, fInicio, fFin);

                // Recuperar los productos que van dentro del combo
                try (PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle)) {
                    psDetalle.setInt(1, id);
                    try (ResultSet rsDetalle = psDetalle.executeQuery()) {
                        while (rsDetalle.next()) {
                            int idProductoReal = rsDetalle.getInt("id_producto");
                            int cantidad = rsDetalle.getInt("cantidad");

                            Producto productoFisico = buscarProductoEnRAM(menu, idProductoReal);
                            if (productoFisico != null) {
                                promo.agregarProducto(productoFisico, cantidad);
                            }
                        }
                    }
                }
                listaPromo.insertarAlFinal(promo);
            }
        } catch (Exception e) {
            System.err.println("Error al cargar promociones: " + e.getMessage());
        }
        return listaPromo;
    }
    public static boolean guardarNuevaPromocion(Promocion promo) {
        String sqlProducto = "INSERT INTO productos (nombre, precio, categoria, stock_directo, imagen_base64, estado, fecha_inicio, fecha_fin) VALUES (?, ?, 'Promocion', 0, ?, 'Activo', ?, ?)";
        String sqlDetalle = "INSERT INTO detalle_combo (id_promocion, id_producto, cantidad) VALUES (?, ?, ?)";

        try (Connection conn = ConexionBD.getConexion()) {
            conn.setAutoCommit(false); // Transacción segura

            // Insertamos la promo en la tabla productos
            int idPromocionGenerado = -1;
            try (PreparedStatement ps = conn.prepareStatement(sqlProducto, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, promo.getNombre());
                ps.setDouble(2, promo.getPrecio());
                ps.setString(3, promo.getImagenURL());
                if (promo.getFechaInicio() != null) {
                    ps.setDate(4, java.sql.Date.valueOf(promo.getFechaInicio()));
                } else {
                    ps.setNull(4, java.sql.Types.DATE);
                }

                if (promo.getFechaFin() != null) {
                    ps.setDate(5, java.sql.Date.valueOf(promo.getFechaFin()));
                } else {
                    ps.setNull(5, java.sql.Types.DATE);
                }

                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    idPromocionGenerado = rs.getInt(1);
                    promo.setId(idPromocionGenerado);
                }
            }

            if (idPromocionGenerado != -1) {
                try (PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle)) {
                    for (DetalleCombo detalle : promo.getProductosCombo()) {
                        psDetalle.setInt(1, idPromocionGenerado);
                        psDetalle.setInt(2, detalle.getProducto().getId());
                        psDetalle.setInt(3, detalle.getCantidad());
                        psDetalle.addBatch();
                    }
                    psDetalle.executeBatch();
                }
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            System.err.println("Error guardando promoción: " + e.getMessage());
            return false;
        }
    }
    private static Producto buscarProductoEnRAM(ListaDE<Producto> menuRAM, int id) {
        Attizos.Backend.Listas.NodoDE<Producto> actual = menuRAM.getCabeza();
        while (actual != null) {
            if (actual.getDato().getId() == id) {
                return actual.getDato();
            }
            actual = actual.getSiguiente();
        }
        return null;
    }
    public static void verificarYDesactivarPromociones() {
        String sqlSelect = "SELECT id_producto, fecha_fin FROM productos WHERE categoria = 'Promocion' AND estado = 'Activo' AND fecha_fin IS NOT NULL";
        String sqlUpdate = "UPDATE productos SET estado = 'Inactivo' WHERE id_producto = ?";

        try (Connection conn = ConexionBD.getConexion();
             PreparedStatement psSelect = conn.prepareStatement(sqlSelect);
             ResultSet rs = psSelect.executeQuery()) {

            LocalDate hoy = LocalDate.now();

            while (rs.next()) {
                int id = rs.getInt("id_producto");
                String fechaFinStr = rs.getString("fecha_fin");

                if (fechaFinStr != null && !fechaFinStr.isEmpty()) {
                    LocalDate fin = LocalDate.parse(fechaFinStr);
                    if (fin.isBefore(hoy)) {
                        try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
                            psUpdate.setInt(1, id);
                            psUpdate.executeUpdate();
                            System.out.println("🚨 Promoción " + id + " caducada y desactivada automáticamente.");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error al verificar caducidad: " + e.getMessage());
        }
    }
}
