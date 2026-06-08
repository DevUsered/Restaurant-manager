package Attizos.Frontend.Cobros;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class LibelulaQrServiceImpl implements PasarelaQrService{

    private static final String APP_KEY = "API_KEY";
    private static final String APP_TOKEN ="APP_TOKEN";

    private static  final String URL_LIBELULA_API = "https://api.sandbox.libelula.bo/v1/transacciones";
    private final HttpClient httpClient;

    public LibelulaQrServiceImpl() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }
    @Override
    public String solicitarQrDinamico(double monto, String idVenta) throws Exception{
        if(APP_KEY.equals("API_KEY")){
            System.out.println("Modo simulador...");
            return "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=Simulacion_Libelula|Venta_"+"AHORA ME DEBES..."+"|Monto_Bs"+monto+ " JAJAJJAJ";
        }
        String jsonBody = "{"
                + "\"app_key\": \"" + APP_KEY + "\","
                + "\"identificador\": \"" + idVenta + "\","
                + "\"monto\": " + monto + ","
                + "\"moneda\": \"BOB\","
                + "\"descripcion\": \"Consumo en Pizzería - Ticket " + idVenta + "\","
                + "\"tipo_cobro\": \"QR_SIMPLE\""
                + "}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL_LIBELULA_API))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + APP_TOKEN)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            String body = response.body();
            if(body.contains("\"url_qr\"")) {
                int inicio = body.indexOf("\"url_qr\":\"") + 10;
                int fin = body.indexOf("\"", inicio);
                return body.substring(inicio, fin);
            }
            throw new Exception("El JSON del banco no contiene la imagen del QR.");
        } else {
            throw new Exception("Error HTTP " + response.statusCode() + " del Banco: " + response.body());
        }
    }
    @Override
    public boolean verificarPago(String idVenta) throws Exception{
        if (APP_KEY.equals("API_KEY")) {
            return false; // Obliga al cajero a usar la confirmación manual en simulador
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL_LIBELULA_API + "/" + idVenta + "/estado"))
                .header("Authorization", "Bearer " + APP_TOKEN)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 200 &&
                (response.body().contains("\"estado\":\"COMPLETADO\"") ||
                        response.body().contains("\"estado\":\"PAGADO\""));
    }
}
