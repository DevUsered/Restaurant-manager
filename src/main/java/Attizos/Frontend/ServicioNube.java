package Attizos.Frontend;

import Attizos.Backend.Api.ApiClient;
import Attizos.Backend.Attizos.Producto;
import Attizos.Backend.Database.ConexionSQLite;
import Attizos.Backend.Database.ProductoDAO;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

public class ServicioNube {
    private static Cloudinary CLOUDINARY;
    private static final String DEFAULT_IMAGE = "default.png";
    

    private static Cloudinary getCloudinary() {
        if (CLOUDINARY == null && ApiClient.credenciales != null) {
            String cloudName = ApiClient.credenciales.getOrDefault("CLOUDINARY_CLOUD_NAME", "").trim();
            String apiKey    = ApiClient.credenciales.getOrDefault("CLOUDINARY_API_KEY", "").trim();
            String apiSecret = ApiClient.credenciales.getOrDefault("CLOUDINARY_API_SECRET", "").trim();

            if (!cloudName.isEmpty() && !apiKey.isEmpty() && !apiSecret.isEmpty()
                    && !cloudName.equals("PON_TU_CLOUD_NAME_AQUI")) {
                CLOUDINARY = new Cloudinary(ObjectUtils.asMap(
                        "cloud_name", cloudName,
                        "api_key", apiKey,
                        "api_secret", apiSecret
                ));
            } else {
                System.out.println("⚠️ Cloudinary inactivo: Credenciales vacías o incompletas.");
            }
        }
        return CLOUDINARY;
    }

    public static String subirImagen(File archivoFisico) {
        Cloudinary cloud = getCloudinary();
        if (cloud == null || archivoFisico == null || !archivoFisico.exists()) {
            return DEFAULT_IMAGE;
        }
        try {
            Map<?, ?> respuesta = cloud.uploader().upload(archivoFisico, ObjectUtils.asMap(
                    "folder", "menu",
                    "use_filename", true,
                    "unique_filename", true
            ));
            return respuesta.get("secure_url").toString();
        } catch (Exception e) {
            System.out.println("Error al subir la imagen: " + e.getMessage());
            return DEFAULT_IMAGE;
        }
    }

    public static String descargarImagen(String urlImagen, int idProducto) {
        if (urlImagen == null || !urlImagen.startsWith("http")) return urlImagen;
        String extension = urlImagen.contains(".png") ? ".png" : ".jpg";
        String nombreArchivo = "producto_" + idProducto + extension;
        File destino = new File(UtilidadesImagen.DIRECTORIO_IMAGENES, nombreArchivo);
        if (destino.exists()) {
            return destino.getAbsolutePath();
        }
        try (InputStream in = URI.create(urlImagen).toURL().openStream();
             FileOutputStream out = new FileOutputStream(destino)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            System.out.println("Imagen descargada: " + destino.getAbsolutePath());
            return destino.getAbsolutePath();
        } catch (Exception e) {
            System.out.println("Error al descargar imagen: " + e.getMessage());
            return urlImagen;
        }
    }

    public static synchronized void sincronizarImagenesPendientes() {
        Cloudinary cloud = getCloudinary();
        if (cloud == null) {
            System.out.println("Cloudinary inactivo: no se sincronizaron las imágenes.");
            return;
        }
        try {
            ArrayList<Producto> productos = ApiClient.obtenerProductosDelServidor();
            if (productos == null) return;

            for (Producto p : productos) {
                String imagenActual = p.getImagenURL();
                if (imagenActual != null && !imagenActual.equals(DEFAULT_IMAGE) && !imagenActual.startsWith("http")) {
                    String nombreArchivo = new File(imagenActual).getName();
                    File archivoLocal = new File(UtilidadesImagen.DIRECTORIO_IMAGENES, nombreArchivo);
                    if (archivoLocal.exists()) {
                        System.out.println("Subiendo imagen a Cloudinary: " + archivoLocal.getName());
                        String urlNube = subirImagen(archivoLocal);
                        if (urlNube != null && urlNube.startsWith("http")) {
                            p.setImagenURL(urlNube);
                            ApiClient.actualizarProductoEnServidor(p);
                            ConexionSQLite.actualizarImagenProductoLocal(p.getId(), urlNube);
                            System.out.println("Imagen sincronizada para producto ID " + p.getId() + ": " + urlNube);
                        }
                    } else {
                        System.out.println("Archivo local no encontrado: " + archivoLocal.getAbsolutePath());
                        ConexionSQLite.actualizarImagenProductoLocal(p.getId(), DEFAULT_IMAGE);
                        ProductoDAO.actualizarImagenProducto(p.getId(), DEFAULT_IMAGE);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error al sincronizar imágenes pendientes: " + e.getMessage());
        }
    }
}