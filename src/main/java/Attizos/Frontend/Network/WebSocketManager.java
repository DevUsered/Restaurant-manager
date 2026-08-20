package Attizos.Frontend.Network;

import javafx.application.Platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

public class WebSocketManager {
    private static WebSocket socketActual;

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

    public static void conectarAlServidor(String ipServidor){
        System.out.println("🔌 Intentando conectar al túnel WebSocket en: " + ipServidor);

        HttpClient client = HttpClient.newHttpClient();
        String urlServidor = "ws://"+ ipServidor + ":8080/ws-sync";

        client.newWebSocketBuilder()
                .buildAsync(URI.create(urlServidor), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket){
                        System.out.println("✅ ¡Túnel WebSocket establecido con éxito!");
                        socketActual = webSocket;
                        WebSocket.Listener.super.onOpen(webSocket);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last){
                        String mensaje = data.toString();
                        System.out.println("📩 Mensaje WS recibido: " + mensaje);
                        Platform.runLater(() ->{
                            procesarEventoDelServidor(mensaje);
                        });
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error){
                        System.err.println("⚠️ Error en WebSocket. Reintentando...");
                        reintentarConexion(ipServidor);
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        System.err.println("⚠️ Túnel WebSocket cerrado por el servidor. Reintentando...");
                        reintentarConexion(ipServidor);
                        return null;
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("⏳ Servidor aún no disponible. Reintentando en 5 segundos...");
                    reintentarConexion(ipServidor);
                    return null;
                });
    }

    private static void reintentarConexion(String ipServidor) {
        new Thread(() -> {
            try {
                Thread.sleep(5000); // Espera 5 segundos
                conectarAlServidor(ipServidor); // Vuelve a intentarlo
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    private static void procesarEventoDelServidor(String mensajeJson){
        System.out.println("📩 Evento recibido del servidor: " + mensajeJson);

        if (mensajeJson.contains("ACTUALIZAR_INVENTARIO") || mensajeJson.contains("SYNC_INVENTARIO")) {
            if (accionVentasYStock != null) {
                System.out.println("🔄 Refrescando stock en pantalla de Ventas...");
                accionVentasYStock.run();
            }
        }
        else if (mensajeJson.contains("SYNC_PEDIDOS")) {
            if(accionCocina != null) {
                accionCocina.run();
            }
            if(accionVentasYStock != null){
                accionVentasYStock.run();
            }
        }
        else if (mensajeJson.contains("SYNC_RESERVAS") && accionReservas != null) {
            accionReservas.run();
        }
        else if (mensajeJson.contains("SYNC_CATALOGO") && accionMenu != null) {
            accionMenu.run();
        }
        else if (mensajeJson.contains("SYNC_REPORTES") && accionReportes != null) {
            accionReportes.run();
        }
        else if(mensajeJson.contains("SYNC_EMPLEADOS") && accionEmpleados != null){
            accionEmpleados.run();
        }
    }
    public static void notificarCambio(String tipoSync) {
        if (socketActual != null && !socketActual.isOutputClosed()) {
            socketActual.sendText(tipoSync, true);
            System.out.println("🚀 Notificación enviada por WebSocket: " + tipoSync);
        } else {
            System.err.println("⚠️ WebSocket no disponible para enviar: " + tipoSync);
        }
    }
}
