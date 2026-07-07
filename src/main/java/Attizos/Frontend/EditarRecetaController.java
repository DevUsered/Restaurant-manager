package Attizos.Frontend;

import Attizos.Backend.Api.ApiClient;
import Attizos.Backend.Attizos.*;
import Attizos.Backend.Database.ConexionSQLite;
import Attizos.Frontend.Network.WebSocketManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.Map;
import java.util.Optional;

public class EditarRecetaController {

    @FXML private javafx.scene.layout.AnchorPane rootPane;
    @FXML private Label lblTituloProducto;
    @FXML private ComboBox<Insumo> cmbInsumos;
    @FXML private TextField txtCantidad;
    @FXML private TableView<ProductosController.DetalleRecetaUI> tablaReceta;
    @FXML private TableColumn<ProductosController.DetalleRecetaUI, String> colInsumo;
    @FXML private TableColumn<ProductosController.DetalleRecetaUI, Double> colCantidad;
    @FXML private Button btnCancelar;

    private double xOffset = 0;
    private double yOffset = 0;

    private Producto productoEdicion;
    private ObservableList<ProductosController.DetalleRecetaUI> listaReceta = FXCollections.observableArrayList();
    private ObservableList<Insumo> masterInsumos = FXCollections.observableArrayList();
    private javafx.collections.transformation.FilteredList<Insumo> filteredInsumos;

    @FXML
    public void initialize() {
        colInsumo.setCellValueFactory(new PropertyValueFactory<>("nombreInsumo"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        tablaReceta.setItems(listaReceta);

        if (App.attizos.getInventario() != null) {
            masterInsumos.addAll(App.attizos.getInventario().getInventarioInsumos().values());
        }
        filteredInsumos = new javafx.collections.transformation.FilteredList<>(masterInsumos, p -> true);
        cmbInsumos.setItems(filteredInsumos);
        cmbInsumos.getEditor().setStyle("-fx-text-fill: #444444; -fx-font-weight: bold;");
        cmbInsumos.setConverter(new javafx.util.StringConverter<Insumo>() {
            @Override
            public String toString(Insumo insumo) {
                return insumo == null ? "" : insumo.getNombre();
            }

            @Override
            public Insumo fromString(String string) {
                return cmbInsumos.getItems().stream()
                        .filter(i -> i.getNombre().equals(string))
                        .findFirst().orElse(null);
            }
        });
        cmbInsumos.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            Insumo seleccionado = cmbInsumos.getSelectionModel().getSelectedItem();
            if (seleccionado != null && seleccionado.getNombre().equals(newValue)) return;

            filteredInsumos.setPredicate(insumo -> {
                if (newValue == null || newValue.isEmpty()) return true;
                return insumo.getNombre().toLowerCase().contains(newValue.toLowerCase());
            });

            if (newValue != null && !newValue.isEmpty() && !cmbInsumos.isShowing()) {
                cmbInsumos.show();
            }
        });
        cmbInsumos.getEditor().setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                txtCantidad.requestFocus();
                event.consume();
            }
        });
        txtCantidad.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                agregarIngrediente(new ActionEvent());
                event.consume();
            }
        });

        configurarMenuContextual();
        if (rootPane != null) {
            rootPane.setOnMousePressed(event -> { xOffset = event.getSceneX(); yOffset = event.getSceneY(); });
            rootPane.setOnMouseDragged(event -> {
                Stage stage = (Stage) rootPane.getScene().getWindow();
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            });
        }
        javafx.application.Platform.runLater(() -> cmbInsumos.requestFocus());
    }

    public void inicializarDatos(Producto producto) {
        this.productoEdicion = producto;
        lblTituloProducto.setText("Receta de: " + producto.getNombre());

        if (producto.getReceta() != null) {
            for (Map.Entry<String, Double> entry : producto.getReceta().getIngredientes().entrySet()) {
                Insumo ins = App.attizos.getInventario().buscarInsumo(entry.getKey());
                if (ins != null) {
                    listaReceta.add(new ProductosController.DetalleRecetaUI(ins, entry.getValue()));
                }
            }
        }
    }

    @FXML
    void agregarIngrediente(ActionEvent event) {
        Insumo insumoSel = cmbInsumos.getValue();
        if (insumoSel == null ){
            String texto = cmbInsumos.getEditor().getText();
            insumoSel = cmbInsumos.getConverter().fromString(texto);
        }
        if (insumoSel == null || txtCantidad.getText().isEmpty()) {
            AlertaPersonalizada.mostrarAlerta("Incompleto", "Seleccione un insumo de la lista y coloque su cantidad.", Alert.AlertType.WARNING);
            return;
        }

        for (ProductosController.DetalleRecetaUI det : listaReceta) {
            if (det.getInsumo().getCodigo().equals(insumoSel.getCodigo())) {
                AlertaPersonalizada.mostrarAlerta("Duplicado", "El insumo ya está en la tabla. Use clic derecho para editar cantidad.", Alert.AlertType.WARNING);
                return;
            }
        }

        try {
            double cant = Double.parseDouble(txtCantidad.getText().replace(",", "."));

            if (cant < 0.001) {
                AlertaPersonalizada.mostrarAlerta("Error", "La cantidad es demasiado pequeña (Mínimo 0.001).", Alert.AlertType.WARNING);
                return;
            }
            String uni = insumoSel.getUnidad().toLowerCase();
            if ((uni.equals("kg") || uni.equals("lt")) && cant > 10) {
                AlertaPersonalizada.mostrarAlerta("Exageración", "No puedes usar más de 10 " + uni + " en una sola porción/plato.", Alert.AlertType.WARNING);
                return;
            }
            if ((uni.equals("g") || uni.equals("ml")) && cant > 10000) {
                AlertaPersonalizada.mostrarAlerta("Exageración", "No puedes usar más de 10,000 " + uni + " en una sola porción.", Alert.AlertType.WARNING);
                return;
            }
            if ((uni.equals("und") || uni.equals("paquete")) && cant > 50) {
                AlertaPersonalizada.mostrarAlerta("Exageración", "No puedes usar más de 50 " + uni + " en una receta individual.", Alert.AlertType.WARNING);
                return;
            }

            listaReceta.add(new ProductosController.DetalleRecetaUI(insumoSel, cant));
            cmbInsumos.getSelectionModel().clearSelection();
            txtCantidad.clear();
            javafx.application.Platform.runLater(() -> cmbInsumos.requestFocus());

        } catch (NumberFormatException e) {
            AlertaPersonalizada.mostrarAlerta("Error", "Cantidad inválida.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    void guardarCambios(ActionEvent event) {
        if (listaReceta.isEmpty()) {
            AlertaPersonalizada.mostrarAlerta("Vacío", "La receta no puede estar vacía.", Alert.AlertType.WARNING);
            return;
        }

        Optional<ButtonType> respuesta = DialogoPersonalizado.mostrarDialogoConfirmacion(
                "Confirmar Cambios",
                "¿Está seguro de sobreescribir la receta de " + productoEdicion.getNombre() + "?",
                "Esta acción afectará los cálculos de stock para futuras ventas."
        );

        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            Receta nuevaReceta = new Receta();
            for (ProductosController.DetalleRecetaUI det : listaReceta) {
                nuevaReceta.agregarIngrediente(det.getInsumo().getCodigo(), det.getCantidad());
            }

            boolean exito = ApiClient.guardarRecetaEnServidor(productoEdicion.getId(), nuevaReceta);

            if (exito) {
                productoEdicion.setReceta(nuevaReceta);

                ApiClient.actualizarProductoEnServidor(productoEdicion);
                String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                App.registrarAuditoria(operador, "Receta", productoEdicion.getNombre(), "Actualización", 0, "Receta modificada vía Modal");

                new Thread(() -> {
                    ConexionSQLite.subirAuditoriaPendiente();
                    ConexionSQLite.sincronizarProductos();

                }).start();
                AlertaPersonalizada.mostrarAlerta("Éxito", "Receta actualizada correctamente.", Alert.AlertType.INFORMATION);
                cerrarVentana();
            } else {
                AlertaPersonalizada.mostrarAlerta("Error", "No se pudo guardar en la Base de Datos.", Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    void cancelar(ActionEvent event) {
        cerrarVentana();
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }

    private void configurarMenuContextual() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem itemModificar = new MenuItem("✏ Modificar cantidad");
        MenuItem itemEliminar = new MenuItem("🗑 Eliminar ingrediente");

        itemModificar.setOnAction(e -> {
            ProductosController.DetalleRecetaUI sel = tablaReceta.getSelectionModel().getSelectedItem();
            if (sel != null) {
                DialogoPersonalizado.mostrarDialogo("Modificar", "Nueva cantidad de: " + sel.getNombreInsumo(), "Cantidad:", String.valueOf(sel.getCantidad()))
                        .ifPresent(val -> {
                            try {
                                double cant = Double.parseDouble(val.replace(",", "."));

                                String uni = sel.getInsumo().getUnidad().toLowerCase();
                                if (cant < 0.001) {
                                    mostrarAlerta("Error", "Mínimo 0.001");
                                    return;
                                }
                                if ((uni.equals("kg") || uni.equals("lt")) && cant > 10) {
                                    mostrarAlerta("Exageración", "Máximo 10 " + uni);
                                    return;
                                }
                                if ((uni.equals("g") || uni.equals("ml")) && cant > 10000) {
                                    mostrarAlerta("Exageración", "Máximo 10,000 " + uni);
                                    return;
                                }
                                if ((uni.equals("und") || uni.equals("paquete")) && cant > 50) {
                                    mostrarAlerta("Exageración", "Máximo 50 " + uni);
                                    return;
                                }

                                sel.setCantidad(cant);
                                tablaReceta.refresh();
                            } catch (NumberFormatException ex) {
                                mostrarAlerta("Error", "Ingrese un número válido");
                            }
                        });
            }
        });

        itemEliminar.setOnAction(e -> {
            ProductosController.DetalleRecetaUI sel = tablaReceta.getSelectionModel().getSelectedItem();
            if (sel != null) listaReceta.remove(sel);
        });

        contextMenu.getItems().addAll(itemModificar, itemEliminar);
        tablaReceta.setRowFactory(tv -> {
            TableRow<ProductosController.DetalleRecetaUI> row = new TableRow<>();
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty()) {
                    tablaReceta.getSelectionModel().select(row.getItem());
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                } else {
                    contextMenu.hide();
                }
            });
            return row;
        });
    }

    private static void mostrarAlerta(String titulo, String mensaje) {
        AlertaPersonalizada.mostrarAlerta(titulo,mensaje,Alert.AlertType.WARNING);
    }
}