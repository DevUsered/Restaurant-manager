package Attizos.Frontend.Cobros;

import Attizos.Backend.Attizos.App;
import Attizos.Backend.Attizos.DetalleCombo;
import Attizos.Backend.Attizos.Producto;
import Attizos.Backend.Attizos.Promocion;
import Attizos.Backend.Database.ConexionBD;
import Attizos.Backend.Database.ProductoDAO;
import Attizos.Backend.Database.PromocionDAO;
import Attizos.Backend.Listas.NodoDE;
import Attizos.Frontend.AlertaPersonalizada;
import Attizos.Frontend.DialogoPersonalizado;
import Attizos.Frontend.UtilidadesImagen;
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

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;

public class GestorPromocionesController {
    @FXML private AnchorPane rootPane;

    @FXML private TableView<Promocion> tablaPromos;
    @FXML
    private TableColumn<Promocion, Integer> colId;
    @FXML private TableColumn<Promocion, String> colNombrePromo, colInicio, colFin, colEstado;
    @FXML private TableColumn<Promocion, Double> colPrecio;

    @FXML private Label lblTituloFormulario;
    @FXML private TextField txtNombre, txtPrecio, txtBuscarProducto, txtCantidad;
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
    public void initialize(){
        configurarArrastreVentana();
        configurarTablas();
        cargarDatos();
        configurarBuscadorYTeclado();
        configurarMenuContextualContenido();
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
                setText(empty ? null : item.getNombre() + " (Bs." + item.getPrecio() + ")");
            }
        });
        cmbProductosMenu.setButtonCell(new ListCell<Producto>() {
            @Override
            protected void updateItem(Producto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item.getNombre());
            }
        });
    }
    private void configurarBuscadorYTeclado() {
        // Filtrado en tiempo real
        txtBuscarProducto.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredMenu.setPredicate(producto -> {
                if (newValue == null || newValue.isEmpty()) return true;
                return producto.getNombre().toLowerCase().contains(newValue.toLowerCase());
            });
            if (newValue != null && !newValue.isEmpty()) cmbProductosMenu.show();
        });

        txtNombre.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) txtPrecio.requestFocus(); });
        txtPrecio.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) txtBuscarProducto.requestFocus(); });

        txtBuscarProducto.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN || e.getCode() == KeyCode.ENTER) {
                cmbProductosMenu.requestFocus();
                cmbProductosMenu.show();
            }
        });

        cmbProductosMenu.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                if (cmbProductosMenu.getValue() != null) txtCantidad.requestFocus();
            }
        });

        txtCantidad.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                btnAgregarAlCombo.fire();
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

        NodoDE<Promocion> acPromo = App.attizos.getPromocionesActivas().getCabeza();
        while (acPromo != null) {
            listaPromosVisibles.add(acPromo.getDato());
            acPromo = acPromo.getSiguiente();
        }

        NodoDE<Producto> acProd = App.attizos.getMenu().getCabeza();
        while (acProd != null) {
            Producto p = acProd.getDato();
            if (!p.isPromocion() && "Activo".equals(p.getEstado())) {
                masterMenu.add(p);
            }
            acProd = acProd.getSiguiente();
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

            // Volver el cursor al buscador para seguir agregando rápido
            txtBuscarProducto.clear();
            cmbProductosMenu.getSelectionModel().clearSelection();
            txtCantidad.clear();
            txtBuscarProducto.requestFocus();

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

            LocalDate inicio = dpInicio.getValue();
            LocalDate fin = dpFin.getValue();

            String nombreImg = "default.png";
            if (archivoImagenSeleccionada != null) {
                nombreImg = UtilidadesImagen.guardarImagenLocal(archivoImagenSeleccionada, nombre);
            } else if (promoEnEdicion != null) {
                nombreImg = promoEnEdicion.getImagenURL();
            }

            Promocion promoAGuardar = new Promocion(promoEnEdicion == null ? 0 : promoEnEdicion.getId(), nombre, precio, nombreImg, inicio, fin);
            for (DetalleCombo dc : detallesTemporales) {
                promoAGuardar.agregarProducto(dc.getProducto(), dc.getCantidad());
            }

            String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";

            if (promoEnEdicion == null) {
                // MODO: CREAR
                if (PromocionDAO.guardarNuevaPromocion(promoAGuardar)) {
                    App.attizos.getPromocionesActivas().insertarAlFinal(promoAGuardar);
                    App.registrarAuditoria(operador, "Promociones", nombre, "Creación", 0, "Nueva promoción registrada a Bs." + precio);
                    AlertaPersonalizada.mostrarAlerta("Éxito", "Promoción creada correctamente.", Alert.AlertType.INFORMATION);
                }
            } else {
                // MODO: EDITAR
                if (actualizarPromocionEnBaseDeDatos(promoAGuardar)) {
                    // Actualizar en RAM
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
                        App.attizos.getPromocionesActivas().eliminarPorValor(sel);

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
        txtBuscarProducto.clear();
        txtCantidad.clear();
        dpInicio.setValue(null);
        dpFin.setValue(null);
        cmbProductosMenu.getSelectionModel().clearSelection();
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
