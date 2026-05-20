package Attizos.Frontend;

import Attizos.Backend.Attizos.*;
import Attizos.Backend.Listas.NodoDE;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ProductosController {

    @FXML private TableView<Producto> tablaMenu;
    @FXML private TableColumn<Producto, Integer> colId;
    @FXML private TableColumn<Producto, String> colNombre, colCategoria, colStock, colTipo;
    @FXML private TableColumn<Producto, Double> colPrecio;

    @FXML private TextField txtBuscador, txtNombre, txtPrecio, txtStock, txtCantidadReceta;
    @FXML private ComboBox<String> cmbCategoria, cmbTipoClase;
    @FXML private ComboBox<Insumo> cmbInsumos;

    @FXML private VBox panelAtributosDinamicos;
    @FXML private VBox vboxCategoria;
    @FXML private VBox vboxStock; // <-- Controlador del Stock Inicial

    @FXML private ImageView imgPreview;
    @FXML private TableView<DetalleRecetaUI> tablaRecetaFila;
    @FXML private TableColumn<DetalleRecetaUI, String> colRecetaInsumo;
    @FXML private TableColumn<DetalleRecetaUI, Double> colRecetaCant;

    private ObservableList<Producto> masterData = FXCollections.observableArrayList();
    private File archivoImagenSeleccionada;
    private ObservableList<DetalleRecetaUI> recetaTemporal = FXCollections.observableArrayList();

    // Campos dinámicos (SIN rastros de ingredientes base)
    private ComboBox<String> cmbTamanoPizza = new ComboBox<>(FXCollections.observableArrayList("Personal", "Mediana", "Familiar", "Gigante"));
    private CheckBox chkExtraQueso = new CheckBox("¿Lleva Extra Queso?");
    private ComboBox<TamanoBebida> cmbTamanoBebida = new ComboBox<>(FXCollections.observableArrayList(TamanoBebida.values()));
    private TextField txtTipoBebida = new TextField();
    private TextField txtSalsaPasta = new TextField();

    @FXML
    public void initialize() {
        configurarTabla();

        cmbTipoClase.setItems(FXCollections.observableArrayList("Pizza", "Bebida", "Pasta", "Calzone", "Postre", "Otro"));
        cmbTipoClase.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> actualizarCamposDinamicos(newVal));

        // Por defecto, ocultar categoría y stock hasta que elijan algo
        vboxCategoria.setVisible(false); vboxCategoria.setManaged(false);
        vboxStock.setVisible(false); vboxStock.setManaged(false);

        colRecetaInsumo.setCellValueFactory(new PropertyValueFactory<>("nombreInsumo"));
        colRecetaCant.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        tablaRecetaFila.setItems(recetaTemporal);

        cargarMenu();
        cargarInsumos();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colStock.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().tieneReceta() ? "Cocina" : String.valueOf(d.getValue().getStock())));
        colTipo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getClass().getSimpleName()));

        tablaMenu.setItems(masterData);
    }

    // TU MÉTODO MANTENIDO Y MEJORADO
    private boolean esDeCocina(String str){
        // Si es Bebida u Otro, NO es de cocina.
        return !str.equalsIgnoreCase("Bebida") && !str.equalsIgnoreCase("Otro");
    }

    private void actualizarCamposDinamicos(String tipo) {
        panelAtributosDinamicos.getChildren().clear();
        if (tipo == null) return;

        boolean esOtro = tipo.equals("Otro");
        vboxCategoria.setVisible(esOtro);
        vboxCategoria.setManaged(esOtro);

        boolean esProductoDeCocina = esDeCocina(tipo);

        // El stock se oculta si ES de cocina
        vboxStock.setVisible(!esProductoDeCocina);
        vboxStock.setManaged(!esProductoDeCocina);

        if(esProductoDeCocina) {
            txtStock.clear();
        }

        // CORRECCIÓN DEL BUG DE VISIBILIDAD: Se debe asignar el valor de 'esProductoDeCocina'
        // para que se oculte (false) o se muestre (true) dinámicamente.
        cmbInsumos.setVisible(esProductoDeCocina);
        cmbInsumos.setManaged(esProductoDeCocina);
        txtCantidadReceta.setVisible(esProductoDeCocina);
        txtCantidadReceta.setManaged(esProductoDeCocina);
        tablaRecetaFila.setVisible(esProductoDeCocina);
        tablaRecetaFila.setManaged(esProductoDeCocina);

        Label lbl = new Label("Configuración de " + tipo + ":");
        lbl.setStyle("-fx-text-fill: #00ff88; -fx-font-weight: bold;");
        panelAtributosDinamicos.getChildren().add(lbl);

        switch (tipo) {
            case "Pizza" -> {
                chkExtraQueso.setStyle("-fx-text-fill: white;");
                panelAtributosDinamicos.getChildren().addAll(new Label("Tamaño:"), cmbTamanoPizza, chkExtraQueso);
            }
            case "Bebida" -> {
                txtTipoBebida.setPromptText("Tipo (Gaseosa, Jugo...)");
                panelAtributosDinamicos.getChildren().addAll(new Label("Tamaño:"), cmbTamanoBebida, txtTipoBebida);
            }
            case "Pasta" -> {
                txtSalsaPasta.setPromptText("Salsa (Boloñesa, Alfredo...)");
                panelAtributosDinamicos.getChildren().addAll(new Label("Salsa:"), txtSalsaPasta);
            }
            case "Calzone", "Postre" -> {
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
    void agregarIngredienteVisual(ActionEvent event) {
        Insumo insumoSeleccionado = cmbInsumos.getSelectionModel().getSelectedItem();
        String cantTexto = txtCantidadReceta.getText().trim();

        if (insumoSeleccionado == null || cantTexto.isEmpty()) {
            mostrarAlerta("Datos incompletos", "Seleccione un insumo y digite la cantidad.");
            return;
        }
        try {
            cantTexto = cantTexto.replace(",", ".");
            double cantidad = Double.parseDouble(cantTexto);
            if (cantidad <= 0) {
                mostrarAlerta("Error", "La cantidad debe ser mayor a 0.");
                return;
            }
            recetaTemporal.add(new DetalleRecetaUI(insumoSeleccionado, cantidad));
            cmbInsumos.getSelectionModel().clearSelection();
            txtCantidadReceta.clear();
        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "Cantidad inválida. Ingrese un número válido (Ej: 1.5).");
        }
    }

    @FXML
    void guardarNuevoProducto(ActionEvent event) {
        try {
            int id = generarSiguienteId();
            String nombre = txtNombre.getText().trim();
            if (nombre.isEmpty()) { mostrarAlerta("Error", "Debe escribir el nombre del producto."); return; }

            String precioTxt = txtPrecio.getText().replace(",", ".");
            double precio = Double.parseDouble(precioTxt);

            String tipo = cmbTipoClase.getValue();
            if (tipo == null) { mostrarAlerta("Error", "Debe seleccionar un tipo de producto."); return; }

            // AUTO-STOCK: Si está oculto asume 0, si es visible lee el TextField
            int stock = 0;
            if (vboxStock.isVisible() && !txtStock.getText().isEmpty()) {
                stock = Integer.parseInt(txtStock.getText());
            }

            // AUTO-CATEGORÍA: Si es "Otro" usa el ComboBox, sino usa el Tipo
            String cat = tipo.equals("Otro") && cmbCategoria.getValue() != null ? cmbCategoria.getValue() : tipo;

            String nombreImagenFinal = "default.png";
            if (archivoImagenSeleccionada != null) {
                String ext = archivoImagenSeleccionada.getName().substring(archivoImagenSeleccionada.getName().lastIndexOf("."));
                nombreImagenFinal = nombre.toLowerCase().replace(" ", "_") + ext;
                File destino = new File("src/main/resources/images/Productos/" + nombreImagenFinal);
                Files.copy(archivoImagenSeleccionada.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // CORRECCIÓN CONSTRUCTORES: Se vuelve a incluir el parámetro 'stock' para que Java compile.
            Producto nuevo;
            switch (tipo) {
                case "Pizza" -> nuevo = new Pizza(id, nombre, precio, cat, nombreImagenFinal, cmbTamanoPizza.getValue(), "", chkExtraQueso.isSelected());
                case "Bebida" -> nuevo = new Bebida(id, nombre, precio, cat, stock, nombreImagenFinal, cmbTamanoBebida.getValue(), txtTipoBebida.getText());
                case "Pasta" -> nuevo = new Pasta(id, nombre, precio, cat, nombreImagenFinal, "", txtSalsaPasta.getText());
                case "Calzone" -> nuevo = new Calzone(id, nombre, precio, cat, nombreImagenFinal, "");
                case "Postre" -> nuevo = new Postre(id, nombre, precio, cat, nombreImagenFinal, "Normal", "Dulce", "Postre");
                default -> nuevo = new Producto(id, nombre, precio, cat, stock, nombreImagenFinal);
            }

            if (!recetaTemporal.isEmpty()) {
                Receta r = new Receta();
                for (DetalleRecetaUI det : recetaTemporal) r.agregarIngrediente(det.getInsumo().getCodigo(), det.getCantidad());
                nuevo.setReceta(r);
            }

            App.attizos.getMenu().insertarAlFinal(nuevo);
            cargarMenu();
            mostrarExito("Éxito", "Producto guardado correctamente.");
            limpiarFormulario();

        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "Verifique que el precio y el stock sean números válidos.");
        } catch (Exception e) {
            mostrarAlerta("Error", "Ocurrió un error al guardar: " + e.getMessage());
        }
    }

    @FXML
    void sumarStockExistente(ActionEvent event) {
        Producto sel = tablaMenu.getSelectionModel().getSelectedItem();
        if (sel == null) { mostrarAlerta("Atención", "Seleccione un producto primero."); return; }
        if (sel.tieneReceta()) { mostrarAlerta("Inválido", "Stock controlado por cocina."); return; }

        TextInputDialog dialogCant = new TextInputDialog();
        dialogCant.setTitle("Sumar Stock");
        dialogCant.setHeaderText("Añadiendo mercadería a: " + sel.getNombre());
        dialogCant.setContentText("Ingrese la cantidad a sumar:");

        dialogCant.showAndWait().ifPresent(cantStr -> {
            try {
                int cantidad = Integer.parseInt(cantStr);
                if (cantidad <= 0) throw new NumberFormatException();

                TextInputDialog dialogCosto = new TextInputDialog("0");
                dialogCosto.setTitle("Costo de Compra");
                dialogCosto.setHeaderText("Costo total por las " + cantidad + " unidades:");
                dialogCosto.setContentText("¿Cuánto costó? (Bs):");

                dialogCosto.showAndWait().ifPresent(costoStr -> {
                    try {
                        double costoTotal = Double.parseDouble(costoStr.replace(",", "."));
                        App.attizos.registerExpense(new Egreso("Compra: " + sel.getNombre() + " (" + cantidad + " und)", costoTotal));
                        sel.aumentarStock(cantidad);
                        tablaMenu.refresh();
                        mostrarExito("Stock Actualizado", "Stock sumado exitosamente.");
                    } catch (NumberFormatException ex) {
                        mostrarAlerta("Error", "El costo debe ser un número válido.");
                    }
                });
            } catch (NumberFormatException e) {
                mostrarAlerta("Error", "La cantidad debe ser un número entero mayor a 0.");
            }
        });
    }

    @FXML
    void restarStock(ActionEvent event) {
        Producto sel = tablaMenu.getSelectionModel().getSelectedItem();
        if (sel == null) { mostrarAlerta("Atención", "Seleccione un producto primero."); return; }
        if (sel.tieneReceta()) { mostrarAlerta("Inválido", "No se puede restar de cocina."); return; }

        TextInputDialog dialogCant = new TextInputDialog();
        dialogCant.setTitle("Restar Stock");
        dialogCant.setHeaderText("Dando de baja mercadería de: " + sel.getNombre());
        dialogCant.setContentText("Ingrese la cantidad a restar:");

        dialogCant.showAndWait().ifPresent(cantStr -> {
            try {
                int cantidad = Integer.parseInt(cantStr);
                if (cantidad <= 0) throw new NumberFormatException();

                TextInputDialog dialogMotivo = new TextInputDialog();
                dialogMotivo.setTitle("Justificación");
                dialogMotivo.setHeaderText("Motivo de la baja:");
                dialogMotivo.setContentText("Describa por qué (Ej: Caducado, Merma):");

                dialogMotivo.showAndWait().ifPresent(motivo -> {
                    if (motivo.trim().isEmpty()) {
                        mostrarAlerta("Obligatorio", "Debe justificar la baja.");
                    } else if (sel.reducirStock(cantidad)) {
                        tablaMenu.refresh();
                        mostrarExito("Baja Exitosa", "Se restaron " + cantidad + " unidades.\nMotivo: " + motivo);
                    } else {
                        mostrarAlerta("Stock Insuficiente", "No hay suficiente stock para restar esa cantidad.");
                    }
                });
            } catch (NumberFormatException e) {
                mostrarAlerta("Error", "La cantidad debe ser un número entero mayor a 0.");
            }
        });
    }

    @FXML
    void eliminarProducto(ActionEvent event) {
        Producto sel = tablaMenu.getSelectionModel().getSelectedItem();
        if (sel == null) { mostrarAlerta("Atención", "Seleccione un producto primero."); return; }

        TextInputDialog dialogMotivo = new TextInputDialog();
        dialogMotivo.setTitle("Eliminar Producto");
        dialogMotivo.setHeaderText("Eliminando permanentemente: " + sel.getNombre());
        dialogMotivo.setContentText("Describa el motivo de la eliminación:");

        dialogMotivo.showAndWait().ifPresent(motivo -> {
            if (motivo.trim().isEmpty()) {
                mostrarAlerta("Seguridad", "Es obligatorio escribir un motivo para eliminar el producto.");
            } else {
                App.attizos.getMenu().eliminarPorValor(sel);
                cargarMenu();
                mostrarExito("Eliminado", "Producto dado de baja permanentemente.\nMotivo: " + motivo);
            }
        });
    }

    private int generarSiguienteId() {
        int max = 0;
        NodoDE<Producto> act = App.attizos.getMenu().getCabeza();
        while (act != null) {
            if (act.getDato().getId() > max) max = act.getDato().getId();
            act = act.getSiguiente();
        }
        return max + 1;
    }

    private void cargarMenu() {
        masterData.clear();
        NodoDE<Producto> act = App.attizos.getMenu().getCabeza();
        while (act != null) {
            masterData.add(act.getDato());
            act = act.getSiguiente();
        }
    }

    private void cargarInsumos() {
        if (App.attizos.getInventario() != null) {
            cmbInsumos.setItems(FXCollections.observableArrayList(App.attizos.getInventario().getInventarioInsumos().values()));
        }
    }

    private void limpiarFormulario() {
        txtNombre.clear(); txtPrecio.clear(); txtStock.clear();
        imgPreview.setImage(null);
        recetaTemporal.clear();
        panelAtributosDinamicos.getChildren().clear();
        vboxCategoria.setVisible(false); vboxCategoria.setManaged(false);
        vboxStock.setVisible(false); vboxStock.setManaged(false);
        cmbTipoClase.getSelectionModel().clearSelection();
    }

    private void mostrarAlerta(String t, String m) { new Alert(Alert.AlertType.WARNING, m).show(); }
    private void mostrarExito(String t, String m) { new Alert(Alert.AlertType.INFORMATION, m).show(); }

    public static class DetalleRecetaUI {
        private Insumo insumo; private double cantidad;
        public DetalleRecetaUI(Insumo i, double c) { this.insumo = i; this.cantidad = c; }
        public Insumo getInsumo() { return insumo; }
        public double getCantidad() { return cantidad; }
        public String getNombreInsumo() { return insumo.getNombre(); }
    }
}