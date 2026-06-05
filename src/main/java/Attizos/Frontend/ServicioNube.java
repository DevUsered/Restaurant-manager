package Attizos.Frontend;

import Attizos.Backend.Attizos.Producto;
import Attizos.Backend.Database.ProductoDAO;
import Attizos.Backend.Listas.ListaDE;
import Attizos.Backend.Listas.NodoDE;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.util.Properties;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

public class ServicioNube {
    private static  Cloudinary CLOUDINARY;
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
        if(archivoFisico == null || !archivoFisico.exists()){
            return "default.png";
        }
        try{
            Map respuesta = CLOUDINARY.uploader().upload(archivoFisico, ObjectUtils.asMap(
                    "folder","menu",
                    "use_filename", true,
                    "unique_filename",true
            ));

            String urlEnLaNube = respuesta.get("secure_url").toString();
            System.out.println("Imagen subida con exito: " + urlEnLaNube);
            return urlEnLaNube;
        }catch (Exception e){
            System.out.println("Error al subir la imagen: " + e.getMessage());
            return "default.png";
        }
    }
    public static void sincronizarImagenesPendientes(){
        try {
            ListaDE<Producto> productos = ProductoDAO.obtenerMenuCompleto();
            NodoDE<Producto> ac = productos.getCabeza();

            while(ac != null){
                Producto p = ac.getDato();
                String imagenActual = p.getImagenURL();

                if(imagenActual != null && !imagenActual.startsWith("http") && !imagenActual.equals("default.png")){
                    File archivoLocal = new File(UtilidadesImagen.DIRECTORIO_IMAGENES, imagenActual);

                    if(archivoLocal.exists()){
                        System.out.println("Subiendo imagen pendiente");
                        String urlNube = subirImagen(archivoLocal);

                        if(urlNube.startsWith("http")){
                            p.setImagenURL(urlNube);
                            ProductoDAO.actualizarImagenProducto(p.getId(), urlNube);
                            System.out.println("Imagen sincronizada para producto: " + p.getNombre());
                        }
                    }
                }
                ac = ac.getSiguiente();
            }
        }catch (Exception e) {
            System.out.println("Error al sincronizar imágenes pendientes: " + e.getMessage());
        }
    }
}
