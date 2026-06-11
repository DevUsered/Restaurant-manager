package Attizos.Frontend;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
public class AlertaPersonalizada {
    public static void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Stage dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.initModality(Modality.APPLICATION_MODAL); // Bloquea la ventana de atrás

        // Configuramos colores e íconos por defecto (Warning - Dorado)
        String colorAcento = "#DAA520";
        String iconoText = "⚠";

        // Cambiamos si es Error (Rojo) o Información (Verde)
        if (tipo == Alert.AlertType.ERROR) {
            colorAcento = "#ff4c4c";
            iconoText = "❌";
        } else if (tipo == Alert.AlertType.INFORMATION) {
            colorAcento = "#218c4e";
            iconoText = "✅";
        }

        VBox cajaDialogo = new VBox(15);
        cajaDialogo.setAlignment(Pos.CENTER);
        cajaDialogo.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20; -fx-border-color: " + colorAcento + "; -fx-border-width: 2; -fx-border-radius: 20; -fx-padding: 30; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0, 0, 5);");
        cajaDialogo.setPrefWidth(350);

        Label lblIcono = new Label(iconoText);
        lblIcono.setStyle("-fx-font-size: 40px; -fx-text-fill: " + colorAcento + ";");

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #111111;");

        Label lblMensaje = new Label(mensaje);
        lblMensaje.setStyle("-fx-font-size: 15px; -fx-text-fill: #555555;");
        lblMensaje.setWrapText(true);
        lblMensaje.setTextAlignment(TextAlignment.CENTER);

        Button btnOk = new Button("Entendido");
        btnOk.setStyle("-fx-background-color: " + colorAcento + "; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 10; -fx-padding: 10 25; -fx-cursor: hand;");
        btnOk.setOnAction(e -> dialogStage.close());

        cajaDialogo.getChildren().addAll(lblIcono, lblTitulo, lblMensaje, btnOk);

        StackPane root = new StackPane(cajaDialogo);
        root.setStyle("-fx-background-color: transparent; -fx-padding: 20;");

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialogStage.setScene(scene);

        // Permitir arrastrar la alerta
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
    }
    public static boolean mostrarConfirmacion(String titulo, String mensaje) {
        // Variable en forma de arreglo para poder modificarla dentro de los eventos de los botones
        final boolean[] respuesta = {false};

        Stage dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.initModality(Modality.APPLICATION_MODAL);

        String colorAcento = "#f39c12"; // Naranja para advertencias o dudas de IA
        String iconoText = "❓";

        VBox cajaDialogo = new VBox(15);
        cajaDialogo.setAlignment(Pos.CENTER);
        cajaDialogo.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20; -fx-border-color: " + colorAcento + "; -fx-border-width: 2; -fx-border-radius: 20; -fx-padding: 30; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0, 0, 5);");
        cajaDialogo.setPrefWidth(400); // Un poco más ancha para que quepan dos botones

        Label lblIcono = new Label(iconoText);
        lblIcono.setStyle("-fx-font-size: 40px; -fx-text-fill: " + colorAcento + ";");

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #111111;");

        Label lblMensaje = new Label(mensaje);
        lblMensaje.setStyle("-fx-font-size: 15px; -fx-text-fill: #555555;");
        lblMensaje.setWrapText(true);
        lblMensaje.setTextAlignment(TextAlignment.CENTER);

        // Contenedor horizontal para los dos botones
        HBox cajaBotones = new HBox(15);
        cajaBotones.setAlignment(Pos.CENTER);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: #333333; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 10; -fx-padding: 10 25; -fx-cursor: hand;");
        btnCancelar.setOnAction(e -> {
            respuesta[0] = false; // El usuario dijo NO
            dialogStage.close();
        });

        Button btnConfirmar = new Button("Continuar");
        btnConfirmar.setStyle("-fx-background-color: " + colorAcento + "; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 10; -fx-padding: 10 25; -fx-cursor: hand;");
        btnConfirmar.setOnAction(e -> {
            respuesta[0] = true; // El usuario dijo SÍ
            dialogStage.close();
        });

        cajaBotones.getChildren().addAll(btnCancelar, btnConfirmar);

        cajaDialogo.getChildren().addAll(lblIcono, lblTitulo, lblMensaje, cajaBotones);

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

        return respuesta[0];
    }
}
