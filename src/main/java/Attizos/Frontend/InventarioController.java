package Attizos.Frontend;

import Attizos.Backend.AI.AuditoriaAI;
import Attizos.Backend.AI.AuditoriaLocal;
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
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

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

    @FXML private VBox panelNotificaciones;
    @FXML private VBox vboxListaAlertas;
    @FXML private Label lblContadorAlertas;

    private ObservableList<Insumo> masterData = FXCollections.observableArrayList();
    private FilteredList<Insumo> filteredData;

    @FXML
    public void initialize() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colStock.setCellFactory(column -> new TableCell<Insumo,Double>(){
            @Override
            protected void updateItem(Double item, boolean empty){
                super.updateItem(item, empty);
                if(empty || item == null){
                    setText(null);
                }else{
                    setText(String.format("%.2f",item));
                }
            }
        });
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockActual"));
        colUnidad.setCellValueFactory(new PropertyValueFactory<>("unidad"));

        colVencimiento.setCellValueFactory(new PropertyValueFactory<>("fechaVencimiento"));
        colVencimiento.setCellFactory(column -> new TableCell<Insumo, LocalDate>(){
            @Override
            protected void updateItem(LocalDate item, boolean empty){
                super.updateItem(item, empty);
                if(empty || item == null){
                    setText(null);
                    setStyle("");
                }else if(item.getYear() >= 2099){
                    setText("No caduca");
                    setStyle("-fx-text-fill: #218c4e; -fx-font-weight: bold;"); // Letra verde para que resalte
                }else{
                    setText(item.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    setStyle("-fx-text-fill: #111111;"); // Color normal
                }
            }
        });

        filteredData = new FilteredList<>(masterData, p -> true);
        tablaInventario.setItems(filteredData);
        tablaInventario.setOnMouseClicked(event ->{
            if(event.getClickCount() == 2){
                Insumo seleccionado = tablaInventario.getSelectionModel().getSelectedItem();
                if(seleccionado != null){
                    txtCodigoInsumo.setText(seleccionado.getCodigo());
                    txtCantidad.requestFocus();
                }
            }
        });

        txtBuscador.textProperty().addListener((obs, old, newVal) -> aplicarFiltros());
        cmbFiltroCategoria.valueProperty().addListener((obs, old, newVal) -> aplicarFiltros());
        UtilidadesUI.formatearDatePicker(dpFechaVencimiento);

        cargarDatos();
        WebSocketManager.setAccionVentasYStock(() ->{
            System.out.println("Movimiento de inventario: Actualizando tabla de inventario.");
            new Thread(() ->{
                App.sincronizarDatosDesdeServidor();
                Platform.runLater(() ->{
                    cargarDatos();
                    tablaInventario.refresh();
                    System.out.println("Inventario actualizado");
                });
            }).start();
        });
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
        Set<String> categoriasUnicas = new HashSet<>();
        categoriasUnicas.add("Todas las categorías");

        List<String> alertas = new ArrayList<>();
        HashMap<String, Insumo> inventarioFresco = App.attizos.getInventario().getInventarioInsumos();

        if (inventarioFresco != null && !inventarioFresco.isEmpty()){
            for (Insumo i : inventarioFresco.values()) {
                masterData.add(i);
                categoriasUnicas.add(i.getCategoria());
                if (i.isVencido()) {
                    alertas.add("❌ CADUCADO: " + i.getNombre());
                }else if(i.isPorVencer()){
                    alertas.add("⏳ POR VENCER: " + i.getNombre() + " (" + i.getFechaVencimiento().format(DateTimeFormatter.ofPattern("dd/MM/yy")) + ")");
                }
                if(i.getStockActual() > 0 && i.getStockActual() <= i.getStockMinimo()){
                    alertas.add("📉 STOCK BAJO: " + i.getNombre() + " (Quedan " + i.getStockActual() + " " + i.getUnidad() + ")");
                }
            }
        }
        cmbFiltroCategoria.setItems(FXCollections.observableArrayList(categoriasUnicas));
        if(cmbFiltroCategoria.getValue() == null) cmbFiltroCategoria.setValue("Todas las categorías");

        masterData.sort(Comparator.comparing(Insumo::getCodigo));
        aplicarFiltros();
        tablaInventario.refresh();
        actualizarPanelNotificaciones(alertas);
        if (seleccionado != null) {
            for(Insumo ins : masterData){
                if(ins.getCodigo().equals(seleccionado.getCodigo())){
                    tablaInventario.getSelectionModel().select(ins);
                    break;
                }
            }
        }
    }
    @FXML
    void toggleNotificaciones(ActionEvent event){
        panelNotificaciones.setVisible(!panelNotificaciones.isVisible());
    }
    private void actualizarPanelNotificaciones(List<String> alertas) {
        vboxListaAlertas.getChildren().clear();

        if (alertas.isEmpty()) {
            lblContadorAlertas.setVisible(false); // Ocultar el círculo rojo
            Label lblOK = new Label("✅ Todo en orden. Stock saludable y sin caducidades cercanas.");
            lblOK.setStyle("-fx-text-fill: #218c4e; -fx-font-weight: bold; -fx-padding: 10;");
            lblOK.setWrapText(true);
            vboxListaAlertas.getChildren().add(lblOK);
        } else {
            lblContadorAlertas.setVisible(true);
            lblContadorAlertas.setText(String.valueOf(alertas.size()));

            for (String textoAlerta : alertas) {
                Label lblItem = new Label(textoAlerta);
                lblItem.setWrapText(true);
                lblItem.setMaxWidth(Double.MAX_VALUE);

                if (textoAlerta.contains("CADUCADO")) {
                    lblItem.setStyle("-fx-background-color: rgba(255, 76, 76, 0.1); -fx-border-color: #ff4c4c; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8; -fx-text-fill: #ff4c4c; -fx-font-weight: bold;");
                } else if (textoAlerta.contains("POR VENCER")) {
                    lblItem.setStyle("-fx-background-color: rgba(243, 156, 18, 0.1); -fx-border-color: #f39c12; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8; -fx-text-fill: #f39c12; -fx-font-weight: bold;");
                } else {
                    lblItem.setStyle("-fx-background-color: rgba(0, 210, 255, 0.1); -fx-border-color: #00d2ff; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8; -fx-text-fill: #008eb3; -fx-font-weight: bold;");
                }

                vboxListaAlertas.getChildren().add(lblItem);
            }
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
            if(cantidad <= 0){
                mostrarAlerta("Error","La cantidad debe ser mayor a 0. ");
                return;
            }
            Inventario inv = App.attizos.getInventario();
            Insumo insumoBase = inv.buscarInsumo(codigoBase);

            if (insumoBase != null) {
                long diasFaltantes = ChronoUnit.DAYS.between(LocalDate.now(), nuevaFecha);
                String veredictoLocal = AuditoriaLocal.auditarIngreso(insumoBase.getNombre(), insumoBase.getCategoria(), cantidad, insumoBase.getUnidad(), diasFaltantes
                );
                if (veredictoLocal.startsWith("ALERTA:")) {
                    boolean forzarGuardado = AlertaPersonalizada.mostrarConfirmacion("Auditoría de Inventario", veredictoLocal + "\n\n¿Estás seguro de registrar este lote?");
                    if (!forzarGuardado) return;
                }
                DialogoPersonalizado.mostrarDialogo("Costo de Compra", "Registro de Gasto: " + cantidad + " " + insumoBase.getUnidad() + " de " + insumoBase.getNombre(), "Ingrese el costo total pagado en Bs:", "0.00")
                        .ifPresent(costoStr -> {
                            try {
                                double costo = Double.parseDouble(costoStr.replace(",", "."));
                                if (costo < 0) {
                                    mostrarAlerta("Error", "El costo no puede ser negativo");
                                    return;
                                }
                                boolean exitoDB = ApiClient.registrarLoteEnServidor(codigoBase, cantidad, costo, nuevaFecha);
                                if (exitoDB) {
                                    ApiClient.registrarEgresoEnServidor("Registrar insumo",costo);
                                    String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                                    App.registrarAuditoria(operador, "Insumo", insumoBase.getNombre(), "Ingreso Lote", cantidad, "Ingreso de nuevo lote. Costo: " + costo);
                                    new Thread(() ->{
                                        ConexionSQLite.sincronizarInsumos();
                                        ArrayList<Insumo> insumosFrescos = ApiClient.obtenerInsumoDelServidor();
                                        Platform.runLater(() ->{
                                            App.attizos.getInventario().getInventarioInsumos().clear();
                                            for(Insumo ins : insumosFrescos){
                                                App.attizos.getInventario().getInventarioInsumos().put(ins.getCodigo(), ins);
                                            }
                                            txtCodigoInsumo.clear();
                                            txtCantidad.clear();
                                            dpFechaVencimiento.setValue(null);
                                            cargarDatos();
                                            mostrarExito("Lote Registrado", "El stock ha sido sumado correctamente en la Base de Datos.");

                                        });
                                    }).start();
                                } else {
                                    mostrarAlerta("Error", "No se pudo registrar el lote. ");
                                }
                            } catch (NumberFormatException e) {
                                mostrarAlerta("Error de Costo", "El costo ingresado no es un número válido.");
                            }
                        });
            }else{
                mostrarAlerta("Insumo no encontrado", "El código base ' "+codigoBase + " ' no existe en el catalogo.");
            }
        }catch (Exception e){
            mostrarAlerta("Error", "Error al procesar la solicitud. " + e.getMessage());
        }
    }

    @FXML
    void disminuirStock(ActionEvent event) {
        Insumo seleccionado = tablaInventario.getSelectionModel().getSelectedItem();
        if(seleccionado == null){
            mostrarAlerta("Selección requerida", "Seleccione un insumo de la tabla.");
            return;
        }
        if(seleccionado.getStockActual() >= 1){
            DialogoPersonalizado.mostrarDialogo("Justificación de Merma / Ajuste", "Descontando 1 unidad de: " + seleccionado.getNombre(), "Describa el motivo (Ej: Caducado, Dañado, Consumo interno):", "")
                    .ifPresent(motivo -> {
                        if(motivo.trim().isEmpty()){
                            mostrarAlerta("Obligatorio", "Debe ingresar una explicación válida para los reportes de almacén.");
                        }else{
                            boolean descuentoExitoso = ApiClient.descontarStockFEFOEnServidor(seleccionado.getCodigo(), 1);
                            if(descuentoExitoso){
                                String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                                App.registrarAuditoria(operador, "Insumo", seleccionado.getNombre(), "Ajuste Stock (-1)", 1.0, "Merma FEFO: " + motivo);

                                new Thread(() -> {
                                    ConexionSQLite.sincronizarInsumos();
                                    ArrayList<Insumo> insumosFrescos = ApiClient.obtenerInsumoDelServidor();

                                    Platform.runLater(() -> {
                                        App.attizos.getInventario().getInventarioInsumos().clear();
                                        for(Insumo ins : insumosFrescos){
                                            App.attizos.getInventario().getInventarioInsumos().put(ins.getCodigo(),ins);
                                        }
                                        txtCodigoInsumo.clear();
                                        txtCantidad.clear();
                                        dpFechaVencimiento.setValue(null);

                                        cargarDatos();

                                        Insumo actualizado = App.attizos.getInventario().buscarInsumo(seleccionado.getCodigo());
                                        if (actualizado != null) {
                                            mostrarExito("Ajuste Realizado", "Se descontó 1 unidad bajo el sistema FEFO.\nQuedan: " + actualizado.getStockActual() + " " + actualizado.getUnidad());
                                            if (actualizado.getStockActual() <= actualizado.getStockMinimo()) {
                                                mostrarAlerta("⚠ Stock Crítico", "El insumo bajó a niveles críticos. ¡Es momento de reabastecer!");
                                            }
                                        } else {
                                            mostrarExito("Insumo Agotado", "El stock llegó a cero en todos los lotes. El catálogo sigue activo.\nMotivo Registrado: " + motivo);
                                        }
                                    });
                                }).start();
                            }
                        }
                    });
        }else {
            mostrarAlerta("Sin Stock", "El insumo ya tiene 0 unidades en todos sus lotes.");
        }
    }

    @FXML
    void eliminarInsumo(ActionEvent event) {
        Insumo seleccionado = tablaInventario.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Selección requerida", "Seleccione el insumo a eliminar.");
            return;
        }

        DialogoPersonalizado.mostrarDialogo("Eliminar Lote / Insumo", "ATENCIÓN: Va a eliminar completamente '" + seleccionado.getCodigo() + "'", "Explique el motivo de la eliminación:", "")
                .ifPresent(motivo -> {
                    if (motivo.trim().isEmpty()) {
                        mostrarAlerta("Obligatorio", "Es obligatorio dejar un registro del porqué se elimina información del sistema.");
                    } else {
                        boolean eliminadoDB = ApiClient.inactivarInsumoEnServidor(seleccionado.getCodigo());
                        if(eliminadoDB) {
                            App.attizos.getInventario().getInventarioInsumos().remove(seleccionado.getCodigo());
                            String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                            App.registrarAuditoria(operador, "Insumo", seleccionado.getNombre(), "Eliminación", seleccionado.getStockActual(), "Eliminación del sistema. Motivo: " + motivo);
                            mostrarExito("Insumo Eliminado", "El registro ha sido eliminado del sistema.\nMotivo guardado: " + motivo);
                            cargarDatos();
                        }
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
    private void mostrarAlerta(String titulo, String mensaje) {
        AlertaPersonalizada.mostrarAlerta(titulo, mensaje, Alert.AlertType.WARNING);
    }

    private void mostrarExito(String titulo, String mensaje) {
        AlertaPersonalizada.mostrarAlerta(titulo, mensaje, Alert.AlertType.INFORMATION);
    }
    @FXML
    void limpiarLotesCaducados(ActionEvent event){
        Insumo seleccionado = tablaInventario.getSelectionModel().getSelectedItem();

        if(seleccionado == null){
            mostrarAlerta("Selección requerida", "Seleccione un insumo de la tabla.");
            return;
        }
        if (!seleccionado.isVencido()) {
            mostrarExito("Todo en orden", "El insumo '" + seleccionado.getNombre() + "' no tiene lotes caducados actualmente.");
            return;
        }
        DialogoPersonalizado.mostrarDialogo(
                "Retirar Merma por Caducidad",
                "Limpieza de lotes vencidos de: " + seleccionado.getNombre(),
                "Escriba una justificación para los reportes de almacén:",
                "Retiro por fecha de vencimiento superada"
        ).ifPresent(motivo -> {
            if (motivo.trim().isEmpty()) {
                mostrarAlerta("Obligatorio", "Debe ingresar una justificación válida para retirar la merma.");
            } else {
                double retirado = ApiClient.darDeBajaLotesVencidosEnServidor(seleccionado.getCodigo());

                if (retirado > 0) {
                    String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                    App.registrarAuditoria(operador, "Insumo", seleccionado.getNombre(), "Merma por Caducidad", retirado, "Limpieza manual: " + motivo);
                    new Thread(() ->{
                        ConexionSQLite.sincronizarInsumos();
                        ArrayList<Insumo> insumosFrescos = ApiClient.obtenerInsumoDelServidor();

                        Platform.runLater(() ->{
                            App.attizos.getInventario().getInventarioInsumos().clear();
                            for(Insumo ins : insumosFrescos){
                                App.attizos.getInventario().getInventarioInsumos().put(ins.getCodigo(),ins);
                            }
                            cargarDatos();
                            mostrarExito("Limpieza Exitosa", "Se retiraron " + retirado + " " + seleccionado.getUnidad() + " caducados.\nLos lotes frescos siguen intactos en el inventario.");

                        });
                    }).start();
                } else {
                    mostrarAlerta("Error", "No se pudo realizar la limpieza en la base de datos.");
                }
            }
        });

    }
}