package Attizos.Frontend;

import Attizos.Backend.Attizos.*;
import Attizos.Backend.Listas.NodoDE;
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
import javafx.stage.Stage;

public class CocinaController {


    @FXML private TableView<Pedido> tablaPedidos;
    @FXML private TableColumn<Pedido, Integer> colIdPedido;
    @FXML private TableColumn<Pedido, String> colEstado;


    @FXML private ListView<String> listaDetallesCocina;
    @FXML private Label lblPedidoActual;
    @FXML private Button btnCerrar;
    @FXML private Button btnConfirmar;

    private ObservableList<Pedido> listaColaPedidos;

    @FXML
    public void initialize() {
        if((App.usuarioLogueado instanceof Cajero) || (App.usuarioLogueado instanceof Admin)){
            btnCerrar.setVisible(false);
            btnConfirmar.setVisible(false);
        }
        colIdPedido.setCellValueFactory(new PropertyValueFactory<>("idPedido"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        listaColaPedidos = FXCollections.observableArrayList();
        tablaPedidos.setItems(listaColaPedidos);

        // 2. Cargar los datos desde el backend
        cargarColaDesdeBackend();

        tablaPedidos.getSelectionModel().selectedItemProperty().addListener((obs, viejo, nuevo) -> {
            mostrarDetallesPedido(nuevo);
        });
    }

    private void cargarColaDesdeBackend() {
        listaColaPedidos.clear();

        NodoDE<Pedido> actual = App.attizos.getPedidos().getCabeza();

        while (actual != null) {
            listaColaPedidos.add(actual.getDato());
            actual = actual.getSiguiente();
        }

        // Limpiamos la vista de detalles si se recarga
        listaDetallesCocina.getItems().clear();
        lblPedidoActual.setText("Seleccione un pedido...");
    }

    private void mostrarDetallesPedido(Pedido pedido) {
        listaDetallesCocina.getItems().clear();

        if (pedido == null) {
            lblPedidoActual.setText("Seleccione un pedido...");
            return;
        }

        lblPedidoActual.setText("Pedido #" + String.format("%03d", pedido.getIdPedido()) + " - " + pedido.getCliente());


        NodoDE<DetalleFactura> actual = pedido.getProductos().getCabeza();
        while (actual != null) {
            DetalleFactura det = actual.getDato();
            // Formato de cocina: "3x Pizza Hawaiana"
            String itemCocina = det.getCantidad() + "x  " + det.getProducto().getNombre();
            listaDetallesCocina.getItems().add(itemCocina);

            actual = actual.getSiguiente();
        }
    }

    @FXML
    void atenderSiguiente(ActionEvent event) {
        if (listaColaPedidos.isEmpty()) {
            mostrarAlerta("Sin pedidos", "No hay pedidos pendientes en la cola.");
            return;
        }
        Pedido pedidoAtendido = App.attizos.atenderSiguientePedido();

        if (pedidoAtendido != null) {
            mostrarExito("¡Plato Listo!", "El Pedido #" + pedidoAtendido.getIdPedido() + " ha sido despachado.");
            cargarColaDesdeBackend();
        } else {
            mostrarAlerta("Error", "No se pudo despachar el pedido.");
        }
    }

    @FXML
    void cancelarPedidoSeleccionado(ActionEvent event) {
        Pedido seleccionado = tablaPedidos.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarAlerta("Selección requerida", "Seleccione un pedido de la lista para cancelarlo.");
            return;
        }

        boolean cancelado = App.attizos.cancelarPedido(seleccionado.getIdPedido());

        if (cancelado) {
            App.attizos.anularFacturaFinanciera(seleccionado.getIdPedido());
            App.attizos.retrocederCorrelativoFactura();
            mostrarExito("Cancelado", "El pedido fue cancelado y los ingredientes regresaron al inventario.");
            cargarColaDesdeBackend();
        } else {
            mostrarAlerta("Error", "No se pudo cancelar el pedido.");
        }
    }

    @FXML
    void cerrarVentana(ActionEvent event) {
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Home.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

            Stage vAc = (Stage) ((Node) event.getSource()).getScene().getWindow();
            vAc.close();
        }catch (Exception e){
            System.out.println("Error al cerrar");
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