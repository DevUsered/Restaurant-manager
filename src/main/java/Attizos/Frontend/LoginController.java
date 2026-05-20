package Attizos.Frontend;

import Attizos.Backend.Attizos.App;
import Attizos.Backend.Attizos.Cocinero;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
public class LoginController {
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private Label lblMensaje;
    @FXML
    void enfocarPassword(ActionEvent event) {
        txtPassword.requestFocus();
    }

    @FXML
    void iniciarSesion(ActionEvent event){
        String user = txtUsuario.getText().trim();
        String pass = txtPassword.getText().trim();

        if(user.isEmpty() || pass.isEmpty()){
            lblMensaje.setText("Por favor, ingrese usuario y contraseña");
            return;
        }
        boolean accesoConcedido = App.validarAcceso(user, pass);
        if(accesoConcedido){
            lblMensaje.setText("-fx-text-fill: #00ff88;");
            lblMensaje.setText("!Acceso concedido! Cargando...");
            if(App.usuarioLogueado instanceof Cocinero){
                abrirCocina();
            }else {
                abrirDashboard();
            }
        }else{
            lblMensaje.setStyle("-fx-text-fill: #ff4c4c;");
            lblMensaje.setText("Usuario o contraseña incorrectos");
        }
    }
    private void abrirDashboard(){
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Dashboard.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.show();

            Stage stageAc = (Stage) btnLogin.getScene().getWindow();
            stageAc.close();
        }catch (IOException e) {
            e.printStackTrace();
            lblMensaje.setText("Error al cargar el sistema. ");
        }
    }
    private void abrirCocina(){
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Cocina.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

            Stage stageAc = (Stage) btnLogin.getScene().getWindow();
            stageAc.close();
        }catch (IOException e){
            e.printStackTrace();
            lblMensaje.setText("Error al cargar el sistema. ");
        }
    }

}
