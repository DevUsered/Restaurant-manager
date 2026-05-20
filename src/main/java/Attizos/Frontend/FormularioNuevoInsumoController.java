package Attizos.Frontend;

import Attizos.Backend.Attizos.App;
import Attizos.Backend.Attizos.Insumo;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Dialog;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.time.LocalDate;

public class FormularioNuevoInsumoController {
    @FXML private TextField txtCodigo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtCategoria;
    @FXML private TextField txtUnidad;
    @FXML private TextField txtStockInicial;
    @FXML private TextField txtStockMin;
    @FXML private TextField txtStockMax;
    @FXML private DatePicker dpVencimiento;
    @FXML private AnchorPane rootPane;
    @FXML private HBox topBar;

    private double xOffset = 0;
    private double yOffset = 0;
    @FXML
    public void initialize(){
        topBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        topBar.setOnMouseDragged(event -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    @FXML
    void guardarNuevoInsumo(ActionEvent event) {
        try{
            String cod = txtCodigo.getText().trim();
            String nombre = txtNombre.getText().trim();
            String categoria = txtCategoria.getText().trim();
            String unidad = txtUnidad.getText().trim();
            if(cod.isEmpty() || nombre.isEmpty() || unidad.isEmpty()){
                mostrarError("Por favor, llene al menos Código, Nombre y Unidad.");
                return;
            }
            if(App.attizos.getInventario().buscarInsumo(cod) != null){
                mostrarError("El código '" + cod + "' ya está registrado en el inventario.");
                return;
            }
            for(Insumo i : App.attizos.getInventario().getInventarioInsumos().values()){
                if(i.getNombre().equalsIgnoreCase(nombre)){
                    mostrarError("Ya existe un insumo registrado con el nombre '" + nombre + "' (Código: " + i.getCodigo() + ").");
                    return;
                }
            }
            double inicial = txtStockInicial.getText().isEmpty() ? 0 : Double.parseDouble(txtStockInicial.getText().replace(",", "."));
            double min = txtStockMin.getText().isEmpty() ? 0 : Double.parseDouble(txtStockMin.getText().replace(",", "."));
            double max = txtStockMax.getText().isEmpty() ? 0 : Double.parseDouble(txtStockMax.getText().replace(",", "."));
            LocalDate vencimiento = dpVencimiento.getValue();

            if(inicial > 0 ){
                javafx.scene.control.TextInputDialog dialogCosto = new javafx.scene.control.TextInputDialog("0.00");
                dialogCosto.setTitle("Costo del stock inicial");
                dialogCosto.setHeaderText("Registro de Gasto: " + inicial + " " + unidad + " de " + nombre);
                dialogCosto.setContentText("Ingrese el costo total pagado en Bs:");
                aplicarEstiloOscuro(dialogCosto); // Aplica el diseño oscuro

                dialogCosto.showAndWait().ifPresent(costoStr ->{
                    try{
                        double costo = Double.parseDouble(costoStr.replace(",","."));
                        App.attizos.registerExpense(new Attizos.Backend.Attizos.Egreso("Stock Inicial Catálogo: "+nombre, costo));
                        ejecutarGuardado(cod, nombre, categoria, unidad, inicial, min, max, vencimiento, event);
                    }catch (NumberFormatException e){
                        mostrarError("El costo ingresado no es válido. ");
                    }
                });
            }else{
                ejecutarGuardado(cod, nombre, categoria, unidad, inicial, min, max, vencimiento, event);
            }
        }catch (NumberFormatException e) {
            mostrarError("Los campos de stock deben ser números válidos (Ej: 1.5).");
        }
    }

    private void ejecutarGuardado(String cod, String nom, String cat, String uni, double inicial, double min, double max, LocalDate ven, ActionEvent event){
        try{
            Insumo nuevo = new Insumo(cod, nom, cat, uni, inicial, min, max, ven);
            App.attizos.getInventario().agregarInsumo(nuevo);

            Alert exito = new Alert(Alert.AlertType.INFORMATION);
            exito.setTitle("¡Éxito!");
            exito.setHeaderText(null);
            exito.setContentText("El insumo '" + nom + "' se agregó correctamente al catálogo.");
            aplicarEstiloOscuro(exito);
            exito.showAndWait();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.close();
        }catch (Exception e){
            mostrarError("Ocurrió un error al intentar guardar: " + e.getMessage());
        }
    }

    private void aplicarEstiloOscuro(Dialog<?> dialog) {
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #1a0a2a; -fx-border-color: #00d2ff; -fx-border-width: 2px;");
        dialogPane.lookupAll(".label").forEach(node -> ((Label) node).setStyle("-fx-text-fill: white; -fx-font-weight: bold;"));
    }

    private void mostrarError(String msj) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setHeaderText("Datos inválidos");
        alerta.setContentText(msj);
        aplicarEstiloOscuro(alerta); // Aplica diseño oscuro
        alerta.showAndWait();
    }
    @FXML
    void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}