package Attizos.Backend.Database;

import java.io.File;
import java.sql.*;

public class ConexionSQLite {
    private static String urlSQLite;

    static{
        String rutaAppData = System.getenv("APPDATA");
        File carpetaAttizos = new File(rutaAppData, "Attizos");

        if(!carpetaAttizos.exists()){
            carpetaAttizos.mkdirs();
        }
        File archivoDB = new File(carpetaAttizos, "Attizos.db");

        urlSQLite = "jdbc:sqlite:" + archivoDB.getAbsolutePath();
        System.out.println("Ruta de la base de datos SQLite: " + archivoDB.getAbsolutePath());
    }
    public static Connection getConexion() throws SQLException {
        try{
            Class.forName("org.sqlite.JDBC");
        }catch (ClassNotFoundException e){
            System.err.println("Error al cargar el driver de SQLite: " + e.getMessage());
            throw new SQLException("Driver de SQLite no encontrado", e);
        }
        return DriverManager.getConnection(urlSQLite);
    }

    public static void inicializarTablasLocales(){
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
        try(Connection conn = getConexion();
            Statement stmt = conn.createStatement()){
            stmt.execute(sqlEmpleados);
            stmt.execute(sqlProductos);
            stmt.execute(sqlInsumos);
            stmt.execute(sqlVentasPendientes);

            System.out.println("Caché SQLite inicializada correctamente. ");
        }catch (SQLException e){
            System.out.println("Error al inicializar tablas locales: " + e.getMessage());
        }
    }
    public static void sincronizarEmpleados(){
        System.out.println("Iniciando sincronización de empleados.");

        String sqlLeerPostgres = "SELECT id_empleado, nombre, cargo, sueldo, username, password_hash, estado FROM empleados";
        String sqlLimpiarSQLite = "DELETE FROM empleados";
        String sqlInsertarSQLite = "INSERT INTO empleados (id_empleado, nombre, cargo, sueldo, username, password_hash, estado) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try(Connection connPG = ConexionBD.getConexion();
            PreparedStatement stmtLeer = connPG.prepareStatement(sqlLeerPostgres);
            ResultSet rs = stmtLeer.executeQuery();

            Connection connSQL = ConexionSQLite.getConexion();
            Statement stmtLimpiar = connSQL.createStatement();
            PreparedStatement stmtInsertar = connSQL.prepareStatement(sqlInsertarSQLite)){

            stmtLimpiar.executeUpdate(sqlLimpiarSQLite);

            int contador = 0;
            while(rs.next()){
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
        }catch (SQLException e){
            System.out.println("Error al sincronizar empleados: " + e.getMessage());
        }
    }
}
