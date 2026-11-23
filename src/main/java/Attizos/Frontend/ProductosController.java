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
import java.text.Normalizer;

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
    @FXML private TableColumn<DetalleRecetaUI, String> colRecetaCant;
    @FXML private CheckBox chkTieneReceta;
    @FXML private TableColumn<Producto, String> colTamano;
    @FXML private ComboBox<String> cmbTamano;
    @FXML private Label lblUnidadInsumo;

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
        if(cmbTamano != null){
            cmbTamano.setItems(FXCollections.observableArrayList(
                "Sin tamaño", "Pequeño", "Mediano", "Grande"
            ));
            cmbTamano.getSelectionModel().selectFirst();
        }
        mostrarOcultarCamposRecetaStock(false);

        colRecetaInsumo.setCellValueFactory(new PropertyValueFactory<>("nombreInsumo"));
        colRecetaCant.setCellValueFactory(new PropertyValueFactory<>("cantidadConUnidad"));
        colTamano.setCellValueFactory(cellData ->{
            String t = cellData.getValue().getTamano();
            return new SimpleStringProperty(t.isEmpty() ? "-" : t);
        });
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
        cmbInsumos.setCellFactory(lv -> new ListCell<Insumo>() {
            @Override
            protected void updateItem(Insumo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    String uni = (item.getUnidad() != null && !item.getUnidad().trim().isEmpty())
                            ? " (" + item.getUnidad().trim() + ")"
                            : "";
                    setText(item.getNombre() + uni);
                    setStyle("-fx-text-fill: #111111; -fx-font-weight: bold;");
                }
            }
        });

        cmbInsumos.setButtonCell(new ListCell<Insumo>() {
            @Override
            protected void updateItem(Insumo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Insumo...");
                    setStyle("-fx-text-fill: #555555; -fx-background-color: transparent;");
                } else {
                    String uni = (item.getUnidad() != null && !item.getUnidad().trim().isEmpty())
                            ? " (" + item.getUnidad().trim() + ")"
                            : "";
                    setText(item.getNombre() + uni);
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
                String uni = (newVal.getUnidad() != null) ? newVal.getUnidad().trim() : "";
                txtCantidadReceta.setPromptText(uni.isEmpty() ? "Cant." : "Cant. (" + uni + ")");
                if (lblUnidadInsumo != null) {
                    String stockFmt = String.format("%.2f", newVal.getStockActual());
                    String textoUnidad = uni.isEmpty()
                            ? "📏 Insumo seleccionado: " + newVal.getNombre() + " (Stock: " + stockFmt + ")"
                            : "📏 Ingrese la cantidad en: " + uni.toUpperCase() + "   (Disponible: " + stockFmt + " " + uni + ")";
                    lblUnidadInsumo.setText(textoUnidad);
                    lblUnidadInsumo.setVisible(true);
                    lblUnidadInsumo.setManaged(true);
                }
                txtCantidadReceta.requestFocus();
            }else{
                txtCantidadReceta.setPromptText("Cant.");
                if(lblUnidadInsumo != null){
                    lblUnidadInsumo.setVisible(false);
                    lblUnidadInsumo.setManaged(false);
                }
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
                    if(producto.getTamano().toLowerCase().contains(filter)) return true;
                    return false;
                });
            });
        }
        WebSocketManager.setAccionMenu(() ->{
            System.out.println("Actualizando menu...");
            new Thread(()->{
                ConexionSQLite.sincronizarProductos();
                ArrayList<Producto> menuFresco = ConexionSQLite.obtenerMenuLocal();

                Platform.runLater(() ->{
                    if(menuFresco != null && !menuFresco.isEmpty()){
                        App.attizos.getMenu().clear();
                        App.attizos.getMenu().addAll(menuFresco);
                    }
                    cargarMenu();
                    cargarCategoriasUnicas();
                    tablaMenu.refresh();
                    System.out.println("Menu actualizado...");
                });
            }).start();
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
                String uniTexto = seleccionado.getUnidad();
                String sufijo = uniTexto.isEmpty() ? "" : " (" + uniTexto + ")";
                String pregunta = uniTexto.isEmpty()
                        ? "Ingrese la nueva cantidad:"
                        : "Ingrese la nueva cantidad en " + uniTexto + ":";

                DialogoPersonalizado.mostrarDialogo(
                        "Modificar Cantidad",
                        "Modificando: " + seleccionado.getNombreInsumo() + sufijo,
                        pregunta,
                        String.valueOf(seleccionado.getCantidad())
                ).ifPresent(nuevaCant -> {
                    try {
                        double cant = Double.parseDouble(nuevaCant.trim().replace(",", "."));

                        if (cant < 0.001) { mostrarAlerta("Error", "La cantidad debe ser mayor a 0.001"); return; }
                        String uni = uniTexto.toLowerCase();
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
        colTipo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().tieneReceta() ? "Preparación" : "Directo"));

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
            String uni = (insumoSeleccionado.getUnidad() != null)
            ? insumoSeleccionado.getUnidad().trim().toLowerCase()
            : "";
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

            
            String cat = cmbCategoria.getEditor().getText().trim();
            if (cat.isEmpty()) { mostrarAlerta("Error", "Debe escribir o seleccionar una categoría."); return; }
            if (normalizarTexto(cat).equals("promocion") || normalizarTexto(cat).equals("promociones")) {
                mostrarAlerta("Categoría Reservada", "Las promociones deben crearse desde el botón 'Promociones'.");
                return;
            }
            cat = resolverCategoriaInteligente(cat);
            cmbCategoria.getEditor().setText(cat);

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
            if(usaReceta && recetaTemporal.isEmpty()){
                mostrarAlerta("Error", "La receta no puede estar vacía.");
                return;
            }
            String nombreArchivoImagen = "default.png";
            if (archivoImagenSeleccionada != null) {
                nombreArchivoImagen = UtilidadesImagen.guardarImagenLocal(archivoImagenSeleccionada, nombre);
            }

            Producto nuevo = new Producto(0, nombre, precio, cat, stock, nombreArchivoImagen,"Activo");
            if(cmbTamano != null && cmbTamano.getValue() != null){
                nuevo.setTamano(cmbTamano.getValue());
            }

            if (usaReceta) {
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
                                    String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                                    App.registrarAuditoria(operador, "Producto", nuevo.getNombre(), "Creación", cant, "Nuevo producto con stock inicial");
                                    sincronizarDespuesDeGuardar();
                                    cargarMenu();
                                    cargarCategoriasUnicas();
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
                    sincronizarDespuesDeGuardar();
                    cargarMenu();
                    cargarCategoriasUnicas();
                    mostrarExito("Éxito", "Producto guardado correctamente.");
                    limpiarFormulario();
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
    private void sincronizarDespuesDeGuardar() {
        new Thread(() -> {
            ConexionSQLite.subirAuditoriaPendiente();
            if (!App.modoOffline) {
                ServicioNube.sincronizarImagenesPendientes();
            }
            ConexionSQLite.sincronizarProductos();
        }).start();
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
                                        new Thread(() ->{
                                            sel.aumentarStock(cantidad);
                                            boolean actualizado = ApiClient.actualizarProductoEnServidor(sel);
                                            Platform.runLater(() ->{
                                                if(actualizado){
                                                    String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                                                    App.registrarAuditoria(operador, "Producto", sel.getNombre(), "Ajuste Stock", cantidad, "Ingreso por compra");

                                                    new Thread(() -> {
                                                        ApiClient.registrarEgresoEnServidor("Compra mercadería: " + sel.getNombre(), costoTotal);
                                                        ConexionSQLite.subirAuditoriaPendiente();
                                                        ConexionSQLite.sincronizarProductos();
                                                    }).start();

                                                    tablaMenu.refresh();
                                                    mostrarExito("Stock Actualizado", "Stock sumado exitosamente.");
                                                }else{
                                                    sel.reducirStock(cantidad);
                                                    mostrarAlerta("Error", "No se pudo actualizar el stock.");
                                                }
                                            });
                                        }).start();
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

                                            new Thread(() -> {
                                                ConexionSQLite.subirAuditoriaPendiente();
                                                ConexionSQLite.sincronizarProductos();
                                            }).start();

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
                        Platform.runLater(() ->{
                            boolean eliminadoDB = ApiClient.inactivarProductoEnServidor(sel.getId());
                            if (eliminadoDB) {
                                App.attizos.getMenu().remove(sel);
                                cargarMenu();
                                String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";

                                new Thread( () -> {
                                    App.registrarAuditoria(operador, "Producto", sel.getNombre(), "Eliminación Lógica", sel.getStock(), motivo);
                                    ConexionSQLite.subirAuditoriaPendiente();
                                    ConexionSQLite.sincronizarProductos();
                                }).start();

                                mostrarExito("Eliminado", "Producto dado de baja. Ya no aparecerá en el menú.\nMotivo: " + motivo);
                            }
                        });

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
        if(cmbTamano != null){
            cmbTamano.getSelectionModel().selectFirst();
        }
        txtCantidadReceta.clear();
        txtCantidadReceta.setPromptText("Cant.");
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
        private Insumo insumo;
        private double cantidad;

        public DetalleRecetaUI(Insumo i, double c) {
            this.insumo = i;
            this.cantidad = c;
        }

        public Insumo getInsumo() { return insumo; }
        public double getCantidad() { return cantidad; }
        public String getNombreInsumo() { return insumo.getNombre(); }
        public void setCantidad(double cant) { this.cantidad = cant; }

        public String getUnidad() {
            return (insumo != null && insumo.getUnidad() != null) ? insumo.getUnidad().trim() : "";
        }

        public String getCantidadConUnidad() {
            String num = (cantidad == Math.floor(cantidad))
                    ? String.valueOf((long) cantidad)
                    : String.valueOf(cantidad);
            String uni = getUnidad();
            return uni.isEmpty() ? num : num + " " + uni;
        }
    }

    private void configurarMenuContextualTablaMenu(){
        ContextMenu contextMenu = new ContextMenu();
        MenuItem itemModificarTamano = new MenuItem("📏 Modificar tamaño");
        itemModificarTamano.setOnAction(e -> {
            Producto seleccionado = tablaMenu.getSelectionModel().getSelectedItem();
            if (seleccionado == null) return;

            List<String> opciones = List.of("Sin tamaño", "Pequeño", "Mediano", "Grande");
            String actual = seleccionado.getTamano().isEmpty() ? "Sin tamaño" : seleccionado.getTamano();

            ChoiceDialog<String> dialog = new ChoiceDialog<>(actual, opciones);
            dialog.setTitle("Modificar Tamaño");
            dialog.setHeaderText("Cambiando tamaño de: " + seleccionado.getNombre());
            dialog.setContentText("Seleccione el tamaño:");

            dialog.showAndWait().ifPresent(nuevoTamano -> {
                String anterior = seleccionado.getTamano();
                seleccionado.setTamano(nuevoTamano);
                boolean actualizado = ApiClient.actualizarProductoEnServidor(seleccionado);
                if (actualizado) {
                    tablaMenu.refresh();
                    String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                    App.registrarAuditoria(operador, "Producto", seleccionado.getNombre(),
                            "Modificación Tamaño", 0, "Tamaño actualizado a: " + nuevoTamano);
                    new Thread(ConexionSQLite::sincronizarProductos).start();
                    mostrarExito("Tamaño Actualizado", "El tamaño se actualizó correctamente.");
                } else {
                    seleccionado.setTamano(anterior);
                    mostrarAlerta("Error", "No se pudo actualizar el tamaño en el servidor.");
                }
            });
        });
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
            if(seleccionado == null) return;
            if(!seleccionado.tieneReceta()){
                mostrarAlerta("Inválido", "El producto no tiene una receta.");
            }else{
                abrirVentanaEdicionReceta(seleccionado);
            }
        });
        MenuItem itemModificarImagen = new MenuItem("🖼 Modificar imagen");
        itemModificarImagen.setOnAction(e -> {
            Producto seleccionado = tablaMenu.getSelectionModel().getSelectedItem();
            if (seleccionado != null) {
                FileChooser fc = new FileChooser();
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
                File nuevoArchivo = fc.showOpenDialog(null);

                if (nuevoArchivo != null) {
                    String nombreArchivoLocal = UtilidadesImagen.guardarImagenLocal(nuevoArchivo, seleccionado.getNombre());

                    seleccionado.setImagenURL(nombreArchivoLocal);
                    tablaMenu.refresh();

                    new Thread(() -> {
                        boolean actualizado = ApiClient.actualizarProductoEnServidor(seleccionado);

                        Platform.runLater(() -> {
                            if (actualizado) {
                                String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                                App.registrarAuditoria(operador, "Producto", seleccionado.getNombre(), "Actualización Imagen", 0, "Nueva imagen asignada al producto.");

                                new Thread(() -> {
                                    ConexionSQLite.subirAuditoriaPendiente();
                                    if (!App.modoOffline) {
                                        ServicioNube.sincronizarImagenesPendientes();
                                    }
                                    ConexionSQLite.sincronizarProductos();
                                }).start();

                                mostrarExito("Imagen Actualizada", "La nueva imagen se guardó correctamente y se sincronizará en la nube.");
                            } else {
                                mostrarAlerta("Error", "No se pudo actualizar el registro de la imagen en el servidor.");
                            }
                        });
                    }).start();
                }
            }
        });

        contextMenu.getItems().addAll(itemModificarPrecio, itemModificarTamano, itemModificarReceta, itemModificarImagen);

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
    private String resolverCategoriaInteligente(String catIngresada) {
        String limpia = catIngresada.trim().replaceAll("\\s+", " ");
        String normIngresada = normalizarTexto(limpia);

        // 1. Si ya existe exactamente igual (ignorando mayúsculas y tildes), reutilizarla directo
        for (String existente : cmbCategoria.getItems()) {
            if (normalizarTexto(existente).equals(normIngresada)) {
                return existente;
            }
        }

        // 2. Buscar categoría existente similar (singular/plural o error de tipeo)
        String similar = buscarCategoriaSimilar(normIngresada);
        if (similar != null) {
            boolean usarExistente = AlertaPersonalizada.mostrarConfirmacion(
                    "Categoría Similar Detectada",
                    "Ya existe la categoría \"" + similar + "\".\n\n" +
                    "¿Desea usar \"" + similar + "\" en lugar de crear \"" + limpia + "\" para evitar duplicados?"
            );
            return usarExistente ? similar : capitalizar(limpia);
        }

        // 3. Si es nueva, de una sola palabra y está en singular, sugerir plural
        String sugerenciaPlural = sugerirPlural(limpia);
        if (sugerenciaPlural != null) {
            boolean usarPlural = AlertaPersonalizada.mostrarConfirmacion(
                    "Sugerencia de Categoría",
                    "La categoría \"" + limpia + "\" es nueva y está en singular.\n\n" +
                    "¿Desea guardarla en plural como \"" + sugerenciaPlural + "\"?"
            );
            return usarPlural ? sugerenciaPlural : capitalizar(limpia);
        }

        return capitalizar(limpia);
    }

    private String buscarCategoriaSimilar(String normIngresada) {
        String mejorCoincidencia = null;
        int mejorDistancia = Integer.MAX_VALUE;

        for (String existente : cmbCategoria.getItems()) {
            String normExistente = normalizarTexto(existente);

            // Coincidencia directa singular/plural (ej: bebida <-> bebidas, postre <-> postres)
            if (sonSingularPlural(normIngresada, normExistente)) {
                return existente;
            }

            // Coincidencia por cercanía ortográfica (ej: pizas <-> pizzas, bevida <-> bebidas)
            int minLen = Math.min(normIngresada.length(), normExistente.length());
            int maxLen = Math.max(normIngresada.length(), normExistente.length());
            if (minLen >= 4) {
                int dist = distanciaLevenshtein(normIngresada, normExistente);
                int limite = (maxLen >= 6) ? 2 : 1;
                if (dist <= limite && dist < mejorDistancia) {
                    mejorDistancia = dist;
                    mejorCoincidencia = existente;
                }
            }
        }
        return mejorCoincidencia;
    }

    private boolean sonSingularPlural(String a, String b) {
        String corto = (a.length() <= b.length()) ? a : b;
        String largo = (a.length() <= b.length()) ? b : a;

        if (largo.equals(corto + "s")) return true;
        if (largo.equals(corto + "es")) return true;
        if (corto.endsWith("z") && largo.equals(corto.substring(0, corto.length() - 1) + "ces")) return true;
        return false;
    }

    private String sugerirPlural(String palabra) {
        // Solo sugerir en palabras simples (evita deformar nombres compuestos como "Comida Rápida")
        if (palabra.contains(" ") || palabra.length() < 3) return null;

        String norm = normalizarTexto(palabra);
        if (norm.endsWith("s") || norm.endsWith("x")) return null;

        String base = capitalizar(palabra);
        char ultima = norm.charAt(norm.length() - 1);

        if (ultima == 'z') {
            return base.substring(0, base.length() - 1) + "ces";
        } else if ("aeiou".indexOf(ultima) >= 0) {
            return base + "s";
        } else {
            return base + "es";
        }
    }

    private String normalizarTexto(String texto) {
        if (texto == null) return "";
        String sinTildes = Normalizer.normalize(texto.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinTildes.replaceAll("\\s+", " ");
    }

    private String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) return "";
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }

    private int distanciaLevenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int costo = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + costo
                );
            }
        }
        return dp[a.length()][b.length()];
    }
}