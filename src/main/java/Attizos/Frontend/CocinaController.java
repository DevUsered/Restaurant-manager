package Attizos.Frontend;

import Attizos.Backend.Attizos.*;
import Attizos.Backend.Database.PedidoDAO;
import Attizos.Backend.Database.FacturaDAO;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.util.Duration;

public class CocinaController {

    @FXML private TableView<Pedido> tablaPedidos;
    // Sugerencia: En tu FXML, cambia el texto de colIdPedido a "TICKET" o "N°"
    @FXML private TableColumn<Pedido, Integer> colIdPedido; 
    
    // Sugerencia: Agrega esta columna en tu FXML para la descripción rápida
    @FXML private TableColumn<Pedido, String> colDescripcion; 
    @FXML private TableColumn<Pedido, String> colEstado;

    @FXML private ListView<String> listaDetallesCocina;
    @FXML private Label lblPedidoActual;
    @FXML private Button btnCerrar;
    @FXML private Button btnConfirmar;

    private ObservableList<Pedido> listaColaPedidos = FXCollections.observableArrayList();
    private Timeline radarDePedidos; // Nuestro reloj automático

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

        iniciarRadarDePedidos();
    }

    private void iniciarRadarDePedidos() {
        cargarColaDesdeBackend(); // Primera carga inmediata

        radarDePedidos = new Timeline(new KeyFrame(Duration.seconds(4), evento -> {
            int indiceSeleccionado = tablaPedidos.getSelectionModel().getSelectedIndex();
            
            cargarColaDesdeBackend();
            
            if(indiceSeleccionado >= 0 && indiceSeleccionado < listaColaPedidos.size()){
                tablaPedidos.getSelectionModel().select(indiceSeleccionado);
            }
        }));
        radarDePedidos.setCycleCount(Timeline.INDEFINITE);
        radarDePedidos.play();
    }

    private void cargarColaDesdeBackend() {
        listaColaPedidos.clear();
        listaColaPedidos.addAll(PedidoDAO.obtenerPedidosPendientes());
    }

    private void mostrarDetallesPedido(Pedido pedido) {
        listaDetallesCocina.getItems().clear();
        if (pedido == null) {
            lblPedidoActual.setText("Seleccione un pedido...");
            return;
        }

        lblPedidoActual.setText("Ticket #" + pedido.getNumeroTicket() + " - " + pedido.getCliente());
        
        listaDetallesCocina.getItems().addAll(PedidoDAO.obtenerDetallesParaCocina(pedido.getIdPedido()));
    }

    @FXML
    void atenderSiguiente(ActionEvent event) {
        if (listaColaPedidos.isEmpty()) return;
        
        Pedido pedidoAtendido = tablaPedidos.getSelectionModel().getSelectedItem();
        if(pedidoAtendido == null){
            pedidoAtendido = listaColaPedidos.get(0);
        }
        
        boolean eliminado = PedidoDAO.eliminarPedidoDespachado(pedidoAtendido.getIdPedido());

        if(eliminado){
            mostrarExito("¡Plato Listo!", "El Ticket #" + pedidoAtendido.getIdPedido() + " ha sido despachado.");
            cargarColaDesdeBackend();
            listaDetallesCocina.getItems().clear();
            lblPedidoActual.setText("Seleccione un pedido...");
        }else{
            mostrarAlerta("Error", "No se pudo despachar el pedido en la Base de Datos.");
        }
    }

    @FXML
    void cancelarPedidoSeleccionado(ActionEvent event) {
        Pedido seleccionado = tablaPedidos.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarAlerta("Atención", "Seleccione un pedido para cancelarlo.");
            return;
        }

        boolean cancelado = FacturaDAO.anularVenta(seleccionado.getIdPedido());

        if (cancelado) {
            mostrarExito("Anulado", "El pedido se anuló contablemente y los ingredientes regresaron al inventario.");
            cargarColaDesdeBackend();
            listaDetallesCocina.getItems().clear();
        } else {
            mostrarAlerta("Error Crítico", "No se pudo anular el pedido o falló la devolución del stock.");
        }
    }

    @FXML
    void cerrarVentana(ActionEvent event) {
        try{
            if(radarDePedidos != null) radarDePedidos.stop(); // Apagamos el reloj antes de irnos

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Home.fxml"));
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