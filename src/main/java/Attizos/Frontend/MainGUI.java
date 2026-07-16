package Attizos.Frontend;

import Attizos.Backend.Attizos.App;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import atlantafx.base.theme.PrimerLight;

import java.io.File;

public class MainGUI extends Application {
    @Override
    public void start(Stage ventanaPrincipal){
        try {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

            String rutaConfig = System.getenv("APPDATA") + File.separator + "Attizos" + File.separator + "configuracion.properties";
            File archivoConfig = new File(rutaConfig);
            FXMLLoader loader;
            if(!archivoConfig.exists()){
                System.out.println("Archivo de configuración no encontrado. Iniciando Wizard de configuración...");
                loader = new FXMLLoader(getClass().getResource("/fxml/setup.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root);
                scene.setFill(Color.TRANSPARENT);
                ventanaPrincipal.initStyle(StageStyle.TRANSPARENT);
                ventanaPrincipal.setScene(scene);
                ventanaPrincipal.setResizable(false);
                ventanaPrincipal.centerOnScreen();
                ventanaPrincipal.setOnCloseRequest(event ->{
                    System.exit(0);
                });
                ventanaPrincipal.show();
            }else {
                System.out.println("Archivo de configuración encontrado. Iniciando aplicación principal...");
                App.iniciarBackend();
                App.iniciarSistema();
                loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
                Parent root = loader.load();
                ventanaPrincipal.initStyle(StageStyle.TRANSPARENT);
                Scene scene = new Scene(root);
                scene.setFill(Color.TRANSPARENT);
                ventanaPrincipal.setScene(scene);
                ventanaPrincipal.setResizable(false);
                ventanaPrincipal.centerOnScreen();
                ventanaPrincipal.setOnCloseRequest(event -> {
                    System.exit(0);
                });
                ventanaPrincipal.show();
            }
        }catch (Exception e){
            System.out.println("Error crítico al arrancar el sistema. ");
            e.printStackTrace();
        }

    }
    public static void main(String[] args) {
        launch(args);
    }
}
