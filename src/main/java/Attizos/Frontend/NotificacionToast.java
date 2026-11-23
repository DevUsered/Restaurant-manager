package Attizos.Frontend;

import Attizos.Frontend.Config.ConfigurationApp;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class NotificacionToast {

    private static final double ANCHO_TOAST = 440;
    private static final double ALTO_ESTIMADO = 96;
    private static final double MARGEN_SUPERIOR = 18;
    private static final double SEPARACION = 10;

    // Cambia a true si prefieres esquina superior derecha en lugar de arriba al centro
    private static final boolean ALINEAR_DERECHA = false;

    // Estilos base reutilizables para el hover
    private static final String ESTILO_TARJETA_BASE =
            "-fx-background-color: rgba(253, 246, 227, 0.88);" + // Crema #FDF6E3 translúcido
            "-fx-background-radius: 20;" +
            "-fx-border-color: rgba(218, 165, 32, 0.65);" +      // Dorado #DAA520 suave
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 20;" +
            "-fx-font-family: 'Segoe UI';" +
            "-fx-cursor: hand;";

    private static final String ESTILO_TARJETA_HOVER =
            "-fx-background-color: rgba(253, 246, 227, 0.96);" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: #DAA520;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 20;" +
            "-fx-font-family: 'Segoe UI';" +
            "-fx-cursor: hand;";

    private static final String ESTILO_BTN_CERRAR =
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #555555;" +
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 50;" +
            "-fx-min-width: 30px;" +
            "-fx-min-height: 30px;" +
            "-fx-max-width: 30px;" +
            "-fx-max-height: 30px;" +
            "-fx-cursor: hand;";

    private static final String ESTILO_BTN_CERRAR_HOVER =
            "-fx-background-color: #FF4C4C;" +
            "-fx-text-fill: #FFFFFF;" +
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 50;" +
            "-fx-min-width: 30px;" +
            "-fx-min-height: 30px;" +
            "-fx-max-width: 30px;" +
            "-fx-max-height: 30px;" +
            "-fx-cursor: hand;";

    private static final List<Stage> toastsActivos = new ArrayList<>();
    private static Stage ownerOculto;
    private static AudioClip sonidoPedidoListo;

    public static void mostrarPedidoListo(int numeroTicket, String nombreCliente) {
        String clienteLimpio = (nombreCliente == null || nombreCliente.trim().isEmpty())
                ? "Cliente"
                : nombreCliente.trim();

        Runnable tarea = () -> crearYMostrarToast(numeroTicket, clienteLimpio);

        if (Platform.isFxApplicationThread()) {
            tarea.run();
        } else {
            Platform.runLater(tarea);
        }
    }

    private static void crearYMostrarToast(int numeroTicket, String nombreCliente) {
        Window ventanaPrevia = Window.getWindows().stream()
                .filter(w -> w.isFocused() && !toastsActivos.contains(w))
                .findFirst()
                .orElse(null);

        Stage owner = obtenerOwnerOculto();
        Stage toastStage = new Stage(StageStyle.TRANSPARENT);
        toastStage.initOwner(owner);
        toastStage.setAlwaysOnTop(true);

        // 1. Círculo dorado con ícono (estilo .menu-button-active / .profile-avatar)
        Label icono = new Label("🔔");
        icono.setStyle("-fx-font-size: 20px; -fx-text-fill: #111111;");
        StackPane circuloIcono = new StackPane(icono);
        circuloIcono.setMinSize(46, 46);
        circuloIcono.setMaxSize(46, 46);
        circuloIcono.setStyle(
                "-fx-background-color: linear-gradient(to right, #FFC107, #DAA520);" +
                "-fx-background-radius: 50;" +
                "-fx-effect: dropshadow(gaussian, rgba(218,165,32,0.35), 8, 0, 0, 2);"
        );

        // 2. Fila superior: estado verde + píldora dorada del ticket
        Label lblEstado = new Label("● PEDIDO LISTO");
        lblEstado.setStyle(
                "-fx-text-fill: #218c4e;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;"
        );

        Label lblTicket = new Label("Ticket #" + numeroTicket);
        lblTicket.setStyle(
                "-fx-background-color: rgba(218, 165, 32, 0.22);" +
                "-fx-border-color: rgba(218, 165, 32, 0.55);" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 2 9 2 9;" +
                "-fx-text-fill: #111111;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;"
        );

        HBox filaSuperior = new HBox(10, lblEstado, lblTicket);
        filaSuperior.setAlignment(Pos.CENTER_LEFT);

        // 3. Nombre del cliente grande y nítido
        Label lblCliente = new Label(nombreCliente);
        lblCliente.setWrapText(true);
        lblCliente.setStyle(
                "-fx-text-fill: #111111;" +
                "-fx-font-size: 21px;" +
                "-fx-font-weight: bold;"
        );

        VBox textos = new VBox(5, filaSuperior, lblCliente);
        textos.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textos, Priority.ALWAYS);

        // 4. Botón cerrar (idéntico a tu .btn-close)
        Button btnCerrar = new Button("✕");
        btnCerrar.setFocusTraversable(false);
        btnCerrar.setStyle(ESTILO_BTN_CERRAR);
        btnCerrar.setOnMouseEntered(e -> btnCerrar.setStyle(ESTILO_BTN_CERRAR_HOVER));
        btnCerrar.setOnMouseExited(e -> btnCerrar.setStyle(ESTILO_BTN_CERRAR));

        // 5. Tarjeta principal crema translúcida
        HBox tarjeta = new HBox(14, circuloIcono, textos, btnCerrar);
        tarjeta.setAlignment(Pos.CENTER_LEFT);
        tarjeta.setPrefWidth(ANCHO_TOAST);
        tarjeta.setMinHeight(Region.USE_PREF_SIZE);
        tarjeta.setPadding(new Insets(14, 16, 14, 16));
        tarjeta.setStyle(ESTILO_TARJETA_BASE);
        tarjeta.setEffect(new DropShadow(22, Color.rgb(0, 0, 0, 0.22)));

        tarjeta.setOnMouseEntered(e -> tarjeta.setStyle(ESTILO_TARJETA_HOVER));
        tarjeta.setOnMouseExited(e -> tarjeta.setStyle(ESTILO_TARJETA_BASE));

        // Contenedor transparente para no recortar la sombra
        StackPane root = new StackPane(tarjeta);
        root.setPadding(new Insets(14));
        root.setStyle("-fx-background-color: transparent;");
        root.setOpacity(0);
        root.setTranslateY(-24);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        toastStage.setScene(scene);

        AtomicBoolean cerrado = new AtomicBoolean(false);
        int segundosVisible = Math.max(2, ConfigurationApp.getSegundosNotificacion());
        PauseTransition autoCierre = new PauseTransition(Duration.seconds(segundosVisible));

        Runnable cerrarConAnimacion = () -> {
            if (!cerrado.compareAndSet(false, true)) return;
            autoCierre.stop();

            FadeTransition fadeOut = new FadeTransition(Duration.millis(220), root);
            fadeOut.setToValue(0);

            TranslateTransition slideOut = new TranslateTransition(Duration.millis(220), root);
            slideOut.setToY(-20);

            ParallelTransition salida = new ParallelTransition(fadeOut, slideOut);
            salida.setOnFinished(e -> {
                toastStage.close();
                toastsActivos.remove(toastStage);
                reordenarToasts();
            });
            salida.play();
        };

        autoCierre.setOnFinished(e -> cerrarConAnimacion.run());
        btnCerrar.setOnAction(e -> cerrarConAnimacion.run());
        tarjeta.setOnMouseClicked(e -> cerrarConAnimacion.run());

        toastsActivos.add(toastStage);
        toastStage.show();
        reordenarToasts();

        if (ventanaPrevia instanceof Stage stagePrevio) {
            Platform.runLater(stagePrevio::requestFocus);
        }

        FadeTransition fadeIn = new FadeTransition(Duration.millis(240), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(240), root);
        slideIn.setFromY(-24);
        slideIn.setToY(0);

        new ParallelTransition(fadeIn, slideIn).play();
        reproducirSonido();
        autoCierre.play();
    }

    private static void reordenarToasts() {
        Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();
        double yActual = pantalla.getMinY() + MARGEN_SUPERIOR;

        for (Stage stage : toastsActivos) {
            double ancho = stage.getWidth() > 0 ? stage.getWidth() : (ANCHO_TOAST + 28);
            double alto = stage.getHeight() > 0 ? stage.getHeight() : (ALTO_ESTIMADO + 28);

            double x = ALINEAR_DERECHA
                    ? pantalla.getMaxX() - ancho - 20
                    : pantalla.getMinX() + (pantalla.getWidth() - ancho) / 2.0;

            stage.setX(x);
            stage.setY(yActual);
            yActual += (alto - 16) + SEPARACION;
        }
    }

    private static Stage obtenerOwnerOculto() {
        if (ownerOculto == null) {
            ownerOculto = new Stage(StageStyle.UTILITY);
            ownerOculto.setOpacity(0);
            ownerOculto.setWidth(1);
            ownerOculto.setHeight(1);
            ownerOculto.setX(-10000);
            ownerOculto.setY(-10000);
            ownerOculto.show();
        }
        return ownerOculto;
    }

    private static void reproducirSonido() {
        try {
            if (sonidoPedidoListo == null) {
                URL ruta = NotificacionToast.class.getResource("/sounds/pedido_listo.mp3");
                if (ruta == null) {
                    ruta = NotificacionToast.class.getResource("/sounds/cocina.mp3");
                }
                if (ruta != null) {
                    sonidoPedidoListo = new AudioClip(ruta.toExternalForm());
                }
            }

            if (sonidoPedidoListo != null) {
                sonidoPedidoListo.play();
            }
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo reproducir el sonido de notificación: " + e.getMessage());
        }
    }
}