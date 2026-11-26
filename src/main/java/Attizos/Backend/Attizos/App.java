package Attizos.Backend.Attizos;

import Attizos.Backend.Api.ApiClient;
import Attizos.Backend.Database.*;
import Attizos.Backend.Listas.*;
import Attizos.Frontend.AlertaPersonalizada;
import Attizos.Frontend.Config.ConfigurationApp;
import Attizos.Frontend.Network.WebSocketManager;
import Attizos.Frontend.ServicioNube;
import javafx.application.Platform;
import javafx.scene.control.Alert;
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
    public static String nombre;
    private static Image logoImageCache;
    private static Process procesoBackend = null;

    public static void iniciarSistema() {
        ConfigurationApp.cargarConfiguracion();
        nombre = ConfigurationApp.getNombreRestaurante();
        attizos = new Restaurante(nombre, "FAST_FOOD");
        cargarCacheLogo();
        ConexionSQLite.inicializarTablasLocales();

        // 1. Carga inmediata desde SQLite local para abrir la ventana al instante (con o sin red)
        System.out.println("Cargando datos locales a RAM...");
        cargarEmpleados();
        cargarInventario();
        cargarProductos();
        modoOffline = true;
        System.out.println("✅ Datos locales listos al instante.");

        // 2. Comprobación de red y sincronización en segundo plano sin congelar el arranque
        new Thread(() -> {
            if (!ApiClient.isServidorDisponible()
                    && procesoBackend != null && procesoBackend.isAlive()
                    && ConexionSQLite.obtenerEmpleadosLocal().isEmpty()) {
                System.out.println("⏳ Base local sin usuarios. Esperando a que el servidor termine de iniciar...");
                for (int i = 0; i < 30 && !ApiClient.isServidorDisponible(); i++) {
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                }
            }
            if (ApiClient.isServidorDisponible()) {
                System.out.println("🔄 Servidor disponible. Sincronizando en segundo plano...");
                modoOffline = false;
                ApiClient.cargarCredencialesDelServidor();
                ConexionSQLite.subirAuditoriaPendiente();
                ConexionSQLite.subirVentasPendientes();
                ServicioNube.sincronizarImagenesPendientes();
                sincronizarDatosDesdeServidor();
                WebSocketManager.conectarAlServidor(ApiClient.getIpServidor());
            } else {
                System.out.println("[Modo Offline] Trabajando con caché local sin esperar red.");
            }
        }).start();
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
        }

        if (ApiClient.isServidorDisponible()) {
            App.modoOffline = false;

            System.out.println("⚡ SQLite vacío. Sincronizando empleados desde el servidor...");
            ConexionSQLite.sincronizarEmpleados();

            user = ConexionSQLite.autenticarUsuarioLocal(username, pass);
            if (user != null) {
                usuarioLogueado = user;

                new Thread(() -> {
                    System.out.println("🌐 Conectando WebSocket y descargando catálogo completo...");
                    String ipServidor = ApiClient.getIpServidor();
                    Attizos.Frontend.Network.WebSocketManager.conectarAlServidor(ipServidor);

                    ConexionSQLite.subirAuditoriaPendiente();
                    ConexionSQLite.subirVentasPendientes();
                    Attizos.Frontend.ServicioNube.sincronizarImagenesPendientes();

                    App.sincronizarDatosDesdeServidor();
                }).start();

                return true;
            }

            user = ApiClient.autenticarUsuarioEnServidor(username, pass);
            if (user != null) {
                usuarioLogueado = user;
                return true;
            }
        }

        return false;
    }

    public static void registrarAuditoria(String operador, String tipoArea, String nombreItem, String accion, double cantidad, String motivo) {
        if(ApiClient.isServidorDisponible()){
            App.modoOffline = false;
            boolean guardadoDB = ApiClient.registrarAuditoriaEnServidor(operador, tipoArea, nombreItem, accion, cantidad, motivo);
            if (!guardadoDB) {
                ConexionSQLite.guardarAuditoriaOffline(operador, tipoArea, nombreItem, accion, cantidad, motivo);
            }else{
                new Thread(() -> ConexionSQLite.subirAuditoriaPendiente()).start();
            }
        }else{
            App.modoOffline = true;
            ConexionSQLite.guardarAuditoriaOffline(operador, tipoArea, nombreItem, accion, cantidad, motivo);
        }
    }

    public static void iniciarBackend() {
        try {
            String rutaBase = System.getenv("APPDATA") + File.separator + "Attizos";
            File archivoConfig = new File(rutaBase + File.separator + "configuracion.properties");
            if (archivoConfig.exists()) {
                java.util.Properties props = new java.util.Properties();
                try (java.io.FileInputStream in = new java.io.FileInputStream(archivoConfig)) {
                    props.load(in);
                }
                String ip = props.getProperty("app.servidor.ip", "localhost");
                ApiClient.configurarIpServidor(ip);
                String modo = props.getProperty("app.modo", "SUCURSAL");

                if (modo.equals("SERVIDOR")) {
                    String pathBackend = rutaBase + File.separator + "backend" + File.separator + "backend.jar";
                    String pathProps = rutaBase + File.separator + "backend" + File.separator + "application.properties";

                    File jarFile = new File(pathBackend);
                    if (jarFile.exists()) {
                        // 1. Matar cualquier instancia previa antes de encender una nueva
                        detenerBackend();
                        Runtime.getRuntime().addShutdownHook(new Thread(App::detenerBackend));

                        System.out.println("🚀 Encendiendo servidor Spring Boot en segundo plano...");

                        String directorioApp = System.getProperty("user.dir");
                        String extensionExe = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
                        String javaBin = directorioApp + File.separator + "jre" + File.separator + "bin" + File.separator + extensionExe;

                        ProcessBuilder pb = new ProcessBuilder(
                                javaBin,
                                "-Dspring.config.additional-location=file:" + pathProps,
                                "-jar", pathBackend
                        );
                        pb.redirectErrorStream(true);
                        procesoBackend = pb.start();

                        new Thread(() -> {
                            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                                    new java.io.InputStreamReader(procesoBackend.getInputStream()))) {
                                String linea;
                                while ((linea = reader.readLine()) != null) {
                                    System.out.println("[Spring Boot] " + linea);

                                    // 2. En cuanto Spring Boot termine de levantar, cargar credenciales y sincronizar
                                    if (linea.contains("Started BackendApplication")) {
                                        new Thread(() -> {
                                            modoOffline = false;
                                            ApiClient.cargarCredencialesDelServidor();
                                            ConexionSQLite.subirAuditoriaPendiente();
                                            ConexionSQLite.subirVentasPendientes();
                                            ServicioNube.sincronizarImagenesPendientes();
                                            sincronizarDatosDesdeServidor();
                                            WebSocketManager.conectarAlServidor(ApiClient.getIpServidor());
                                        }).start();
                                    } else if (linea.contains("CÓDIGO DE EQUIPO:")) {
                                        try {
                                            String codigo = linea.split("CÓDIGO DE EQUIPO:")[1].trim();
                                            Platform.runLater(() -> {
                                                AlertaPersonalizada.mostrarAlerta(
                                                        "Sistema No Activado",
                                                        "Para poder utilizar el SISTEMA, debe activar su licencia. \n" +
                                                                "Por favor, envíe este código de seguridad al administrador (+591 71709949):\n\n" + codigo,
                                                        Alert.AlertType.ERROR
                                                );
                                                System.exit(0);
                                            });
                                        } catch (Exception e) {
                                        }
                                    } else if (linea.contains("ALERTA DE SEGURIDAD:") || linea.contains("LICENCIA VENCIDA") || linea.contains("ha expirado")) {
                                        String mensajeAlerta = linea.replace("[Firebase] Alerta:", "").replace("[Offline] Alerta:", "").trim();
                                        Platform.runLater(() -> {
                                            AlertaPersonalizada.mostrarAlerta(
                                                    "Problema de Licencia",
                                                    mensajeAlerta + "\nComuníquese con soporte técnico al +591 71709949.",
                                                    Alert.AlertType.ERROR
                                            );
                                            System.exit(0);
                                        });
                                    } else if (linea.contains("ALERTA_VENCIMIENTO:")) {
                                        try {
                                            String[] partes = linea.split("ALERTA_VENCIMIENTO:")[1].trim().split("\\|");
                                            String dias = partes[0];
                                            String fecha = partes[1];

                                            String textoBase = dias.equals("0")
                                                    ? "¡Atención! Su licencia vence HOY (" + fecha + ")."
                                                    : "¡Atención! Su licencia de vencerá en " + dias + " días (" + fecha + ").";

                                            final String mensajeFinal = textoBase + "\nPor favor, comuníquese con soporte para renovar su suscripción y evitar interrupciones.";

                                            Platform.runLater(() -> {
                                                Attizos.Frontend.AlertaPersonalizada.mostrarAlerta(
                                                        "Renovación de Licencia Próxima",
                                                        mensajeFinal,
                                                        Alert.AlertType.WARNING
                                                );
                                            });
                                        } catch (Exception e) {
                                            System.err.println("Error procesando aviso de vencimiento: " + e.getMessage());
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("🛑 Consola de Spring Boot desconectada.");
                            }
                        }).start();
                    } else {
                        System.out.println("❌ No se encontró el archivo backend.jar en la ruta: " + pathBackend);
                        final String rutaFaltante = pathBackend;
                        Platform.runLater(() -> {
                            AlertaPersonalizada.mostrarAlerta(
                                    "Error de Instalación",
                                    "No se encontró el archivo del servidor en la ruta:\n" + rutaFaltante,
                                    Alert.AlertType.ERROR
                            );
                        });
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error al iniciar backend: " + e.getMessage());
            final String mensajeError = e.getMessage();
            Platform.runLater(() -> {
                AlertaPersonalizada.mostrarAlerta(
                        "Error Crítico",
                        "El servidor backend no pudo arrancar. Detalle:\n" + mensajeError,
                        Alert.AlertType.ERROR
                );
            });
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

    public static void cargarCacheLogo() {
        try {
            logoImageCache = new Image(App.class.getResourceAsStream("/images/logo_personal.png"));
        }catch (Exception e){
            System.out.println("NO SE ENCONTRO LA IMAGEN");
        }
    }
    public static void detenerBackend() {
        try {
            boolean cerradoAlguno = false;
            if (procesoBackend != null && procesoBackend.isAlive()) {
                procesoBackend.descendants().forEach(ProcessHandle::destroyForcibly);
                procesoBackend.destroyForcibly();
                procesoBackend.waitFor(3, TimeUnit.SECONDS);
                System.out.println("🛑 Servidor backend detenido correctamente.");
                cerradoAlguno = true;
            }
            for (ProcessHandle p : ProcessHandle.allProcesses().toList()) {
                if (p.info().commandLine().orElse("").contains("backend.jar")) {
                    System.out.println("🧹 Cerrando instancia anterior de backend.jar (PID " + p.pid() + ")...");
                    p.destroyForcibly();
                    cerradoAlguno = true;
                }
            }
            if (cerradoAlguno) {
                Thread.sleep(500);
            }
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo detener el backend anterior: " + e.getMessage());
        }
    }
}