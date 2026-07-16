package Attizos.Frontend;

import Attizos.Backend.Attizos.App;
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
    @FXML private ImageView logoEmpresa;

    public void initialize(){
        logoEmpresa.setImage(App.getLogoImageCache());
    }

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
        boolean accesoConsedido = App.autenticarUsuario(user, pass);
        if(accesoConsedido){
            lblMensaje.setStyle("-fx-text-fill: #00ff88; -fx-font: bold 14px 'Arial';");
            lblMensaje.setText("!Acceso concedido!");

            String cargo = App.usuarioLogueado.getCargo();
            if(cargo.equalsIgnoreCase("Cocinero") || cargo.equalsIgnoreCase("Chef")){
                abrirCocina();
            }else{
                abrirDashboard();
            }
        }else{
            lblMensaje.setStyle("-fx-text-fill: #ff4c4c; -fx-border-color: #ff4cbc; -fx-border-width: 1; -fx-border-radius: 5; -fx-padding: 5; -fx-font: bold 14px 'Arial';");
            lblMensaje.setText("Usuario o contraseña incorrecto. \nPor favor intente de nuevo.");
            txtUsuario.clear();
            txtPassword.clear();
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
