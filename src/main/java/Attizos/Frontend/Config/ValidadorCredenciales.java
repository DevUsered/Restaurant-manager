package Attizos.Frontend.Config;

import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Base64;

public class ValidadorCredenciales {

    public static boolean probarPostgreSQLReal(String ip, String usuario, String password) {
        String urlJdbc = "jdbc:postgresql://" + ip + "/attizos_db";
        System.out.println("🔄 Verificando credenciales de PostgreSQL en: " + urlJdbc);
        try {
            DriverManager.setLoginTimeout(3); // 3 segundos máximo de espera
            try (Connection conn = DriverManager.getConnection(urlJdbc, usuario, password)) {
                // Ejecutamos una consulta real para validar los permisos
                return conn.isValid(3);
            }
        } catch (Exception e) {
            System.err.println("❌ Rechazado por PostgreSQL: " + e.getMessage());
            return false;
        }
    }

    /**
     * PRUEBA REAL DE CLOUDINARY:
     * Hace una llamada HTTP autenticada a la API de Cloudinary usando Key y Secret.
     * Si las credenciales no existen o están mal, Cloudinary devuelve error (401/404) y esto devuelve false.
     */
    public static boolean probarCloudinaryReal(String urlCloudinary) {
        if (urlCloudinary == null || urlCloudinary.trim().isEmpty()) {
            return true; // Si lo dejó en blanco, no validamos (es opcional)
        }

        System.out.println("🔄 Conectando con los servidores de Cloudinary para verificar credenciales...");
        try {
            // 1. Extraemos las partes de cloudinary://KEY:SECRET@CLOUD_NAME
            String base = urlCloudinary.trim().substring(13);
            String[] partesArroba = base.split("@");
            String cloudName = partesArroba[1];
            String[] credenciales = partesArroba[0].split(":");
            String apiKey = credenciales[0];
            String apiSecret = credenciales[1];

            // 2. Armamos la URL de la API oficial de Cloudinary (hacemos un ping a su servidor)
            String apiEndpoint = "https://api.cloudinary.com/v1_1/" + cloudName + "/ping";
            URL url = new URL(apiEndpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);

            // 3. Autenticación Básica HTTP (Key:Secret en Base64)
            String auth = apiKey + ":" + apiSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);

            // 4. Si el código HTTP es 200 (OK), las credenciales SON 100% REALES
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                System.out.println("✅ Credenciales de Cloudinary verificadas exitosamente.");
                return true;
            } else {
                System.err.println("❌ Cloudinary rechazó las credenciales. Código HTTP: " + responseCode);
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Error al verificar Cloudinary: " + e.getMessage());
            return false;
        }
    }

    /**
     * PRUEBA DE RED PARA SUCURSALES:
     * Verifica que la computadora central exista y tenga el puerto de Attizos (8080) abierto.
     */
    public static boolean probarServidorCentral(String ipCentral) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(ipCentral, 8080), 2500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}