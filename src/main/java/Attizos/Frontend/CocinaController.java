package Attizos.Frontend;

import Attizos.Backend.Api.ApiClient;
import Attizos.Backend.Attizos.*;
import Attizos.Backend.Database.*;
import Attizos.Frontend.Network.WebSocketManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.net.DatagramSocket;
import java.net.Inet4Address;

public class CocinaController {

    @FXML private TableView<Pedido> tablaPedidos;
    @FXML private TableColumn<Pedido, Integer> colIdPedido;
    @FXML private TableColumn<Pedido, String> colEstado;

    @FXML private ListView<String> listaDetallesCocina;
    @FXML private Label lblPedidoActual;
    @FXML private Button btnCerrar;
    @FXML private Button btnConfirmar;

    private ObservableList<Pedido> listaColaPedidos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if(App.usuarioLogueado != null){
            String cargo = App.usuarioLogueado.getCargo();
            if(!cargo.equalsIgnoreCase("Cocinero") && !cargo.equalsIgnoreCase("Chef")){
                btnCerrar.setVisible(false);
                btnConfirmar.setVisible(false);
            }
        }

        colIdPedido.setCellValueFactory(new PropertyValueFactory<>("numeroTicket"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));


        tablaPedidos.setItems(listaColaPedidos);

        tablaPedidos.getSelectionModel().selectedItemProperty().addListener((obs, viejo, nuevo) -> {
            mostrarDetallesPedido(nuevo);
        });

        listaDetallesCocina.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-font-size: 45px; -fx-font-weight: bold; -fx-text-fill: #111111; -fx-padding: 15px;");
                }
            }
        });

        cargarColaDesdeBackend();

        // 🔥 Escuchador del WebSocket en tiempo real
        WebSocketManager.setAccionCocina(() ->{
            System.out.println("🔄 Evento WS recibido en Cocina. Actualizando tabla visual...");
            cargarColaDesdeBackend();
        });
        WebSocketManager.asegurarConexion(ApiClient.getIpServidor());
    }

    private void cargarColaDesdeBackend() {
        new Thread(() -> {
            try {
                // Pequeña pausa para que la Base de Datos asiente los cambios
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ArrayList<Pedido> pedidosFrescos = ApiClient.obtenerPedidosPendientes();

            Platform.runLater(() -> {
                boolean llegoNuevo = false;

                if (pedidosFrescos != null && !pedidosFrescos.isEmpty()) {
                    if (listaColaPedidos.isEmpty()) {
                        llegoNuevo = true;
                    } else {
                        for (Pedido pFresco : pedidosFrescos) {
                            boolean yaEstaba = false;
                            for (Pedido pViejo : listaColaPedidos) {
                                if (pViejo.getIdPedido() == pFresco.getIdPedido()) {
                                    yaEstaba = true;
                                    break;
                                }
                            }
                            if (!yaEstaba) {
                                llegoNuevo = true;
                                break;
                            }
                        }
                    }
                }

                if (llegoNuevo) {
                    reproducirCampanaJavaFX();
                }

                int indiceSeleccionado = tablaPedidos.getSelectionModel().getSelectedIndex();

                // 🔥 LÓGICA LIMPIA (Igual a la de Reservas) 🔥
                listaColaPedidos.clear();
                if (pedidosFrescos != null) {
                    listaColaPedidos.addAll(pedidosFrescos);
                }
                tablaPedidos.refresh(); // Refrescamos directamente sin desconectar

                // Restaurar selección o seleccionar el primero
                if (!listaColaPedidos.isEmpty()) {
                    if (indiceSeleccionado >= 0 && indiceSeleccionado < listaColaPedidos.size()) {
                        tablaPedidos.getSelectionModel().select(indiceSeleccionado);
                    } else {
                        tablaPedidos.getSelectionModel().select(0);
                    }
                } else {
                    listaDetallesCocina.getItems().clear();
                    lblPedidoActual.setText("No hay pedidos pendientes.");
                }
            });
        }).start();
    }

    private void mostrarDetallesPedido(Pedido pedido) {
        listaDetallesCocina.getItems().clear();
        if (pedido == null) {
            lblPedidoActual.setText("Seleccione un pedido...");
            return;
        }

        lblPedidoActual.setText("Ticket #" + pedido.getNumeroTicket() + " - " + pedido.getCliente());

        new Thread(() -> {
            List<String> detalles = ApiClient.obtenerDetallesParaCocina(pedido.getIdPedido());
            Platform.runLater(() -> {
                listaDetallesCocina.getItems().clear();
                if (detalles != null) {
                    listaDetallesCocina.getItems().addAll(detalles);
                }
            });
        }).start();
    }

    @FXML
    void atenderSiguiente(ActionEvent event) {
        if (listaColaPedidos.isEmpty()) return;

        Pedido pedidoAtendido = tablaPedidos.getSelectionModel().getSelectedItem();
        if(pedidoAtendido == null){
            pedidoAtendido = listaColaPedidos.get(0);
        }

        final Pedido pedidoAEliminar = pedidoAtendido;

        new Thread(() -> {
            boolean eliminado = ApiClient.eliminarPedidoDespachado(pedidoAEliminar.getIdPedido());

            Platform.runLater(() -> {
                if(eliminado){
                    cargarColaDesdeBackend();
                    listaDetallesCocina.getItems().clear();
                    lblPedidoActual.setText("Seleccione un pedido...");
                }else{
                    mostrarAlerta("Error", "No se pudo despachar el pedido en la Base de Datos.");
                }
            });
        }).start();
    }

    @FXML
    void cancelarPedidoSeleccionado(ActionEvent event) {
        Pedido seleccionado = tablaPedidos.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarAlerta("Atención", "Seleccione un pedido para cancelarlo.");
            return;
        }

        new Thread(() -> {
            boolean cancelado = ApiClient.cancelarPedidoEnServidor(seleccionado.getIdPedido());

            if (cancelado) {
                ConexionSQLite.sincronizarInsumos();
                ConexionSQLite.sincronizarProductos();

                Platform.runLater(() -> {
                    actualizarDespuesDeCancelar();
                    cargarColaDesdeBackend();
                    listaDetallesCocina.getItems().clear();
                    lblPedidoActual.setText("Seleccione un pedido...");

                    mostrarExito("Anulado", "El pedido se anuló contablemente y los ingredientes regresaron al inventario.");
                });
            } else {
                Platform.runLater(() -> {
                    mostrarAlerta("Error Crítico", "No se pudo anular el pedido o falló la devolución del stock.");
                });
            }
        }).start();
    }

    private void actualizarDespuesDeCancelar(){
        App.attizos.getInventario().getInventarioInsumos().clear();
        App.attizos.getInventario().getInventarioInsumos().putAll(ConexionSQLite.obtenerInventarioLocal());

        App.attizos.getMenu().clear();
        ArrayList<Producto> menuAct = ConexionSQLite.obtenerMenuLocal();

        App.attizos.getMenu().addAll(menuAct);
        RecetaDAO.cargarRecetas();
        App.attizos.setPromocionesActivas(ConexionSQLite.obtenerPromocionesLocal(menuAct));
        System.out.println("✅ RAM de cocina actualizada con el stock devuelto.");
    }

    @FXML
    void cerrarVentana(ActionEvent event) {
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.show();

            Stage vAc = (Stage) ((Node) event.getSource()).getScene().getWindow();
            vAc.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        AlertaPersonalizada.mostrarAlerta(titulo,mensaje,Alert.AlertType.WARNING);
    }
    private void mostrarExito(String titulo, String mensaje) {
        AlertaPersonalizada.mostrarAlerta(titulo, mensaje, Alert.AlertType.INFORMATION);
    }

    @FXML
    void mostrarEnlaceWeb(ActionEvent event) {
        String ipConfigurada = ApiClient.getIpServidor();
        String ipReal = (ipConfigurada != null
                && !ipConfigurada.equalsIgnoreCase("localhost")
                && !ipConfigurada.equals("127.0.0.1"))
                ? ipConfigurada
                : obtenerIpLocal();

        String enlaceWeb = "http://" + ipReal + ":8080/cocina.html";
        String mensaje = "Escanee este código QR con la cámara de su celular o tablet,\n"
                + "o ingrese manualmente al siguiente enlace:\n\n"
                + enlaceWeb;

        try {
            javafx.scene.image.ImageView imagenQR = generarQR(enlaceWeb);
            AlertaPersonalizada.mostrarAlertaConImagen("Vincular Pantalla", mensaje, imagenQR);
        } catch (Exception e) {
            System.out.println("Error al generar QR: " + e.getMessage());
            AlertaPersonalizada.mostrarAlerta("Vincular Pantalla", mensaje, Alert.AlertType.INFORMATION);
        }
    }

    private javafx.scene.image.ImageView generarQR(String texto) throws Exception {
        com.google.zxing.qrcode.QRCodeWriter qrCodeWriter = new com.google.zxing.qrcode.QRCodeWriter();
        com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(texto, com.google.zxing.BarcodeFormat.QR_CODE, 200, 200);

        javafx.scene.image.WritableImage writableImage = new javafx.scene.image.WritableImage(200, 200);
        javafx.scene.image.PixelWriter pixelWriter = writableImage.getPixelWriter();

        for (int x = 0; x < 200; x++) {
            for (int y = 0; y < 200; y++) {
                pixelWriter.setColor(x, y, bitMatrix.get(x, y) ? javafx.scene.paint.Color.BLACK : javafx.scene.paint.Color.WHITE);
            }
        }
        return new javafx.scene.image.ImageView(writableImage);
    }

    public String obtenerIpLocal() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            InetAddress localAddr = socket.getLocalAddress();
            if (localAddr instanceof Inet4Address
                    && !localAddr.isLoopbackAddress()
                    && !localAddr.isLinkLocalAddress()
                    && !localAddr.isAnyLocalAddress()) {
                return localAddr.getHostAddress();
            }
        } catch (Exception ignored) {
        }

        String ipRespaldo = null;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual() || ni.isPointToPoint()) {
                    continue;
                }

                String nombre = (ni.getName() + " " + ni.getDisplayName()).toLowerCase();
                if (nombre.contains("virtual") || nombre.contains("vmware") || nombre.contains("vbox")
                        || nombre.contains("docker") || nombre.contains("wsl") || nombre.contains("vethernet")
                        || nombre.contains("hyper-v") || nombre.contains("vpn") || nombre.contains("tailscale")
                        || nombre.contains("zerotier") || nombre.contains("radmin")) {
                    continue;
                }

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address
                            && !addr.isLoopbackAddress()
                            && !addr.isLinkLocalAddress()) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("192.168.")) {
                            return ip;
                        }
                        if (addr.isSiteLocalAddress() && ipRespaldo == null) {
                            ipRespaldo = ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ipRespaldo != null ? ipRespaldo : "localhost";
    }

    private void reproducirCampanaJavaFX() {
        if (App.usuarioLogueado != null) {
            String cargo = App.usuarioLogueado.getCargo();
            if (cargo.equalsIgnoreCase("Cocinero") || cargo.equalsIgnoreCase("Chef")) {
                try {
                    URL rutaAudio = getClass().getResource("/sounds/cocina.mp3");
                    if (rutaAudio != null) {
                        javafx.scene.media.AudioClip sonido = new javafx.scene.media.AudioClip(rutaAudio.toExternalForm());
                        sonido.play();
                    }
                } catch (Exception e) {
                    System.out.println("Error de audio: " + e.getMessage());
                }
            }
        }
    }
}