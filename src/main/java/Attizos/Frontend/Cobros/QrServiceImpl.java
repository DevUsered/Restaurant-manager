package Attizos.Frontend.Cobros;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;

public class QrServiceImpl implements PasarelaQrService {

    // Variables leídas del archivo seguro
    private String apiUrlBase;
    private String userName;
    private String passwordEncriptado;
    private String cuentaAbonoEncriptada;

    private final HttpClient httpClient;
    private String tokenAccesoActual = null;


    public QrServiceImpl() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        cargarCredenciales();
    }

    private void cargarCredenciales() {
        try {
            File archivoConfig = new File(System.getenv("APPDATA") + File.separator + "Attizos" + File.separator + "credenciales_economico.properties");
            if (archivoConfig.exists()) {
                Properties prop = new Properties();
                try (FileInputStream fis = new FileInputStream(archivoConfig)) {
                    prop.load(fis);
                    // Para pruebas: https://apimktdesa.baneco.com.bo/ApiGateway
                    this.apiUrlBase = prop.getProperty("API_URL_BASE");
                    this.userName = prop.getProperty("USER_NAME");
                    this.passwordEncriptado = prop.getProperty("PASSWORD_ENCRIPTADO");
                    this.cuentaAbonoEncriptada = prop.getProperty("CUENTA_ABONO_ENCRIPTADA");
                }
            } else {
                System.err.println("⚠️ Faltan credenciales del Banco Económico en AppData.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private String obtenerToken() throws Exception {
        if (tokenAccesoActual != null) {
            return tokenAccesoActual;
        }

        System.out.println("🔐 Autenticando con Banco Económico...");


        String jsonCuerpo = String.format("{\"userName\":\"%s\", \"password\":\"%s\"}", userName, passwordEncriptado);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrlBase + "/api/authentication/authenticate")) //
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonCuerpo))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            // El banco devuelve responseCode 0 si es exitoso
            String codigoRespuesta = extraerValorJson(response.body(), "responseCode");
            if (codigoRespuesta.equals("0")) {
                this.tokenAccesoActual = extraerValorJson(response.body(), "token"); //[cite: 1]
                return this.tokenAccesoActual;
            } else {
                String error = extraerValorJson(response.body(), "message");
                throw new Exception("Error del banco: " + error);
            }
        } else {
            throw new Exception("Error Auth Banco Económico: HTTP " + response.statusCode());
        }
    }

    // 2. GENERAR EL QR
    @Override
    public String solicitarQrDinamico(double monto, String idVenta) throws Exception {
        String token = obtenerToken();
        System.out.println("🔗 Generando QR Banco Económico por Bs. " + monto);

        // Generamos la fecha de vencimiento (hoy)[cite: 1]
        String fechaVencimiento = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));


        String jsonCuerpo = String.format(
                "{" +
                        "\"transactionId\": \"%s\", " +
                        "\"accountCredit\": \"%s\", " +
                        "\"currency\": \"BOB\", " +
                        "\"amount\": %.2f, " +
                        "\"description\": \"Pago en Attizos\", " +
                        "\"dueDate\": \"%s\", " +
                        "\"singleUse\": true, " +
                        "\"modifyAmount\": false" +
                        "}",
                idVenta, cuentaAbonoEncriptada, monto, fechaVencimiento
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrlBase + "/api/qrsimple/generateQR")) //[cite: 1]
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonCuerpo))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String codigoRespuesta = extraerValorJson(response.body(), "responseCode");
            if (codigoRespuesta.equals("0")) {

                String qrId = extraerValorJson(response.body(), "qrId");
                String qrBase64 = extraerValorJson(response.body(), "qrImage");

                return qrId + "|" + qrBase64;
            } else {
                throw new Exception("Error del banco al generar: " + extraerValorJson(response.body(), "message"));
            }
        } else {
            throw new Exception("Error HTTP al generar QR: " + response.statusCode());
        }
    }

    // 3. CONSULTAR ESTADO DEL QR
    @Override
    public boolean verificarPago(String qrIdBancario) throws Exception {
        String token = obtenerToken();

        String jsonCuerpo = String.format("{\"qrId\": \"%s\"}", qrIdBancario);


        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrlBase + "/api/qrsimple/statusQR")) //[cite: 1]
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .method("GET", HttpRequest.BodyPublishers.ofString(jsonCuerpo))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String codigoRespuesta = extraerValorJson(response.body(), "responseCode");
            if (codigoRespuesta.equals("0")) {
                // El banco devuelve statusQRCode: 1 para "pagado"[cite: 1]
                String estado = extraerValorJson(response.body(), "statusQrCode");
                return estado.equals("1");
            }
        }
        return false;
    }


    private String extraerValorJson(String json, String clave) {
        try {
            String[] partes = json.split("\"" + clave + "\":\\s*\"");
            if (partes.length > 1) {
                return partes[1].split("\"")[0];
            } else {
                partes = json.split("\"" + clave + "\":\\s*");
                return partes[1].split("[,}\\]]")[0].trim();
            }
        } catch (Exception e) {
            return "";
        }
    }
}