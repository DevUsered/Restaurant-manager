package Attizos.Backend.Database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
        String rutaAppData = System.getProperty("APPDATA");
        File carpetaAttizos = new File(rutaAppData, "Attizos");

        if(!carpetaAttizos.exists()){
            carpetaAttizos.mkdirs();
        }

        File archivoConfig = new File(carpetaAttizos, "config.properties");

        Properties propiedades = new Properties();
        if(archivoConfig.exists()){
            try(FileInputStream fis = new FileInputStream(archivoConfig)){
                propiedades.load(fis);
                url = propiedades.getProperty("db.url");
                user = propiedades.getProperty("db.user");
                password = propiedades.getProperty("db.password");
                System.out.println("Archivo config.properties cargado correctamente.");
            } catch (IOException e) {
                System.out.println("Error al cargar config.properties: " + e.getMessage());
                cargarValoresPorDefecto();
            }
        }else{
            System.out.println("Creando archivo config.properties. ");
            cargarValoresPorDefecto();
            propiedades.setProperty("db.url",url);
            propiedades.setProperty("db.user",user);
            propiedades.setProperty("db.password",password);

            try(FileOutputStream fos = new FileOutputStream(archivoConfig)){
                propiedades.store(fos, "Configuración de Base de datos");
                System.out.println("Archivo creado exitosamente en: " + archivoConfig.getAbsolutePath());
            }catch (IOException e){
                System.err.println("Error al crear archivo config.properties: " + e.getMessage());
            }
        }
    }
    private static void cargarValoresPorDefecto(){
        url = "jdbc:postgresql://localhost:5432/attizos_db";
        user = "postgres";
        password = "admin";
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
