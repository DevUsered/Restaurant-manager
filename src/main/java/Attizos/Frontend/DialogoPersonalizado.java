package Attizos.Frontend;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Optional;
public class DialogoPersonalizado {
    public static Optional<String> mostrarDialogo(String titulo, String encabezado, String mensaje, String valorPorDefecto) {
        Stage dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.initModality(Modality.APPLICATION_MODAL); // Bloquea la ventana de atrás

        String[] resultado = new String[1]; // Array para guardar la respuesta

        // Contenedor principal de la tarjeta de diálogo
        VBox cajaDialogo = new VBox(15);
        cajaDialogo.setAlignment(Pos.CENTER_LEFT);
        cajaDialogo.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20; -fx-border-color: #DAA520; -fx-border-width: 2; -fx-border-radius: 20; -fx-padding: 30; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0, 0, 5);");
        cajaDialogo.setPrefWidth(400);

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #111111;");

        Label lblEncabezado = new Label(encabezado);
        lblEncabezado.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #DAA520;");
        lblEncabezado.setWrapText(true);

        Label lblMensaje = new Label(mensaje);
        lblMensaje.setStyle("-fx-font-size: 14px; -fx-text-fill: #555555;");
        lblMensaje.setWrapText(true);

        // Campo de entrada de texto moderno
        TextField txtInput = new TextField(valorPorDefecto != null ? valorPorDefecto : "");
        txtInput.setStyle("-fx-background-color: #F5F5F5; -fx-text-fill: #111111; -fx-border-color: #DDDDDD; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 14px;");

        // Efecto hover/focus para el TextField
        txtInput.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                txtInput.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #111111; -fx-border-color: #DAA520; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 14px;");
            } else {
                txtInput.setStyle("-fx-background-color: #F5F5F5; -fx-text-fill: #111111; -fx-border-color: #DDDDDD; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 14px;");
            }
        });

        // Botones
        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setStyle("-fx-background-color: transparent; -fx-border-color: #DDDDDD; -fx-border-width: 2; -fx-border-radius: 10; -fx-text-fill: #555555; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 10; -fx-padding: 8 20; -fx-cursor: hand;");
        btnCancelar.setOnAction(e -> {
            resultado[0] = null;
            dialogStage.close();
        });

        Button btnAceptar = new Button("Aceptar");
        btnAceptar.setStyle("-fx-background-color: linear-gradient(to right, #FFC107, #DAA520); -fx-text-fill: #111111; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 10; -fx-padding: 10 20; -fx-cursor: hand;");
        btnAceptar.setOnAction(e -> {
            resultado[0] = txtInput.getText();
            dialogStage.close();
        });

        // Permitir que "Enter" active el botón Aceptar
        txtInput.setOnAction(e -> btnAceptar.fire());

        HBox botones = new HBox(15, btnCancelar, btnAceptar);
        botones.setAlignment(Pos.CENTER_RIGHT);
        botones.setPadding(new Insets(10, 0, 0, 0));

        cajaDialogo.getChildren().addAll(lblTitulo, lblEncabezado, lblMensaje, txtInput, botones);

        StackPane root = new StackPane(cajaDialogo);
        root.setStyle("-fx-background-color: transparent; -fx-padding: 20;");

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialogStage.setScene(scene);

        // Permitir arrastrar el diálogo
        final double[] xOffset = {0};
        final double[] yOffset = {0};
        root.setOnMousePressed(event -> {
            xOffset[0] = event.getSceneX();
            yOffset[0] = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            dialogStage.setX(event.getScreenX() - xOffset[0]);
            dialogStage.setY(event.getScreenY() - yOffset[0]);
        });

        dialogStage.showAndWait();

        // Devolvemos el resultado empaquetado en un Optional (igual que TextInputDialog)
        return Optional.ofNullable(resultado[0]);
    }
    public static Optional<ButtonType> mostrarDialogoConfirmacion(String titulo, String encabezado, String mensaje) {
        Stage dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.initModality(Modality.APPLICATION_MODAL);

        ButtonType[] resultado = new ButtonType[]{ButtonType.CANCEL}; 

        VBox cajaDialogo = new VBox(15);
        cajaDialogo.setAlignment(Pos.CENTER_LEFT);
        cajaDialogo.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20; -fx-border-color: #DAA520; -fx-border-width: 2; -fx-border-radius: 20; -fx-padding: 30; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0, 0, 5);");
        cajaDialogo.setPrefWidth(400);

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #111111;");

        Label lblEncabezado = new Label(encabezado);
        lblEncabezado.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #DAA520;");
        lblEncabezado.setWrapText(true);

        Label lblMensaje = new Label(mensaje);
        lblMensaje.setStyle("-fx-font-size: 14px; -fx-text-fill: #555555;");
        lblMensaje.setWrapText(true);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setStyle("-fx-background-color: transparent; -fx-border-color: #DDDDDD; -fx-border-width: 2; -fx-border-radius: 10; -fx-text-fill: #555555; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 10; -fx-padding: 8 20; -fx-cursor: hand;");
        btnCancelar.setOnAction(e -> {
            resultado[0] = ButtonType.CANCEL;
            dialogStage.close();
        });

        Button btnAceptar = new Button("Aceptar");
        btnAceptar.setStyle("-fx-background-color: linear-gradient(to right, #FFC107, #DAA520); -fx-text-fill: #111111; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 10; -fx-padding: 10 20; -fx-cursor: hand;");
        btnAceptar.setOnAction(e -> {
            resultado[0] = ButtonType.OK;
            dialogStage.close();
        });

        HBox botones = new HBox(15, btnCancelar, btnAceptar);
        botones.setAlignment(Pos.CENTER_RIGHT);
        botones.setPadding(new Insets(10, 0, 0, 0));

        cajaDialogo.getChildren().addAll(lblTitulo, lblEncabezado, lblMensaje, botones);

        StackPane root = new StackPane(cajaDialogo);
        root.setStyle("-fx-background-color: transparent; -fx-padding: 20;");

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialogStage.setScene(scene);

        final double[] xOffset = {0};
        final double[] yOffset = {0};
        root.setOnMousePressed(event -> {
            xOffset[0] = event.getSceneX();
            yOffset[0] = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            dialogStage.setX(event.getScreenX() - xOffset[0]);
            dialogStage.setY(event.getScreenY() - yOffset[0]);
        });

        dialogStage.showAndWait();

        return Optional.of(resultado[0]);
    }

}
