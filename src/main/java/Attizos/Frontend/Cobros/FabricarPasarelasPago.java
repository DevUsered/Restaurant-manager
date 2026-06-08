package Attizos.Frontend.Cobros;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class FabricarPasarelasPago {
    public static PasarelaQrService obtenerPasarelaActiva() {
        String pasarelaConfigurada = "LIBELULA"; // Valor por defecto

        // Leemos la configuración desde AppData
        try {
            File archivoConfig = new File(System.getenv("APPDATA") + File.separator + "Attizos" + File.separator + "config.properties");
            if (archivoConfig.exists()) {
                Properties prop = new Properties();
                try (FileInputStream fis = new FileInputStream(archivoConfig)) {
                    prop.load(fis);
                    // Si el archivo tiene la llave PASARELA_PAGO, la usamos
                    if (prop.getProperty("PASARELA_PAGO") != null) {
                        pasarelaConfigurada = prop.getProperty("PASARELA_PAGO").toUpperCase();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo leer la configuración de la pasarela. Usando defecto.");
        }

        switch (pasarelaConfigurada) {
            case "BCP":
                System.out.println("✅ Cargando módulo de pagos: BCP");
                return new BcpQrServiceImpl();
            case "SINTESIS":
            case "LIBELULA":
            default:
                System.out.println("✅ Cargando módulo de pagos: LIBÉLULA");
                return new LibelulaQrServiceImpl();
        }
    }
}
