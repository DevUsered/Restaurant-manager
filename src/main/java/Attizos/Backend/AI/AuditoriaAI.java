package Attizos.Backend.AI;

import Attizos.Backend.Database.ConexionBD;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AuditoriaAI {
    public static final String urlAPI = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=";

    public static String analizarIngresoGeneral(String nombreInsumo, String categoria, double cantidad, String unidad, long diasVencimiento) {
        String apiKey = ConexionBD.geminiKey;

        if (apiKey == null || apiKey.equals("PON_TU_API_KEY_AQUI") || apiKey.trim().isEmpty()) {
            return "OMITIR";
        }

        String urlApi = urlAPI + apiKey;
        try {
            String prompt = "Eres el auditor experto de inventario de un restaurante. "+
                    "IMPORTANTE: Si los días restantes superan los 20,000 (lo que equivale al año 2099), " +
                    "significa que el sistema lo catalogó como un producto 'No Perecedero' (ej. sal, azúcar, químicos). " +
                    "En ese caso único, NO lances alerta por exceso de tiempo de vida y considera la fecha como correcta."+
                    "El usuario intenta ingresar un nuevo lote con los siguientes datos: " +
                    "Producto: '" + nombreInsumo + "', Categoría: '" + categoria + "', " +
                    "Cantidad: " + cantidad + " " + unidad + ", " +
                    "Días de vida útil restantes: " + diasVencimiento + " días. " +
                    "Analiza estrictamente lo siguiente: " +
                    "1. ¿El nombre es un producto real o parece texto basura (ej. 'asdf')? " +
                    "2. ¿La cantidad ingresada es ridículamente alta o baja para un restaurante comercial? " +
                    "3. ¿El tiempo de caducidad es biológicamente lógico para ese producto fresco o envasado? " +
                    "Si TODO es perfectamente normal y lógico, responde EXACTAMENTE con la palabra 'OK'. " +
                    "Si detectas alguna anomalía o algo sospechoso, responde SOLO con una advertencia clara y breve que empiece con 'ALERTA:'.";

            prompt = prompt.replace("\n", " ").replace("\"", "\\\"");

            String jsonPayload = "{\"contents\": [{\"parts\": [{\"text\": \"" + prompt + "\"}]}]}";

            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlApi))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return extraerTextoDelJson(response.body());

        } catch (Exception e) {
            System.err.println("❌ IA no disponible: " + e.getMessage());
            return "OMITIR";
        }
    }
    public static String analizarCreacionInsumo(String nombre, String categoria, String unidad, double stockMin, double stockMax, long diasVencimiento) {
        String apiKey = ConexionBD.geminiKey;

        if (apiKey == null || apiKey.equals("PON_TU_API_KEY_AQUI") || apiKey.trim().isEmpty()) {
            return "OMITIR";
        }

        String urlApi = urlAPI + apiKey;

        try {
            String prompt = "Eres el auditor de inventario de un restaurante. " +
                    "El usuario está creando un nuevo insumo en la base de datos con estos parámetros: " +
                    "Nombre: '" + nombre + "', Categoría: '" + categoria + "', " +
                    "Unidad de medida: '" + unidad + "', " +
                    "Stock Mínimo permitido: " + stockMin + " " + unidad + ", " +
                    "Stock Máximo permitido: " + stockMax + " " + unidad + ", " +
                    "El primer lote vencerá en " + diasVencimiento + " días. " +
                    "Analiza estrictamente: " +
                    "1. ¿La unidad de medida tiene sentido físico para el producto (ej. arroz en litros es un error)? " +
                    "2. ¿El stock máximo es una exageración total para un restaurante (ej. 50,000 kg de sal)? " +
                    "3. ¿El stock mínimo es lógicamente mayor al máximo (error de tipeo)? " +
                    "4. ¿El tiempo de vida útil es biológicamente imposible para ese producto (ej. carne que dura 5 años)? " +
                    "Si TODO es perfectamente lógico, responde EXACTAMENTE con la palabra 'OK'. " +
                    "Si hay CUALQUIER anomalía, responde SOLO con una advertencia breve que empiece con 'ALERTA:'.";

            prompt = prompt.replace("\n", " ").replace("\"", "\\\"");
            String jsonPayload = "{\"contents\": [{\"parts\": [{\"text\": \"" + prompt + "\"}]}]}";

            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlApi))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return extraerTextoDelJson(response.body());

        } catch (Exception e) {
            System.err.println("❌ IA no disponible: " + e.getMessage());
            return "OMITIR";
        }
    }

    private static String extraerTextoDelJson(String json) {
        int inicio = json.indexOf("\"text\": \"");
        if (inicio == -1) return "OK";

        inicio += 9;
        int fin = json.indexOf("\"", inicio);

        if (inicio > 8 && fin > inicio) {
            return json.substring(inicio, fin).replace("\\n", "\n").trim();
        }
        return "OK";
    }
}