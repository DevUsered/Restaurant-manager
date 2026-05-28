package Attizos.Frontend;

import Attizos.Backend.Attizos.App;
import Attizos.Backend.Attizos.Insumo;
import Attizos.Backend.Database.InsumoDAO;
import Attizos.Backend.Database.ReportesDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.time.DateTimeException;
import java.time.LocalDate;
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

    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        UtilidadesUI.formatearDatePicker(dpVencimiento);
        topBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        topBar.setOnMouseDragged(event -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
        cmbUnidad.getItems().addAll("g", "kg", "ml", "lt", "und", "paquete", "lb", "oz");
        Set<String> categoriasUnicas = new HashSet<>();
        HashMap<String, Insumo> invetarioDB = InsumoDAO.obtenerInventarioActivo();
        if (invetarioDB != null) {
            for (Insumo i : invetarioDB.values()) {
                categoriasUnicas.add(i.getCategoria());
            }
        }
        cmbCategoria.setStyle("-fx-background-color: #FDF6E3; -fx-border-color: #DAA520; -fx-border-radius: 5; -fx-background-radius: 5;");
        cmbCategoria.getEditor().setStyle("-fx-text-fill: #111111; -fx-font-weight: bold; -fx-background-color: transparent;");
        cmbCategoria.getItems().addAll(categoriasUnicas);
        saltoConEnter(txtCodigo, txtNombre);
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
            HashMap<String, Insumo> inventarioActual = InsumoDAO.obtenerInventarioActivo();
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
            LocalDate vencimiento = dpVencimiento.getValue();
            if(vencimiento == null){
                mostrarError("Por favor, seleccione o ingrese la fecha de vencimiento.");
                return;
            }
            if(vencimiento.isBefore(LocalDate.now())){
                mostrarError("No se puede agregar un insumo vencido. ");
                return;
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
                        if(costo < 0){
                            mostrarError("El costo no puede ser negativo. ");
                            return;
                        }
                        ejecutarGuardado(cod, nombre, categoria, unidad, inicial, min, max, vencimiento, costo, event);
                    } catch (NumberFormatException e) {
                        mostrarError("El costo ingresado no es válido.");
                    }
                });
            } else {
                ejecutarGuardado(cod, nombre, categoria, unidad, inicial, min, max, vencimiento, 0.0, event);
            }
        } catch (NumberFormatException e) {
            mostrarError("Los campos de stock deben ser números válidos (Ej: 1.5).");
        }
    }

    private void ejecutarGuardado(String cod, String nom, String cat, String uni, double inicial, double min, double max, LocalDate ven,double costo, ActionEvent event) {
        try {
            Insumo nuevo = new Insumo(cod, nom, cat, uni, inicial, min, max, ven);
            boolean guardado = InsumoDAO.insertarInsumoNuevo(nuevo, costo);
            if(guardado) {
                if(costo > 0){
                    ReportesDAO.registrarEgreso("Stock Inicial Catálogo: "+nom, costo);
                }
                String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                App.registrarAuditoria(
                        operador,
                        "Insumo",
                        nuevo.getNombre(),
                        "Creación",
                        nuevo.getStockActual(),
                        "Ingreso inicial de nuevo insumo al catálogo"
                );

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
}