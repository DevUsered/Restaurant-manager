package Attizos.Frontend;

import javafx.scene.image.Image;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

public class UtilidadesImagen {
    private static final HashMap<String, Image> cacheRAM = new HashMap<>();
    public static final String DIRECTORIO_IMAGENES = System.getenv("APPDATA") +
            File.separator + "Attizos" +
            File.separator + "Imagenes";
    static{
        File directorio = new File(DIRECTORIO_IMAGENES);
        if(!directorio.exists()){
            directorio.mkdirs();
        }
    }

    private static Image getDefaultImage(){
        if(!cacheRAM.containsKey("default")){
            try{
                InputStream is = UtilidadesImagen.class.getResourceAsStream("/images/default.png");
                if(is != null){
                    cacheRAM.put("default", new Image(is));
                }
            }catch (Exception e){
                System.out.println("Error al cargar imagen por defecto: " + e.getMessage());
            }
        }
        return cacheRAM.get("default");
    }
    public static Image obtenerImagenOptimizada(String rutaUrl){
        Image imgDefecto = getDefaultImage();
        if(rutaUrl == null || rutaUrl.trim().isEmpty() || rutaUrl.equals("default.png")){
            return imgDefecto;
        }
        if(cacheRAM.containsKey(rutaUrl)) {
            return cacheRAM.get(rutaUrl);
        }
        Image imagenFinal = null;
        try{
            String nombreArchivo = rutaUrl.replace("\\", "/");
            if (nombreArchivo.contains("/")) {
                nombreArchivo = nombreArchivo.substring(nombreArchivo.lastIndexOf("/") + 1);
            }
            File archivoLocal = new File(DIRECTORIO_IMAGENES, nombreArchivo);

            if(archivoLocal.exists()){
                imagenFinal = new Image(archivoLocal.toURI().toString(), true);
            }else if(rutaUrl.startsWith("http")){
                imagenFinal = new Image(rutaUrl, true);
            }
        }catch(Exception e){
            System.out.println("Error al cargar imagen optimizada: " + e.getMessage());
        }
        if(imagenFinal == null || imagenFinal.isError()){
            imagenFinal = imgDefecto;
        }else{
            cacheRAM.put(rutaUrl, imagenFinal);
        }
        return imagenFinal;
    }


    public static String guardarImagenLocal(File archivo, String nombreProducto){
        if(archivo == null || !archivo.exists()){
            return "default.png";
        }
        try{
            File directorio = new File(DIRECTORIO_IMAGENES);
            if(!directorio.exists()){
                directorio.mkdirs();
            }
            String extension = archivo.getName().substring(archivo.getName().lastIndexOf("."));
            String nombreArchivo = nombreProducto.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
            String nombreFinal = nombreArchivo + "_"+System.currentTimeMillis() + extension;

            Path destino = Paths.get(DIRECTORIO_IMAGENES, nombreFinal);
            Files.copy(archivo.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
            return nombreFinal;
        }catch (IOException e){
            System.out.println("Error al guardar la imagen: " + e.getMessage());
            return "default.png";
        }
    }
    public static Image cargarImagenLocal(String nombreArchivo){
        if (nombreArchivo == null || nombreArchivo.equals("default.png") || nombreArchivo.trim().isEmpty()) {
            return new Image(UtilidadesImagen.class.getResourceAsStream("/images/default.png"));
        }
        File archivoLocal = new File(DIRECTORIO_IMAGENES, nombreArchivo);
        if (archivoLocal.exists()) {
            return new Image(archivoLocal.toURI().toString());
        }else{
            return new Image(UtilidadesImagen.class.getResourceAsStream("/images/default.png"));
        }
    }
    public static Image cargarImagencache(String urlNube){
        if (urlNube == null || urlNube.trim().isEmpty() || urlNube.equals("default.png")) {
            return new Image(UtilidadesImagen.class.getResourceAsStream("/images/default.png"));
        }
        try{
            String nombreArchivo = urlNube.substring(urlNube.lastIndexOf("/") + 1);

            File directorio = new File(DIRECTORIO_IMAGENES);
            if(!directorio.exists()) directorio.mkdirs();

            File archivoLocal = new File(directorio, nombreArchivo);
            if (archivoLocal.exists()) {
                return new Image(archivoLocal.toURI().toString());
            }
            if(urlNube.startsWith("http")){
                System.out.println("Descargando nueva imagen al caché local. "+nombreArchivo);

                try(InputStream in = new java.net.URL(urlNube).openStream()){
                    Files.copy(in, archivoLocal.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    return new Image(archivoLocal.toURI().toString());
                }
            }
        }catch (Exception e){
            System.out.println("Error al cargar la imagen desde la nube: " + e.getMessage());
        }
        return new Image(UtilidadesImagen.class.getResourceAsStream("/images/default.png"));
    }
}
