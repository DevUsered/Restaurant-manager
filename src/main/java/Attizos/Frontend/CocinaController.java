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
import javafx.scene.media.AudioClip;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CocinaController {

    @FXML private TableView<Pedido> tablaPedidos;
    @FXML private TableColumn<Pedido, Integer> colIdPedido;
    
    @FXML private TableColumn<Pedido, String> colDescripcion;
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
        
        colIdPedido.setCellValueFactory(new PropertyValueFactory<>("numeroTicket")); // Usamos el número diario
        
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
        WebSocketManager.setAccionCocina(() ->{
            System.out.println("Pedido recibido en la cocina. Actualizando la tabla...");
            String cargo = App.usuarioLogueado.getCargo();
            if(cargo.equalsIgnoreCase("Cocinero") || cargo.equalsIgnoreCase("Chef")){
                Platform.runLater(() -> {
                    try{
                        URL rutaAudio = getClass().getResource("/sounds/cocina.mp3");
                        if(rutaAudio != null){
                            AudioClip sonido = new AudioClip(rutaAudio.toExternalForm());
                            sonido.play();
                        }else{
                            System.out.println("No se pudo reproducir el sonido de cocina.");
                        }
                    }catch (Exception e){
                        System.out.println("Error al reproducir el sonido de cocina:" + e.getMessage());
                    }
                });
            }
            cargarColaDesdeBackend();
        });

    }

    private void cargarColaDesdeBackend() {
        new Thread(() -> {
            ArrayList<Pedido> pedidosFrescos = ApiClient.obtenerPedidosPendientes();

            Platform.runLater(() -> {
                int indiceSeleccionado = tablaPedidos.getSelectionModel().getSelectedIndex();

                listaColaPedidos.clear();
                if (pedidosFrescos != null) {
                    listaColaPedidos.addAll(pedidosFrescos);
                }
                tablaPedidos.refresh();

                if (indiceSeleccionado >= 0 && indiceSeleccionado < listaColaPedidos.size()) {
                    tablaPedidos.getSelectionModel().select(indiceSeleccionado);
                }
                else if(!listaColaPedidos.isEmpty()){
                    tablaPedidos.getSelectionModel().select(0);
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
}