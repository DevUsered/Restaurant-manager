package Attizos.Backend.Attizos;

import Attizos.Backend.Api.ApiClient;
import Attizos.Backend.Database.*;
import Attizos.Backend.Listas.*;
import Attizos.Frontend.Config.ConfigurationApp;
import Attizos.Frontend.Network.WebSocketManager;
import Attizos.Frontend.ServicioNube;
import javafx.application.Platform;
import javafx.scene.image.Image;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App {
    public static Restaurante attizos;
    public static Empleado usuarioLogueado;
    public static boolean modoOffline = true;
    public static String nombre = ConfigurationApp.getNombreRestaurante();
    private static Image logoImageCache;
    private static Process procesoBackend = null;

    public static void iniciarSistema() {
        attizos = new Restaurante(nombre, "FAST_FOOD");
        cargarCacheLogo();
        ApiClient.cargarCredencialesDelServidor();
        ConexionSQLite.inicializarTablasLocales();

        System.out.println("Cargando datos locales...");
        cargarEmpleados();
        cargarInventario();
        cargarProductos();
        System.out.println("✅ Datos locales cargados.");

        Thread hiloInicial = new Thread(() ->{
            if(ApiClient.isServidorDisponible()){
                    System.out.println("🔄 Sincronizando datos con la Base de Datos...");
                    modoOffline = false;
                    ConexionSQLite.subirAuditoriaPendiente();
                    ServicioNube.sincronizarImagenesPendientes();
                    sincronizarDatosDesdeServidor();

                    String ipServidor = ApiClient.getIpServidor();
                   WebSocketManager.conectarAlServidor(ipServidor);
            }else{
                System.err.println("[Segundo Plano] Sin internet. Attizos sigue funcionando al 100% en modo local.");
                modoOffline = true;
            }
        });
        hiloInicial.setDaemon(true);
        hiloInicial.start();
    }
    public static void sincronizarDatosDesdeServidor() {
        try {
            ConexionSQLite.actualizarCacheCompleta();

            ArrayList<Insumo> stockRealNube = ApiClient.obtenerInsumoDelServidor();
            ArrayList<Producto> menuActualizado = ConexionSQLite.obtenerMenuLocal();
            ArrayList<Promocion> promocionDB = ConexionSQLite.obtenerPromocionesLocal(menuActualizado);

            Platform.runLater(() -> {
                if (stockRealNube != null && !stockRealNube.isEmpty()) {
                    attizos.getInventario().getInventarioInsumos().clear();
                    for (Insumo ins : stockRealNube) {
                        attizos.getInventario().getInventarioInsumos().put(ins.getCodigo(), ins);
                    }
                }
                attizos.setMenu(menuActualizado);
                attizos.setPromocionesActivas(promocionDB);
                System.out.println("✅ RAM actualizada con los datos frescos del servidor.");
            });
        } catch (Exception e) {
            System.err.println("❌ Error al sincronizar datos desde el servidor: " + e.getMessage());
        }
    }
    public static void cargarInventario() {
        HashMap<String, Insumo> inventario = ConexionSQLite.obtenerInventarioLocal();
        if (inventario != null && !inventario.isEmpty()) {
            for (Insumo i : inventario.values()) {
                System.out.println("DEBUG: Cargando insumo " + i.getNombre() + " con stock: " + i.getStockActual());
                attizos.getInventario().agregarInsumo(i);
            }
            System.out.println("✅ Inventario cargado en RAM con " + inventario.size() + " insumos.");
        } else {
            System.out.println("La BD local esta vacio");
        }
    }

    public static void cargarEmpleados() {
        ArrayList<Empleado> personaDB = ConexionSQLite.obtenerEmpleadosLocal();
        if (personaDB != null && !personaDB.isEmpty()) {
            for (Empleado emp : personaDB) {
                attizos.agregarEmpleado(emp);
            }
            System.out.println("Empleados cargados");
        }
    }

    public static void cargarProductos() {
        ArrayList<Producto> menuDB = ConexionSQLite.obtenerMenuLocal();
        if (menuDB != null && !menuDB.isEmpty()) {
            for (Producto p : menuDB) {
                attizos.getMenu().add(p);
            }
            ArrayList<Promocion> promocionDB = ConexionSQLite.obtenerPromocionesLocal(menuDB);
            attizos.setPromocionesActivas(promocionDB);
            System.out.println("Promociones cargados. ");
        } else {
            System.out.println("⚠️ La tabla de productos está vacía en la BD");
        }
    }

    public static boolean autenticarUsuario(String username, String pass) {
        Empleado user = ConexionSQLite.autenticarUsuarioLocal(username, pass);
        if (user != null) {
            usuarioLogueado = user;
            return true;
        } else if(!modoOffline){
            user = ApiClient.autenticarUsuarioEnServidor(username, pass);
            if(user != null){
                usuarioLogueado = user;
                return  true;
            }
        }
        return false;
    }

    public static void registrarAuditoria(String operador, String tipoArea, String nombreItem, String accion, double cantidad, String motivo) {
        if(modoOffline){
            ConexionSQLite.guardarAuditoriaOffline(operador,tipoArea,nombreItem,accion,cantidad,motivo);
        }else {
            boolean guardadoDB = ApiClient.registrarAuditoriaEnServidor(operador, tipoArea, nombreItem, accion, cantidad, motivo);

            if (!guardadoDB) {
                ConexionSQLite.guardarAuditoriaOffline(operador, tipoArea, nombreItem, accion, cantidad, motivo);
            }
        }
    }
    public static void iniciarBackend(){
        try{
            String rutaBase = System.getenv("APPDATA") + File.separator + "Attizos";
            File archivoConfig = new File(rutaBase + File.separator + "configuracion.properties");
            if(archivoConfig.exists()){
                java.util.Properties props = new java.util.Properties();
                try (java.io.FileInputStream in = new java.io.FileInputStream(archivoConfig)) {
                    props.load(in);
                }
                String ip = props.getProperty("app.servidor.ip", "localhost");
                ApiClient.configurarIpServidor(ip);
                String modo = props.getProperty("app.modo", "SUCURSAL");

                if(modo.equals("SERVIDOR")){
                    String pathBackend = rutaBase + File.separator + "backend" + File.separator + "backend.jar";
                    String pathProps = rutaBase + File.separator + "backend" + File.separator + "application.properties";

                    File jarFile = new File(pathBackend);
                    if(jarFile.exists()){
                        System.out.println("🚀 Encendiendo servidor Spring Boot en segundo plano...");

                        // 1. PRO-TIP: Usamos -Dspring.config.additional-location antes del -jar para obligarlo a leer tu archivo externo
                        ProcessBuilder pb = new ProcessBuilder(
                                "java",
                                "-Dspring.config.additional-location=file:" + pathProps,
                                "-jar", pathBackend
                        );
                        pb.redirectErrorStream(true);
                        procesoBackend = pb.start(); // Guardamos el proceso

                        // 2. SOLUCIÓN AL DEADLOCK: Hilo lector que vacía el buffer de Windows y te muestra los logs en IntelliJ
                        new Thread(() -> {
                            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                                    new java.io.InputStreamReader(procesoBackend.getInputStream()))) {
                                String linea;
                                while ((linea = reader.readLine()) != null) {
                                    // Te imprimimos el log con una etiqueta clara para que lo veas todo
                                    System.out.println("[Spring Boot] " + linea);
                                }
                            } catch (Exception e) {
                                System.err.println("🛑 Consola de Spring Boot desconectada.");
                            }
                        }).start();

                        // 3. ANCLA DE APAGADO: Al cerrar tu JavaFX, matamos el servidor limpiamente
                        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                            if (procesoBackend != null && procesoBackend.isAlive()) {
                                System.out.println("🛑 Apagando servidor Spring Boot y liberando puertos...");
                                procesoBackend.destroyForcibly();
                            }
                        }));
                    }else{
                        System.out.println("❌ No se encontró el archivo backend.jar en la ruta: " + pathBackend);
                    }
                }
            }
        }catch (Exception e){
            System.err.println("Error al iniciar backend: " + e.getMessage());
        }
    }
    public static Restaurante getAttizos() {
        return attizos;
    }

    public static void setAttizos(Restaurante attizos) {
        App.attizos = attizos;
    }

    public static Empleado getUsuarioLogueado() {
        return usuarioLogueado;
    }

    public static void setUsuarioLogueado(Empleado usuarioLogueado) {
        App.usuarioLogueado = usuarioLogueado;
    }

    public static boolean isModoOffline() {
        return modoOffline;
    }

    public static void setModoOffline(boolean modoOffline) {
        App.modoOffline = modoOffline;
    }

    public static String getNombre() {
        return nombre;
    }

    public static void setNombre(String nombre) {
        App.nombre = nombre;
    }
    public static Image getLogoImageCache() {
        return logoImageCache;
    }
    public static void setLogoImageCache(Image newImage) {
        logoImageCache = newImage;
    }
    public static void cargarCacheLogo(){
        String rutaOLink = ConfigurationApp.getRutaLogo();
        if (rutaOLink == null || rutaOLink.isEmpty() || rutaOLink.equals("default_logo.png")) {
            try {
                logoImageCache = new Image(App.class.getResourceAsStream("/images/Logo_attizos.png"));
            } catch (Exception e) {
                System.out.println("⚠️ No se encontró logo por defecto en recursos.");
            }
            return;
        }
        if (!rutaOLink.startsWith("http")) {
            File archivoLocal = new File(rutaOLink);
            if (archivoLocal.exists()) {
                logoImageCache = new Image(archivoLocal.toURI().toString());
            }
            return;
        }
        String rutaCarpetaCache = System.getenv("APPDATA") + File.separator + "Attizos" + File.separator + "images";
        File carpeta = new File(rutaCarpetaCache);
        if (!carpeta.exists()) carpeta.mkdirs();

        String nombreArchivoCache = "logo_cache_" + Math.abs(rutaOLink.hashCode()) + ".png";
        File archivoCache = new File(carpeta, nombreArchivoCache);

        if (archivoCache.exists()) {
            // ¡EL LOGO YA ESTÁ EN EL DISCO! Lo leemos al instante sin usar internet
            System.out.println("⚡ Cargando logo desde la caché local del disco...");
            logoImageCache = new Image(archivoCache.toURI().toString());
        }else{
            System.out.println("☁️ Descargando nuevo logo desde Cloudinary a la caché local...");

            logoImageCache = new Image(rutaOLink, true);
            new Thread(() ->{
                try (InputStream in = new URL(rutaOLink).openStream();
                     FileOutputStream out = new FileOutputStream(archivoCache)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    System.out.println("✅ Logo de Cloudinary guardado exitosamente en caché local.");
                } catch (Exception e) {
                    System.err.println("❌ Error guardando el logo en caché: " + e.getMessage());
                }
            }).start();
        }
    }
}