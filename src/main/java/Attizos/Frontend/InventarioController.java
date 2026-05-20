package Attizos.Frontend;

import Attizos.Backend.Attizos.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class InventarioController {
    @FXML private TextArea txtAlertas;

    @FXML private TableView<Insumo> tablaInventario;
    @FXML private TableColumn<Insumo, String> colCodigo;
    @FXML private TableColumn<Insumo, String> colNombre;
    @FXML private TableColumn<Insumo, String> colCategoria;
    @FXML private TableColumn<Insumo, Double> colStock;
    @FXML private TableColumn<Insumo, String> colUnidad;
    @FXML private TableColumn<Insumo, LocalDate> colVencimiento;

    @FXML private TextField txtBuscador;
    @FXML private ComboBox<String> cmbFiltroCategoria;

    @FXML private TextField txtCodigoInsumo;
    @FXML private TextField txtCantidad;
    @FXML private DatePicker dpFechaVencimiento;

    private ObservableList<Insumo> masterData = FXCollections.observableArrayList();
    private FilteredList<Insumo> filteredData;

    @FXML
    public void initialize() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockActual"));
        colUnidad.setCellValueFactory(new PropertyValueFactory<>("unidad"));

        colVencimiento.setCellValueFactory(new PropertyValueFactory<>("fechaVencimiento"));
        colVencimiento.setCellFactory(column -> new TableCell<Insumo, LocalDate>(){
            @Override
            protected void updateItem(LocalDate item, boolean empy){
                super.updateItem(item, empy);
                if(empy || item == null){
                    setText(null);
                }else{
                    setText(item.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                }
            }
        });

        filteredData = new FilteredList<>(masterData, p -> true);
        tablaInventario.setItems(filteredData);

        txtBuscador.textProperty().addListener((obs, old, newVal) -> aplicarFiltros());
        cmbFiltroCategoria.valueProperty().addListener((obs, old, newVal) -> aplicarFiltros());

        cargarDatos();
    }

    private void aplicarFiltros(){
        String busqueda = txtBuscador.getText() == null ? "" : txtBuscador.getText().toLowerCase();
        String categoria = cmbFiltroCategoria.getValue();

        filteredData.setPredicate(insumo ->{
            boolean coincideNombre = insumo.getNombre().toLowerCase().contains(busqueda) ||
                    insumo.getCodigo().toLowerCase().contains(busqueda);
            boolean coincideCat = (categoria == null || categoria.equals("Todas las categorías") ||
                    insumo.getCategoria().equals(categoria));
            return coincideNombre && coincideCat;
        });
    }

    private void cargarDatos(){
        Insumo seleccionado = tablaInventario.getSelectionModel().getSelectedItem();
        masterData.clear();
        boolean hayProblemas = false;
        StringBuilder alertasConsolidadas = new StringBuilder();
        Set<String> categoriasUnicas = new HashSet<>();
        categoriasUnicas.add("Todas las categorías");

        if (App.attizos != null && App.attizos.getInventario() != null){
            for (Insumo i : App.attizos.getInventario().getInventarioInsumos().values()) {
                masterData.add(i);
                categoriasUnicas.add(i.getCategoria());
                if (i.isVencido()) {
                    alertasConsolidadas.append("❌ ").append(i.getNombre()).append(" CADUCADO.\n");
                    hayProblemas = true;
                }else if(i.isPorVencer()){
                    alertasConsolidadas.append("⚠ ").append(i.getNombre()).append(" por vencer el ").append(i.getFechaVencimiento() != null ? i.getFechaVencimiento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A").append(".\n");
                    hayProblemas = true;
                }
                if(i.getStockActual() > 0 && i.getStockActual() <= i.getStockMinimo()){
                    alertasConsolidadas.append("📉 STOCK BAJO: ").append(i.getNombre())
                            .append(" (Quedan ").append(i.getStockActual()).append(").\n");
                    hayProblemas = true;
                }
            }
        }
        cmbFiltroCategoria.setItems(FXCollections.observableArrayList(categoriasUnicas));
        if(cmbFiltroCategoria.getValue() == null) cmbFiltroCategoria.setValue("Todas las categorías");

        masterData.sort(Comparator.comparing(Insumo::getCodigo));
        aplicarFiltros();
        tablaInventario.refresh();

        if (hayProblemas) {
            txtAlertas.setText(alertasConsolidadas.toString());
        } else {
            txtAlertas.setText("✅ Todo en orden. Stock saludable y sin caducidades cercanas.");
        }
        if (seleccionado != null) {
            tablaInventario.getSelectionModel().select(seleccionado);
        }
    }

    @FXML
    void registrarIngreso(ActionEvent event) {
        String codigoBase = txtCodigoInsumo.getText().trim();
        String cantTexto = txtCantidad.getText().trim();
        LocalDate nuevaFecha = dpFechaVencimiento.getValue();

        if (codigoBase.isEmpty() || cantTexto.isEmpty() || nuevaFecha == null) {
            mostrarAlerta("Datos incompletos", "Por favor ingrese código, cantidad y la fecha de caducidad.");
            return;
        }

        try {
            double cantidad = Double.parseDouble(cantTexto.replace(",", "."));
            Inventario inv = App.attizos.getInventario();
            Insumo insumoBase = inv.buscarInsumo(codigoBase);

            if (insumoBase != null) {
                TextInputDialog dialogCosto = new TextInputDialog("0.00");
                dialogCosto.setTitle("Costo de Compra");
                dialogCosto.setHeaderText("Registro de Gasto: " + cantidad + " " + insumoBase.getUnidad() + " de " + insumoBase.getNombre());
                dialogCosto.setContentText("Ingrese el costo total pagado en Bs:");
                aplicarEstiloOscuro(dialogCosto); // Diseño oscuro para el popup

                dialogCosto.showAndWait().ifPresent(costoStr -> {
                    try {
                        double costo = Double.parseDouble(costoStr.replace(",", "."));
                        App.attizos.registerExpense(new Egreso("Compra Almacén: " + insumoBase.getNombre(), costo));

                        if (insumoBase.getStockActual() == 0) {
                            insumoBase.setStockActual(cantidad);
                            insumoBase.setFechaVencimiento(nuevaFecha);
                            mostrarExito("Stock Reabastecido", "Lote actualizado.\nCosto Registrado: Bs. " + costo);
                        }
                        else if ((insumoBase.getFechaVencimiento() == null && nuevaFecha == null) ||
                                (insumoBase.getFechaVencimiento() != null && insumoBase.getFechaVencimiento().equals(nuevaFecha))) {
                            insumoBase.setStockActual(insumoBase.getStockActual() + cantidad);
                            mostrarExito("Stock Actualizado", "Se sumaron al lote existente.\nCosto Registrado: Bs. " + costo);
                        }
                        else {
                            String fechaCorta = nuevaFecha.format(DateTimeFormatter.ofPattern("yyMMdd"));
                            String codigoLote = codigoBase + "-L" + fechaCorta;

                            Insumo loteExistente = inv.buscarInsumo(codigoLote);
                            if(loteExistente != null){
                                loteExistente.setStockActual(loteExistente.getStockActual() + cantidad);
                                mostrarExito("Lote Actualizado", "Se sumó stock al lote: " + codigoLote + "\nCosto Registrado: Bs. " + costo);
                            } else {
                                Insumo nuevoLote = new Insumo(codigoLote, insumoBase.getNombre(), insumoBase.getCategoria(),
                                        insumoBase.getUnidad(), cantidad, insumoBase.getStockMinimo(),
                                        insumoBase.getStockMaximo(), nuevaFecha);
                                inv.agregarInsumo(nuevoLote);
                                mostrarExito("Nuevo Lote Creado", "Lote registrado: " + codigoLote + "\nCosto Registrado: Bs. " + costo);
                            }
                        }

                        txtCodigoInsumo.clear();
                        txtCantidad.clear();
                        dpFechaVencimiento.setValue(null);
                        cargarDatos();

                    } catch (NumberFormatException e) {
                        mostrarAlerta("Error de Costo", "El costo ingresado no es un número válido.");
                    }
                });
            } else {
                mostrarAlerta("Insumo no encontrado", "❌ El código base '" + codigoBase + "' no existe en el catálogo.");
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("Error de formato", "La cantidad debe ser un número válido.");
        }
    }

    @FXML
    void disminuirStock(ActionEvent event) {
        Insumo seleccionado = tablaInventario.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Selección requerida", "Seleccione un insumo de la tabla.");
            return;
        }

        if (seleccionado.getStockActual() >= 1) {
            TextInputDialog dialogMotivo = new TextInputDialog();
            dialogMotivo.setTitle("Justificación de Merma / Ajuste");
            dialogMotivo.setHeaderText("Descontando 1 unidad de: " + seleccionado.getNombre());
            dialogMotivo.setContentText("Describa el motivo (Ej: Caducado, Dañado, Consumo interno):");
            aplicarEstiloOscuro(dialogMotivo);

            dialogMotivo.showAndWait().ifPresent(motivo -> {
                if(motivo.trim().isEmpty()){
                    mostrarAlerta("Obligatorio", "Debe ingresar una explicación válida para los reportes de almacén.");
                } else {
                    double nuevoStock = seleccionado.getStockActual() - 1;
                    seleccionado.setStockActual(nuevoStock);

                    if (nuevoStock == 0) {
                        if (seleccionado.getCodigo().contains("-L")) {
                            App.attizos.getInventario().getInventarioInsumos().remove(seleccionado.getCodigo());
                            mostrarExito("Lote Agotado", "El lote temporal se agotó y fue removido.\nMotivo Registrado: " + motivo);
                        } else {
                            mostrarExito("Insumo Agotado", "El insumo base llegó a 0 y se mantendrá en el catálogo.\nMotivo Registrado: " + motivo);
                        }
                    } else {
                        mostrarExito("Ajuste Realizado", "Se descontó 1 unidad de " + seleccionado.getNombre() + ".\nMotivo Registrado: " + motivo);
                        if (nuevoStock <= seleccionado.getStockMinimo()) {
                            mostrarAlerta("⚠ Stock Crítico", "El insumo " + seleccionado.getNombre() + " bajó a " + nuevoStock + " " + seleccionado.getUnidad() + ". ¡Es momento de reabastecer!");
                        }
                    }
                    cargarDatos();
                }
            });
        } else {
            mostrarAlerta("Sin Stock", "El insumo ya tiene 0 unidades.");
        }
    }

    @FXML
    void eliminarInsumo(ActionEvent event) {
        Insumo seleccionado = tablaInventario.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Selección requerida", "Seleccione el insumo a eliminar.");
            return;
        }

        TextInputDialog dialogMotivo = new TextInputDialog();
        dialogMotivo.setTitle("Eliminar Lote / Insumo");
        dialogMotivo.setHeaderText("ATENCIÓN: Va a eliminar completamente '" + seleccionado.getCodigo() + "'");
        dialogMotivo.setContentText("Explique el motivo de la eliminación:");
        aplicarEstiloOscuro(dialogMotivo);

        dialogMotivo.showAndWait().ifPresent(motivo -> {
            if (motivo.trim().isEmpty()) {
                mostrarAlerta("Obligatorio", "Es obligatorio dejar un registro del porqué se elimina información del sistema.");
            } else {
                App.attizos.getInventario().getInventarioInsumos().remove(seleccionado.getCodigo());
                mostrarExito("Insumo Eliminado", "El registro ha sido eliminado del sistema.\nMotivo guardado: " + motivo);
                cargarDatos();
            }
        });
    }

    @FXML
    void abrirFormularioNuevoInsumo(ActionEvent event){
        try{
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/FormularioNuevoInsumo.fxml"));
            Stage stage = new Stage();
            Scene scene = new Scene(root);

            scene.setFill(Color.TRANSPARENT);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setScene(scene);
            stage.setTitle("Registrar Insumo");
            stage.setResizable(false);

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            cargarDatos();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void aplicarEstiloOscuro(Dialog<?> dialog) {
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #1a0a2a; -fx-border-color: #00d2ff; -fx-border-width: 2px;");
        dialogPane.lookupAll(".label").forEach(node -> ((Label) node).setStyle("-fx-text-fill: white; -fx-font-weight: bold;"));
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        aplicarEstiloOscuro(alert);
        alert.showAndWait();
    }

    private void mostrarExito(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        aplicarEstiloOscuro(alert);
        alert.showAndWait();
    }
}