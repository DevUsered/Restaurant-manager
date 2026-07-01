package Attizos.Frontend;

import Attizos.Backend.Attizos.App;
import Attizos.Backend.Database.ConexionSQLite;
import Attizos.Frontend.Network.WebSocketManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class MainGUI extends Application {
    @Override
    public void start(Stage ventanaPrincipal){
        try {
            Application.setUserAgentStylesheet(new atlantafx.base.theme.PrimerDark().getUserAgentStylesheet());
            App.iniciarSistema();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Home.fxml"));
            Parent root = loader.load();
            ventanaPrincipal.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            ventanaPrincipal.setScene(scene);
            ventanaPrincipal.setResizable(false);
            ventanaPrincipal.centerOnScreen();
            ventanaPrincipal.setOnCloseRequest(event ->{
                System.exit(0);
            });
            ventanaPrincipal.show();
        }catch (Exception e){
            System.out.println("Error crítico al arrancar el sistema. ");
            e.printStackTrace();
        }

    }
    public static void main(String[] args) {
        launch(args);
    }
}
