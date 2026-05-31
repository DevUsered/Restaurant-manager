package Attizos.Frontend;

import Attizos.Backend.Attizos.*;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;

public class DashboardController {
    @FXML private AnchorPane rootPane;
    @FXML private VBox sidebar;
    @FXML private Button btnToggle;
    @FXML private Label lblName;
    @FXML private Label lblCargo;
    @FXML private VBox vBCajero;
    @FXML private VBox vBAdmin;
    @FXML private Label lblAdmin;
    @FXML private StackPane contentArea;

    private boolean menuAbierto = true;
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        if (rootPane != null && !rootPane.getChildren().isEmpty()) {
            HBox contenedor = (HBox) rootPane.getChildren().get(0);
            contenedor.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 25; -fx-border-color: rgba(218, 165, 32, 0.4); -fx-border-width: 1.5; -fx-border-radius: 25;");
        }
        configurarVentana();
        cargarPVentas();

        Platform.runLater(() -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

            stage.setWidth(screenBounds.getWidth() * 0.85);
            stage.setHeight(screenBounds.getHeight() * 0.85);

            stage.setMinWidth(950);
            stage.setMinHeight(600);
            stage.centerOnScreen();
        });

        if(App.usuarioLogueado != null){
            lblName.setText(App.usuarioLogueado.getNombre());
            lblCargo.setText(App.usuarioLogueado.getCargo());
            restriccion();
        } else {
            lblName.setText("Modo Prueba");
            lblCargo.setText("Desarrollador");
        }
    }

    private void configurarVentana() {
        sidebar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        sidebar.setOnMouseDragged(event -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            if(!stage.isMaximized()) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });

        rootPane.setOnMousePressed(event -> {
            if (event.getTarget() == rootPane || event.getTarget().getClass().getSimpleName().equals("AnchorPane")) {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            }
        });
        rootPane.setOnMouseDragged(event -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            if(!stage.isMaximized() && (event.getTarget() == rootPane || event.getTarget().getClass().getSimpleName().equals("AnchorPane"))) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });
    }

    @FXML
    void toggleMenu(ActionEvent event) {
        Timeline timeline = new Timeline();
        double anchoFinal = menuAbierto ? 0 : 260;
        double opacidadFinal = menuAbierto ? 0 : 1;
        double rotacionBoton = menuAbierto ? 90 : 0;
        double desplazamientoBoton = menuAbierto ? -5 : 0;

        KeyValue kvWidth = new KeyValue(sidebar.prefWidthProperty(), anchoFinal, Interpolator.EASE_BOTH);
        KeyValue kvMinWidth = new KeyValue(sidebar.minWidthProperty(), anchoFinal, Interpolator.EASE_BOTH);
        KeyValue kvOpacity = new KeyValue(sidebar.opacityProperty(), opacidadFinal, Interpolator.EASE_BOTH);
        KeyValue kvRotate = new KeyValue(btnToggle.rotateProperty(), rotacionBoton, Interpolator.EASE_BOTH);
        KeyValue kvTranslate = new KeyValue(btnToggle.translateXProperty(), desplazamientoBoton, Interpolator.EASE_BOTH);

        KeyFrame frame = new KeyFrame(Duration.millis(300), kvWidth, kvMinWidth, kvOpacity, kvRotate, kvTranslate);
        timeline.getKeyFrames().add(frame);
        timeline.play();
        menuAbierto = !menuAbierto;
    }

    @FXML
    void cerrarVentana(ActionEvent event){
        System.exit(0);
    }

    @FXML
    void minimizarVentana(ActionEvent event){
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    void maximizarVentana(ActionEvent event) {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        HBox contenedor = (HBox) rootPane.getChildren().get(0);

        if (stage.isMaximized()) {
            stage.setMaximized(false);
            AnchorPane.setTopAnchor(contenedor, 20.0);
            AnchorPane.setBottomAnchor(contenedor, 20.0);
            AnchorPane.setLeftAnchor(contenedor, 20.0);
            AnchorPane.setRightAnchor(contenedor, 20.0);
            contenedor.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 25; -fx-border-color: rgba(218, 165, 32, 0.4); -fx-border-width: 1.5; -fx-border-radius: 25");
        } else {
            stage.setMaximized(true);
            AnchorPane.setTopAnchor(contenedor, 0.0);
            AnchorPane.setBottomAnchor(contenedor, 0.0);
            AnchorPane.setLeftAnchor(contenedor, 0.0);
            AnchorPane.setRightAnchor(contenedor, 0.0);
            contenedor.setStyle("-fx-background-color: #fdf6e3; -fx-background-radius: 0; -fx-border-radius: 0; -fx-border-color: transparent");
        }
    }

    @FXML void openSales(ActionEvent event) { cargarPVentas(); }

    private void cargarPVentas(){
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Sales.fxml"));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (Exception e){ e.printStackTrace(); }
    }

    @FXML
    void openOrders(ActionEvent event){
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Cocina.fxml"));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (Exception e){ e.printStackTrace(); }
    }

    @FXML
    void openReservations(ActionEvent event){
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Reservas.fxml"));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (Exception e){ e.printStackTrace(); }
    }

    @FXML
    void openProducts(ActionEvent event){
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Productos.fxml"));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
            Stage stage = (Stage) rootPane.getScene().getWindow();
            if(!stage.isMaximized()) {
                stage.sizeToScene();
                stage.centerOnScreen();
            }
        } catch (Exception e){ e.printStackTrace(); }
    }

    @FXML void openInventory(ActionEvent event) {
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Inventario.fxml"));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
            Stage stage = (Stage) rootPane.getScene().getWindow();
            if(!stage.isMaximized()){
                stage.sizeToScene();
                stage.centerOnScreen();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @FXML void openEmploys(ActionEvent event) {
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Empleados.fxml"));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
            Stage stage = (Stage) rootPane.getScene().getWindow();
            if(!stage.isMaximized()){
                stage.sizeToScene();
                stage.centerOnScreen();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @FXML void openReports(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Reportes.fxml"));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
            Stage stage = (Stage) rootPane.getScene().getWindow();
            if (!stage.isMaximized()) {
                stage.sizeToScene();
                stage.centerOnScreen();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void restriccion(){
        if(App.usuarioLogueado != null && !App.usuarioLogueado.getCargo().equalsIgnoreCase("Administrador") && !App.usuarioLogueado.getCargo().equalsIgnoreCase("Admin"))
        {
            vBAdmin.setVisible(false);
            vBAdmin.setManaged(false);
            lblAdmin.setVisible(false);
            lblAdmin.setManaged(false);
        }
    }

    @FXML
    void logout(ActionEvent event){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Home.fxml"));
            Stage stage = new Stage();
            Parent root = loader.load();
            stage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.show();


            Stage vAc = (Stage) ((Node) event.getSource()).getScene().getWindow();
            vAc.close();
        } catch(IOException e ){
            e.printStackTrace();
        }
    }
}