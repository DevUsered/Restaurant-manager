package Attizos.Frontend;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class CobroQRController {
    @FXML private AnchorPane rootPane;
    @FXML private Label lblMonto;
    @FXML private ImageView imgQR;
    @FXML private Label lblEstado;
    @FXML private ProgressIndicator progressIndicador;
    @FXML private Button btnCancelar;

    private double xOffset = 0;
    private double yOffset = 0;

    private boolean pagoCompletado = false;

    @FXML
    public void initialize(){
        if(rootPane != null){
            rootPane.setOnMousePressed(event ->{
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });
            rootPane.setOnMouseDragged(event ->{
                Stage stage = (Stage) rootPane.getScene().getWindow();
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            });
        }
    }
    public void inicializarCobro(double montoTotal) {
        lblMonto.setText(String.format("Bs. %.2f", montoTotal));
        lblEstado.setText("Generando código QR seguro...");
        lblEstado.setStyle("-fx-text-fill: #555555;");// Gris
        progressIndicador.setVisible(true);
        pagoCompletado = false;

        int idTransaccion = (int) (Math.random() * 10000);
        String urlQR = ServicioPagosQR.generarLinkQr(montoTotal, idTransaccion);
        Image imagenQR = new Image(urlQR, true);
        imagenQR.progressProperty().addListener((obs, oldProgress, newProgress) -> {
            if (newProgress.doubleValue() == 1.0) {
                if (!imagenQR.isError()) {
                    imgQR.setImage(imagenQR);
                    lblEstado.setText("QR Listo. Esperando pago desde la App del banco...");
                } else {
                    lblEstado.setText("Error al generar el QR. Verifique su internet.");
                    lblEstado.setStyle("-fx-text-fill: #ff4c4c;"); // Rojo error
                    progressIndicador.setVisible(false);
                }
            }
        });
    }
    @FXML
    void simularPagoExitoso(ActionEvent event) {
        // Simulamos que el "Hilo de Sondeo" detectó el pago en el banco
        pagoCompletado = true;

        // Cambiamos la interfaz a verde
        progressIndicador.setVisible(false);
        lblEstado.setText("¡Pago Confirmado!");
        lblEstado.setStyle("-fx-text-fill: #218c4e; -fx-font-size: 16px;"); // Verde éxito

        // Cerramos la ventana automáticamente después de 1 segundo para que el usuario vea el éxito
        javafx.application.Platform.runLater(() -> {
            try {
                Thread.sleep(1000);
                cerrarVentana();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    void cancelarPago(ActionEvent event) {
        // Si el cliente se arrepiente y quiere pagar en efectivo
        pagoCompletado = false;
        cerrarVentana();
    }

    private void cerrarVentana() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.close();
    }

    public boolean isPagoCompletado() {
        return pagoCompletado;
    }
}
