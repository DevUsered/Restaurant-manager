package Attizos.Backend.Api;

import Attizos.Backend.Attizos.Empleado;
import Attizos.Backend.Attizos.Insumo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;

public class ApiClient {
    private static final String BASE_URL = "http://localhost:8080/api";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    public static ArrayList<Empleado> obtenerEmpleadosDelServidor(){
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/empleados"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String json = response.body();
                return mapper.readValue(json, new TypeReference<ArrayList<Empleado>>() {});
            } else {
                System.err.println("Error del servidor Spring Boot: Código " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Falla de conexión con el backend: " + e.getMessage());
        }

        return new ArrayList<>();
    }

    public static boolean guardarEmpleadoEnServidor(Empleado empleado) {
        try {
            String jsonEmpleado = mapper.writeValueAsString(empleado);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/empleados"))
                    .header("Content-Type", "application/json") // Avisamos que enviamos JSON
                    .POST(HttpRequest.BodyPublishers.ofString(jsonEmpleado))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                return true;
            } else {
                System.err.println("Error al guardar en el servidor. Código: " + response.statusCode());
                System.err.println("Respuesta: " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Falla de conexión al intentar guardar: " + e.getMessage());
            return false;
        }
    }
    public static boolean actualizarEmpleadoEnServidor(Empleado empleado) {
        try {
            String jsonEmpleado = mapper.writeValueAsString(empleado);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/empleados"))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonEmpleado)) // Usamos PUT
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code >= 200 && code < 300) {
                return true;
            } else {
                System.err.println("Error al actualizar. Código: " + code + ". Razón: " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Falla al actualizar en servidor: " + e.getMessage());
            return false;
        }
    }

    public static boolean inactivarEmpleadoEnServidor(String idEmpleado) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/empleados/" + idEmpleado + "/inactivar"))
                    .PUT(HttpRequest.BodyPublishers.noBody()) // No enviamos JSON, solo la orden en la URL
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code >= 200 && code < 300) {
                return true;
            } else {
                System.err.println("Error al inactivar. Código: " + code + ". Razón: " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Falla al inactivar en servidor: " + e.getMessage());
            return false;
        }
    }

    public static boolean registrarPagoEnServidor(String idEmpleado) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/empleados/" + idEmpleado + "/pago"))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            // En tu método registrarPagoEnServidor:
            int code = response.statusCode();
            if (code >= 200 && code < 300) {
                return true;
            } else {
                System.err.println("Error al registrar pago. Código: " + code + ". Razón: " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Falla al registrar pago en servidor: " + e.getMessage());
            return false;
        }
    }
    public static ArrayList<Insumo> obtenerInsumoDelServidor(){
        try{
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/insumos"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String json = response.body();
                return mapper.readValue(json, new TypeReference<ArrayList<Insumo>>() {});
            } else {
                System.err.println("Error del servidor Spring Boot: Código " + response.statusCode());
            }
        }catch(Exception e){
            System.out.println("Falla de conexión: "+ e.getMessage());
        }
        return new ArrayList<>();
    }
    public static String obtenerSiguienteCodigoInsumo() {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/insumos/siguiente-codigo")).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) return response.body();
        } catch (Exception e) { System.err.println("Error código: " + e.getMessage()); }
        return "INS-001";
    }

    public static boolean guardarInsumoEnServidor(Insumo insumo, double costoInicial) {
        try {
            String json = mapper.writeValueAsString(insumo);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/insumos?costoInicial=" + costoInicial))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) { return false; }
    }

    public static boolean inactivarInsumoEnServidor(String codigo) {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/insumos/" + codigo + "/inactivar")).PUT(HttpRequest.BodyPublishers.noBody()).build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) { return false; }
    }

    public static boolean registrarLoteEnServidor(String codigo, double cantidad, double costo, LocalDate vencimiento) {
        try {
            String url = String.format("%s/insumos/%s/lotes?cantidad=%s&costo=%s&vencimiento=%s", BASE_URL, codigo, cantidad, costo, vencimiento.toString());
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).POST(HttpRequest.BodyPublishers.noBody()).build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) { return false; }
    }
    public static boolean descontarStockFEFOEnServidor(String codigo, double cantidad) {
        try {
            String url = String.format("%s/insumos/%s/descontar?cantidad=%s", BASE_URL, codigo, cantidad);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            int code = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
            return code >= 200 && code < 300;
        } catch (Exception e) {
            System.err.println("Falla al descontar stock FEFO: " + e.getMessage());
            return false;
        }
    }
    public static double darDeBajaLotesVencidosEnServidor(String codigo) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/insumos/" + codigo + "/vencidos"))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int code = response.statusCode();
            if (code >= 200 && code < 300) {

                return Double.parseDouble(response.body());
            } else {
                System.err.println("Error al limpiar lotes vencidos. Código: " + code + ". Razón: " + response.body());
                return -1;
            }
        } catch (Exception e) {
            System.err.println("Falla de conexión al limpiar vencidos: " + e.getMessage());
            return -1;
        }
    }
}
