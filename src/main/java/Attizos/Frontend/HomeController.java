package Attizos.Frontend;

import Attizos.Backend.Attizos.App;
import Attizos.Backend.Attizos.Insumo;
import Attizos.Backend.Attizos.Producto;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.stage.StageStyle;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HomeController {
    @FXML private FlowPane containerProducts;
    @FXML private FlowPane flowCategorias;

    private String categoriaActiva = "Todo";

    @FXML
    public void initialize(){
        updateCategories();
        filterAndDisplay("Todo");
    }

    private void updateCategories(){
        flowCategorias.getChildren().clear();


        Button btnAll = crearBotonCategoria("Todo");
        flowCategorias.getChildren().add(btnAll);

        // 2. Extraemos las categorías únicas del menú
        Set<String> categorias = new HashSet<>();
        for(Producto p : App.attizos.getMenu()){
            if(p.getEstado() != null && p.getEstado().equals("Activo") && !p.isPromocion() && !p.getCategoria().equalsIgnoreCase("Promocion")) {
                categorias.add(p.getCategoria());
            }
        }
        for (String cat : categorias) {
            flowCategorias.getChildren().add(crearBotonCategoria(cat));
        }
    }

    private Button crearBotonCategoria(String nombreCat) {
        Button btn = new Button(nombreCat);
        // Aplica el color dorado si es la categoría actual
        btn.getStyleClass().add(nombreCat.equals(categoriaActiva) ? "menu-button-active" : "menu-button");

        btn.setOnAction(e -> {
            categoriaActiva = nombreCat;
            updateCategories(); // Repintamos los botones para actualizar el color del seleccionado
            filterAndDisplay(categoriaActiva);
        });
        return btn;
    }

    private void filterAndDisplay(String categorie){
        containerProducts.getChildren().clear();
        for(Producto p : App.attizos.getMenu()){
            if(p.getEstado() != null && p.getEstado().equals("Activo") && !p.getCategoria().equalsIgnoreCase("Promocion")) {
                if (categorie.equals("Todo") || p.getCategoria().equalsIgnoreCase(categorie)) {
                    StackPane newCard = createCard(p);
                    containerProducts.getChildren().add(newCard);
                }
            }
        }
    }

    private StackPane createCard(Producto p){
        StackPane card = new StackPane();
        card.getStyleClass().add("product-card");
        card.setPrefSize(240, 260);

        VBox content = new VBox(15);
        content.setAlignment(javafx.geometry.Pos.CENTER);

        Label name = new Label(p.getNombre());
        name.getStyleClass().add("product-name");

        Label price = new Label(String.format("Bs.%.2f", p.getPrecio()));
        price.getStyleClass().add("product-price");

        ImageView imagen = new ImageView();
        String datoImagen = p.getImagenURL();
        Image imgOptimizada = UtilidadesImagen.obtenerImagenOptimizada(datoImagen);
        imagen.setImage(imgOptimizada);
        imagen.setFitHeight(120);
        imagen.setPreserveRatio(true);
        content.getChildren().addAll(name, imagen, price);

        VBox overlay = new VBox(15);
        overlay.getStyleClass().add("product-overlay");
        overlay.setAlignment(javafx.geometry.Pos.CENTER);
        overlay.setOpacity(0); // Oculto por defecto

        Label lblInfo = new Label();
        lblInfo.getStyleClass().add("overlay-text");
        lblInfo.setWrapText(true);

        String infoTexto = generarTextoIngredientes(p);
        lblInfo.setText(infoTexto);
        overlay.getChildren().add(lblInfo);

        //ANIMACIONES
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), overlay);
        fadeIn.setToValue(1.0);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), overlay);
        fadeOut.setToValue(0.0);
        card.setOnMouseEntered(e -> {
            fadeOut.stop();
            fadeIn.play();
            card.setStyle("-fx-scale-x: 1.05; -fx-scale-y: 1.05;");
        });

        card.setOnMouseExited(e -> {
            fadeIn.stop();
            fadeOut.play();
            card.setStyle("-fx-scale-x: 1.0; -fx-scale-y: 1.0;");
        });

        card.getChildren().addAll(content, overlay);

        return card;
    }
    private String generarTextoIngredientes(Producto p) {
        if (p.tieneReceta() && p.getReceta() != null && App.attizos.getInventario() != null) {
            StringBuilder sb = new StringBuilder("✨ Preparado con:\n\n");
            for (String codInsumo : p.getReceta().getIngredientes().keySet()) {
                Insumo ins = App.attizos.getInventario().buscarInsumo(codInsumo);
                if (ins != null) {
                    sb.append(" * ").append(ins.getNombre()).append("\n");
                }
            }
            return sb.toString();
        }
        return "✨ Especialidad de la casa\n¡Listo para disfrutar!";
    }

    @FXML
    void login(ActionEvent event){
        try{
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
            Stage stage = new Stage();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            scene.setFill(Color.TRANSPARENT);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.show();

            Stage ventanaActual = (Stage) ((Node) event.getSource()).getScene().getWindow();
            ventanaActual.close();
        }catch (IOException e){
            System.out.println("Error al cargar la ventana de Login: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML
    void cerrarApp(ActionEvent event) {
        System.exit(0);
    }
}