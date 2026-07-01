package Attizos.Frontend;

import Attizos.Backend.AI.AuditoriaLocal;
import Attizos.Backend.Api.ApiClient;
import Attizos.Backend.Attizos.*;
import Attizos.Backend.Database.*;
import Attizos.Frontend.Network.WebSocketManager;
import javafx.application.Platform;
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
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.util.*;
import java.io.File;

public class ProductosController {

    @FXML private TableView<Producto> tablaMenu;
    @FXML private TableColumn<Producto, Integer> colId;
    @FXML private TableColumn<Producto, String> colNombre, colCategoria, colStock, colTipo;
    @FXML private TableColumn<Producto, Double> colPrecio;

    @FXML private Button btnAgregarIngrediente;
    @FXML private TextField txtBuscador, txtNombre, txtPrecio, txtStock, txtCantidadReceta, txtBuscarInsumo;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private ComboBox<Insumo> cmbInsumos;

    @FXML private VBox vboxCategoria;
    @FXML private VBox vboxStock;
    @FXML private VBox vboxReceta;

    @FXML private ImageView imgPreview;
    @FXML private TableView<DetalleRecetaUI> tablaRecetaFila;
    @FXML private TableColumn<DetalleRecetaUI, String> colRecetaInsumo;
    @FXML private TableColumn<DetalleRecetaUI, Double> colRecetaCant;
    @FXML private CheckBox chkTieneReceta;

    private ObservableList<Producto> masterData = FXCollections.observableArrayList();
    private FilteredList<Producto> filteredData;
    private File archivoImagenSeleccionada;
    private ObservableList<DetalleRecetaUI> recetaTemporal = FXCollections.observableArrayList();

    private ObservableList<Insumo> masterInsumos = FXCollections.observableArrayList();
    private FilteredList<Insumo> filteredInsumos;


    @FXML
    public void initialize() {
        configurarTabla();
        if (chkTieneReceta != null) {
            chkTieneReceta.setStyle("-fx-text-fill: #111111; -fx-font-weight: bold;");
            chkTieneReceta.selectedProperty().addListener((obs, oldVal, isChecked) -> {
                mostrarOcultarCamposRecetaStock(isChecked);
            });
        }
        if (vboxCategoria != null) { vboxCategoria.setVisible(true); vboxCategoria.setManaged(true); }
        mostrarOcultarCamposRecetaStock(false);

        colRecetaInsumo.setCellValueFactory(new PropertyValueFactory<>("nombreInsumo"));
        colRecetaCant.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        tablaRecetaFila.setItems(recetaTemporal);
        tablaRecetaFila.setPrefHeight(120);

        configurarMenuContextualReceta();
        configurarMenuContextualTablaMenu();
        filteredInsumos = new FilteredList<>(masterInsumos, p -> true);
        cmbInsumos.setItems(filteredInsumos);
        cargarMenu();
        cargarCategoriasUnicas();
        cargarInsumos();

        cmbInsumos.setStyle("-fx-base: #FFFFFF; -fx-background-color: #F5F5F5; -fx-border-color: #DDDDDD; -fx-border-radius: 5;");
        cmbInsumos.setButtonCell(new ListCell<Insumo>() {
            @Override
            protected void updateItem(Insumo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Insumo...");
                    setStyle("-fx-text-fill: #555555; -fx-background-color: transparent;");
                } else {
                    setText(item.getNombre());
                    setStyle("-fx-text-fill: #111111; -fx-font-weight: bold; -fx-background-color: transparent;");
                }
            }
        });

        if(txtBuscarInsumo != null){
            txtBuscarInsumo.textProperty().addListener((observable, oldValue, newValue) ->{
                filteredInsumos.setPredicate(insumo ->{
                    if(newValue == null || newValue.isEmpty()) return true;
                    String filtro = newValue.toLowerCase();
                    return insumo.getNombre().toLowerCase().contains(filtro);
                });
                if(newValue != null && !newValue.isEmpty()){
                    cmbInsumos.show();
                }
            });
            txtBuscarInsumo.setOnKeyPressed(event ->{
                if(event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.ENTER){
                    cmbInsumos.requestFocus();
                    cmbInsumos.show();
                    event.consume();
                }
            });
        }
        cmbInsumos.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) ->{
            if(newVal != null){
                txtCantidadReceta.requestFocus();
            }
        });
        cmbInsumos.setOnKeyPressed(event ->{
            if(event.getCode() == KeyCode.ENTER){
                if(cmbInsumos.getSelectionModel().getSelectedItem() != null){
                    agregarIngredienteVisual(new ActionEvent());
                }
                event.consume();
            }
        });
        UtilidadesUI.saltarConEnter(txtNombre, txtPrecio);
        UtilidadesUI.saltarConEnter(txtPrecio, txtStock);
        txtCantidadReceta.setOnKeyPressed(event ->{
            if(event.getCode() == KeyCode.ENTER){
                agregarIngredienteVisual(new ActionEvent());
                event.consume();
            }
        });

        if (txtBuscador != null) {
            txtBuscador.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredData.setPredicate(producto -> {
                    if (newValue == null || newValue.isEmpty()) return true;
                    String filter = newValue.toLowerCase();
                    if (producto.getNombre().toLowerCase().contains(filter)) return true;
                    if (producto.getCategoria().toLowerCase().contains(filter)) return true;
                    if (String.valueOf(producto.getId()).contains(filter)) return true;
                    return false;
                });
            });
        }
        WebSocketManager.setAccionMenu(() ->{
            System.out.println("Actualizando menu...");
            Platform.runLater( () ->{
                cargarMenu();
                cargarCategoriasUnicas();
                tablaMenu.refresh();
            });
        });
    }
    private void mostrarOcultarCamposRecetaStock(boolean esCocina) {
        if (vboxStock != null) {
            vboxStock.setVisible(!esCocina);
            vboxStock.setManaged(!esCocina);
            if (esCocina) txtStock.clear();
        }
        if (vboxReceta != null) {
            vboxReceta.setVisible(esCocina);
            vboxReceta.setManaged(esCocina);
            tablaRecetaFila.setPrefHeight(esCocina ? 280 : 120);
        }
    }
    private void configurarMenuContextualReceta() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem itemModificar = new MenuItem("✏ Modificar cantidad");
        MenuItem itemEliminar = new MenuItem("🗑 Eliminar");

        itemModificar.setOnAction(e -> {
            DetalleRecetaUI seleccionado = tablaRecetaFila.getSelectionModel().getSelectedItem();
            if (seleccionado != null) {
                DialogoPersonalizado.mostrarDialogo("Modificar Cantidad", "Modificando cantidad de: " + seleccionado.getNombreInsumo(), "Ingrese la nueva cantidad:", String.valueOf(seleccionado.getCantidad()))
                        .ifPresent(nuevaCant -> {
                            try {
                                double cant = Double.parseDouble(nuevaCant.replace(",", "."));

                                if (cant < 0.001) { mostrarAlerta("Error", "La cantidad debe ser mayor a 0.001"); return; }
                                String uni = seleccionado.getInsumo().getUnidad().toLowerCase();
                                if ((uni.equals("kg") || uni.equals("lt")) && cant > 10) { mostrarAlerta("Exageración", "No puedes usar más de 10 " + uni + " en un plato."); return; }
                                if ((uni.equals("g") || uni.equals("ml")) && cant > 10000) { mostrarAlerta("Exageración", "No puedes usar más de 10,000 " + uni + " en un plato."); return; }
                                if ((uni.equals("und") || uni.equals("paquete")) && cant > 50) { mostrarAlerta("Exageración", "No puedes usar más de 50 " + uni + " en un plato."); return; }

                                seleccionado.setCantidad(cant);
                                tablaRecetaFila.refresh();
                            } catch (NumberFormatException ex) {
                                mostrarAlerta("Error", "Ingrese un número válido");
                            }
                        });
            }
        });

        itemEliminar.setOnAction(e -> {
            DetalleRecetaUI seleccionado = tablaRecetaFila.getSelectionModel().getSelectedItem();
            if (seleccionado != null) recetaTemporal.remove(seleccionado);
        });

        contextMenu.getItems().addAll(itemModificar, itemEliminar);

        tablaRecetaFila.setRowFactory(tv -> {
            TableRow<DetalleRecetaUI> row = new TableRow<>();
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty()) {
                    tablaRecetaFila.getSelectionModel().select(row.getItem());
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                } else {
                    contextMenu.hide();
                }
            });
            return row;
        });
    }
    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colStock.setCellValueFactory(cellData -> {
            Producto p = cellData.getValue();
            return new SimpleStringProperty(p.tieneReceta() ? "Cocina" : String.valueOf((int) p.getStock()));
        });
        colTipo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategoria()));

        filteredData = new FilteredList<>(masterData, p -> true);
        tablaMenu.setItems(filteredData);
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
        for (DetalleRecetaUI detalle : recetaTemporal) {
            if (detalle.getInsumo().getCodigo().equals(insumoSeleccionado.getCodigo())) {
                mostrarAlerta("Ingrediente Duplicado", "El insumo ya está agregado. Haga clic derecho en la tabla para modificarlo.");
                txtCantidadReceta.clear();
                return;
            }
        }
        try {
            double cantidad = Double.parseDouble(cantTexto.replace(",", "."));

            if (cantidad < 0.001) {
                mostrarAlerta("Error", "La cantidad es demasiado pequeña (Mínimo 0.001).");
                return;
            }
            String uni = insumoSeleccionado.getUnidad().toLowerCase();
            if ((uni.equals("kg") || uni.equals("lt")) && cantidad > 10) {
                mostrarAlerta("Exageración", "No puedes usar más de 10 " + uni + " en una sola porción/plato.");
                return;
            }
            if ((uni.equals("g") || uni.equals("ml")) && cantidad > 10000) {
                mostrarAlerta("Exageración", "No puedes usar más de 10,000 " + uni + " en una sola porción.");
                return;
            }
            if ((uni.equals("und") || uni.equals("paquete")) && cantidad > 50) {
                mostrarAlerta("Exageración", "No puedes usar más de 50 " + uni + " en una receta individual.");
                return;
            }

            recetaTemporal.add(new DetalleRecetaUI(insumoSeleccionado, cantidad));
            cmbInsumos.getSelectionModel().clearSelection();
            txtCantidadReceta.clear();
            if(txtBuscarInsumo != null){
                txtBuscarInsumo.clear();
                javafx.application.Platform.runLater(() -> txtBuscarInsumo.requestFocus());
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "Cantidad inválida. Ingrese un número válido (Ej: 1.5).");
        }
    }
    @FXML
    void guardarNuevoProducto(ActionEvent event) {
        try {
            String nombre = txtNombre.getText().trim();
            if (nombre.isEmpty()) { mostrarAlerta("Error", "Debe escribir el nombre del producto."); return; }

            String precioTxt = txtPrecio.getText().replace(",", ".");
            double precio = Double.parseDouble(precioTxt);
            if(precio < 0){ mostrarAlerta("Error", "El precio no puede ser negativo."); return; }

            // Usamos el valor del editor para permitir texto libre
            String cat = cmbCategoria.getEditor().getText().trim();
            if (cat.isEmpty()) { mostrarAlerta("Error", "Debe escribir o seleccionar una categoría."); return; }

            if(archivoImagenSeleccionada == null){
                java.util.Optional<String> resultado = DialogoPersonalizado.mostrarDialogo(
                        "Confirmar Imagen", "No se ha seleccionado ninguna imagen",
                        "¿Está seguro de que desea guardar el producto con la imagen por defecto?", "Sí, usar por defecto"
                );
                if(resultado.isEmpty()) return;
            }

            boolean usaReceta = (chkTieneReceta != null && chkTieneReceta.isSelected());

            int stock = 0;
            if (!usaReceta && vboxStock != null && vboxStock.isVisible() && !txtStock.getText().isEmpty()) {
                stock = Integer.parseInt(txtStock.getText());
                if(stock < 0){ mostrarAlerta("Error", "El stock no puede ser negativo."); return; }
            }

            String nombreArchivoImagen = "default.png";
            if (archivoImagenSeleccionada != null) {
                nombreArchivoImagen = UtilidadesImagen.guardarImagenLocal(archivoImagenSeleccionada, nombre);
            }

            Producto nuevo = new Producto(0, nombre, precio, cat, stock, nombreArchivoImagen,"Activo");

            if (usaReceta) {
                if(recetaTemporal.isEmpty()){ mostrarAlerta("Error", "La receta no puede estar vacía."); return; }
                Receta r = new Receta();
                for (DetalleRecetaUI det : recetaTemporal) {
                    r.agregarIngrediente(det.getInsumo().getCodigo(), det.getCantidad());
                }
                nuevo.setReceta(r);
            }

            int cant = stock;
            if(stock > 0 && !usaReceta){
                DialogoPersonalizado.mostrarDialogo("Registrar Compra", "Registrando compra inicial para: " + nuevo.getNombre(), "¿Cuánto costó esta compra? (Bs):", "0")
                        .ifPresent(costoStr -> {
                            try {
                                double costoTotal = Double.parseDouble(costoStr.replace(",", "."));
                                if(costoTotal < 0){ mostrarAlerta("Error", "El costo no puede ser negativo."); return; }

                                boolean guardadoDB = ApiClient.guardarProductoEnServidor(nuevo);
                                if(guardadoDB) {
                                    ApiClient.registrarEgresoEnServidor("Compra Inicial: "+nuevo.getNombre(), costoTotal);
                                    App.attizos.getMenu().add(nuevo);
                                    ConexionSQLite.sincronizarProductos();

                                    String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                                    App.registrarAuditoria(operador, "Producto", nuevo.getNombre(), "Creación", cant, "Nuevo producto con stock inicial");
                                    cargarMenu();
                                    mostrarExito("Éxito", "Producto guardado correctamente con stock inicial.\nCompra registrada.");
                                    limpiarFormulario();
                                }else{
                                    mostrarAlerta("Error", "No se pudo guardar el producto. ");
                                }
                            } catch (NumberFormatException ex) {
                                mostrarAlerta("Error", "El costo debe ser un número válido.");
                            }
                        });
            }else {
                boolean guardadoDB = ApiClient.guardarProductoEnServidor(nuevo);
                if(guardadoDB) {
                    if(usaReceta && nuevo.getReceta() != null){
                        ApiClient.guardarRecetaEnServidor(nuevo.getId(), nuevo.getReceta());
                    }
                    App.attizos.getMenu().add(nuevo);
                    String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                    App.registrarAuditoria(operador, "Producto", nuevo.getNombre(), "Creación", 0, usaReceta ? "Nuevo producto de preparación" : "Producto sin stock inicial");
                    cargarMenu();
                    mostrarExito("Éxito", "Producto guardado correctamente.");
                    limpiarFormulario();
                    if (!App.modoOffline) {
                        new Thread(() -> ServicioNube.sincronizarImagenesPendientes()).start();
                    }
                }else{
                    mostrarAlerta("Error", "No se pudo guardar el producto. ");
                }
            }

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

        DialogoPersonalizado.mostrarDialogo("Sumar Stock", "Añadiendo mercadería a: " + sel.getNombre(), "Ingrese la cantidad a sumar:", "")
                .ifPresent(cantStr -> {
                    try {
                        int cantidad = Integer.parseInt(cantStr);
                        if (cantidad <= 0) throw new NumberFormatException();

                        DialogoPersonalizado.mostrarDialogo("Costo de Compra", "Costo total por las " + cantidad + " unidades:", "¿Cuánto costó? (Bs):", "0")
                                .ifPresent(costoStr -> {
                                    try {
                                        double costoTotal = Double.parseDouble(costoStr.replace(",", "."));
                                        if(costoTotal < 0){ mostrarAlerta("Error", "El costo no puede ser negativo."); return; }
                                        sel.aumentarStock(cantidad);
                                        boolean actualizado = ApiClient.actualizarProductoEnServidor(sel);
                                        if(actualizado){
                                            ApiClient.registrarEgresoEnServidor("Compra: "+sel.getNombre()+ " ("+cantidad+ " und)", costoTotal);
                                            String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                                            App.registrarAuditoria(operador, "Producto", sel.getNombre(), "Ajuste Stock", cantidad, "Ingreso de mercadería. Costo: Bs. " + costoTotal);
                                            tablaMenu.refresh();
                                            mostrarExito("Stock Actualizado", "Stock sumado exitosamente.");
                                        }else{
                                            mostrarAlerta("Error", "No se pudo actualiza el stock. ");
                                        }
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

        DialogoPersonalizado.mostrarDialogo("Restar Stock", "Dando de baja mercadería de: " + sel.getNombre(), "Ingrese la cantidad a restar:", "")
                .ifPresent(cantStr -> {
                    try {
                        int cantidad = Integer.parseInt(cantStr);
                        if (cantidad <= 0) { mostrarAlerta("Error","Ingrese cantidad válida. "); return; }

                        DialogoPersonalizado.mostrarDialogo("Justificación", "Motivo de la baja:", "Describa por qué (Ej: Caducado, Merma):", "")
                                .ifPresent(motivo -> {
                                    if (motivo.trim().isEmpty()) {
                                        mostrarAlerta("Obligatorio", "Debe justificar la baja.");
                                    } else if (sel.reducirStock(cantidad)) {
                                        boolean actualizado = ApiClient.actualizarProductoEnServidor(sel);
                                        if(actualizado) {
                                            String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                                            App.registrarAuditoria(operador, "Producto", sel.getNombre(), "Ajuste Stock", cantidad, "Baja de mercadería: " + motivo);
                                            tablaMenu.refresh();
                                            mostrarExito("Baja Exitosa", "Se restaron " + cantidad + " unidades.\nMotivo: " + motivo);
                                        }else{
                                            sel.aumentarStock(cantidad);
                                            mostrarAlerta("Error", "No se puedo restar el stock. ");
                                        }
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

        DialogoPersonalizado.mostrarDialogo("Eliminar Producto","Eliminando permanentemente: "+sel.getNombre(),"Describa el motivo de la eliminación:","")
                .ifPresent(motivo ->{
                    if(motivo.trim().isEmpty()){
                        mostrarAlerta("Seguridad", "Es obligatorio escribir un motivo para eliminar el producto.");
                    }else {
                        boolean eliminadoDB = ApiClient.inactivarProductoEnServidor(sel.getId());
                        if (eliminadoDB) {
                            App.attizos.getMenu().remove(sel);
                            cargarMenu();
                            String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                            App.registrarAuditoria(operador, "Producto", sel.getNombre(), "Eliminación Lógica", sel.getStock(), motivo);
                            mostrarExito("Eliminado", "Producto dado de baja. Ya no aparecerá en el menú.\nMotivo: " + motivo);
                        }
                    }
                });
    }

    private void cargarMenu() {
        masterData.clear();
        ArrayList<Producto> menuDB = App.attizos.getMenu();
        if(menuDB != null) {
            for(Producto p : menuDB) {
                if(p.getEstado() != null && p.getEstado().equals("Activo") && !p.isPromocion() && !p.getCategoria().equalsIgnoreCase("Promocion")) {
                    masterData.add(p);
                }
            }
        }
    }

    private void cargarInsumos() {
        masterInsumos.clear();
        HashMap<String, Insumo> inventarioDB = App.attizos.getInventario().getInventarioInsumos();
        if(inventarioDB != null){
            List<Insumo> insumos = inventarioDB.values().stream()
                    .sorted(Comparator.comparing(Insumo::getNombre, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            masterInsumos.addAll(insumos);
        }
    }

    private void cargarCategoriasUnicas() {
        Set<String> categorias = new HashSet<>();
        if (App.attizos.getMenu() != null) {
            for (Producto p : App.attizos.getMenu()) {
                if (p.getCategoria() != null && !p.getCategoria().equalsIgnoreCase("Promocion")) {
                    categorias.add(p.getCategoria());
                }
            }
        }
        cmbCategoria.setItems(FXCollections.observableArrayList(categorias));
    }

    private void limpiarFormulario() {
        txtNombre.clear(); txtPrecio.clear();
        if (txtStock != null) txtStock.clear();
        txtCantidadReceta.clear();
        if (txtBuscador != null) txtBuscador.clear();
        if(txtBuscarInsumo != null) txtBuscarInsumo.clear();

        imgPreview.setImage(null);
        archivoImagenSeleccionada = null;
        recetaTemporal.clear();

        if (chkTieneReceta != null) chkTieneReceta.setSelected(false);

        if (vboxCategoria != null) { vboxCategoria.setVisible(true); vboxCategoria.setManaged(true); }
        mostrarOcultarCamposRecetaStock(false);

        cmbInsumos.getSelectionModel().clearSelection();
        if(cmbCategoria != null){ cmbCategoria.getEditor().clear(); } // Limpiamos el texto editable

        tablaRecetaFila.setPrefHeight(120);
    }

    private void mostrarAlerta(String t, String m) {
        AlertaPersonalizada.mostrarAlerta(t,m, Alert.AlertType.WARNING);
    }
    private void mostrarExito(String t, String m) {
        AlertaPersonalizada.mostrarAlerta(t,m, Alert.AlertType.INFORMATION);
    }

    public static class DetalleRecetaUI {
        private Insumo insumo; private double cantidad;
        public DetalleRecetaUI(Insumo i, double c) { this.insumo = i; this.cantidad = c; }
        public Insumo getInsumo() { return insumo; }
        public double getCantidad() { return cantidad; }
        public String getNombreInsumo() { return insumo.getNombre(); }
        public void setCantidad(double cant) { cantidad = cant; }
    }

    private void configurarMenuContextualTablaMenu(){
        ContextMenu contextMenu = new ContextMenu();
        MenuItem itemModificarPrecio = new MenuItem(" $ Modificar precio");
        itemModificarPrecio.setOnAction(e -> {
            Producto seleccionado = tablaMenu.getSelectionModel().getSelectedItem();
            if (seleccionado != null) {
                DialogoPersonalizado.mostrarDialogo("Modificar Precio",
                                "Cambiando precio de: " + seleccionado.getNombre(),
                                "Ingrese el nuevo precio (Bs):",
                                String.valueOf(seleccionado.getPrecio()))
                        .ifPresent(nuevoPrecioStr -> {
                            try {
                                double nuevoPrecio = Double.parseDouble(nuevoPrecioStr.replace(",", "."));
                                if (nuevoPrecio >= 0) {
                                    seleccionado.setPrecio(nuevoPrecio);
                                    boolean actualizado = ApiClient.actualizarProductoEnServidor(seleccionado);
                                    if(actualizado) {
                                        tablaMenu.refresh();
                                        String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                                        App.registrarAuditoria(operador, "Producto", seleccionado.getNombre(), "Modificación Precio", 0, "Precio actualizado a Bs. " + nuevoPrecio);
                                        mostrarExito("Precio Actualizado", "El precio se actualizó correctamente.");
                                    }else{
                                        mostrarAlerta("Error", "No se pudo actualizar el precio. ");
                                    }
                                } else {
                                    mostrarAlerta("Error", "El precio no puede ser negativo.");
                                }
                            } catch (NumberFormatException ex) {
                                mostrarAlerta("Error", "Ingrese un número válido para el precio.");
                            }
                        });
            }
        });
        MenuItem itemModificarReceta = new MenuItem("✏ Modificar receta");
        itemModificarReceta.setOnAction(e -> {
            Producto seleccionado = tablaMenu.getSelectionModel().getSelectedItem();
            if(!seleccionado.tieneReceta()){
                mostrarAlerta("Inválido", "El producto no tiene una receta.");
            }else{
                abrirVentanaEdicionReceta(seleccionado);
            }
        });

        contextMenu.getItems().addAll(itemModificarPrecio, itemModificarReceta);

        tablaMenu.setRowFactory(tv -> {
            TableRow<Producto> row = new TableRow<>();
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty()) {
                    tablaMenu.getSelectionModel().select(row.getItem());
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                } else {
                    contextMenu.hide();
                }
            });
            return row;
        });
    }

    @FXML
    void actualizarRecetaExistente(ActionEvent event){
        Producto sel = tablaMenu.getSelectionModel().getSelectedItem();
        if(sel == null){ mostrarAlerta("Atención", "Seleccione un producto primero."); return; }
        if(!sel.tieneReceta()){ mostrarAlerta("Inválido", "El producto no tiene una receta."); return; }
        if(recetaTemporal.isEmpty()){ mostrarAlerta("Inválido", "La receta del producto está vacía."); return; }

        Receta receta = new Receta();
        for(DetalleRecetaUI det : recetaTemporal){
            receta.agregarIngrediente(det.getInsumo().getCodigo(), det.getCantidad());
        }
        boolean exitoDB = ApiClient.guardarRecetaEnServidor(sel.getId(), receta);
        if(exitoDB){
            sel.setReceta(receta);
            String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
            App.registrarAuditoria(operador, "Producto", sel.getNombre(), "Actualización Receta", 0, "Receta actualizada.");
            mostrarExito("Receta Actualizada", "La receta se actualizó correctamente.");
            limpiarFormulario();
        }else{
            mostrarAlerta("Error", "No se pudo actualizar la receta.");
        }
    }

    private void abrirVentanaEdicionReceta(Producto producto){
        try{
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/EditarReceta.fxml"));
            javafx.scene.Parent root = loader.load();

            EditarRecetaController controller = loader.getController();
            controller.inicializarDatos(producto);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

            stage.setScene(scene);
            stage.showAndWait();
        }catch(Exception e){
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir la ventana de edición de recetas.");
        }
    }

    @FXML
    void abrirVentanaPromocion(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/GestorPromociones.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

            stage.setScene(scene);
            stage.showAndWait();

            cargarMenu();
            tablaMenu.refresh();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el creador de promociones. Faltan archivos FXML.");
        }
    }
}