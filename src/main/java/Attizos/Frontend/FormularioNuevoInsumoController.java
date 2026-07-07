package Attizos.Frontend;

import Attizos.Backend.AI.AuditoriaAI;
import Attizos.Backend.AI.AuditoriaLocal;
import Attizos.Backend.Api.ApiClient;
import Attizos.Backend.Attizos.App;
import Attizos.Backend.Attizos.Insumo;
import Attizos.Backend.Database.ConexionSQLite;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class FormularioNuevoInsumoController {
    @FXML private TextField txtCodigo;
    @FXML private TextField txtNombre;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private ComboBox<String>cmbUnidad;
    @FXML private TextField txtStockInicial;
    @FXML private TextField txtStockMin;
    @FXML private TextField txtStockMax;
    @FXML private DatePicker dpVencimiento;
    @FXML private AnchorPane rootPane;
    @FXML private HBox topBar;
    @FXML private CheckBox chkNoCaduca;

    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        UtilidadesUI.formatearDatePicker(dpVencimiento);
        topBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        dpVencimiento.setDisable(true);
        if (chkNoCaduca != null) chkNoCaduca.setDisable(true);

        txtStockInicial.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double stock = Double.parseDouble(newVal.replace(",", "."));
                boolean tieneStock = stock > 0;

                if (chkNoCaduca != null) chkNoCaduca.setDisable(!tieneStock);

                if (chkNoCaduca != null && chkNoCaduca.isSelected()) {
                    dpVencimiento.setDisable(true);
                } else {
                    dpVencimiento.setDisable(!tieneStock);
                }

                if (!tieneStock) {
                    dpVencimiento.setValue(null);
                    if (chkNoCaduca != null) chkNoCaduca.setSelected(false);
                }
            } catch (NumberFormatException e) {
                dpVencimiento.setDisable(true);
                if (chkNoCaduca != null) {
                    chkNoCaduca.setDisable(true);
                    chkNoCaduca.setSelected(false);
                }
            }
        });

        if (chkNoCaduca != null) {
            chkNoCaduca.setOnAction(e -> {
                if (chkNoCaduca.isSelected()) {
                    dpVencimiento.setValue(LocalDate.of(2099, 12, 31));
                    dpVencimiento.setDisable(true);
                } else {
                    // Devolvemos el control al usuario
                    dpVencimiento.setValue(null);
                    dpVencimiento.setDisable(false);
                }
            });
        }

        topBar.setOnMouseDragged(event -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
        cmbUnidad.getItems().addAll("g", "kg", "ml", "lt", "und", "paquete", "lb", "oz");
        Set<String> categoriasUnicas = new HashSet<>();
        HashMap<String, Insumo> invetarioDB = App.attizos.getInventario().getInventarioInsumos();
        if (invetarioDB != null) {
            for (Insumo i : invetarioDB.values()) {
                categoriasUnicas.add(i.getCategoria());
            }
        }
        cmbCategoria.setStyle("-fx-background-color: #FDF6E3; -fx-border-color: #DAA520; -fx-border-radius: 5; -fx-background-radius: 5;");
        cmbCategoria.getEditor().setStyle("-fx-text-fill: #111111; -fx-font-weight: bold; -fx-background-color: transparent;");
        cmbCategoria.getItems().addAll(categoriasUnicas);

        saltoConEnter(txtNombre, cmbCategoria);
        saltoConEnter(cmbCategoria, cmbUnidad);
        saltoConEnter(cmbUnidad, txtStockInicial);
        saltoConEnter(txtStockInicial, txtStockMin);
        saltoConEnter(txtStockMin, txtStockMax);
        saltoConEnter(txtStockMax, dpVencimiento);

        dpVencimiento.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                guardarNuevoInsumo(new ActionEvent(dpVencimiento, dpVencimiento));
            }
        });
        txtCodigo.setText(generarCodigoAutomatico());
        javafx.application.Platform.runLater(() -> txtNombre.requestFocus());

    }

    @FXML
    void guardarNuevoInsumo(ActionEvent event) {
        try {
            String cod = txtCodigo.getText().trim();
            String nombre = txtNombre.getText().trim();
            String categoria = cmbCategoria.getEditor().getText().trim();
            String unidad = cmbUnidad.getValue() != null ? cmbUnidad.getValue() : "";

            if (cod.isEmpty() || nombre.isEmpty() || unidad.isEmpty()) {
                mostrarError("Por favor, llene al menos Código, Nombre y Unidad.");
                return;
            }
            HashMap<String, Insumo> inventarioActual = App.attizos.getInventario().getInventarioInsumos();
            if (inventarioActual.containsKey(cod)) {
                mostrarError("El código '" + cod + "' ya está registrado en el inventario.");
                return;
            }
            for (Insumo i : inventarioActual.values()) {
                if (i.getNombre().equalsIgnoreCase(nombre)) {
                    mostrarError("Ya existe un insumo registrado con el nombre '" + nombre + "' (Código: " + i.getCodigo() + ").");
                    return;
                }
            }

            double inicial = txtStockInicial.getText().isEmpty() ? 0 : Double.parseDouble(txtStockInicial.getText().replace(",", "."));
            double min = txtStockMin.getText().isEmpty() ? 0 : Double.parseDouble(txtStockMin.getText().replace(",", "."));
            double max = txtStockMax.getText().isEmpty() ? 0 : Double.parseDouble(txtStockMax.getText().replace(",", "."));
            if(inicial < 0){
                mostrarError("El stock inicial no puede ser negativo. ");
                return;
            }
            if(min < 0 || max < 0){
                mostrarError("El stock no puede ser negativo. ");
                return;
            }
            if(min > max){
                mostrarError("El stock mínino no puedes ser mayor que el stock máximo");
                return;
            }
            LocalDate vencimientoTemp = dpVencimiento.getValue();
            if(inicial <= 0){
                vencimientoTemp = LocalDate.of(2099, 12, 31);
            }else if(vencimientoTemp == null){
                mostrarError("Por favor, seleccione o ingrese la fecha de vencimiento.");
                return;
            }
            if(vencimientoTemp.isBefore(LocalDate.now())){
                mostrarError("No se puede agregar un insumo vencido. ");
                return;
            }
            final LocalDate vencimientoFinal = vencimientoTemp;
            long diasFaltantes = ChronoUnit.DAYS.between(LocalDate.now(), vencimientoFinal);
            String veredictoLocal = AuditoriaLocal.auditarCreacion(nombre, categoria, unidad, min, max, diasFaltantes);
            if (veredictoLocal.startsWith("ALERTA:")) {
                boolean forzarGuardado = AlertaPersonalizada.mostrarConfirmacion(
                        "Auditoría Local: Catálogo",
                        veredictoLocal + "\n\n¿Deseas ignorar la advertencia y crear el insumo en el catálogo?"
                );
                            if (!forzarGuardado) {
                                return;
                            }
                        }

                        if (inicial > 0) {
                            DialogoPersonalizado.mostrarDialogo(
                                    "Costo del stock inicial",
                                    "Registro de Gasto: " + inicial + " " + unidad + " de " + nombre,
                                    "Ingrese el costo total pagado en Bs:",
                                    "0.00"
                            ).ifPresent(costoStr -> {
                                try {
                                    double costo = Double.parseDouble(costoStr.replace(",", "."));
                                    if (costo < 0) {
                                        mostrarError("El costo no puede ser negativo. ");
                                        return;
                                    }
                                    ejecutarGuardado(cod, nombre, categoria, unidad, inicial, min, max, vencimientoFinal, costo, event);
                                } catch (NumberFormatException e) {
                                    mostrarError("El costo ingresado no es válido.");
                                }
                            });
                        } else {
                            ejecutarGuardado(cod, nombre, categoria, unidad, inicial, min, max, vencimientoFinal, 0.0, event);
                        }
        } catch (NumberFormatException e) {
            mostrarError("Los campos de stock deben ser números válidos (Ej: 1.5).");
        }
    }


    private void ejecutarGuardado(String cod, String nom, String cat, String uni, double inicial, double min, double max, LocalDate ven,double costo, ActionEvent event) {
        try {
            double multiplicador = 1;
            String unidadFinal = uni;
            switch (uni.toLowerCase()){
                case "kg":
                    multiplicador = 1000;
                    unidadFinal = "g";
                    break;
                case "lt":
                    multiplicador = 1000;
                    unidadFinal = "ml";
                    break;
                case "lb":
                    multiplicador = 453.592;
                    unidadFinal = "g";
                case "oz":
                    multiplicador = 28.3495;
                    unidadFinal = "g";
                    break;
            }
            double stockInicial = inicial * multiplicador;
            double stockMinimo = min * multiplicador;
            double stockMaximo = max * multiplicador;
            Insumo nuevo = new Insumo(cod, nom, cat, unidadFinal, stockInicial, stockMinimo, stockMaximo, ven); // Corrección: Usar unidadFinal y variables de stock calculadas
            boolean guardado = ApiClient.guardarInsumoEnServidor(nuevo, costo);
            if(guardado) {
                if(costo > 0){
                    ApiClient.registrarEgresoEnServidor("Stock Inicial Catálogo: "+nom,costo);
                }
                App.attizos.getInventario().getInventarioInsumos().put(nuevo.getCodigo(), nuevo);

                // Hilo secundario para actualizar SQLite
                new Thread(() -> ConexionSQLite.sincronizarInsumos()).start();

                String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                App.registrarAuditoria(
                        operador,
                        "Insumo",
                        nuevo.getNombre(),
                        "Creación",
                        nuevo.getStockActual(),
                        "Ingreso inicial de nuevo insumo al catálogo"
                );
                new Thread(() ->{
                    ConexionSQLite.subirAuditoriaPendiente();
                    ConexionSQLite.sincronizarInsumos();
                }).start();

                AlertaPersonalizada.mostrarAlerta("¡Éxito!", "El insumo '" + nom + "' se agregó correctamente al catálogo.", Alert.AlertType.INFORMATION);

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.close();
            }else{
                mostrarError("No se pudo guardar el insumo en la Base de Datos. Revise su conexión.");
            }
        } catch (Exception e) {
            mostrarError("Ocurrió un error al intentar guardar: " + e.getMessage());
        }
    }
    private void mostrarError(String msj) {
        AlertaPersonalizada.mostrarAlerta("Datos inválidos", msj, Alert.AlertType.ERROR);
    }

    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
    private void saltoConEnter(Node campoAc, Node sigCampo){
        campoAc.setOnKeyPressed(event ->{
            switch (event.getCode()){
                case ENTER:
                    sigCampo.requestFocus();
                    event.consume();
                    break;
                default:
                    break;
            }
        });
    }

    private String generarCodigoAutomatico() {
        return ApiClient.obtenerSiguienteCodigoInsumo();
    }
}