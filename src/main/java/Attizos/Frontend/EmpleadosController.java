package Attizos.Frontend;

import Attizos.Backend.Attizos.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Comparator;

public class EmpleadosController {

    // --- TABLA DE PLANILLA ---
    @FXML
    private TableView<Empleado> tablaEmpleados;
    @FXML
    private TableColumn<Empleado, String> colId;
    @FXML
    private TableColumn<Empleado, String> colNombre;
    @FXML
    private TableColumn<Empleado, String> colCargo;
    @FXML
    private TableColumn<Empleado, Double> colSueldo;
    @FXML
    private TableColumn<Empleado, String> colUsername;

    // --- FORMULARIO ---
    @FXML private TextField txtBuscador;
    @FXML
    private TextField txtId;
    @FXML
    private TextField txtSueldo;
    @FXML
    private TextField txtNombre;
    @FXML
    private ComboBox<String> cmbCargo;
    @FXML
    private TextField txtUsername;
    @FXML
    private PasswordField txtPassword;


    private ObservableList<Empleado> masterData;
    private FilteredList<Empleado> filteredData;

    @FXML
    public void initialize() {
        // 1. Configurar las columnas básicas
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCargo.setCellValueFactory(new PropertyValueFactory<>("cargo"));
        colSueldo.setCellValueFactory(new PropertyValueFactory<>("sueldo"));

        colUsername.setCellValueFactory(cellData -> {
            Empleado emp = cellData.getValue();
            if (emp instanceof Usuario) {
                return new SimpleStringProperty(((Usuario) emp).getUsername());
            } else {
                return new SimpleStringProperty("N/A");
            }
        });

        masterData = FXCollections.observableArrayList();
        filteredData = new FilteredList<>(masterData, p -> true);
        tablaEmpleados.setItems(filteredData);

        //Lógica del buscador
        txtBuscador.textProperty().addListener((observable, oldValue, newValue) ->{
            filteredData.setPredicate(emp ->{
                if(newValue == null || newValue.isEmpty()){
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();

                if (emp.getNombre().toLowerCase().contains(lowerCaseFilter)) return true;
                if (emp.getId().toLowerCase().contains(lowerCaseFilter)) return true;
                if (emp.getCargo().toLowerCase().contains(lowerCaseFilter)) return true;

                return false;
            });
        });

        ObservableList<String> cargosComunes = FXCollections.observableArrayList(
                "Administrador", "Cajero", "Cocinero", "Mesero", "Limpieza", "Seguridad", "Otro (Escribir...)"
        );
        cmbCargo.setItems(cargosComunes);
        cmbCargo.setEditable(true);

        cmbCargo.valueProperty().addListener((observable, valorViejo, valorNuevo) -> {
            if (valorNuevo != null && (valorNuevo.equalsIgnoreCase("Administrador") || valorNuevo.equalsIgnoreCase("Cajero") ||
                    valorNuevo.equalsIgnoreCase("Cocinero"))) {
                txtUsername.setDisable(false);
                txtPassword.setDisable(false);
            } else {
                txtUsername.setDisable(true);
                txtPassword.setDisable(true);
                txtUsername.clear();
                txtPassword.clear();
            }
        });

        cargarEmpleados();
    }

    @FXML
    void guardarEmpleado(ActionEvent event) {
        try {
            String id = txtId.getText().trim();
            double sueldo = Double.parseDouble(txtSueldo.getText().trim().replace(",", "."));
            String nombre = txtNombre.getText().trim();
            String cargo = cmbCargo.getValue();

            if (id.isEmpty() || nombre.isEmpty() || cargo == null || cargo.trim().isEmpty() || cargo.equals("Otro (Escribir...)")) {
                mostrarAlerta("Datos incompletos", "El ID, nombre y el cargo son obligatorios.");
                return;
            }
            if(App.attizos != null && App.attizos.buscarEmpleado(id) != null){
                mostrarAlerta("ID Duplicado", "Ya existe un empleado registrado con el CI/ID: " + id);
                return;
            }
            Empleado nuevoEmpleado;

            if (cargo.equalsIgnoreCase("Administrador")) {
                String user = txtUsername.getText().trim();
                String pass = txtPassword.getText().trim();
                if (user.isEmpty() || pass.isEmpty()) {
                    mostrarAlerta("Faltan Credenciales", "Un Administrador necesita Usuario y Contraseña.");
                    return;
                }
                nuevoEmpleado = new Admin(id, nombre, user, pass, sueldo);

            } else if (cargo.equalsIgnoreCase("Cajero")) {
                String user = txtUsername.getText().trim();
                String pass = txtPassword.getText().trim();
                if (user.isEmpty() || pass.isEmpty()) {
                    mostrarAlerta("Faltan Credenciales", "Un Cajero necesita Usuario y Contraseña.");
                    return;
                }
                nuevoEmpleado = new Cajero(id, nombre, sueldo, user, pass);

            } else if(cargo.equalsIgnoreCase("Cocinero")) {
                String user = txtUsername.getText().trim();
                String pass = txtPassword.getText().trim();
                if (user.isEmpty() || pass.isEmpty()) {
                    mostrarAlerta("Faltan Credenciales", "Un Cocinero necesita Usuario y Contraseña.");
                    return;
                }
                nuevoEmpleado = new Cocinero(id, nombre, sueldo, user, pass);
            }else{
                nuevoEmpleado = new Empleado(id,nombre,cargo,sueldo);
            }
            masterData.add(nuevoEmpleado);

            if (App.attizos != null ) {
                App.attizos.agregarEmpleado(nuevoEmpleado);
            }
            limpiarFormulario(null);
            mostrarExito("Contratado", "El empleado " + nombre + " fue registrado con éxito.");

        } catch (NumberFormatException e) {
            mostrarAlerta("Error de formato", "El ID y el Sueldo deben ser números.");
        }
    }

    @FXML
    void eliminarEmpleado(ActionEvent event) {
        int index = tablaEmpleados.getSelectionModel().getSelectedIndex();
        if (index >= 0) {
            Empleado despedido = masterData.get(index);

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmar Despido");
            alert.setHeaderText(null);
            alert.setContentText("¿Está seguro que desea despedir a " + despedido.getNombre() + "?");
            aplicarEstiloOscuro(alert);

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    masterData.remove(index);
                    if (App.attizos != null) {
                        App.attizos.eliminarEmpleado(despedido.getId());
                    }
                    mostrarExito("Despedido", "El empleado ha sido removido de la planilla.");
                }
            });
        } else {
            mostrarAlerta("Selección requerida", "Seleccione un empleado de la tabla para despedirlo.");
        }
    }

    @FXML
    void limpiarFormulario(ActionEvent event) {
        txtId.clear();
        txtSueldo.clear();
        txtNombre.clear();
        cmbCargo.getSelectionModel().clearSelection();
        cmbCargo.getEditor().clear();
        txtUsername.clear();
        txtPassword.clear();

        txtId.setDisable(false);
        txtNombre.setDisable(false);
    }

    private void cargarEmpleados() {
        masterData.clear();
        if (App.attizos != null && App.attizos.getEmpleados() != null) {
            for (Empleado emp : App.attizos.getEmpleados()) {
                masterData.add(emp);
            }
        }
        masterData.sort(Comparator.comparing(Empleado::getCargo));
    }

    @FXML
    void cargarParaEditar(ActionEvent event) {
        Empleado seleccionado = tablaEmpleados.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Selección requerida", "Primero seleccione un empleado de la tabla para editar.");
            return;
        }
        txtId.setText(String.valueOf(seleccionado.getId()));
        txtNombre.setText(seleccionado.getNombre());
        txtSueldo.setText(String.valueOf(seleccionado.getSueldo()));
        cmbCargo.setValue(seleccionado.getCargo());

        if (seleccionado instanceof Usuario) {
            txtUsername.setText(((Usuario) seleccionado).getUsername());
            txtPassword.setText(((Usuario) seleccionado).getPassword());
            txtUsername.setDisable(false);
            txtPassword.setDisable(false);
        } else {
            txtUsername.clear();
            txtPassword.clear();
            txtUsername.setDisable(true);
            txtPassword.setDisable(true);
        }
        txtId.setDisable(true);
        txtNombre.setDisable(true);
    }

    @FXML
    void actualizarEmpleado(ActionEvent event) {
        if (!txtId.isDisabled()) {
            mostrarAlerta("Modo incorrecto", "Para actualizar, primero seleccione un empleado y presione 'Cargar para Editar'.");
            return;
        }

        try {
            String id = txtId.getText().trim();
            double sueldo = Double.parseDouble(txtSueldo.getText().trim().replace(",", "."));
            String nombre = txtNombre.getText().trim();
            String cargo = cmbCargo.getValue();

            if (cargo == null || cargo.trim().isEmpty() || cargo.equals("Otro (Escribir...)")) {
                mostrarAlerta("Datos incompletos", "El cargo es obligatorio.");
                return;
            }

            Empleado empleadoActualizado;

            if (cargo.equalsIgnoreCase("Administrador")) {
                String user = txtUsername.getText().trim();
                String pass = txtPassword.getText().trim();
                if (user.isEmpty() || pass.isEmpty()) {
                    mostrarAlerta("Faltan Credenciales", "Debe asignar un usuario y contraseña.");
                    return;
                }
                empleadoActualizado = new Admin(id, nombre, user, pass, sueldo);

            } else if (cargo.equalsIgnoreCase("Cajero")) {
                String user = txtUsername.getText().trim();
                String pass = txtPassword.getText().trim();
                if (user.isEmpty() || pass.isEmpty()) {
                    mostrarAlerta("Faltan Credenciales", "Debe asignar un usuario y contraseña.");
                    return;
                }
                empleadoActualizado = new Cajero(id, nombre, sueldo, user, pass);
            }else if(cargo.equalsIgnoreCase("Cocinero")){
                String user = txtUsername.getText().trim();
                String pass = txtPassword.getText().trim();
                if (user.isEmpty() || pass.isEmpty()) {
                    mostrarAlerta("Faltan Credenciales", "Debe asignar un usuario y contraseña.");
                    return;
                }
                empleadoActualizado = new Cocinero(id, nombre, sueldo, user, pass);
            } else {
                empleadoActualizado = new Empleado(id, nombre, cargo, sueldo);
            }
            int index = tablaEmpleados.getSelectionModel().getSelectedIndex();
            if (index >= 0) {
                masterData.set(index, empleadoActualizado);
            }

            if (App.attizos != null) {
                App.attizos.eliminarEmpleado(id);
                App.attizos.agregarEmpleado(empleadoActualizado);
            }

            limpiarFormulario(null);
            mostrarExito("Actualizado", "Los datos de " + nombre + " fueron actualizados correctamente.");

        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "Revise que el sueldo sea un número válido.");
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