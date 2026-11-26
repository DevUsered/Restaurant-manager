package Attizos.Frontend.Cobros;

import Attizos.Backend.Api.ApiClient;
import Attizos.Frontend.Network.WebSocketManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private volatile boolean esperandoPago = false;
    private volatile String qrIdActual = null;
    private final AtomicBoolean cierreIniciado = new AtomicBoolean(false);

    // Estado para alternar entre QR de cobro y QR del enlace para el celular
    private Image imagenQrCobro = null;
    private Image imagenQrEnlace = null;
    private boolean mostrandoQrEnlace = false;
    private String textoEstadoCobroActual = "Generando código QR...";
    private Button btnAlternarEnlace;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @FXML
    public void initialize() {
        if (rootPane != null) {
            rootPane.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });
            rootPane.setOnMouseDragged(event -> {
                Stage stage = (Stage) rootPane.getScene().getWindow();
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            });

            agregarBarraVinculacionCelular();
        }
    }

    private void agregarBarraVinculacionCelular() {
        String urlCliente = obtenerUrlPantallaCliente();
        imagenQrEnlace = generarQrEnlaceLocal(urlCliente, 240);

        Label lblUrl = new Label("🔗 " + urlCliente);
        lblUrl.setStyle("-fx-text-fill: #555555; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");
        lblUrl.setTooltip(new Tooltip("Clic para copiar el enlace de la pantalla QR"));
        lblUrl.setOnMouseClicked(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(urlCliente);
            Clipboard.getSystemClipboard().setContent(content);
            lblUrl.setText("✅ Enlace copiado al portapapeles");
            new Thread(() -> {
                try { Thread.sleep(1800); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> lblUrl.setText("🔗 " + urlCliente));
            }).start();
        });

        btnAlternarEnlace = new Button("📱 Vincular celular");
        btnAlternarEnlace.setFocusTraversable(false);
        btnAlternarEnlace.setStyle(
                "-fx-background-color: rgba(218, 165, 32, 0.18);" +
                "-fx-border-color: #DAA520;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-text-fill: #111111;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 4 10;" +
                "-fx-cursor: hand;"
        );
        btnAlternarEnlace.setOnAction(e -> alternarVistaQr());

        HBox barraEnlace = new HBox(8, lblUrl, btnAlternarEnlace);
        barraEnlace.setAlignment(Pos.CENTER);
        barraEnlace.setPadding(new Insets(6, 10, 4, 10));

        // Si el FXML tiene un VBox principal dentro del AnchorPane, lo agregamos al final sin tapar botones
        if (rootPane.getChildren().size() == 1 && rootPane.getChildren().get(0) instanceof VBox contenedorPrincipal) {
            contenedorPrincipal.getChildren().add(barraEnlace);
        } else {
            AnchorPane.setBottomAnchor(barraEnlace, 6.0);
            AnchorPane.setLeftAnchor(barraEnlace, 12.0);
            AnchorPane.setRightAnchor(barraEnlace, 12.0);
            rootPane.getChildren().add(barraEnlace);
        }
    }

    private void alternarVistaQr() {
        mostrandoQrEnlace = !mostrandoQrEnlace;

        if (mostrandoQrEnlace && imagenQrEnlace != null) {
            imgQR.setImage(imagenQrEnlace);
            lblEstado.setText("Escanea con la cámara del celular para abrir la pantalla QR");
            lblEstado.setStyle("-fx-text-fill: #8a6400; -fx-font-weight: bold;");
            btnAlternarEnlace.setText("💳 Ver QR de cobro");
        } else {
            if (imagenQrCobro != null) {
                imgQR.setImage(imagenQrCobro);
            }
            lblEstado.setText(textoEstadoCobroActual);
            lblEstado.setStyle("-fx-text-fill: #555555;");
            btnAlternarEnlace.setText("📱 Vincular celular");
        }
    }

    public void inicializarCobro(double montoTotal) {
        inicializarCobro(montoTotal, "");
    }

    public void inicializarCobro(double montoTotal, String nombreCliente) {
        lblMonto.setText(String.format("Bs. %.2f", montoTotal));
        textoEstadoCobroActual = "Generando código QR...";
        lblEstado.setText(textoEstadoCobroActual);
        lblEstado.setStyle("-fx-text-fill: #555555;");
        progressIndicador.setVisible(true);

        pagoCompletado = false;
        esperandoPago = true;
        qrIdActual = null;
        imagenQrCobro = null;
        mostrandoQrEnlace = false;
        cierreIniciado.set(false);
        if (btnAlternarEnlace != null) {
            btnAlternarEnlace.setText("📱 Vincular celular");
        }

        String clienteLimpio = (nombreCliente != null && !nombreCliente.trim().isEmpty())
                ? nombreCliente.trim()
                : "Mostrador";
        String glosa = "Pedido - " + clienteLimpio;

        WebSocketManager.setAccionPagoQr(mensajeJson -> {
            try {
                JsonNode json = MAPPER.readTree(mensajeJson);
                String qrEvento = json.path("qrId").asText("");
                String voucher = json.path("voucherId").asText("");

                if (esperandoPago && (qrIdActual == null || qrIdActual.equals(qrEvento))) {
                    confirmarPagoYCerrar(voucher);
                }
            } catch (Exception e) {
                System.err.println("⚠️ Error leyendo evento PAGO_CONFIRMADO: " + e.getMessage());
            }
        });

        new Thread(() -> {
            try {
                String url = obtenerBaseUrlQr() + "/generar";
                String bodyJson = MAPPER.writeValueAsString(Map.of(
                        "monto", montoTotal,
                        "glosa", glosa,
                        "datosAdicionales", "Craft POS"
                ));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode json = MAPPER.readTree(response.body());

                if (response.statusCode() == 200 && json.path("exito").asBoolean(false)) {
                    qrIdActual = json.path("qrId").asText();
                    String base64 = json.path("imagenBase64").asText("");
                    if (base64.contains(",")) {
                        base64 = base64.substring(base64.indexOf(",") + 1);
                    }

                    byte[] imageBytes = Base64.getDecoder().decode(base64);
                    Image imageQR = new Image(new ByteArrayInputStream(imageBytes));

                    Platform.runLater(() -> {
                        imagenQrCobro = imageQR;
                        textoEstadoCobroActual = "QR en pantalla del cliente • Esperando pago...";
                        if (!mostrandoQrEnlace) {
                            imgQR.setImage(imageQR);
                            lblEstado.setText(textoEstadoCobroActual);
                        }
                        iniciarRespaldoEstado();
                    });
                } else {
                    String msgError = json.path("mensaje").asText("No se pudo generar el QR.");
                    Platform.runLater(() -> {
                        esperandoPago = false;
                        textoEstadoCobroActual = msgError;
                        if (!mostrandoQrEnlace) {
                            lblEstado.setText(msgError);
                            lblEstado.setStyle("-fx-text-fill: #ff4c4c;");
                        }
                        progressIndicador.setVisible(false);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    esperandoPago = false;
                    textoEstadoCobroActual = "Error al conectar con el servidor QR.";
                    if (!mostrandoQrEnlace) {
                        lblEstado.setText(textoEstadoCobroActual);
                        lblEstado.setStyle("-fx-text-fill: #ff4c4c;");
                    }
                    progressIndicador.setVisible(false);
                });
            }
        }, "qr-generar-thread").start();
    }

    private void iniciarRespaldoEstado() {
        Thread hiloRespaldo = new Thread(() -> {
            try {
                while (esperandoPago) {
                    Thread.sleep(3000);
                    if (!esperandoPago) break;

                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(obtenerBaseUrlQr() + "/estado"))
                            .timeout(Duration.ofSeconds(5))
                            .GET()
                            .build();

                    HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
                    if (resp.statusCode() == 200) {
                        JsonNode json = MAPPER.readTree(resp.body());
                        String estado = json.path("estado").asText("");
                        String voucher = json.path("voucherId").asText("");

                        if ("PAGADO".equalsIgnoreCase(estado)) {
                            Platform.runLater(() -> confirmarPagoYCerrar(voucher));
                            break;
                        } else if ("EXPIRADO".equalsIgnoreCase(estado) || "ERROR".equalsIgnoreCase(estado)) {
                            Platform.runLater(() -> {
                                esperandoPago = false;
                                progressIndicador.setVisible(false);
                                lblEstado.setText("El código QR expiró o fue cancelado.");
                                lblEstado.setStyle("-fx-text-fill: #ff4c4c;");
                            });
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }, "qr-respaldo-estado");
        hiloRespaldo.setDaemon(true);
        hiloRespaldo.start();
    }

    private void confirmarPagoYCerrar(String voucherId) {
        if (!cierreIniciado.compareAndSet(false, true)) return;
        esperandoPago = false;
        mostrandoQrEnlace = false;
        WebSocketManager.setAccionPagoQr(null);

        if (imagenQrCobro != null) {
            imgQR.setImage(imagenQrCobro);
        }
        progressIndicador.setVisible(false);
        btnCancelar.setDisable(true);
        if (btnAlternarEnlace != null) {
            btnAlternarEnlace.setDisable(true);
        }

        String textoVoucher = (voucherId != null && !voucherId.isBlank()) ? " (Comp. " + voucherId + ")" : "";
        lblEstado.setText("¡Pago Confirmado!" + textoVoucher);
        lblEstado.setStyle("-fx-text-fill: #218c4e; -fx-font-weight: bold; -fx-font-size: 15px;");

        new Thread(() -> {
            try { Thread.sleep(1300); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                pagoCompletado = true;
                cerrarVentana();
            });
        }).start();
    }

    @FXML
    void cancelarPago(ActionEvent event) {
        esperandoPago = false;
        pagoCompletado = false;
        WebSocketManager.setAccionPagoQr(null);
        notificarCancelacionAlServidor();
        cerrarVentana();
    }

    public void limpiarAlCerrar() {
        WebSocketManager.setAccionPagoQr(null);
        if (esperandoPago && !pagoCompletado) {
            esperandoPago = false;
            notificarCancelacionAlServidor();
        }
    }

    private void notificarCancelacionAlServidor() {
        Thread hiloCancel = new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(obtenerBaseUrlQr() + "/cancelar"))
                        .timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{}"))
                        .build();
                HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            } catch (Exception ignored) {
            }
        }, "qr-cancelar-thread");
        hiloCancel.setDaemon(true);
        hiloCancel.start();
    }

    private String obtenerBaseUrlQr() {
        String ip = ApiClient.getIpServidor();
        if (ip == null || ip.trim().isEmpty()) {
            ip = "localhost";
        }
        return "http://" + ip.trim() + ":8080/api/qr";
    }

    private String obtenerUrlPantallaCliente() {
        String ip = ApiClient.getIpServidor();
        if (ip == null || ip.trim().isEmpty() || ip.equalsIgnoreCase("localhost") || ip.equals("127.0.0.1")) {
            ip = detectarIpLanLocal();
        }
        return "http://" + ip.trim() + ":8080/qr-cliente.html";
    }

    private String detectarIpLanLocal() {
        // 1. Consulta la ruta de salida real del sistema operativo (ignora tarjetas virtuales)
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            String ipSalida = socket.getLocalAddress().getHostAddress();
            if (ipSalida != null && !ipSalida.startsWith("127.") && !ipSalida.equals("0.0.0.0")) {
                return ipSalida;
            }
        } catch (Exception ignored) {
        }

        // 2. Respaldo si no hay acceso a internet pero sí red Wi-Fi/LAN local
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;

                String nombre = ni.getDisplayName().toLowerCase();
                if (nombre.contains("virtual") || nombre.contains("vmware") || nombre.contains("vbox") || nombre.contains("docker")) {
                    continue;
                }

                Enumeration<InetAddress> direcciones = ni.getInetAddresses();
                while (direcciones.hasMoreElements()) {
                    InetAddress addr = direcciones.nextElement();
                    if (addr instanceof Inet4Address && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "localhost";
    }

    private Image generarQrEnlaceLocal(String texto, int tamano) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(texto, BarcodeFormat.QR_CODE, tamano, tamano);
            WritableImage img = new WritableImage(tamano, tamano);
            PixelWriter pw = img.getPixelWriter();
            Color negro = Color.rgb(17, 17, 17);

            for (int y = 0; y < tamano; y++) {
                for (int x = 0; x < tamano; x++) {
                    pw.setColor(x, y, matrix.get(x, y) ? negro : Color.WHITE);
                }
            }
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    private void cerrarVentana() {
        if (rootPane != null && rootPane.getScene() != null) {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }

    public boolean isPagoCompletado() {
        return pagoCompletado;
    }
}