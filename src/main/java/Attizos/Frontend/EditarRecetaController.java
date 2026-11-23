package Attizos.Frontend;

import Attizos.Backend.Api.ApiClient;
import Attizos.Backend.Attizos.*;
import Attizos.Backend.Database.ConexionSQLite;
import Attizos.Frontend.Network.WebSocketManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

public class EditarRecetaController {

    @FXML private AnchorPane rootPane;
    @FXML private Label lblTituloProducto;
    @FXML private Label lblUnidadInsumo; // Opcional en EditarReceta.fxml (null-safe)
    @FXML private ComboBox<Insumo> cmbInsumos;
    @FXML private TextField txtCantidad;
    @FXML private TableView<ProductosController.DetalleRecetaUI> tablaReceta;
    @FXML private TableColumn<ProductosController.DetalleRecetaUI, String> colInsumo;
    @FXML private TableColumn<ProductosController.DetalleRecetaUI, String> colCantidad;
    @FXML private Button btnCancelar;

    private double xOffset = 0;
    private double yOffset = 0;

    private Producto productoEdicion;
    private ObservableList<ProductosController.DetalleRecetaUI> listaReceta = FXCollections.observableArrayList();
    private ObservableList<Insumo> masterInsumos = FXCollections.observableArrayList();
    private FilteredList<Insumo> filteredInsumos;

    @FXML
    public void initialize() {
        colInsumo.setCellValueFactory(new PropertyValueFactory<>("nombreInsumo"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidadConUnidad"));
        tablaReceta.setItems(listaReceta);

        if (App.attizos.getInventario() != null && App.attizos.getInventario().getInventarioInsumos() != null) {
            masterInsumos.addAll(
                    App.attizos.getInventario().getInventarioInsumos().values().stream()
                            .sorted(Comparator.comparing(Insumo::getNombre, String.CASE_INSENSITIVE_ORDER))
                            .toList()
            );
        }

        filteredInsumos = new FilteredList<>(masterInsumos, p -> true);
        cmbInsumos.setItems(filteredInsumos);
        cmbInsumos.getEditor().setStyle("-fx-text-fill: #444444; -fx-font-weight: bold;");

        cmbInsumos.setCellFactory(lv -> new ListCell<Insumo>() {
            @Override
            protected void updateItem(Insumo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(formatearInsumo(item));
                    setStyle("-fx-text-fill: #111111; -fx-font-weight: bold;");
                }
            }
        });

        cmbInsumos.setConverter(new StringConverter<Insumo>() {
            @Override
            public String toString(Insumo insumo) {
                return formatearInsumo(insumo);
            }

            @Override
            public Insumo fromString(String texto) {
                if (texto == null || texto.trim().isEmpty()) return null;
                String limpio = texto.trim();
                return masterInsumos.stream()
                        .filter(i -> formatearInsumo(i).equalsIgnoreCase(limpio)
                                || i.getNombre().equalsIgnoreCase(limpio))
                        .findFirst()
                        .orElse(null);
            }
        });

        cmbInsumos.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            Insumo seleccionado = cmbInsumos.getSelectionModel().getSelectedItem();
            if (seleccionado != null && formatearInsumo(seleccionado).equals(newValue)) {
                return;
            }

            filteredInsumos.setPredicate(insumo -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String filtro = newValue.toLowerCase();
                return insumo.getNombre().toLowerCase().contains(filtro)
                        || obtenerUnidadLimpia(insumo).toLowerCase().contains(filtro);
            });

            if (newValue != null && !newValue.isEmpty() && !cmbInsumos.isShowing()) {
                cmbInsumos.show();
            }
        });

        cmbInsumos.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            actualizarIndicadorUnidad(newVal);
        });

        cmbInsumos.getEditor().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (cmbInsumos.getValue() == null && !filteredInsumos.isEmpty()) {
                    cmbInsumos.getSelectionModel().select(filteredInsumos.get(0));
                }
                txtCantidad.requestFocus();
                event.consume();
            }
        });

        txtCantidad.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                agregarIngrediente(new ActionEvent());
                event.consume();
            }
        });

        configurarMenuContextual();
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
        Platform.runLater(() -> cmbInsumos.requestFocus());
    }

    public void inicializarDatos(Producto producto) {
        this.productoEdicion = producto;
        listaReceta.clear();
        lblTituloProducto.setText("Receta de: " + producto.getNombreConTamano());

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
        if (insumoSel == null) {
            String texto = cmbInsumos.getEditor().getText();
            insumoSel = cmbInsumos.getConverter().fromString(texto);
        }
        if (insumoSel == null || txtCantidad.getText().trim().isEmpty()) {
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
            double cant = Double.parseDouble(txtCantidad.getText().trim().replace(",", "."));
            String uni = obtenerUnidadLimpia(insumoSel).toLowerCase();

            if (!validarRangoCantidad(cant, uni)) {
                return;
            }

            listaReceta.add(new ProductosController.DetalleRecetaUI(insumoSel, cant));
            cmbInsumos.getSelectionModel().clearSelection();
            cmbInsumos.getEditor().clear();
            txtCantidad.clear();
            actualizarIndicadorUnidad(null);
            Platform.runLater(() -> cmbInsumos.requestFocus());

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
                "¿Está seguro de sobreescribir la receta de " + productoEdicion.getNombreConTamano() + "?",
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
                String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                App.registrarAuditoria(operador, "Receta", productoEdicion.getNombreConTamano(), "Actualización", 0, "Receta modificada vía Modal");

                new Thread(() -> {
                    ConexionSQLite.subirAuditoriaPendiente();
                    ConexionSQLite.sincronizarProductos();
                    WebSocketManager.notificarCambio("SYNC_CATALOGO");
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
                String uniTexto = sel.getUnidad();
                String sufijo = uniTexto.isEmpty() ? "" : " (" + uniTexto + ")";
                String pregunta = uniTexto.isEmpty()
                        ? "Ingrese la nueva cantidad:"
                        : "Ingrese la nueva cantidad en " + uniTexto + ":";

                DialogoPersonalizado.mostrarDialogo(
                        "Modificar Cantidad",
                        "Modificando: " + sel.getNombreInsumo() + sufijo,
                        pregunta,
                        String.valueOf(sel.getCantidad())
                ).ifPresent(val -> {
                    try {
                        double cant = Double.parseDouble(val.trim().replace(",", "."));
                        if (!validarRangoCantidad(cant, uniTexto.toLowerCase())) {
                            return;
                        }
                        sel.setCantidad(cant);
                        tablaReceta.refresh();
                    } catch (NumberFormatException ex) {
                        mostrarAlerta("Error", "Ingrese un número válido.");
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

    private void actualizarIndicadorUnidad(Insumo insumo) {
        if (insumo == null) {
            txtCantidad.setPromptText("Cant.");
            if (lblUnidadInsumo != null) {
                lblUnidadInsumo.setVisible(false);
                lblUnidadInsumo.setManaged(false);
            }
            return;
        }

        String uni = obtenerUnidadLimpia(insumo);
        txtCantidad.setPromptText(uni.isEmpty() ? "Cant." : "Cant. (" + uni + ")");

        if (lblUnidadInsumo != null) {
            String stockFmt = String.format("%.2f", insumo.getStockActual());
            String texto = uni.isEmpty()
                    ? "📏 Insumo: " + insumo.getNombre() + " (Stock: " + stockFmt + ")"
                    : "📏 Ingrese la cantidad en: " + uni.toUpperCase() + "   (Disponible: " + stockFmt + " " + uni + ")";
            lblUnidadInsumo.setText(texto);
            lblUnidadInsumo.setVisible(true);
            lblUnidadInsumo.setManaged(true);
        }
    }

    private boolean validarRangoCantidad(double cant, String uni) {
        if (cant < 0.001) {
            mostrarAlerta("Error", "La cantidad es demasiado pequeña (Mínimo 0.001).");
            return false;
        }
        if ((uni.equals("kg") || uni.equals("lt")) && cant > 10) {
            mostrarAlerta("Exageración", "No puedes usar más de 10 " + uni + " en una sola porción/plato.");
            return false;
        }
        if ((uni.equals("g") || uni.equals("ml")) && cant > 10000) {
            mostrarAlerta("Exageración", "No puedes usar más de 10,000 " + uni + " en una sola porción.");
            return false;
        }
        if ((uni.equals("und") || uni.equals("paquete")) && cant > 50) {
            mostrarAlerta("Exageración", "No puedes usar más de 50 " + uni + " en una receta individual.");
            return false;
        }
        return true;
    }

    private String formatearInsumo(Insumo insumo) {
        if (insumo == null) return "";
        String uni = obtenerUnidadLimpia(insumo);
        return uni.isEmpty() ? insumo.getNombre() : insumo.getNombre() + " (" + uni + ")";
    }

    private String obtenerUnidadLimpia(Insumo insumo) {
        return (insumo != null && insumo.getUnidad() != null) ? insumo.getUnidad().trim() : "";
    }

    private static void mostrarAlerta(String titulo, String mensaje) {
        AlertaPersonalizada.mostrarAlerta(titulo, mensaje, Alert.AlertType.WARNING);
    }
}