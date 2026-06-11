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
    public static String geminiKey = "";

    static {
        String rutaAppData = System.getenv("APPDATA");
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

                geminiKey = propiedades.getProperty("GEMINI_API_KEY", "");
                boolean necesitaActualizar = false;
                if (!propiedades.containsKey("CLOUDINARY_CLOUD_NAME")) {
                    propiedades.setProperty("CLOUDINARY_CLOUD_NAME", "PON_TU_CLOUD_NAME_AQUI");
                    propiedades.setProperty("CLOUDINARY_API_KEY", "PON_TU_API_KEY_AQUI");
                    propiedades.setProperty("CLOUDINARY_API_SECRET", "PON_TU_API_SECRET_AQUI");
                    necesitaActualizar = true;
                }
                if(!propiedades.containsKey("GEMINI_API_KEY")) {
                    propiedades.setProperty("GEMINI_API_KEY", "PON_TU_API_KEY_AQUI");
                    necesitaActualizar = true;
                }
                if(necesitaActualizar){
                    try (FileOutputStream fos = new FileOutputStream(archivoConfig)) {
                        propiedades.store(fos, "Configuración actualizada con Cloudinary");
                    }catch (IOException e){
                        System.err.println("Error al actualizar config.properties: " + e.getMessage());
                        cargarValoresPorDefecto();
                        propiedades.setProperty("db.url", url);
                        propiedades.setProperty("db.user", user);
                        propiedades.setProperty("db.password", password);
                        propiedades.setProperty("CLOUDINARY_CLOUD_NAME", "PON_TU_CLOUD_NAME_AQUI");
                        propiedades.setProperty("CLOUDINARY_API_KEY", "PON_TU_API_KEY_AQUI");
                        propiedades.setProperty("CLOUDINARY_API_SECRET", "PON_TU_API_SECRET_AQUI");
                        propiedades.setProperty("GEMINI_API_KEY", "PON_TU_API_KEY_AQUI");
                        try (FileOutputStream fos = new FileOutputStream(archivoConfig)) {
                            propiedades.store(fos, "Configuración recreada");
                        } catch (IOException e2) {
                            System.err.println("No se pudo recrear config.properties: " + e2.getMessage());
                        }
                    }
                    }
            } catch (IOException ex) {
                System.out.println("Error al cargar config.properties: " + ex.getMessage());
                cargarValoresPorDefecto();
            }
        }else{
            System.out.println("Creando archivo config.properties. ");
            cargarValoresPorDefecto();
            propiedades.setProperty("db.url",url);
            propiedades.setProperty("db.user",user);
            propiedades.setProperty("db.password",password);

            propiedades.setProperty("CLOUDINARY_CLOUD_NAME", "PON_TU_CLOUD_NAME_AQUI");
            propiedades.setProperty("CLOUDINARY_API_KEY", "PON_TU_API_KEY_AQUI");
            propiedades.setProperty("CLOUDINARY_API_SECRET", "PON_TU_API_SECRET_AQUI");
            propiedades.setProperty("GEMINI_API_KEY", "PON_TU_API_KEY_AQUI");

            try(FileOutputStream fos = new FileOutputStream(archivoConfig)){
                propiedades.store(fos, "Configuración de Base de datos y Servicios en la Nube");
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
