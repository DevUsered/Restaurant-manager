package Attizos.Frontend;
import javafx.scene.image.Image;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
public class UtilidadesImagen {
    public static String convertirImagenABase64(File archivo){
        if(archivo == null) return null;
        try{
            byte[] fileContent = Files.readAllBytes(archivo.toPath());
            return Base64.getEncoder().encodeToString(fileContent);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
    public static Image convertirBase64AImagen(String base64String) {
        if (base64String == null || base64String.trim().isEmpty() || base64String.equals("default.png")) {
            return null;
        }
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64String);
            return new Image(new ByteArrayInputStream(decodedBytes));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
