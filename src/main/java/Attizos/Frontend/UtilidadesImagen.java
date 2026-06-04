package Attizos.Frontend;

import javafx.scene.image.Image;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
public class UtilidadesImagen {

    private static final String DIRECTORIO_IMAGENES = System.getenv("APPDATA") +
                                                        File.separator + "Attizos" +
                                                        File.separator + "Imagenes";

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
}
