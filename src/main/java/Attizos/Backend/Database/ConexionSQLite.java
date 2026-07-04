package Attizos.Backend.Database;

import Attizos.Backend.Api.ApiClient;
import Attizos.Backend.Attizos.*;
import Attizos.Backend.Listas.*;
import Attizos.Frontend.UtilidadesImagen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ConexionSQLite {
    private static String urlSQLite;

    static {
        String rutaAppData = System.getenv("APPDATA");
        File carpetaAttizos = new File(rutaAppData, "Attizos");

        if (!carpetaAttizos.exists()) {
            carpetaAttizos.mkdirs();
        }
        File archivoDB = new File(carpetaAttizos, "Attizos.db");

        urlSQLite = "jdbc:sqlite:" + archivoDB.getAbsolutePath();
        System.out.println("Ruta de la base de datos SQLite: " + archivoDB.getAbsolutePath());
    }

    public static Connection getConexion() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Error al cargar el driver de SQLite: " + e.getMessage());
            throw new SQLException("Driver de SQLite no encontrado", e);
        }
        return DriverManager.getConnection(urlSQLite);
    }

    public static void inicializarTablasLocales() {
        String sqlEmpleados = "CREATE TABLE IF NOT EXISTS empleados ("
                + "id_empleado TEXT PRIMARY KEY, "
                + "nombre TEXT NOT NULL, "
                + "cargo TEXT NOT NULL, "
                + "sueldo REAL NOT NULL, "
                + "username TEXT, "
                + "password_hash TEXT, "
                + "estado TEXT DEFAULT 'Activo', "
                + "fecha_ultimo_pago TEXT, "
                + "fecha_contrato TEXT"
                + ");";
        String sqlInsumos = "CREATE TABLE IF NOT EXISTS insumos_catalogo ("
                + "codigo TEXT PRIMARY KEY, "
                + "nombre TEXT NOT NULL, "
                + "categoria TEXT, "
                + "unidad_medida TEXT, "
                + "stock_minimo REAL DEFAULT 0, "
                + "stock_maximo REAL DEFAULT 0, "
                + "stock_actual REAL DEFAULT 0,"
                + "estado TEXT DEFAULT 'Activo'"
                + ");";
        String sqlProductos = "CREATE TABLE IF NOT EXISTS productos ("
                + "id_producto INTEGER PRIMARY KEY, "
                + "nombre TEXT NOT NULL, "
                + "precio REAL NOT NULL, "
                + "categoria TEXT, "
                + "tipo_clase TEXT, "
                + "stock_directo INTEGER DEFAULT 0, "
                + "tiene_receta INTEGER DEFAULT 0, "
                + "imagen_base64 TEXT, "
                + "atributos_extra TEXT, "
                + "fecha_inicio TEXT, "
                + "fecha_fin TEXT, "
                + "estado TEXT DEFAULT 'Activo'"
                + ");";
        String sqlDetalleCombo = "CREATE TABLE IF NOT EXISTS detalle_combo ("
                + "id_promocion INTEGER NOT NULL, "
                + "id_producto INTEGER NOT NULL, "
                + "cantidad INTEGER NOT NULL"
                + ");";
        String sqlVentasPendientes = "CREATE TABLE IF NOT EXISTS ventas_pendientes ("
                + "id_local INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "json_venta TEXT NOT NULL, "
                + "fecha_hora TEXT NOT NULL, "
                + "estado TEXT DEFAULT 'pendiente'"
                + ");";
        String sqlAuditoria = "CREATE TABLE IF NOT EXISTS auditoria_pendiente ("
                + "id_local INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "operador TEXT, "
                + "tipo_area TEXT, "
                + "nombre_item TEXT, "
                + "accion TEXT, "
                + "cantidad REAL, "
                + "motivo TEXT, "
                + "estado TEXT DEFAULT 'pendiente'"
                + ");";
        String sqlSecuencia = "CREATE TABLE IF NOT EXISTS secuencia_tickets ("
                + "id INTEGER PRIMARY KEY CHECK (id = 1), "
                + "fecha TEXT, "
                + "ultimo_numero INTEGER"
                + ");";
        String sqlRecetasLocal = "CREATE TABLE IF NOT EXISTS recetas_local ("
                + "id_producto INTEGER NOT NULL, "
                + "codigo_insumo TEXT NOT NULL, "
                + "cantidad REAL NOT NULL"
                + ");";

        try (Connection conn = getConexion();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlEmpleados);
            stmt.execute(sqlProductos);
            stmt.execute(sqlDetalleCombo);
            stmt.execute(sqlInsumos);
            stmt.execute(sqlVentasPendientes);
            stmt.execute(sqlAuditoria);
            stmt.execute(sqlSecuencia);
            stmt.execute(sqlRecetasLocal);
            stmt.execute("INSERT OR IGNORE INTO secuencia_tickets (id, fecha, ultimo_numero) VALUES (1, '2000-01-01', 0);");

            System.out.println("Caché SQLite inicializada correctamente. ");
        } catch (SQLException e) {
            System.out.println("Error al inicializar tablas locales: " + e.getMessage());
        }
    }
    public static void actualizarCacheCompleta() {
        System.out.println("Actualizando caché local completa...");
        int promosCaducadas = ApiClient.verificarCaducidadPromociones();
        if (promosCaducadas > 0) {
            System.out.println("🚨 El servidor desactivó " + promosCaducadas + " promociones vencidas.");
        }
        sincronizarEmpleados();
        sincronizarInsumos();
        sincronizarProductos();
        sincronizarDetallesCombo();
    }

    public static void sincronizarEmpleados() {
        System.out.println("Iniciando sincronización de empleados via API REST.");
        String sqlLimpiarSQLite = "DELETE FROM empleados";
        String sqlInsertarSQLite = "INSERT INTO empleados (id_empleado, nombre, cargo, sueldo, username, password_hash, estado, fecha_ultimo_pago, fecha_contrato) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connSQL = getConexion();
             Statement stmtLimpiar = connSQL.createStatement();
             PreparedStatement stmtInsertar = connSQL.prepareStatement(sqlInsertarSQLite)) {
            stmtLimpiar.executeUpdate(sqlLimpiarSQLite);
            ArrayList<Empleado> empleadosDelServidor = ApiClient.obtenerEmpleadosDelServidor();

            int contador = 0;
            for(Empleado emp : empleadosDelServidor){
                stmtInsertar.setString(1, emp.getIdEmpleado());
                stmtInsertar.setString(2, emp.getNombre());
                stmtInsertar.setString(3, emp.getCargo());
                stmtInsertar.setDouble(4, emp.getSueldo());
                stmtInsertar.setString(5, emp.getUsername());
                stmtInsertar.setString(6, emp.getPasswordHash());
                stmtInsertar.setString(7, emp.getEstado());

                stmtInsertar.setString(8, emp.getFechaUltimoPago() != null ? emp.getFechaUltimoPago().toString() : null);
                stmtInsertar.setString(9, emp.getFechaContrato() != null ? emp.getFechaContrato().toString() : null);

                stmtInsertar.executeUpdate();
                contador++;
            }
            System.out.println("Sincronización de empleados completada. Total sincronizados: " + contador);
        } catch (SQLException e) {
            System.out.println("Error al sincronizar empleados: " + e.getMessage());
        }
    }

    public static void sincronizarInsumos() {
        System.out.println("Sincronizando insumos...");
        String sqlLimpiarSQLite = "DELETE FROM insumos_catalogo";
        String sqlInsertarSQLite = "INSERT INTO insumos_catalogo (codigo, nombre, categoria, unidad_medida, stock_minimo, stock_maximo, stock_actual, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connSQL = getConexion();
             Statement stmtLimpiar = connSQL.createStatement();
             PreparedStatement stmtInsertar = connSQL.prepareStatement(sqlInsertarSQLite)) {

            stmtLimpiar.executeUpdate(sqlLimpiarSQLite);
            ArrayList<Insumo> insumosDelServidor = ApiClient.obtenerInsumoDelServidor();
            int contador = 0;

            for(Insumo ins : insumosDelServidor){
                stmtInsertar.setString(1, ins.getCodigo());
                stmtInsertar.setString(2, ins.getNombre());
                stmtInsertar.setString(3, ins.getCategoria());
                stmtInsertar.setString(4, ins.getUnidad());
                stmtInsertar.setDouble(5, ins.getStockMinimo());
                stmtInsertar.setDouble(6, ins.getStockMaximo());
                stmtInsertar.setDouble(7, ins.getStockActual());
                stmtInsertar.setString(8, ins.getEstado());

                stmtInsertar.executeUpdate();
                contador++;
            }
            System.out.println("Insumos sincronizados: " + contador);
        } catch (SQLException e) {
            System.out.println("Error al sincronizar insumos: " + e.getMessage());
        }
    }

    public static void sincronizarProductos() {
        System.out.println("Sincronizando productos...");
        String sqlLimpiarSQLite = "DELETE FROM productos";
        String sqlLimpiarRecetas = "DELETE FROM recetas_local";
        
        String sqlInsertarSQLite = "INSERT OR REPLACE INTO productos " +
                "(id_producto, nombre, precio, categoria, tipo_clase, stock_directo, tiene_receta, imagen_base64, atributos_extra, fecha_inicio, fecha_fin, estado) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlInsertarReceta = "INSERT INTO recetas_local (id_producto, codigo_insumo, cantidad) VALUES (?, ?, ?)";

        try (Connection connSQL = getConexion();
             Statement stmtLimpiar = connSQL.createStatement();
             PreparedStatement stmtInsertar = connSQL.prepareStatement(sqlInsertarSQLite);
             PreparedStatement stmtReceta = connSQL.prepareStatement(sqlInsertarReceta)) {

            stmtLimpiar.executeUpdate(sqlLimpiarSQLite);
            stmtLimpiar.executeUpdate(sqlLimpiarRecetas);
            ArrayList<Producto> productosDelServidor = ApiClient.obtenerProductosDelServidor();
            int contador = 0;
            
            for(Producto p : productosDelServidor) {
                if(p.getCategoria() != null && p.getCategoria().equalsIgnoreCase("Promocion")) {
                    continue; 
                }

                stmtInsertar.setInt(1, p.getId());
                stmtInsertar.setString(2, p.getNombre());
                stmtInsertar.setDouble(3, p.getPrecio());
                stmtInsertar.setString(4, p.getCategoria());
                stmtInsertar.setString(5, "Producto");
                stmtInsertar.setInt(6, (int) p.getStock());
                stmtInsertar.setInt(7, p.tieneReceta() ? 1 : 0);

                String imagenLocal = asegurarImagenLocal(p.getImagenURL(), p.getId());
                stmtInsertar.setString(8, imagenLocal);

                String atributos = (p.getAtributosDinamicos() != null) ? p.getAtributosDinamicos().toString() : "{}";
                stmtInsertar.setString(9, atributos);
                
                stmtInsertar.setNull(10, java.sql.Types.VARCHAR);
                stmtInsertar.setNull(11, java.sql.Types.VARCHAR);
                
                stmtInsertar.setString(12, p.getEstado());

                stmtInsertar.executeUpdate();
                if (p.tieneReceta() && p.getReceta() != null) {
                    for (Map.Entry<String, Double> ingrediente : p.getReceta().getIngredientes().entrySet()) {
                        stmtReceta.setInt(1, p.getId());
                        stmtReceta.setString(2, ingrediente.getKey());
                        stmtReceta.setDouble(3, ingrediente.getValue());
                        stmtReceta.executeUpdate();
                    }
                }
                contador++;
            }
            System.out.println("Productos sincronizados desde la nube: " + contador);
        } catch (SQLException e) {
            System.out.println("Error al sincronizar productos locales: " + e.getMessage());
        }
    }
    public static void sincronizarDetallesCombo() {
        System.out.println("Sincronizando promociones y combos...");
        String sqlLimpiarDetalle = "DELETE FROM detalle_combo";
        
        String sqlInsertarPromo = "INSERT OR REPLACE INTO productos " +
                "(id_producto, nombre, precio, categoria, tipo_clase, stock_directo, tiene_receta, imagen_base64, atributos_extra, fecha_inicio, fecha_fin, estado) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                
        String sqlInsertarDetalle = "INSERT OR REPLACE INTO detalle_combo (id_promocion, id_producto, cantidad) VALUES (?, ?, ?)";

        try (Connection connSQL = getConexion();
             Statement stmtLimpiar = connSQL.createStatement();
             PreparedStatement stmtPromo = connSQL.prepareStatement(sqlInsertarPromo);
             PreparedStatement stmtDetalle = connSQL.prepareStatement(sqlInsertarDetalle)) {

            stmtLimpiar.executeUpdate(sqlLimpiarDetalle);
            ArrayList<Promocion> promosDelServidor = ApiClient.obtenerPromocionesDelServidor();
            
            int contadorPromo = 0;
            int contadorDetalle = 0;
            
            for (Promocion promo : promosDelServidor) {
                stmtPromo.setInt(1, promo.getId());
                stmtPromo.setString(2, promo.getNombre());
                stmtPromo.setDouble(3, promo.getPrecio());
                stmtPromo.setString(4, "Promocion");
                stmtPromo.setString(5, "Combo");
                stmtPromo.setInt(6, 0); // No maneja stock directo
                stmtPromo.setInt(7, 0); // No tiene receta de cocina directa
                
                String imgPromo = asegurarImagenLocal(promo.getImagenURL(), promo.getId());
                stmtPromo.setString(8, imgPromo);
                
                stmtPromo.setString(9, "{}"); // Atributos extra vacíos
                
                if (promo.getFechaInicio() != null) stmtPromo.setString(10, promo.getFechaInicio().toString());
                else stmtPromo.setNull(10, java.sql.Types.VARCHAR);
                
                if (promo.getFechaFin() != null) stmtPromo.setString(11, promo.getFechaFin().toString());
                else stmtPromo.setNull(11, java.sql.Types.VARCHAR);
                
                stmtPromo.setString(12, promo.getEstado());
                
                stmtPromo.executeUpdate();
                contadorPromo++;

                if (promo.getProductosCombo() != null) {
                    for (DetalleCombo detalle : promo.getProductosCombo()) {
                        stmtDetalle.setInt(1, promo.getId());
                        stmtDetalle.setInt(2, detalle.getProducto().getId());
                        stmtDetalle.setInt(3, detalle.getCantidad());
                        stmtDetalle.executeUpdate();
                        contadorDetalle++;
                    }
                }
            }
            System.out.println("Promociones sincronizadas: " + contadorPromo + " (con " + contadorDetalle + " detalles internos).");
        } catch (SQLException e) {
            System.out.println("Error al sincronizar combos locales: " + e.getMessage());
        }
    }
    public static ArrayList<Empleado> obtenerEmpleadosLocal() {
        ArrayList<Empleado> lista = new ArrayList<>();
        String sql = "SELECT id_empleado, nombre, cargo, sueldo, username, estado, fecha_ultimo_pago, fecha_contrato FROM empleados WHERE estado = 'Activo'";

        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Empleado emp = new Empleado();
                emp.setIdEmpleado(rs.getString("id_empleado")); // CORREGIDO: Nombre correcto del método
                emp.setNombre(rs.getString("nombre"));
                emp.setCargo(rs.getString("cargo"));
                emp.setSueldo(rs.getDouble("sueldo"));
                emp.setEstado(rs.getString("estado"));

                String userName = rs.getString("username");
                if (userName != null && !userName.trim().isEmpty()) {
                    emp.setUsername(userName);
                }

                String fechaStr = rs.getString("fecha_ultimo_pago");
                if (fechaStr != null && !fechaStr.trim().isEmpty() && !fechaStr.equals("null")) {
                    emp.setFechaUltimoPago(LocalDate.parse(fechaStr));
                }
                String fechaContratoStr = rs.getString("fecha_contrato");
                if (fechaContratoStr != null && !fechaContratoStr.trim().isEmpty() && !fechaContratoStr.equals("null")) {
                    emp.setFechaContrato(LocalDate.parse(fechaContratoStr));
                }
                lista.add(emp);
            }
        } catch (SQLException e) {
            System.out.println("Error al leer empleados locales: " + e.getMessage());
        }
        return lista;
    }

    public static HashMap<String, Insumo> obtenerInventarioLocal() {
        HashMap<String, Insumo> inventario = new HashMap<>();
        String sql = "SELECT codigo, nombre, categoria, unidad_medida, stock_minimo, stock_maximo, stock_actual, estado FROM insumos_catalogo WHERE estado = 'Activo'";

        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String codigo = rs.getString("codigo");
                String nombre = rs.getString("nombre");
                String categoria = rs.getString("categoria");
                String unidad = rs.getString("unidad_medida");
                double min = rs.getDouble("stock_minimo");
                double max = rs.getDouble("stock_maximo");
                double actual = rs.getDouble("stock_actual");

                Insumo i = new Insumo(codigo, nombre, categoria, unidad, actual, min, max, LocalDate.now().plusYears(1));
                inventario.put(codigo, i);
            }
        } catch (SQLException e) {
            System.out.println("Error al leer inventario local: " + e.getMessage());
        }
        return inventario;
    }

    public static ArrayList<Producto> obtenerMenuLocal() {
        ArrayList<Producto> menu = new ArrayList<>();
        String sql = "SELECT id_producto, nombre, precio, categoria, stock_directo, tiene_receta, imagen_base64, atributos_extra, estado " +
                "FROM productos WHERE estado = 'Activo' AND categoria != 'Promocion' ORDER BY id_producto";
        String sqlReceta = "SELECT codigo_insumo, cantidad FROM recetas_local WHERE id_producto = ?";

        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id_producto");
                String nombre = rs.getString("nombre");
                double precio = rs.getDouble("precio");
                String categoria = rs.getString("categoria");
                int stockDirecto = rs.getInt("stock_directo");
                boolean tieneReceta = rs.getInt("tiene_receta") == 1;
                String imagenBase64 = rs.getString("imagen_base64");
                String jsonStr = rs.getString("atributos_extra");

                if (imagenBase64 == null || imagenBase64.trim().isEmpty()) {
                    imagenBase64 = "default.png";
                }

                Producto nuevoProducto = new Producto(id, nombre, precio, categoria, stockDirecto, imagenBase64, rs.getString("estado"));
                nuevoProducto.setTieneReceta(tieneReceta);
                if(tieneReceta){
                    Receta receta = new Receta();
                    try (PreparedStatement psReceta = conn.prepareStatement(sqlReceta)) {
                        psReceta.setInt(1, id);
                        try (ResultSet rsReceta = psReceta.executeQuery()) {
                            while (rsReceta.next()) {
                                receta.agregarIngrediente(rsReceta.getString("codigo_insumo"), rsReceta.getDouble("cantidad"));
                            }
                        }
                    }
                    nuevoProducto.setReceta(receta);
                }

                if (jsonStr != null && jsonStr.length() > 2) {
                    String contenido = jsonStr.substring(1, jsonStr.length() - 1);
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

                menu.add(nuevoProducto);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al leer el menú local: " + e.getMessage());
        }
        return menu;
    }

    public static Empleado autenticarUsuarioLocal(String username, String passwordHash) {
        String sql = "SELECT id_empleado, nombre, cargo, sueldo, username, estado FROM empleados WHERE username = ? AND password_hash = ? AND estado = 'Activo'";

        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, passwordHash);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Empleado emp = new Empleado();
                    emp.setIdEmpleado(rs.getString("id_empleado"));
                    emp.setNombre(rs.getString("nombre"));
                    emp.setCargo(rs.getString("cargo"));
                    emp.setSueldo(rs.getDouble("sueldo"));
                    emp.setEstado(rs.getString("estado"));
                    emp.setUsername(rs.getString("username"));
                    return emp;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error en autenticación local SQLite: " + e.getMessage());
        }
        return null;
    }
    public static boolean guardarAuditoriaOffline(String operador, String tipoArea, String nombreItem, String accion, double cantidad, String motivo) {
        String sql = "INSERT INTO auditoria_pendiente (operador, tipo_area, nombre_item, accion, cantidad, motivo) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, operador);
            ps.setString(2, tipoArea);
            ps.setString(3, nombreItem);
            ps.setString(4, accion);
            ps.setDouble(5, cantidad);
            ps.setString(6, motivo);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Error al guardar auditoría offline: " + e.getMessage());
            return false;
        }
    }
    public static void subirAuditoriaPendiente() {
        String sqlLeer = "SELECT id_local, operador, tipo_area, nombre_item, accion, cantidad, motivo FROM auditoria_pendiente WHERE estado = 'pendiente'";
        String sqlActualizar = "UPDATE auditoria_pendiente SET estado = 'sincronizado' WHERE id_local = ?";

        try (Connection connLocal = getConexion();
             PreparedStatement psLeer = connLocal.prepareStatement(sqlLeer);
             ResultSet rs = psLeer.executeQuery()) {

            int sincronizadas = 0;
            while (rs.next()) {
                int idLocal = rs.getInt("id_local");
                boolean exito = ApiClient.registrarAuditoriaEnServidor(
                        rs.getString("operador"), rs.getString("tipo_area"),
                        rs.getString("nombre_item"), rs.getString("accion"),
                        rs.getDouble("cantidad"), rs.getString("motivo")
                );

                if (exito) {
                    try (PreparedStatement psActualizar = connLocal.prepareStatement(sqlActualizar)) {
                        psActualizar.setInt(1, idLocal);
                        psActualizar.executeUpdate();
                        sincronizadas++;
                    }
                }
            }
            if (sincronizadas > 0) {
                System.out.println("☁️ ✅ Se subieron " + sincronizadas + " registros de auditoría a la nube.");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al sincronizar auditoría: " + e.getMessage());
        }
    }
    public static boolean guardarVentaOffline(String jsonVenta) {
        String sql = "INSERT INTO ventas_pendientes (json_venta, fecha_hora) VALUES (?, ?)";
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, jsonVenta);
            ps.setString(2, java.time.LocalDateTime.now().toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Error al guardar venta offline: " + e.getMessage());
            return false;
        }
    }

    public static void actualizarSecuenciaLocal(int numeroOnline) {
        String fechaHoy = java.time.LocalDate.now().toString();
        String sql = "UPDATE secuencia_tickets SET fecha = ?, ultimo_numero = ? WHERE id = 1";
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fechaHoy);
            ps.setInt(2, numeroOnline);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al actualizar secuencia: " + e.getMessage());
        }
    }

    public static int obtenerSiguienteTicketOffline() {
        String fechaHoy = java.time.LocalDate.now().toString();
        int siguiente = 1;
        try (Connection conn = getConexion()) {
            String sqlLeer = "SELECT fecha, ultimo_numero FROM secuencia_tickets WHERE id = 1";
            try (PreparedStatement ps = conn.prepareStatement(sqlLeer);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String fechaBD = rs.getString("fecha");
                    int ultimo = rs.getInt("ultimo_numero");
                    if (fechaHoy.equals(fechaBD)) {
                        siguiente = ultimo + 1;
                    }
                }
            }

            String sqlAct = "UPDATE secuencia_tickets SET fecha = ?, ultimo_numero = ? WHERE id = 1";
            try (PreparedStatement psAct = conn.prepareStatement(sqlAct)) {
                psAct.setString(1, fechaHoy);
                psAct.setInt(2, siguiente);
                psAct.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error en secuencia offline: " + e.getMessage());
        }
        return siguiente;
    }
    public static ArrayList<Promocion> obtenerPromocionesLocal(ArrayList<Producto> menu) {
        ArrayList<Promocion> listaPromo = new ArrayList<>();
        String sqlPromo = "SELECT id_producto, nombre, precio, imagen_base64, fecha_inicio, fecha_fin FROM productos WHERE categoria = 'Promocion' AND estado = 'Activo'";
        // Usamos id_producto tal como lo definiste al crear tu tabla detalle_combo en SQLite
        String sqlDetalle = "SELECT id_producto, cantidad FROM detalle_combo WHERE id_promocion = ?";

        try (Connection conn = getConexion();
             PreparedStatement psPromo = conn.prepareStatement(sqlPromo);
             ResultSet rsPromo = psPromo.executeQuery()) {

            while (rsPromo.next()) {
                int id = rsPromo.getInt("id_producto");
                String nombre = rsPromo.getString("nombre");
                double precio = rsPromo.getDouble("precio");
                String img = rsPromo.getString("imagen_base64");

                String fInicioStr = rsPromo.getString("fecha_inicio");
                String fFinStr = rsPromo.getString("fecha_fin");

                // Verificación segura por si la base de datos devuelve "null" en texto
                LocalDate fInicio = (fInicioStr != null && !fInicioStr.trim().isEmpty() && !fInicioStr.equals("null")) ? LocalDate.parse(fInicioStr) : null;
                LocalDate fFin = (fFinStr != null && !fFinStr.trim().isEmpty() && !fFinStr.equals("null")) ? LocalDate.parse(fFinStr) : null;

                Promocion promo = new Promocion(id, nombre, precio, img, fInicio, fFin);

                // Buscar qué lleva por dentro este combo
                try (PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle)) {
                    psDetalle.setInt(1, id);
                    try (ResultSet rsDetalle = psDetalle.executeQuery()) {
                        while (rsDetalle.next()) {
                            int idProductoReal = rsDetalle.getInt("id_producto");
                            int cantidad = rsDetalle.getInt("cantidad");

                            // Buscar producto físico dentro del menú que ya cargamos en RAM
                            Producto productoFisico = null;
                            for(Producto p : menu){
                                if(p.getId() == idProductoReal){
                                    productoFisico = p;
                                    break;
                                }
                            }
                            if (productoFisico != null) {
                                promo.agregarProducto(productoFisico, cantidad);
                            }
                        }
                    }
                }
                listaPromo.add(promo);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al leer promociones locales: " + e.getMessage());
        }
        return listaPromo;
    }
    public static void actualizarImagenProductoLocal(int idProucto, String nuevaRutaImagen){
        String sql = "UPDATE productos SET imagen_base64 = ? WHERE id_producto = ?";
        try(Connection conn = getConexion();
            PreparedStatement ps = conn.prepareStatement(sql)
        ){
            ps.setString(1, nuevaRutaImagen);
            ps.setString(2, String.valueOf(idProucto));
            ps.executeUpdate();
        }catch (SQLException e){
            System.out.println("Error al actualizar imagen de producto: " + e.getMessage());
        }
    }
    private static String descargarImagenSiEsUrl(String urlImagen, int idProducto) {
        if (urlImagen == null || !urlImagen.startsWith("http")) return urlImagen;
        String extension = urlImagen.contains(".png") ? ".png" : ".jpg";
        String nombreArchivo = "producto_" + idProducto + extension;
        File destino = new File(UtilidadesImagen.DIRECTORIO_IMAGENES, nombreArchivo);
        if (destino.exists()) return destino.getAbsolutePath();
        try (InputStream in = new URL(urlImagen).openStream();
             FileOutputStream out = new FileOutputStream(destino)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) out.write(buffer, 0, bytesRead);
            return destino.getAbsolutePath();
        } catch (Exception e) {
            System.err.println("Error descargando " + urlImagen + ": " + e.getMessage());
            return urlImagen;
        }
    }
    private static String asegurarImagenLocal(String imagenUrlOrPath, int idProducto) {
        if (imagenUrlOrPath == null || imagenUrlOrPath.equals("default.png"))
            return "default.png";
        if (!imagenUrlOrPath.startsWith("http"))
            return imagenUrlOrPath; // ya es ruta local

        // Es URL, descargar
        String ext = imagenUrlOrPath.contains(".png") ? ".png" : ".jpg";
        String nombre = "producto_" + idProducto + ext;
        File destino = new File(UtilidadesImagen.DIRECTORIO_IMAGENES, nombre);
        if (destino.exists()) return destino.getAbsolutePath();

        try (InputStream in = new URL(imagenUrlOrPath).openStream();
             FileOutputStream out = new FileOutputStream(destino)) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
            return destino.getAbsolutePath();
        } catch (Exception e) {
            System.err.println("Error descargando imagen para producto " + idProducto + ": " + e.getMessage());
            return "default.png";
        }
    }
    public static void subirVentasPendientes() {
        String sqlLeer = "SELECT id_local, json_venta FROM ventas_pendientes WHERE estado = 'pendiente'";
        String sqlActualizar = "UPDATE ventas_pendientes SET estado = 'sincronizado' WHERE id_local = ?";

        try (Connection connLocal = getConexion();
             PreparedStatement psLeer = connLocal.prepareStatement(sqlLeer);
             ResultSet rs = psLeer.executeQuery()) {

            int sincronizadas = 0;
            while (rs.next()) {
                int idLocal = rs.getInt("id_local");
                String jsonVenta = rs.getString("json_venta");

                // Enviamos el JSON guardado directamente al servidor Spring Boot
                boolean exito = ApiClient.enviarVentaOfflineAServidor(jsonVenta);

                if (exito) {
                    try (PreparedStatement psActualizar = connLocal.prepareStatement(sqlActualizar)) {
                        psActualizar.setInt(1, idLocal);
                        psActualizar.executeUpdate();
                        sincronizadas++;
                    }
                }
            }
            if (sincronizadas > 0) {
                System.out.println("☁️ ✅ ¡Rescate Offline exitoso! Se subieron " + sincronizadas + " ventas pendientes a la nube.");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al sincronizar ventas pendientes: " + e.getMessage());
        }
    }
}