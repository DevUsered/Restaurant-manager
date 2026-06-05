package Attizos.Frontend;

import Attizos.Backend.Attizos.App;
import Attizos.Backend.Attizos.Insumo;
import Attizos.Backend.Attizos.Producto;
import Attizos.Backend.Listas.NodoDE;
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
    @FXML private FlowPane flowCategorias; // Reemplaza al ComboBox

    private String categoriaActiva = "Todo";

    @FXML
    public void initialize(){
        updateCategories();
        filterAndDisplay("Todo");
    }

    private void updateCategories(){
        flowCategorias.getChildren().clear();

        // 1. Botón "Todo" por defecto
        Button btnAll = crearBotonCategoria("Todo");
        flowCategorias.getChildren().add(btnAll);

        // 2. Extraemos las categorías únicas del menú
        Set<String> categorias = new HashSet<>();
        NodoDE<Producto> actual = App.attizos.getMenu().getCabeza();
        while(actual != null){
            categorias.add(actual.getDato().getCategoria());
            actual = actual.getSiguiente();
        }


        for (String cat : categorias) {
            flowCategorias.getChildren().add(crearBotonCategoria(cat));
        }
    }

    // Método auxiliar para no repetir código creando botones
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

        NodoDE<Producto> actual = App.attizos.getMenu().getCabeza();
        while(actual != null){
            Producto p = actual.getDato();
            if(p.getEstado() != null && p.getEstado().equals("Activo")) {
                if (categorie.equals("Todo") || p.getCategoria().equalsIgnoreCase(categorie)) {
                    StackPane newCard = createCard(p);
                    containerProducts.getChildren().add(newCard);
                }
            }
            actual = actual.getSiguiente();
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

        StringBuilder infoTexto = new StringBuilder();

        if (p.tieneReceta() && p.getReceta() != null && App.attizos.getInventario() != null) {
            infoTexto.append("✨ Preparado con:\n\n");
            for (String codInsumo : p.getReceta().getIngredientes().keySet()) {
                Insumo ins = App.attizos.getInventario().buscarInsumo(codInsumo);
                if (ins != null) {
                    infoTexto.append(" * ").append(ins.getNombre()).append("\n");
                }
            }
        }
        if(infoTexto.length() == 0){
            infoTexto.append("✨ Especialidad de la casa\n¡Listo para disfrutar!");
        }
        lblInfo.setText(infoTexto.toString());
        overlay.getChildren().add(lblInfo);

        // --- ANIMACIONES ---
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), overlay);
        fadeIn.setToValue(1.0);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), overlay);
        fadeOut.setToValue(0.0);

        // Eventos del mouse
        card.setOnMouseEntered(e -> {
            fadeOut.stop();
            fadeIn.play();
            card.setStyle("-fx-scale-x: 1.05; -fx-scale-y: 1.05;"); // Efecto zoom a la tarjeta
        });

        card.setOnMouseExited(e -> {
            fadeIn.stop();
            fadeOut.play();
            card.setStyle("-fx-scale-x: 1.0; -fx-scale-y: 1.0;"); // Volver a la normalidad
        });

        card.getChildren().addAll(content, overlay);

        return card;
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