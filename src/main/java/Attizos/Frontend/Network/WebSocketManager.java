package Attizos.Frontend.Network;

import javafx.application.Platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import Attizos.Backend.Attizos.App;
import Attizos.Frontend.NotificacionToast;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WebSocketManager {
    private static volatile WebSocket socketActual;
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ws-reconnect");
        t.setDaemon(true);
        return t;
    });
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final AtomicBoolean conectando = new AtomicBoolean(false);
    private static final AtomicBoolean reintentoProgramado = new AtomicBoolean(false);

    private static Runnable accionVentasYStock;
    private static Runnable accionCocina;
    private static Runnable accionReservas;
    private static Runnable accionEmpleados;
    private static Runnable accionMenu;
    private static Runnable accionReportes;

    public static void setAccionVentasYStock(Runnable accion) { accionVentasYStock = accion; }
    public static void setAccionCocina(Runnable accion) { accionCocina = accion; }
    public static void setAccionReservas(Runnable accion) { accionReservas = accion; }
    public static void setAccionMenu(Runnable accion) { accionMenu = accion; }
    public static void setAccionReportes(Runnable accion) { accionReportes = accion; }
    public static void setAccionEmpleados(Runnable accion) { accionEmpleados = accion; }

    public static boolean estaConectado() {
        return socketActual != null && !socketActual.isOutputClosed() && !socketActual.isInputClosed();
    }

    // Llama a este método desde el inicio de la app y desde cualquier controlador (ej. CocinaController)
    public static void asegurarConexion(String ipServidor) {
        if (estaConectado()) return;
        conectarAlServidor(ipServidor);
    }

    public static void conectarAlServidor(String ipServidor) {
        if (ipServidor == null || ipServidor.trim().isEmpty()) {
            ipServidor = "localhost";
        }
        final String ipFinal = ipServidor.trim();

        if (estaConectado() || !conectando.compareAndSet(false, true)) {
            return;
        }

        String urlServidor = "ws://" + ipFinal + ":8080/ws-sync";
        System.out.println("🔌 Intentando conectar al túnel WebSocket en: " + urlServidor);

        StringBuilder bufferMensaje = new StringBuilder();

        CLIENT.newWebSocketBuilder()
                .buildAsync(URI.create(urlServidor), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        socketActual = webSocket;
                        conectando.set(false);
                        reintentoProgramado.set(false);
                        System.out.println("✅ ¡Túnel WebSocket establecido con éxito!");

                        // Al conectar (o reconectar tras arranque lento de Spring Boot), refrescamos cocina
                        if (accionCocina != null) {
                            Platform.runLater(accionCocina);
                        }

                        webSocket.request(1);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        bufferMensaje.append(data);
                        if (last) {
                            String mensaje = bufferMensaje.toString();
                            bufferMensaje.setLength(0);
                            System.out.println("📩 Mensaje WS recibido: " + mensaje);
                            Platform.runLater(() -> procesarEventoDelServidor(mensaje));
                        }
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        System.err.println("⚠️ Error en WebSocket: " + error.getMessage());
                        limpiarYReintentar(ipFinal);
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        System.err.println("⚠️ Túnel WebSocket cerrado (" + statusCode + "). Reintentando...");
                        limpiarYReintentar(ipFinal);
                        return null;
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("⏳ Servidor aún no disponible para WebSocket. Reintentando en 3s...");
                    limpiarYReintentar(ipFinal);
                    return null;
                });
    }

    private static void limpiarYReintentar(String ipServidor) {
        socketActual = null;
        conectando.set(false);

        if (reintentoProgramado.compareAndSet(false, true)) {
            SCHEDULER.schedule(() -> {
                reintentoProgramado.set(false);
                conectarAlServidor(ipServidor);
            }, 3, TimeUnit.SECONDS);
        }
    }

    private static void procesarEventoDelServidor(String mensajeJson) {
        System.out.println("📩 Evento recibido del servidor: " + mensajeJson);

        if (mensajeJson.contains("ACTUALIZAR_INVENTARIO") || mensajeJson.contains("SYNC_INVENTARIO")) {
            if (accionVentasYStock != null) {
                System.out.println("🔄 Refrescando stock en pantalla de Ventas...");
                accionVentasYStock.run();
            }
        } else if (mensajeJson.contains("SYNC_PEDIDOS")) {
            if (accionCocina != null) {
                accionCocina.run();
            }
            if (accionVentasYStock != null) {
                accionVentasYStock.run();
            }
        } else if (mensajeJson.contains("SYNC_RESERVAS") && accionReservas != null) {
            accionReservas.run();
        } else if (mensajeJson.contains("SYNC_CATALOGO") && accionMenu != null) {
            accionMenu.run();
        } else if (mensajeJson.contains("SYNC_REPORTES") && accionReportes != null) {
            accionReportes.run();
        } else if (mensajeJson.contains("SYNC_EMPLEADOS") && accionEmpleados != null) {
            accionEmpleados.run();
        }else if(mensajeJson.contains("PEDIDO_LISTO")){
            manejarPedidoListo(mensajeJson);
        }
    }
    private static void manejarPedidoListo(String mensajeJson) {
        if (App.usuarioLogueado == null) {
            return;
        }

        // Evita que la notificación le salga al cocinero/chef que acaba de despachar
        String cargo = App.usuarioLogueado.getCargo();
        if (cargo != null && (cargo.equalsIgnoreCase("Cocinero") || cargo.equalsIgnoreCase("Chef"))) {
            return;
        }

        try {
            JsonNode json = MAPPER.readTree(mensajeJson);
            int ticket = json.path("ticket").asInt(0);
            String cliente = json.path("cliente").asText("Cliente");
            NotificacionToast.mostrarPedidoListo(ticket, cliente);
        } catch (Exception e) {
            System.err.println("⚠️ Error al procesar notificación PEDIDO_LISTO: " + e.getMessage());
        }
    }

    public static void notificarCambio(String tipoSync) {
        if (estaConectado()) {
            socketActual.sendText(tipoSync, true);
            System.out.println("🚀 Notificación enviada por WebSocket: " + tipoSync);
        } else {
            System.err.println("⚠️ WebSocket no disponible para enviar: " + tipoSync);
        }
    }
}