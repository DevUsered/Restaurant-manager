package Attizos.Backend.Database;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class  ConexionBD {
    private static String url;
    private static String user;
    private static String password;

    static {
        Properties propiedades = new Properties();
        File archivoConfig = new File("config.properties");
        if(archivoConfig.exists()){
            try(FileInputStream fis = new FileInputStream(archivoConfig)){
                propiedades.load(fis);
                url = propiedades.getProperty("db.url");
                user = propiedades.getProperty("db.user");
                password = propiedades.getProperty("db.password");
                System.out.println("Archivo config.properties cargado correctamente.");
            }catch (IOException e){
                System.err.println("Error al cargar config.properties, usando valores por defecto: " + e.getMessage());
                cargarValoresPorDefecto();
            }
        }else{
            System.out.println("⚠️ No se encontró config.properties en la raíz. Usando configuración de desarrollo (localhost).");
            cargarValoresPorDefecto();
        }
    }
    private static void cargarValoresPorDefecto(){
        url = "jdbc:postgresql://localhost:5432/attizos_db";
        user = "postgres";
        password = "Abril2026Temporal";
    }
    public static Connection getConexion() throws SQLException {
        try{
            Class.forName("org.postgresql.Driver");
        }catch (ClassNotFoundException e){
            System.err.println("Error al cargar el driver de PostgreSQL: " + e.getMessage());
            throw new SQLException("Driver de PostgreSQL no encontrado.");
        }
        return DriverManager.getConnection(url, user, password);
    }

}
