package Attizos.Backend.Database;

import Attizos.Backend.Attizos.*;
import Attizos.Backend.Listas.*;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;

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
                + "estado TEXT DEFAULT 'Activo'"
                + ");";
        String sqlInsumos = "CREATE TABLE IF NOT EXISTS insumos_catalogo ("
                + "codigo TEXT PRIMARY KEY, "
                + "nombre TEXT NOT NULL, "
                + "categoria TEXT, "
                + "unidad_medida TEXT, "
                + "stock_minimo REAL DEFAULT 0, "
                + "stock_maximo REAL DEFAULT 0, "
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
                + "estado TEXT DEFAULT 'Activo'"
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


        try (Connection conn = getConexion();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlEmpleados);
            stmt.execute(sqlProductos);
            stmt.execute(sqlInsumos);
            stmt.execute(sqlVentasPendientes);
            stmt.execute(sqlAuditoria);
            stmt.execute(sqlSecuencia);
            stmt.execute("INSERT OR IGNORE INTO secuencia_tickets (id, fecha, ultimo_numero) VALUES (1, '2000-01-01', 0);");

            System.out.println("Caché SQLite inicializada correctamente. ");
        } catch (SQLException e) {
            System.out.println("Error al inicializar tablas locales: " + e.getMessage());
        }
    }
    public static void actualizarCacheCompleta() {
        System.out.println("Actualizando caché local completa...");
        sincronizarEmpleados();
        sincronizarInsumos();
        sincronizarProductos();
    }

    public static void sincronizarEmpleados() {
        System.out.println("Iniciando sincronización de empleados.");
        String sqlLeerPostgres = "SELECT id_empleado, nombre, cargo, sueldo, username, password_hash, estado FROM empleados";
        String sqlLimpiarSQLite = "DELETE FROM empleados";
        String sqlInsertarSQLite = "INSERT INTO empleados (id_empleado, nombre, cargo, sueldo, username, password_hash, estado) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connPG = ConexionBD.getConexion();
             PreparedStatement stmtLeer = connPG.prepareStatement(sqlLeerPostgres);
             ResultSet rs = stmtLeer.executeQuery();

             Connection connSQL = getConexion();
             Statement stmtLimpiar = connSQL.createStatement();
             PreparedStatement stmtInsertar = connSQL.prepareStatement(sqlInsertarSQLite)) {

            stmtLimpiar.executeUpdate(sqlLimpiarSQLite);

            int contador = 0;
            while (rs.next()) {
                stmtInsertar.setString(1, rs.getString("id_empleado"));
                stmtInsertar.setString(2, rs.getString("nombre"));
                stmtInsertar.setString(3, rs.getString("cargo"));
                stmtInsertar.setDouble(4, rs.getDouble("sueldo"));
                stmtInsertar.setString(5, rs.getString("username"));
                stmtInsertar.setString(6, rs.getString("password_hash"));
                stmtInsertar.setString(7, rs.getString("estado"));

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
        String sqlLeerPG = "SELECT codigo, nombre, categoria, unidad_medida, stock_minimo, stock_maximo, estado FROM insumos_catalogo";
        String sqlLimpiarSQLite = "DELETE FROM insumos_catalogo";
        String sqlInsertarSQLite = "INSERT INTO insumos_catalogo (codigo, nombre, categoria, unidad_medida, stock_minimo, stock_maximo, estado) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connPG = ConexionBD.getConexion();
             PreparedStatement stmtLeer = connPG.prepareStatement(sqlLeerPG);
             ResultSet rs = stmtLeer.executeQuery();
             Connection connSQL = getConexion();
             Statement stmtLimpiar = connSQL.createStatement();
             PreparedStatement stmtInsertar = connSQL.prepareStatement(sqlInsertarSQLite)) {

            stmtLimpiar.executeUpdate(sqlLimpiarSQLite);
            int contador = 0;

            while (rs.next()) {
                stmtInsertar.setString(1, rs.getString("codigo"));
                stmtInsertar.setString(2, rs.getString("nombre"));
                stmtInsertar.setString(3, rs.getString("categoria"));
                stmtInsertar.setString(4, rs.getString("unidad_medida"));
                stmtInsertar.setDouble(5, rs.getDouble("stock_minimo"));
                stmtInsertar.setDouble(6, rs.getDouble("stock_maximo"));
                stmtInsertar.setString(7, rs.getString("estado"));

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
        String sqlLeerPG = "SELECT id_producto, nombre, precio, categoria, tipo_clase, stock_directo, tiene_receta, imagen_base64, atributos_extra, estado FROM productos";
        String sqlLimpiarSQLite = "DELETE FROM productos";
        String sqlInsertarSQLite = "INSERT INTO productos (id_producto, nombre, precio, categoria, tipo_clase, stock_directo, tiene_receta, imagen_base64, atributos_extra, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connPG = ConexionBD.getConexion();
             PreparedStatement stmtLeer = connPG.prepareStatement(sqlLeerPG);
             ResultSet rs = stmtLeer.executeQuery();

             Connection connSQL = getConexion();
             Statement stmtLimpiar = connSQL.createStatement();
             PreparedStatement stmtInsertar = connSQL.prepareStatement(sqlInsertarSQLite)) {

            stmtLimpiar.executeUpdate(sqlLimpiarSQLite);
            int contador = 0;

            while (rs.next()) {
                stmtInsertar.setInt(1, rs.getInt("id_producto"));
                stmtInsertar.setString(2, rs.getString("nombre"));
                stmtInsertar.setDouble(3, rs.getDouble("precio"));
                stmtInsertar.setString(4, rs.getString("categoria"));
                stmtInsertar.setString(5, rs.getString("tipo_clase"));
                stmtInsertar.setInt(6, rs.getInt("stock_directo"));
                // CORREGIDO: Manejo seguro del boolean de Postgres a int de SQLite
                stmtInsertar.setInt(7, rs.getBoolean("tiene_receta") ? 1 : 0);
                stmtInsertar.setString(8, rs.getString("imagen_base64"));
                stmtInsertar.setString(9, rs.getString("atributos_extra"));
                stmtInsertar.setString(10, rs.getString("estado"));

                stmtInsertar.executeUpdate();
                contador++;
            }
            System.out.println("Productos sincronizados: " + contador);
        } catch (SQLException e) {
            System.out.println("Error al sincronizar productos: " + e.getMessage());
        }
    }
    public static ArrayList<Empleado> obtenerEmpleadosLocal() {
        ArrayList<Empleado> lista = new ArrayList<>();
        String sql = "SELECT id_empleado, nombre, cargo, sueldo, username, estado FROM empleados WHERE estado = 'Activo'";

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

                lista.add(emp);
            }
        } catch (SQLException e) {
            System.out.println("Error al leer empleados locales: " + e.getMessage());
        }
        return lista;
    }

    public static HashMap<String, Insumo> obtenerInventarioLocal() {
        HashMap<String, Insumo> inventario = new HashMap<>();
        String sql = "SELECT codigo, nombre, categoria, unidad_medida, stock_minimo, stock_maximo, estado FROM insumos_catalogo WHERE estado = 'Activo'";

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

                Insumo i = new Insumo(codigo, nombre, categoria, unidad, 0.0, min, max, LocalDate.now().plusYears(1));
                inventario.put(codigo, i);
            }
        } catch (SQLException e) {
            System.out.println("Error al leer inventario local: " + e.getMessage());
        }
        return inventario;
    }

    public static ListaDE<Producto> obtenerMenuLocal() {
        ListaDE<Producto> menu = new ListaDE<>();
        String sql = "SELECT id_producto, nombre, precio, categoria, stock_directo, tiene_receta, imagen_base64, atributos_extra, estado FROM productos WHERE estado = 'Activo' ORDER BY id_producto";

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
                    imagenBase64 = "\\src\\main\\resources\\images\\default.png";
                }

                Producto nuevoProducto = new Producto(id, nombre, precio, categoria, stockDirecto, imagenBase64, rs.getString("estado"));

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

                menu.insertarAlFinal(nuevoProducto);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al leer el menú local: " + e.getMessage());
        }
        return menu;
    }

    // MÉTODO NUEVO AGREGADO: Para autenticar el Login si se corta el Internet
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
                boolean exito = ReportesDAO.registrarAuditoria(
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
}