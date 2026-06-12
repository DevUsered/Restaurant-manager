package Attizos.Backend.Api;

import Attizos.Backend.Attizos.Empleado;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;

public class ApiClient {
    private static final String BASE_URL = "http://localhost:8080/api";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
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
}
