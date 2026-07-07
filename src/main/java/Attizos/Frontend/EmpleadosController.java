package Attizos.Frontend;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import Attizos.Backend.Api.ApiClient;
import Attizos.Backend.Attizos.*;
import Attizos.Backend.Database.ConexionSQLite;
import Attizos.Frontend.Network.WebSocketManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import javax.swing.*;

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
    @FXML
    private TableColumn<Empleado, Boolean> colEstadoPago;
    @FXML
    private TableColumn<Empleado, String> colFechaContrato;

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
        colId.setCellValueFactory(new PropertyValueFactory<>("idEmpleado"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCargo.setCellValueFactory(new PropertyValueFactory<>("cargo"));
        colSueldo.setCellValueFactory(new PropertyValueFactory<>("sueldo"));
        colEstadoPago.setCellValueFactory(new PropertyValueFactory<>("pagadoEsteMes"));
        colEstadoPago.setCellFactory(column -> new javafx.scene.control.TableCell<Empleado, Boolean>() {
            @Override
            protected void updateItem(Boolean pagado, boolean empty) {
                super.updateItem(pagado, empty);
                if (empty || pagado == null) {
                    setText(null);
                    setStyle("");
                } else {
                    if (pagado) {
                        setText("✅ Pagado");
                        setStyle("-fx-text-fill: #218c4e; -fx-font-weight: bold; -fx-alignment: CENTER;"); // Verde
                    } else {
                        setText("❌ Pendiente");
                        setStyle("-fx-text-fill: #ff4c4c; -fx-font-weight: bold; -fx-alignment: CENTER;"); // Rojo
                    }
                }
            }
        });
        colFechaContrato.setCellValueFactory(cellData -> {
            LocalDate fecha = cellData.getValue().getFechaContrato();
            return new SimpleStringProperty(fecha != null ? fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A");
        });

        colUsername.setCellValueFactory(cellData -> {
            Empleado emp = cellData.getValue();
            if (emp.getUsername() != null && !emp.getUsername().trim().isEmpty()) {
                return new SimpleStringProperty(emp.getUsername());
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
                if (emp.getIdEmpleado().toLowerCase().contains(lowerCaseFilter)) return true;
                if (emp.getCargo().toLowerCase().contains(lowerCaseFilter)) return true;

                return false;
            });
        });

        ObservableList<String> cargosComunes = FXCollections.observableArrayList(
                "Administrador", "Cajero", "Cocinero", "Mesero", "Limpieza", "Seguridad", "Otro (Escribir...)"
        );
        cmbCargo.setItems(cargosComunes);
        cmbCargo.setEditable(true);
        cmbCargo.getEditor().setStyle("-fx-background-color: transparent; -fx-text-fill: #111111;");
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
        WebSocketManager.setAccionEmpleados(() ->{
            System.out.println("Hubo modificacion en la tabla de empleados. Actualizando la tabla...");
            new Thread(() ->{
                ConexionSQLite.sincronizarEmpleados();
                Platform.runLater(() -> {
                    App.attizos.getEmpleados().clear();
                    App.cargarEmpleados();
                    cargarEmpleados();
                    System.out.println("Cargar empleados...");
                });

            }).start();

        });
    }

    @FXML
    void guardarEmpleado(ActionEvent event) {
        try {
            String id = txtId.getText().trim();
            double sueldo = Double.parseDouble(txtSueldo.getText().trim().replace(",", "."));
            if(sueldo <= 0){
                mostrarAlerta("Sueldo inválido","Ingrese un sueldo válido.");
                return;
            }
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

            Empleado nuevoEmpleado = new Empleado();
            nuevoEmpleado.setIdEmpleado(id);
            nuevoEmpleado.setNombre(nombre);
            nuevoEmpleado.setCargo(cargo);
            nuevoEmpleado.setSueldo(sueldo);
            nuevoEmpleado.setEstado("Activo");
            nuevoEmpleado.setFechaContrato(LocalDate.now());

            if(cargo.equalsIgnoreCase("Administrador") || cargo.equalsIgnoreCase("Cajero") || cargo.equalsIgnoreCase("Cocinero")){
                String user = txtUsername.getText().trim();
                String pass = txtPassword.getText().trim();
                if(user.isEmpty() || pass.isEmpty()){
                    mostrarAlerta("Faltan Credenciales", "Debe asignar un usuario y contraseña.");
                    return;
                }
                nuevoEmpleado.setUsername(user);
                nuevoEmpleado.setPasswordHash(pass);
            }

            UtilidadesUI.ejecutarTareaAsincrona(
                    tablaEmpleados,
                    () -> {
                        boolean guardadoDB = ApiClient.guardarEmpleadoEnServidor(nuevoEmpleado);
                        if (guardadoDB) {
                            ConexionSQLite.sincronizarEmpleados();
                        }
                        return guardadoDB;
                    },
                    (exito) -> {
                        if (exito) {
                            masterData.add(nuevoEmpleado);
                            if(App.attizos != null){
                                App.attizos.agregarEmpleado(nuevoEmpleado);
                            }
                            String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                            App.registrarAuditoria(operador, "Empleado", nombre, "Contratación", sueldo, "Nuevo empleado registrado: " + cargo);

                            new Thread(() -> ConexionSQLite.subirAuditoriaPendiente()).start();
                            limpiarFormulario(null);
                            mostrarExito("Contratado", "El empleado " + nombre + " fue registrado con éxito.");
                        } else {
                            mostrarAlerta("Error de Persistencia", "No se pudo registrar el empleado en el servidor.");
                        }
                    }
            );

        } catch (NumberFormatException e) {
            mostrarAlerta("Error de formato", "El ID y el Sueldo deben ser válidos.");
        }
    }

    @FXML
    void eliminarEmpleado(ActionEvent event) {
        int index = tablaEmpleados.getSelectionModel().getSelectedIndex();
        if (index >= 0) {
            Empleado despedido = filteredData.get(index);

            DialogoPersonalizado.mostrarDialogo("Confirmar Despido", "Cuidado: Va a eliminar al empleado " + despedido.getNombre(), "Escriba 'SI' para confirmar el despido:", "")
                    .ifPresent(respuesta -> {
                        if (respuesta.trim().equalsIgnoreCase("SI")) {

                            UtilidadesUI.ejecutarTareaAsincrona(
                                    tablaEmpleados,
                                    () -> {
                                        boolean eliminadoDB = ApiClient.inactivarEmpleadoEnServidor(despedido.getIdEmpleado());
                                        if (eliminadoDB) {
                                            ConexionSQLite.sincronizarEmpleados();
                                        }
                                        return eliminadoDB;
                                    },
                                    (exito) -> {
                                        if (exito) {
                                            masterData.remove(index);
                                            if (App.attizos != null) {
                                                App.attizos.eliminarEmpleado(despedido.getIdEmpleado());
                                                String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                                                App.registrarAuditoria(operador, "Empleado", despedido.getNombre(), "Despido", 0, "Despedir un empleado");
                                                new Thread(() -> ConexionSQLite.subirAuditoriaPendiente()).start();
                                            }
                                            mostrarExito("Despedido", "El empleado ha sido removido de la planilla.");
                                        } else {
                                            mostrarAlerta("Error de persistencia", "No se pudo eliminar el empleado.");
                                        }
                                    }
                            );

                        } else {
                            mostrarAlerta("Cancelado", "El empleado NO fue despedido.");
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
        if(App.attizos != null && App.attizos.getEmpleados() != null){
            masterData.addAll(App.attizos.getEmpleados());
            }
        masterData.sort(Comparator.comparing(Empleado::getCargo));
        tablaEmpleados.refresh();
    }

    @FXML
    void cargarParaEditar(ActionEvent event) {
        Empleado seleccionado = tablaEmpleados.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Selección requerida", "Primero seleccione un empleado de la tabla para editar.");
            return;
        }
        txtId.setText(String.valueOf(seleccionado.getIdEmpleado()));
        txtNombre.setText(seleccionado.getNombre());
        txtSueldo.setText(String.valueOf(seleccionado.getSueldo()));
        cmbCargo.setValue(seleccionado.getCargo());

        if (seleccionado.getUsername() != null && !seleccionado.getUsername().trim().isEmpty()) {
            txtUsername.setText(seleccionado.getUsername());
            txtPassword.setText(seleccionado.getPasswordHash());
            txtUsername.setDisable(false);
            txtPassword.setDisable(false);
        } else {
            txtUsername.clear();
            txtPassword.clear();
            txtUsername.setDisable(true);
            txtPassword.setDisable(true);
        }
        txtId.setDisable(true);
    }

    @FXML
    void actualizarEmpleado(ActionEvent event) {
        if (!txtId.isDisabled()) {
            mostrarAlerta("Modo incorrecto", "Para actualizar, primero seleccione un empleado y presione 'Cargar para Editar'.");
            return;
        }
        Empleado seleccionadoOriginal = tablaEmpleados.getSelectionModel().getSelectedItem();
        if(seleccionadoOriginal == null) return;

        try {
            String id = txtId.getText().trim();
            double sueldo = Double.parseDouble(txtSueldo.getText().trim().replace(",", "."));
            if(sueldo <= 0){ mostrarAlerta("Sueldo inválido","Ingrese un sueldo válido."); return; }
            String nombre = txtNombre.getText().trim();
            String cargo = cmbCargo.getValue();

            if (cargo == null || cargo.trim().isEmpty() || cargo.equals("Otro (Escribir...)")) {
                mostrarAlerta("Datos incompletos", "El cargo es obligatorio.");
                return;
            }

            Empleado empleadoActualizado = new Empleado();
            empleadoActualizado.setIdEmpleado(id);
            empleadoActualizado.setNombre(nombre);
            empleadoActualizado.setCargo(cargo);
            empleadoActualizado.setSueldo(sueldo);
            empleadoActualizado.setEstado("Activo");
            empleadoActualizado.setFechaUltimoPago(seleccionadoOriginal.getFechaUltimoPago());
            empleadoActualizado.setFechaContrato(seleccionadoOriginal.getFechaContrato());

            if (cargo.equalsIgnoreCase("Administrador") || cargo.equalsIgnoreCase("Cajero") || cargo.equalsIgnoreCase("Cocinero")) {
                String user = txtUsername.getText().trim();
                String pass = txtPassword.getText().trim();
                if (user.isEmpty() || pass.isEmpty()) {
                    mostrarAlerta("Faltan Credenciales", "Debe asignar un usuario y contraseña para ese cargo.");
                    return;
                }
                empleadoActualizado.setUsername(user);
                empleadoActualizado.setPasswordHash(pass);
            }

            // Usamos tu super herramienta para no congelar la pantalla
            UtilidadesUI.ejecutarTareaAsincrona(
                    tablaEmpleados,
                    () -> {
                        // TAREA LENTA: API y Sincronización SQLite (Hilo secundario)
                        boolean actualizadoDB = ApiClient.actualizarEmpleadoEnServidor(empleadoActualizado);
                        if (actualizadoDB) {
                            ConexionSQLite.sincronizarEmpleados();
                        }
                        return actualizadoDB;
                    },
                    (exito) -> {
                        if (exito) {
                            for(int i = 0; i < masterData.size(); i++){
                                if(masterData.get(i).getIdEmpleado().equals(id)){
                                    masterData.set(i, empleadoActualizado);
                                    break;
                                }
                            }
                            if(App.attizos != null){
                                App.attizos.eliminarEmpleado(id);
                                App.attizos.agregarEmpleado(empleadoActualizado);
                            }
                            String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                            App.registrarAuditoria(operador, "Empleado", nombre, "Actualización", sueldo, "Datos modificados (Cargo/Sueldo)");
                            new Thread(() -> ConexionSQLite.subirAuditoriaPendiente()).start();
                            limpiarFormulario(null);
                            mostrarExito("Actualizado", "Los datos de " + nombre + " fueron actualizados correctamente.");
                        } else {
                            mostrarAlerta("Error de Persistencia", "No se pudieron actualizar los cambios en el servidor.");
                        }
                    }
            );

        } catch (NumberFormatException e) {
            mostrarAlerta("Error", "Revise que el sueldo sea un número válido.");
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        AlertaPersonalizada.mostrarAlerta(titulo, mensaje, Alert.AlertType.WARNING);
    }

    private void mostrarExito(String titulo, String mensaje) {
        AlertaPersonalizada.mostrarAlerta(titulo, mensaje, Alert.AlertType.INFORMATION);
    }
    @FXML
    void pagarSueldoEmpleado(ActionEvent event){
        Empleado sel = tablaEmpleados.getSelectionModel().getSelectedItem();

        if(sel == null){
            mostrarAlerta("Atención", "Seleccione un empleado paar cancelar. ");
            return;
        }
        String mesActual = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("es","ES")));
        mesActual = mesActual.substring(0, 1).toUpperCase() + mesActual.substring(1);
        DialogoPersonalizado.mostrarDialogo(
                "Pago de Sueldo",
                "Iniciando pago para: "+sel.getNombre() + "\nSueldo base registrado: Bs" + sel.getSueldo(),
                "Ingrese el mes o periodo a pagar (Ej: Mayo 2026, Adelanto, etc.):",
                mesActual
        ).ifPresent(periodo ->{
            if(periodo.trim().isEmpty()){
                mostrarAlerta("Periodo requerido", "Debe ingresar un periodo para registrar el pago.");
                return;
            }
            String conceptoEgreso = "Sueldo: "+sel.getNombre() + " ("+periodo.trim()+")";

            boolean yaPagado = ApiClient.existeEgresoPorConcepto(conceptoEgreso);
            if(yaPagado){
                Optional<String> confirmacion = DialogoPersonalizado.mostrarDialogo(
                        "⚠ ADVERTENCIA DE PAGO DUPLICADO",
                        "El sistema detecta que YA SE LE CANCELÓ a " + sel.getNombre() + " por el periodo: " + periodo + ".",
                        "Si esto es un bono extra o saldo restante, escriba 'CONFIRMAR'. De lo contrario, deje en blanco para abortar:",
                        ""
                );
                if (confirmacion.isEmpty() || !confirmacion.get().equalsIgnoreCase("CONFIRMAR")) {
                    mostrarAlerta("Operación Cancelada", "Se abortó el pago para evitar duplicidad.");
                    return;
                }
                conceptoEgreso = "Adelanto Extra: " + sel.getNombre() + " (" + periodo.trim() + ")";
            }
            final String conceptoFinal = conceptoEgreso;
            DialogoPersonalizado.mostrarDialogo(
                    "Monto a Cancelar",
                    "Periodo a pagar: "+periodo,
                    "Verifique o modifique el monto exacto a pagar (Bs)",
                    String.valueOf((sel.getSueldo()))
            ).ifPresent(montoStr ->{
                try{
                    double montoPagar = Double.parseDouble(montoStr.replace(",", "."));
                    if(montoPagar <= 0){
                        mostrarAlerta("Error", "El monto a cancelar debe ser mayor a 0.");
                        return;
                    }
                    UtilidadesUI.ejecutarTareaAsincrona(
                            tablaEmpleados,
                            () ->{
                                boolean egresoRegistrado = ApiClient.registrarEgresoEnServidor(conceptoFinal, montoPagar);
                                if(egresoRegistrado){
                                    String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                                    App.registrarAuditoria(operador, "Empleado", sel.getNombre(), "Pago de Sueldo", montoPagar, "Pago del periodo: " + periodo);

                                    ApiClient.registrarPagoEnServidor(sel.getIdEmpleado());
                                    sel.setFechaUltimoPago(LocalDate.now());

                                    ConexionSQLite.sincronizarEmpleados();
                                    ConexionSQLite.subirAuditoriaPendiente();
                                }
                                return egresoRegistrado;
                            },
                            (exito) ->{
                                if(exito){
                                    mostrarExito("Pago Registrado", "✅ Se han cancelado Bs. " + montoPagar + " a " + sel.getNombre() + ".\nEl gasto ya figura en los reportes de la empresa.");
                                    tablaEmpleados.refresh();
                                } else {
                                    mostrarAlerta("Error", "No se pudo registrar el pago en la base de datos. Revise su conexión.");
                                }
                            }
                    );
                }catch (NumberFormatException e){
                    mostrarAlerta("Error de formato", "El monto ingresado no es válido. Ingrese un número para el monto a pagar.");
                }
            });
        });
    }
}