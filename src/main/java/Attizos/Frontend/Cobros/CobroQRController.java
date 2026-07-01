package Attizos.Frontend.Cobros;

import javafx.application.Platform;
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
    private volatile boolean esperandoPago = true;
    private final PasarelaQrService servicioPagos = FabricarPasarelasPago.obtenerPasarelaActiva();

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
        lblEstado.setStyle("-fx-text-fill: #555555;");
        progressIndicador.setVisible(true);
        pagoCompletado = false;
        esperandoPago = true;

        int idTransaccion = (int) (Math.random() * 10000);

        new Thread(() -> {
            try {
                String urlQR = servicioPagos.solicitarQrDinamico(montoTotal, String.valueOf(idTransaccion));
                javafx.scene.image.Image imagenQR = new javafx.scene.image.Image(urlQR, true);

                imagenQR.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                    if (newProgress.doubleValue() == 1.0 && !imagenQR.isError()) {
                        javafx.application.Platform.runLater(() -> {
                            imgQR.setImage(imagenQR);
                            lblEstado.setText("QR Listo. Esperando pago desde la App del banco...");
                            iniciarSondeoDePago(String.valueOf(idTransaccion));
                        });
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    lblEstado.setText("Error al conectar con la pasarela.");
                    lblEstado.setStyle("-fx-text-fill: #ff4c4c;");
                    progressIndicador.setVisible(false);
                });
            }
        }).start();
    }
    private void iniciarSondeoDePago(String idTransaccion) {
        new Thread(() -> {
            try {
                while (esperandoPago) {
                    Thread.sleep(4000);
                    if (!esperandoPago) break;

                    boolean pagado = servicioPagos.verificarPago(idTransaccion);

                    if (pagado) {
                        esperandoPago = false;
                        javafx.application.Platform.runLater(() -> {
                            progressIndicador.setVisible(false);
                            lblEstado.setText("¡Pago Verificado en el Banco!");
                            lblEstado.setStyle("-fx-text-fill: #218c4e; -fx-font-size: 16px;");

                            new Thread(() -> {
                                try { Thread.sleep(1500); } catch (Exception e) {}
                                javafx.application.Platform.runLater(() -> {
                                    pagoCompletado = true;
                                    cerrarVentana();
                                });
                            }).start();
                        });
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ Sondeo interrumpido.");
            }
        }).start();
    }
    @FXML
    void simularPagoExitoso(ActionEvent event) {
        esperandoPago = false;
        pagoCompletado = true;
        progressIndicador.setVisible(false);
        lblEstado.setText("¡Pago Confirmado!");
        lblEstado.setStyle("-fx-text-fill: #218c4e; -fx-font-size: 16px;"); // Verde éxito
        new Thread(() ->{
            try{ Thread.sleep(1000);}catch (Exception e){}
            Platform.runLater(this::cerrarVentana);
        }).start();
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
