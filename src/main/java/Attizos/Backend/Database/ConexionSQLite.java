package Attizos.Backend.Database;

import Attizos.Backend.Attizos.*;

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
        try (Connection conn = getConexion();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlEmpleados);
            stmt.execute(sqlProductos);
            stmt.execute(sqlInsumos);
            stmt.execute(sqlVentasPendientes);

            System.out.println("Caché SQLite inicializada correctamente. ");
        } catch (SQLException e) {
            System.out.println("Error al inicializar tablas locales: " + e.getMessage());
        }
    }

    public static void sincronizarEmpleados() {
        System.out.println("Iniciando sincronización de empleados.");

        String sqlLeerPostgres = "SELECT id_empleado, nombre, cargo, sueldo, username, password_hash, estado FROM empleados";
        String sqlLimpiarSQLite = "DELETE FROM empleados";
        String sqlInsertarSQLite = "INSERT INTO empleados (id_empleado, nombre, cargo, sueldo, username, password_hash, estado) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connPG = ConexionBD.getConexion();
             PreparedStatement stmtLeer = connPG.prepareStatement(sqlLeerPostgres);
             ResultSet rs = stmtLeer.executeQuery();

             Connection connSQL = ConexionSQLite.getConexion();
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

             Connection connSQL = ConexionBD.getConexion();
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
                stmtInsertar.setInt(7, rs.getInt("tiene_receta"));
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

    public static void actualizarCacheCompleta() {
        System.out.println("Actualizando caché local completa...");
        sincronizarEmpleados();
        sincronizarInsumos();
        sincronizarProductos();
    }
    public static ArrayList<Empleado> obtenerEmpleadosLocal(){
        ArrayList<Empleado> lista = new ArrayList<>();
        String sql = "SELECT id_empleado, nombre, cargo, sueldo, username, estado FROM empleados WHERE estado = 'Activo'";

        try(Connection conn = getConexion();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ){
            while(rs.next()){
                String userName = rs.getString("username");
                Empleado emp;
                if(userName != null && !userName.trim().isEmpty()){
                    Usuario user = new Usuario();
                    user.setUsername(userName);
                    emp = user;
                }else{
                    emp = new Empleado();
                }
                emp.setId(rs.getString("id_empleado"));
                emp.setNombre(rs.getString("nombre"));
                emp.setCargo(rs.getString("cargo"));
                emp.setSueldo(rs.getDouble("sueldo"));
                lista.add(emp);
            }
        }catch(SQLException e){
            System.out.println("Error al leer empleados locales: "+e.getMessage());
        }
        return lista;
    }
    public static HashMap<String, Insumo> obtenerInventarioLocal(){
        HashMap<String, Insumo> inventario = new HashMap<>();
        String sql = "SELECT codigo, nombre, categoria, unidad_medida, stock_minimo, stock_maximo, estado FROM insumos_catalogo WHERE estado = 'Activo'";

        try(Connection conn = getConexion();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
        ){
            while(rs.next()){
                String codigo = rs.getString("codigo");
                String nombre = rs.getString("nombre");
                String categoria = rs.getString("categoria");
                String unidad = rs.getString("unidad_medida");
                double min = rs.getDouble("stock_minimo");
                double max = rs.getDouble("stock_maximo");

                Insumo i = new Insumo(codigo, nombre, categoria, unidad, 0.0, min, max, LocalDate.now().plusYears(1));
                inventario.put(codigo, i);
            }
        }catch(SQLException e){
            System.out.println("Error al leer invetario local: "+e.getMessage());
        }
        return inventario;
    }
}
