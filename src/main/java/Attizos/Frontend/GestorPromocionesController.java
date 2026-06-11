package Attizos.Frontend;

import Attizos.Backend.Attizos.App;
import Attizos.Backend.Attizos.DetalleCombo;
import Attizos.Backend.Attizos.Producto;
import Attizos.Backend.Attizos.Promocion;
import Attizos.Backend.Database.ConexionBD;
import Attizos.Backend.Database.ProductoDAO;
import Attizos.Backend.Database.PromocionDAO;
import Attizos.Backend.Listas.NodoDE;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;

public class GestorPromocionesController {

    @FXML private AnchorPane rootPane;

    @FXML private TableView<Promocion> tablaPromos;
    @FXML private TableColumn<Promocion, Integer> colId;
    @FXML private TableColumn<Promocion, String> colNombrePromo, colInicio, colFin, colEstado;
    @FXML private TableColumn<Promocion, Double> colPrecio;

    @FXML private Label lblTituloFormulario;
    @FXML private TextField txtNombre, txtPrecio, txtCantidad;
    @FXML private DatePicker dpInicio, dpFin;
    @FXML private ComboBox<Producto> cmbProductosMenu;
    @FXML private ImageView imgPreview;
    @FXML private Button btnAgregarAlCombo;

    @FXML private TableView<DetalleCombo> tablaContenido;
    @FXML private TableColumn<DetalleCombo, String> colCProducto;
    @FXML private TableColumn<DetalleCombo, Integer> colCCantidad;

    private ObservableList<Promocion> listaPromosVisibles = FXCollections.observableArrayList();
    private ObservableList<DetalleCombo> detallesTemporales = FXCollections.observableArrayList();

    private ObservableList<Producto> masterMenu = FXCollections.observableArrayList();
    private FilteredList<Producto> filteredMenu;

    private File archivoImagenSeleccionada;
    private Promocion promoEnEdicion = null;
    private double xOffset = 0, yOffset = 0;

    @FXML
    public void initialize() {
        configurarArrastreVentana();
        estilizarControles();
        configurarTablas();
        cargarDatos();
        configurarBuscadorInteligente();
        configurarMenuContextualContenido();

    }

    private void estilizarControles() {
        String estiloTextoOscuro = "-fx-text-fill: #111111; -fx-font-weight: bold;";

        txtCantidad.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #DDDDDD; -fx-border-radius: 5; -fx-padding: 8; -fx-prompt-text-fill: #999999; " + estiloTextoOscuro);
        dpInicio.getEditor().setStyle(estiloTextoOscuro + " -fx-background-color: #FFFFFF;");
        dpFin.getEditor().setStyle(estiloTextoOscuro + " -fx-background-color: #FFFFFF;");

        // Estilizar el campo de texto interno del ComboBox Editable para máxima visibilidad
        cmbProductosMenu.getEditor().setStyle(estiloTextoOscuro + " -fx-background-color: transparent; -fx-prompt-text-fill: #999999;");
    }

    private void configurarBuscadorInteligente() {
        // Permitir que el ComboBox convierta el texto escrito al Objeto Producto real
        cmbProductosMenu.setConverter(new StringConverter<Producto>() {
            @Override
            public String toString(Producto p) {
                return p == null ? "" : p.getNombre();
            }
            @Override
            public Producto fromString(String string) {
                return masterMenu.stream().filter(p -> p.getNombre().equals(string)).findFirst().orElse(null);
            }
        });

        cmbProductosMenu.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            Producto seleccionado = cmbProductosMenu.getSelectionModel().getSelectedItem();
            if (seleccionado != null && seleccionado.getNombre().equals(newVal)) return;

            filteredMenu.setPredicate(producto -> {
                if (newVal == null || newVal.isEmpty()) return true;
                return producto.getNombre().toLowerCase().contains(newVal.toLowerCase());
            });

            if (newVal != null && !newVal.isEmpty() && !cmbProductosMenu.isShowing()) {
                cmbProductosMenu.show();
            }
        });

        // Control de teclado avanzado en el ComboBox
        cmbProductosMenu.getEditor().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                if (cmbProductosMenu.getValue() != null) {
                    txtCantidad.requestFocus();
                }
            } else if (e.getCode() == KeyCode.DOWN || e.getCode() == KeyCode.UP) {
                if (!cmbProductosMenu.isShowing()) cmbProductosMenu.show();
            }
        });

        txtNombre.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) txtPrecio.requestFocus(); });
        txtPrecio.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) cmbProductosMenu.requestFocus(); });

        txtCantidad.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                btnAgregarAlCombo.fire();
            }
        });
    }

    private void configurarArrastreVentana() {
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

    private void configurarTablas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombrePromo.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colInicio.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFechaInicio() != null ? c.getValue().getFechaInicio().toString() : "Siempre"));
        colFin.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFechaFin() != null ? c.getValue().getFechaFin().toString() : "Indefinido"));
        tablaPromos.setItems(listaPromosVisibles);

        colCProducto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProducto().getNombre()));
        colCCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        tablaContenido.setItems(detallesTemporales);

        filteredMenu = new FilteredList<>(masterMenu, p -> true);
        cmbProductosMenu.setItems(filteredMenu);

        cmbProductosMenu.setCellFactory(lv -> new ListCell<Producto>() {
            @Override
            protected void updateItem(Producto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item.getNombre() + " (Bs." + item.getPrecio() + ")");
                    setStyle("-fx-text-fill: #111111; -fx-font-weight: bold;");
                }
            }
        });
    }

    private void configurarMenuContextualContenido() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem itemEliminar = new MenuItem("🗑 Quitar del Combo");
        itemEliminar.setOnAction(e -> {
            DetalleCombo seleccionado = tablaContenido.getSelectionModel().getSelectedItem();
            if (seleccionado != null) detallesTemporales.remove(seleccionado);
        });
        contextMenu.getItems().add(itemEliminar);
        tablaContenido.setContextMenu(contextMenu);
    }

    private void cargarDatos() {
        listaPromosVisibles.clear();
        masterMenu.clear();

        for(Promocion promo : App.attizos.getPromocionesActivas()) {
            if (promo.getEstado() != null && promo.getEstado().equals("Activo")) {
                listaPromosVisibles.add(promo);
            }
        }
    }

    @FXML
    void seleccionarImagen(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
        archivoImagenSeleccionada = fc.showOpenDialog(null);
        if (archivoImagenSeleccionada != null) {
            imgPreview.setImage(new Image(archivoImagenSeleccionada.toURI().toString()));
        }
    }

    @FXML
    void agregarAlCombo(ActionEvent event) {
        Producto seleccionado = cmbProductosMenu.getValue();
        String cantStr = txtCantidad.getText().trim();

        if (seleccionado == null || cantStr.isEmpty()) {
            AlertaPersonalizada.mostrarAlerta("Atención", "Seleccione un producto y digite la cantidad.", Alert.AlertType.WARNING);
            return;
        }

        try {
            int cantidad = Integer.parseInt(cantStr);
            if (cantidad <= 0) throw new NumberFormatException();

            boolean existe = false;
            for (DetalleCombo d : detallesTemporales) {
                if (d.getProducto().getId() == seleccionado.getId()) {
                    d.setCantidad(d.getCantidad() + cantidad);
                    existe = true;
                    break;
                }
            }
            if (!existe) detallesTemporales.add(new DetalleCombo(seleccionado, cantidad));

            tablaContenido.refresh();

            cmbProductosMenu.getSelectionModel().clearSelection();
            cmbProductosMenu.getEditor().clear();
            txtCantidad.clear();
            cmbProductosMenu.requestFocus();

        } catch (NumberFormatException e) {
            AlertaPersonalizada.mostrarAlerta("Error", "La cantidad debe ser un número entero válido.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    void guardarPromocion(ActionEvent event) {
        try {
            String nombre = txtNombre.getText().trim();
            if (nombre.isEmpty()) { AlertaPersonalizada.mostrarAlerta("Error", "Ingrese nombre del combo.", Alert.AlertType.WARNING); return; }

            double precio = Double.parseDouble(txtPrecio.getText().replace(",", "."));
            if (precio <= 0) { AlertaPersonalizada.mostrarAlerta("Error", "Precio inválido.", Alert.AlertType.WARNING); return; }

            if (detallesTemporales.isEmpty()) {
                AlertaPersonalizada.mostrarAlerta("Error", "El combo debe tener al menos un producto.", Alert.AlertType.WARNING);
                return;
            }

            String nombreImg = "default.png";
            if (archivoImagenSeleccionada != null) {
                nombreImg = UtilidadesImagen.guardarImagenLocal(archivoImagenSeleccionada, nombre);
            } else if (promoEnEdicion != null) {
                nombreImg = promoEnEdicion.getImagenURL();
            } else {
                java.util.Optional<String> resultado = DialogoPersonalizado.mostrarDialogo(
                        "Confirmar Imagen",
                        "No se ha seleccionado ninguna portada para el combo.",
                        "¿Está seguro de que desea guardar el combo con la imagen por defecto?",
                        "Sí, usar por defecto"
                );
                if (resultado.isEmpty()) return;
            }

            LocalDate inicio = dpInicio.getValue();
            LocalDate fin = dpFin.getValue();

            Promocion promoAGuardar = new Promocion(promoEnEdicion == null ? 0 : promoEnEdicion.getId(), nombre, precio, nombreImg, inicio, fin);
            for (DetalleCombo dc : detallesTemporales) {
                promoAGuardar.agregarProducto(dc.getProducto(), dc.getCantidad());
            }

            String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";

            if (promoEnEdicion == null) {
                if (PromocionDAO.guardarNuevaPromocion(promoAGuardar)) {
                    App.attizos.getPromocionesActivas().add(promoAGuardar);
                    App.registrarAuditoria(operador, "Promociones", nombre, "Creación", 0, "Nueva promoción a Bs." + precio);
                    AlertaPersonalizada.mostrarAlerta("Éxito", "Promoción creada correctamente.", Alert.AlertType.INFORMATION);
                }
            } else {
                if (actualizarPromocionEnBaseDeDatos(promoAGuardar)) {
                    promoEnEdicion.setNombre(nombre);
                    promoEnEdicion.setPrecio(precio);
                    promoEnEdicion.setFechaInicio(inicio);
                    promoEnEdicion.setFechaFin(fin);
                    promoEnEdicion.setImagenURL(nombreImg);
                    promoEnEdicion.getProductosCombo().clear();
                    promoEnEdicion.getProductosCombo().addAll(detallesTemporales);

                    App.registrarAuditoria(operador, "Promociones", nombre, "Edición", 0, "Promoción modificada.");
                    AlertaPersonalizada.mostrarAlerta("Éxito", "Promoción actualizada.", Alert.AlertType.INFORMATION);
                }
            }

            cargarDatos();
            limpiarFormulario();

        } catch (NumberFormatException e) {
            AlertaPersonalizada.mostrarAlerta("Error", "El precio debe ser numérico.", Alert.AlertType.ERROR);
        }
    }

    private boolean actualizarPromocionEnBaseDeDatos(Promocion promo) {
        if (!ProductoDAO.actualizarProducto(promo)) return false;

        String sqlDelete = "DELETE FROM detalle_combo WHERE id_promocion = ?";
        String sqlInsert = "INSERT INTO detalle_combo (id_promocion, id_producto, cantidad) VALUES (?, ?, ?)";

        try (Connection conn = ConexionBD.getConexion()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psDel = conn.prepareStatement(sqlDelete)) {
                psDel.setInt(1, promo.getId());
                psDel.executeUpdate();
            }

            try (PreparedStatement psIns = conn.prepareStatement(sqlInsert)) {
                for (DetalleCombo dc : promo.getProductosCombo()) {
                    psIns.setInt(1, promo.getId());
                    psIns.setInt(2, dc.getProducto().getId());
                    psIns.setInt(3, dc.getCantidad());
                    psIns.addBatch();
                }
                psIns.executeBatch();
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            System.err.println("Error actualizando promo: " + e.getMessage());
            return false;
        }
    }

    @FXML
    void cargarParaEditar(ActionEvent event) {
        Promocion sel = tablaPromos.getSelectionModel().getSelectedItem();
        if (sel == null) { AlertaPersonalizada.mostrarAlerta("Atención", "Seleccione una promoción.", Alert.AlertType.WARNING); return; }

        promoEnEdicion = sel;
        lblTituloFormulario.setText("Editando: " + sel.getNombre());
        txtNombre.setText(sel.getNombre());
        txtPrecio.setText(String.valueOf(sel.getPrecio()));
        dpInicio.setValue(sel.getFechaInicio());
        dpFin.setValue(sel.getFechaFin());

        detallesTemporales.clear();
        detallesTemporales.addAll(sel.getProductosCombo());

        String imgPath = sel.getImagenURL();
        if (imgPath != null && !imgPath.equals("default.png")) {
            File file = new File(System.getenv("APPDATA") + "\\Attizos\\Imagenes\\" + imgPath);
            if (file.exists()) imgPreview.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    void terminarPromocion(ActionEvent event) {
        Promocion sel = tablaPromos.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        String nuevoEstado = sel.getEstado().equals("Activo") ? "Inactivo" : "Activo";
        sel.setEstado(nuevoEstado);

        if (ProductoDAO.actualizarProducto(sel)) {
            String op = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
            App.registrarAuditoria(op, "Promociones", sel.getNombre(), "Cambio Estado", 0, "Estado cambiado a: " + nuevoEstado);
            cargarDatos();
            tablaPromos.refresh();
        }
    }

    @FXML
    void eliminarPromocion(ActionEvent event) {
        Promocion sel = tablaPromos.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        DialogoPersonalizado.mostrarDialogo("Eliminar Promo", "Borrando: " + sel.getNombre(), "Motivo de la eliminación:", "")
                .ifPresent(motivo -> {
                    if (motivo.isEmpty()) {
                        AlertaPersonalizada.mostrarAlerta("Error", "Debe justificar la eliminación.", Alert.AlertType.WARNING);
                    } else if (ProductoDAO.eliminarProducto(sel.getId())) {
                        App.attizos.getPromocionesActivas().remove(sel);

                        String op = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                        App.registrarAuditoria(op, "Promociones", sel.getNombre(), "Eliminación", 0, "Motivo: " + motivo);

                        cargarDatos();
                        AlertaPersonalizada.mostrarAlerta("Eliminado", "Promoción eliminada con éxito.", Alert.AlertType.INFORMATION);
                    }
                });
    }

    @FXML
    void limpiarFormularioAction(ActionEvent event) {
        limpiarFormulario();
    }

    private void limpiarFormulario() {
        promoEnEdicion = null;
        lblTituloFormulario.setText("Crear Nuevo Combo");
        txtNombre.clear();
        txtPrecio.clear();
        cmbProductosMenu.getSelectionModel().clearSelection();
        cmbProductosMenu.getEditor().clear();
        txtCantidad.clear();
        dpInicio.setValue(null);
        dpFin.setValue(null);
        imgPreview.setImage(null);
        archivoImagenSeleccionada = null;
        detallesTemporales.clear();
    }

    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.close();
    }
}