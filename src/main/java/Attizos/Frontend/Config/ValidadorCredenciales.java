package Attizos.Frontend.Config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;

public class ValidadorCredenciales {

    public static boolean probarPostgreSQLReal(String ip, String usuario, String password) {
        String urlJdbc = "jdbc:postgresql://" + ip + "/attizos_db";
        System.out.println("🔄 Verificando credenciales de PostgreSQL en: " + urlJdbc);
        try {
            DriverManager.setLoginTimeout(3);
            try (Connection conn = DriverManager.getConnection(urlJdbc, usuario, password)) {
                return conn.isValid(3);
            }
        } catch (Exception e) {
            System.err.println("❌ Rechazado por PostgreSQL: " + e.getMessage());
            return false;
        }
    }

    
    public static boolean probarCloudinaryReal(String cloudName, String apiKey, String apiSecret) {
        if ((cloudName == null || cloudName.isBlank()) &&
            (apiKey == null || apiKey.isBlank()) &&
            (apiSecret == null || apiSecret.isBlank())) {
            return true; // Opcional si los 3 campos se dejan vacíos
        }

        if (cloudName == null || cloudName.isBlank() ||
            apiKey == null || apiKey.isBlank() ||
            apiSecret == null || apiSecret.isBlank()) {
            return false; // Incompleto: faltan una o más credenciales
        }

        System.out.println("🔄 Verificando las 3 credenciales de Cloudinary con firma SHA-1...");
        try {
            Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName.trim(),
                    "api_key", apiKey.trim(),
                    "api_secret", apiSecret.trim()
            ));

            // Prueba la firma SHA-1 real con un ID inexistente (no crea ni borra nada)
            Map<?, ?> respuesta = cloudinary.uploader().destroy(
                    "craftpos_verificacion_credenciales",
                    ObjectUtils.emptyMap()
            );

            Object resultado = respuesta.get("result");
            if ("not found".equals(resultado) || "ok".equals(resultado)) {
                System.out.println("✅ Las 3 credenciales de Cloudinary son válidas.");
                return true;
            }

            System.err.println("❌ Respuesta inesperada de Cloudinary: " + respuesta);
            return false;
        } catch (Exception e) {
            // Si cloud_name, api_key o api_secret fallan, Cloudinary lanza excepción aquí
            System.err.println("❌ Credenciales de Cloudinary inválidas: " + e.getMessage());
            return false;
        }
    }

    public static boolean probarServidorCentral(String ipCentral) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(ipCentral, 8080), 2500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}