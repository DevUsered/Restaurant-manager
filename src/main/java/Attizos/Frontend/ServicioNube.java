package Attizos.Frontend;

import Attizos.Backend.Attizos.Producto;
import Attizos.Backend.Database.ConexionSQLite;
import Attizos.Backend.Database.ProductoDAO;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

public class ServicioNube {
    private static  Cloudinary CLOUDINARY;
    private static final String DEFAULT_IMAGE = "default.png";
    static {
        File archivoConfigAppData = new File(System.getenv("APPDATA") + File.separator + "Attizos" + File.separator + "config.properties");
        Properties prop = new Properties();

        try (java.io.FileInputStream input = new java.io.FileInputStream(archivoConfigAppData)) {
            prop.load(input);

            String cloudName = prop.getProperty("CLOUDINARY_CLOUD_NAME");

            if (cloudName != null && !cloudName.equals("PON_TU_CLOUD_NAME_AQUI")) {
                CLOUDINARY = new Cloudinary(ObjectUtils.asMap(
                        "cloud_name", cloudName,
                        "api_key", prop.getProperty("CLOUDINARY_API_KEY"),
                        "api_secret", prop.getProperty("CLOUDINARY_API_SECRET")
                ));
            } else {
                System.out.println("⚠️ Cloudinary inactivo: Faltan las credenciales reales en config.properties");
            }
        } catch (Exception e) {
            System.err.println("❌ Error al cargar llaves de Cloudinary: " + e.getMessage());
        }
    }

    public static String subirImagen(File archivoFisico){
        if(CLOUDINARY == null){
            return DEFAULT_IMAGE;
        }
        if(archivoFisico == null || !archivoFisico.exists()){
            return DEFAULT_IMAGE;
        }
        try{
            Map respuesta = CLOUDINARY.uploader().upload(archivoFisico, ObjectUtils.asMap(
                    "folder","menu",
                    "use_filename", true,
                    "unique_filename",true
            ));
            return respuesta.get("secure_url").toString();
        }catch (Exception e){
            System.out.println("Error al subir la imagen: " + e.getMessage());
            return DEFAULT_IMAGE;
        }
    }
    public static String descargarImagen(String urlImagen, int idProducto){
        if (urlImagen == null || !urlImagen.startsWith("http")) return urlImagen;
        String extension = urlImagen.contains(".png") ? ".png" : ".jpg";
        String nombreArchivo = "producto_"+ idProducto + extension;
        File destino = new File(UtilidadesImagen.DIRECTORIO_IMAGENES, nombreArchivo);
        if(destino.exists()){
            return destino.getAbsolutePath();
        }
        try(InputStream in = new URL(urlImagen).openStream();
            FileOutputStream out = new FileOutputStream(destino)){
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            System.out.println("Imagen descargada: " + destino.getAbsolutePath());
            return destino.getAbsolutePath();
        }catch (Exception e){
            System.out.println("Error al descargar imagen: " + e.getMessage());
            return urlImagen;
        }
    }
    public static void sincronizarImagenesPendientes(){
        if(CLOUDINARY == null){
            System.out.println("Cloudinary inactivo: no se sincronizaron las imánes.");
            return;
        }
        try {
            ArrayList<Producto> productos = ProductoDAO.obtenerMenuCompleto();
            for(Producto p : productos){
                String imagenActual = p.getImagenURL();
                if(imagenActual != null && !imagenActual.startsWith("http")) {
                    String rutaLocal = descargarImagen(imagenActual, p.getId());
                    if (!rutaLocal.equals(imagenActual)) {
                        ConexionSQLite.actualizarImagenProductoLocal(p.getId(), rutaLocal);
                        p.setImagenURL(rutaLocal);
                    }
                    continue;
                }
                if(imagenActual != null && !imagenActual.equals(DEFAULT_IMAGE) && !imagenActual.startsWith("http")){

                    String nombreArchivo = new File(imagenActual).getName();
                    File archivoLocal = new File(UtilidadesImagen.DIRECTORIO_IMAGENES, nombreArchivo);
                    if(archivoLocal.exists()){
                        String urlNube = subirImagen(archivoLocal);
                        if(urlNube != null && urlNube.startsWith("http")){
                            ProductoDAO.actualizarImagenProducto(p.getId(), urlNube);
                            ConexionSQLite.actualizarImagenProductoLocal(p.getId(), urlNube);
                            System.out.println("Imagen sincronizada para producto ID " + p.getId() + ": " + urlNube);
                        }
                    }else{
                        System.out.println("Archivo local no encontrado: " + archivoLocal.getAbsolutePath());
                        ConexionSQLite.actualizarImagenProductoLocal(p.getId(), DEFAULT_IMAGE);
                        ProductoDAO.actualizarImagenProducto(p.getId(), DEFAULT_IMAGE);
                    }
                }
            }
        }catch (Exception e) {
            System.out.println("Error al sincronizar imágenes pendientes: " + e.getMessage());
        }
    }
}
