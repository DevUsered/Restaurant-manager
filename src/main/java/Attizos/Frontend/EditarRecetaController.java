package Attizos.Frontend;

import Attizos.Backend.Attizos.*;
import Attizos.Backend.Database.RecetaDAO;
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

    @FXML
    public void initialize() {
        colInsumo.setCellValueFactory(new PropertyValueFactory<>("nombreInsumo"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        tablaReceta.setItems(listaReceta);

        if (App.attizos.getInventario() != null) {
            cmbInsumos.setItems(FXCollections.observableArrayList(App.attizos.getInventario().getInventarioInsumos().values()));
        }

        cmbInsumos.setButtonCell(new ListCell<Insumo>() {
            @Override
            protected void updateItem(Insumo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Seleccione insumo...");
                } else {
                    setText(item.getNombre());
                }
            }
        });
        cmbInsumos.setCellFactory(lv -> new ListCell<Insumo>() {
            @Override
            protected void updateItem(Insumo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNombre());
                }
            }
        });

        configurarMenuContextual();
        UtilidadesUI.saltarConEnter(txtCantidad, cmbInsumos);

        if (rootPane != null) {
            rootPane.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });
            rootPane.setOnMouseDragged(event -> {
                Stage stage = (Stage) rootPane.getScene().getWindow();
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            });
        }
    }

    public void inicializarDatos(Producto producto) {
        this.productoEdicion = producto;
        lblTituloProducto.setText("Receta de: " + producto.getNombre());

        // Cargamos los ingredientes actuales desde la memoria RAM (Velocidad Extrema)
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
        if (insumoSel == null || txtCantidad.getText().isEmpty()) return;

        for (ProductosController.DetalleRecetaUI det : listaReceta) {
            if (det.getInsumo().getCodigo().equals(insumoSel.getCodigo())) {
                AlertaPersonalizada.mostrarAlerta("Duplicado", "El insumo ya está en la tabla. Use clic derecho para editar cantidad.", Alert.AlertType.WARNING);
                return;
            }
        }

        try {
            double cant = Double.parseDouble(txtCantidad.getText().replace(",", "."));
            if (cant > 0) {
                listaReceta.add(new ProductosController.DetalleRecetaUI(insumoSel, cant));
                cmbInsumos.getSelectionModel().clearSelection();
                txtCantidad.clear();
            }
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

            boolean exito = RecetaDAO.guardarReceta(productoEdicion.getId(), nuevaReceta);

            if (exito) {
                productoEdicion.setReceta(nuevaReceta);
                String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                App.registrarAuditoria(operador, "Receta", productoEdicion.getNombre(), "Actualización", 0, "Receta modificada vía Modal");

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
                                double nuevaCant = Double.parseDouble(val.replace(",", "."));
                                if (nuevaCant > 0) {
                                    sel.setCantidad(nuevaCant);
                                    tablaReceta.refresh();
                                }
                            } catch (NumberFormatException ex) {}
                        });
            }
        });

        itemEliminar.setOnAction(e -> {
            ProductosController.DetalleRecetaUI sel = tablaReceta.getSelectionModel().getSelectedItem();
            if (sel != null) listaReceta.remove(sel);
        });

        contextMenu.getItems().addAll(itemModificar, itemEliminar);
        tablaReceta.setContextMenu(contextMenu);
    }
}