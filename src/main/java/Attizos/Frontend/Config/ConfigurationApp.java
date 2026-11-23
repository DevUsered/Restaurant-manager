package Attizos.Frontend.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class ConfigurationApp {
    private static final String RUTA_CARPETA = System.getenv("APPDATA") + File.separator + "Attizos";
    private static final String RUTA_ARCHIVO = RUTA_CARPETA + File.separator + "configuracion.properties";
    private static Properties propiedades = new Properties();

    public static boolean esPrimeraVez = true;
    public static String modoOperacion = "SERVIDOR";
    public static boolean tieneCocina = true;
    public static boolean impresionActivada = true;
    public static String ipServidor = "localhost";
    public static String nombreRestaurante;
    public static int segundosNotificacion = 0;

    public static void cargarConfiguracion() {
        File archivoConfig = new File(RUTA_ARCHIVO);

        if (!archivoConfig.exists()) {
            esPrimeraVez = true;
            return;
        }
        try (FileInputStream fis = new FileInputStream(archivoConfig)) {
            propiedades.load(fis);
            esPrimeraVez = false;
            modoOperacion = propiedades.getProperty("app.modo", "SERVIDOR");
            tieneCocina = Boolean.parseBoolean(propiedades.getProperty("app.modulo.cocina", "true"));
            impresionActivada = Boolean.parseBoolean(propiedades.getProperty("app.modulo.impresion", "true"));
            ipServidor = propiedades.getProperty("app.servidor.ip", "localhost");
            nombreRestaurante = propiedades.getProperty("app.negocio.nombre", "Mi Restaurante");
            try{
                int seg = Integer.parseInt(propiedades.getProperty("app.notificacion.segundos", "8"));
                segundosNotificacion = Math.max(2, Math.min(60, seg));
            }catch(NumberFormatException e){
                segundosNotificacion = 8;
            }

            Attizos.Backend.Api.ApiClient.configurarIpServidor(ipServidor);

        } catch (Exception e) {
            System.err.println("Error al leer configuración local: " + e.getMessage());
        }
    }
    public static void guardarConfiguracionNueva(boolean usarCocina, boolean imprimir, String ip, String nombre) {
        guardarConfiguracionNueva(usarCocina, imprimir, ip, nombre, segundosNotificacion);
    }

    public static void guardarConfiguracionNueva(boolean usarCocina, boolean imprimir, String ip, String nombre, int segundosNotif) {
        File carpeta = new File(RUTA_CARPETA);
        if (!carpeta.exists()) carpeta.mkdirs();

        int segundosSeguros = Math.max(2, Math.min(60, segundosNotif));

        propiedades.setProperty("app.modo", modoOperacion);
        propiedades.setProperty("app.modulo.cocina", String.valueOf(usarCocina));
        propiedades.setProperty("app.modulo.impresion", String.valueOf(imprimir));
        propiedades.setProperty("app.servidor.ip", ip);
        propiedades.setProperty("app.negocio.nombre", nombre);
        propiedades.setProperty("app.notificacion.segundos", String.valueOf(segundosSeguros));

        try (FileOutputStream fos = new FileOutputStream(RUTA_ARCHIVO)) {
            propiedades.store(fos, "Configuración del Punto de Venta Craft POS");

            // Actualizar variables en RAM
            tieneCocina = usarCocina;
            impresionActivada = imprimir;
            ipServidor = ip;
            nombreRestaurante = nombre;
            esPrimeraVez = false;

            Attizos.Backend.Api.ApiClient.configurarIpServidor(ipServidor);
        } catch (Exception e) {
            System.err.println("Error al guardar configuración: " + e.getMessage());
        }
    }
    public static int getSegundosNotificacion(){
        return segundosNotificacion;
    }
    public static void setSegundosNotificacion(){
        ConfigurationApp.segundosNotificacion = Math.max(2, Math.min(60, segundosNotificacion));
    }

    public static String getModoOperacion() {
        return modoOperacion;
    }

    public static void setModoOperacion(String modoOperacion) {
        ConfigurationApp.modoOperacion = modoOperacion;
    }

    public static boolean isImpresionActivada() {
        return impresionActivada;
    }

    public static void setImpresionActivada(boolean impresionActivada) {
        ConfigurationApp.impresionActivada = impresionActivada;
    }

    public static Properties getPropiedades() { return propiedades; }
    public static void setPropiedades(Properties propiedades) { ConfigurationApp.propiedades = propiedades; }
    public static boolean isEsPrimeraVez() { return esPrimeraVez; }
    public static void setEsPrimeraVez(boolean esPrimeraVez) { ConfigurationApp.esPrimeraVez = esPrimeraVez; }
    public static boolean isTieneCocina() { return tieneCocina; }
    public static void setTieneCocina(boolean tieneCocina) { ConfigurationApp.tieneCocina = tieneCocina; }
    public static String getIpServidor() { return ipServidor; }
    public static void setIpServidor(String ipServidor) { ConfigurationApp.ipServidor = ipServidor; }
    public static String getNombreRestaurante() { return nombreRestaurante; }
    public static void setNombreRestaurante(String nombreRestaurante) { ConfigurationApp.nombreRestaurante = nombreRestaurante; }
}