package Attizos.Frontend;

import Attizos.Backend.Attizos.*;
import Attizos.Backend.Database.*;
import Attizos.Backend.Listas.NodoDE;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.List;

public class ReportesController {

    // --- PESTAÑA 1: FINANZAS Y VENTAS ---
    @FXML private DatePicker dpInicio;
    @FXML private DatePicker dpFin;
    @FXML private Label lblIngresos;
    @FXML private Label lblEgresos;
    @FXML private Label lblBalance;
    @FXML private Label lblTicketProm;

    @FXML private TableView<Factura> tablaVentas;
    @FXML private TableColumn<Factura, Integer> colNroFac;
    @FXML private TableColumn<Factura, String> colFechaFac;
    @FXML private TableColumn<Factura, String> colClienteFac;
    @FXML private TableColumn<Factura, Double> colTotalFac;

    @FXML private TableView<Egreso> tablaEgresos;
    @FXML private TableColumn<Egreso, String> colFechaEgreso;
    @FXML private TableColumn<Egreso, String> colConceptoEgreso;
    @FXML private TableColumn<Egreso, Double> colMontoEgreso;

    // --- PESTAÑA 2: PLANILLA Y SUELDOS ---
    @FXML private Label lblTotalSueldos;
    @FXML private TableView<Empleado> tablaEmpleadosReporte;
    @FXML private TableColumn<Empleado, String> colEmpId;
    @FXML private TableColumn<Empleado, String> colEmpNombre;
    @FXML private TableColumn<Empleado, String> colEmpCargo;
    @FXML private TableColumn<Empleado, Double> colEmpSueldo;

    // --- PESTAÑA 3: TRAZABILIDAD (SEGUIDOR) ---
    @FXML private ComboBox<String> cmbTipoLog;
    @FXML private TextField txtBusquedaLog;
    @FXML private ComboBox<String> cmbAccionLog;
    @FXML private ComboBox<String> cmbUsuarioLog;
    @FXML private TableView<RegistroAuditoria> tablaLogSeguimiento;
    @FXML private TableColumn<RegistroAuditoria, String> colFechaLog;
    @FXML private TableColumn<RegistroAuditoria, String> colUsuarioLog;
    @FXML private TableColumn<RegistroAuditoria, String> colTipoItemLog;
    @FXML private TableColumn<RegistroAuditoria, String> colItemLog;
    @FXML private TableColumn<RegistroAuditoria, String> colAccionLog;
    @FXML private TableColumn<RegistroAuditoria, Double> colCantidadLog;
    @FXML private TableColumn<RegistroAuditoria, String> colJustificacionLog;

    // --- PESTAÑA 4: GRÁFICOS ---
    @FXML private LineChart<String, Number> chartVentas;

    // Listas observables
    private ObservableList<Factura> listaFacturas = FXCollections.observableArrayList();
    private ObservableList<Egreso> listaEgresos = FXCollections.observableArrayList();
    private ObservableList<Empleado> listaEmpleados = FXCollections.observableArrayList();

    // Listas para el buscador en tiempo real de Auditoría
    private ObservableList<RegistroAuditoria> masterLogs = FXCollections.observableArrayList();
    private FilteredList<RegistroAuditoria> filteredLogs;

    @FXML
    public void initialize() {
        configurarTablas();
        configurarBotonDobleClic();
        configurarFiltrosTrazabilidad();
        generarReportes();

        UtilidadesUI.formatearDatePicker(dpInicio);
        UtilidadesUI.formatearDatePicker(dpFin);
    }

    private void configurarTablas() {
        colNroFac.setCellValueFactory(new PropertyValueFactory<>("numeroFactura"));
        colClienteFac.setCellValueFactory(new PropertyValueFactory<>("nombreCliente"));
        colTotalFac.setCellValueFactory(new PropertyValueFactory<>("total"));
        colFechaFac.setCellValueFactory(cellData -> {
            LocalDateTime fecha = cellData.getValue().getFecha();
            return new SimpleStringProperty(fecha != null ? fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A");
        });
        tablaVentas.setItems(listaFacturas);

        colConceptoEgreso.setCellValueFactory(new PropertyValueFactory<>("description"));
        colMontoEgreso.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        colFechaEgreso.setCellValueFactory(cellData -> {
            LocalDate fecha = cellData.getValue().getDate();
            return new SimpleStringProperty(fecha != null ? fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        });
        tablaEgresos.setItems(listaEgresos);

        colEmpId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEmpNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colEmpCargo.setCellValueFactory(new PropertyValueFactory<>("cargo"));
        colEmpSueldo.setCellValueFactory(new PropertyValueFactory<>("sueldo"));
        tablaEmpleadosReporte.setItems(listaEmpleados);

        // Tabla de Auditoría
        colFechaLog.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));
        colUsuarioLog.setCellValueFactory(new PropertyValueFactory<>("operador"));
        colTipoItemLog.setCellValueFactory(new PropertyValueFactory<>("tipoArea"));
        colItemLog.setCellValueFactory(new PropertyValueFactory<>("nombreItem"));
        colAccionLog.setCellValueFactory(new PropertyValueFactory<>("accion"));
        colCantidadLog.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colJustificacionLog.setCellValueFactory(new PropertyValueFactory<>("motivo"));

        filteredLogs = new FilteredList<>(masterLogs, p -> true);
        tablaLogSeguimiento.setItems(filteredLogs);
    }

    private void configurarFiltrosTrazabilidad() {
        String estiloInputs = "-fx-text-fill: #111111; -fx-font-weight: bold; -fx-background-color: #f0f0f0; -fx-border-color: #a9a9a9; -fx-border-radius: 4; -fx-background-radius: 4;";

        txtBusquedaLog.setStyle(estiloInputs);
        cmbTipoLog.setStyle(estiloInputs);
        cmbAccionLog.setStyle(estiloInputs);
        cmbUsuarioLog.setStyle(estiloInputs);
        cmbTipoLog.setItems(FXCollections.observableArrayList("Todos"));
        cmbTipoLog.setValue("Todos");

        cmbAccionLog.setItems(FXCollections.observableArrayList("Todas"));
        cmbAccionLog.setValue("Todas");

        cmbUsuarioLog.setItems(FXCollections.observableArrayList("Todos"));
        cmbUsuarioLog.setValue("Todos");

        txtBusquedaLog.textProperty().addListener((obs, old, newVal) -> aplicarFiltroAuditoria());
        cmbTipoLog.valueProperty().addListener((obs, old, newVal) -> aplicarFiltroAuditoria());
        cmbAccionLog.valueProperty().addListener((obs, old, newVal) -> aplicarFiltroAuditoria());
        cmbUsuarioLog.valueProperty().addListener((obs, old, newVal) -> aplicarFiltroAuditoria());
    }

    private void actualizarFiltrosDinamicos() {
        Set<String> tipos = new HashSet<>();
        Set<String> acciones = new HashSet<>();
        Set<String> usuarios = new HashSet<>();

        tipos.add("Todos");
        acciones.add("Todas");
        usuarios.add("Todos");

        // Leer los datos reales que existen
        for (RegistroAuditoria log : masterLogs) {
            if (log.getTipoArea() != null) tipos.add(log.getTipoArea());
            if (log.getAccion() != null) acciones.add(log.getAccion());
            if (log.getOperador() != null) usuarios.add(log.getOperador());
        }

        String tipoAct = cmbTipoLog.getValue();
        String accionAct = cmbAccionLog.getValue();
        String userAct = cmbUsuarioLog.getValue();

        cmbTipoLog.setItems(FXCollections.observableArrayList(tipos));
        cmbAccionLog.setItems(FXCollections.observableArrayList(acciones));
        cmbUsuarioLog.setItems(FXCollections.observableArrayList(usuarios));

        cmbTipoLog.setValue(tipos.contains(tipoAct) ? tipoAct : "Todos");
        cmbAccionLog.setValue(acciones.contains(accionAct) ? accionAct : "Todas");
        cmbUsuarioLog.setValue(usuarios.contains(userAct) ? userAct : "Todos");
    }

    private void aplicarFiltroAuditoria() {
        filteredLogs.setPredicate(log -> {
            boolean matchBuscador = true;
            boolean matchTipo = true;
            boolean matchAccion = true;
            boolean matchUsuario = true;

            String texto = txtBusquedaLog.getText();
            if (texto != null && !texto.isEmpty()) {
                String filter = texto.toLowerCase();
                String fechaHoraStr = log.getFechaHora() != null ? log.getFechaHora().toLowerCase() : "";
                String operadorStr = log.getOperador() != null ? log.getOperador().toLowerCase() : "";
                String tipoAreaStr = log.getTipoArea() != null ? log.getTipoArea().toLowerCase() : "";
                String nombreItemStr = log.getNombreItem() != null ? log.getNombreItem().toLowerCase() : "";
                String accionStr = log.getAccion() != null ? log.getAccion().toLowerCase() : "";
                String cantidadStr = String.valueOf(log.getCantidad()).toLowerCase();
                String motivoStr = log.getMotivo() != null ? log.getMotivo().toLowerCase() : "";

                matchBuscador = fechaHoraStr.contains(filter) ||
                        operadorStr.contains(filter) ||
                        tipoAreaStr.contains(filter) ||
                        nombreItemStr.contains(filter) ||
                        accionStr.contains(filter) ||
                        cantidadStr.contains(filter) ||
                        motivoStr.contains(filter);
            }

            if (cmbTipoLog.getValue() != null && !cmbTipoLog.getValue().equals("Todos")) {
                matchTipo = log.getTipoArea() != null && log.getTipoArea().equalsIgnoreCase(cmbTipoLog.getValue());
            }

            if (cmbAccionLog.getValue() != null && !cmbAccionLog.getValue().equals("Todas")) {
                matchAccion = log.getAccion() != null && log.getAccion().equalsIgnoreCase(cmbAccionLog.getValue());
            }

            if (cmbUsuarioLog.getValue() != null && !cmbUsuarioLog.getValue().equals("Todos")) {
                matchUsuario = log.getOperador() != null && log.getOperador().equalsIgnoreCase(cmbUsuarioLog.getValue());
            }

            return matchBuscador && matchTipo && matchAccion && matchUsuario;
        });
    }

    @FXML
    void actualizarPorFechas(ActionEvent event) {
        LocalDate inicio = dpInicio.getValue();
        LocalDate fin = dpFin.getValue();

        if (inicio != null && fin != null) {
            if (inicio.isAfter(fin) || inicio.isEqual(fin)) {
                AlertaPersonalizada.mostrarAlerta("Rango de Fechas Inválido",
                        "La fecha de inicio debe ser anterior a la fecha de fin.", Alert.AlertType.WARNING);
                return;
            }
        }
        generarReportes();
    }

    private void generarReportes() {
        double ingresosTotales = 0.0;
        double egresosTotales = 0.0;
        int contadorVentas = 0;

        LocalDate filtroInicio = dpInicio.getValue();
        LocalDate filtroFin = dpFin.getValue();

        listaFacturas.clear();
        listaEgresos.clear();
        listaEmpleados.clear();
        masterLogs.clear(); 
        Map<String, Double> ventasPorDia = new TreeMap<>();

        List<Factura> facturasDB = ReportesDAO.obtenerFacturas();
        for(Factura f : facturasDB){
            boolean enRango = true;
            if(f.getFecha() != null){
                LocalDate fechaFac = f.getFecha().toLocalDate();
                if (filtroInicio != null && fechaFac.isBefore(filtroInicio)) enRango = false;
                if (filtroFin != null && fechaFac.isAfter(filtroFin)) enRango = false;
            }
            if(enRango){
                listaFacturas.add(f);
                ingresosTotales += f.getTotal();
                contadorVentas++;
                if(f.getFecha() != null){
                    String dia = f.getFecha().format(DateTimeFormatter.ofPattern("dd/MM"));
                    ventasPorDia.put(dia, ventasPorDia.getOrDefault(dia, 0.0) + f.getTotal());
                }
            }
        }

        List<Egreso> egresosDB = ReportesDAO.obtenerEgresos();
        for (Egreso e : egresosDB) {
            boolean enRango = true;
            if (e.getDate() != null) {
                LocalDate fechaEgr = e.getDate();
                if (filtroInicio != null && fechaEgr.isBefore(filtroInicio)) enRango = false;
                if (filtroFin != null && fechaEgr.isAfter(filtroFin)) enRango = false;
            }
            if(enRango) {
                listaEgresos.add(e);
                egresosTotales += e.getTotalAmount();
            }
        }

        List<Empleado> empleadosDB = ReportesDAO.obtenerEmpleados();
        double sumaSueldos = 0.0;
        for (Empleado emp : empleadosDB) {
            listaEmpleados.add(emp);
            sumaSueldos += emp.getSueldo();
        }
        List<RegistroAuditoria> auditoriaDB = ReportesDAO.obtenerAuditoria();
        masterLogs.addAll(auditoriaDB);


        double balance = ingresosTotales - egresosTotales;
        double ticketPromedio = (contadorVentas > 0) ? (ingresosTotales / contadorVentas) : 0.0;

        lblIngresos.setText("Bs. " + String.format("%.2f", ingresosTotales));
        lblEgresos.setText("Bs. " + String.format("%.2f", egresosTotales));
        lblBalance.setText("Bs. " + String.format("%.2f", balance));
        lblTicketProm.setText("Bs. " + String.format("%.2f", ticketPromedio));
        lblTotalSueldos.setText("Bs. " + String.format("%.2f", sumaSueldos));

        if (balance >= 0) {
            lblBalance.setStyle("-fx-text-fill: #218c4e;");
        } else {
            lblBalance.setStyle("-fx-text-fill: #ff4c4c;");
        }

        chartVentas.getData().clear();
        XYChart.Series<String, Number> serieVentas = new XYChart.Series<>();
        serieVentas.setName("Ingresos por Día");
        for (Map.Entry<String, Double> entry : ventasPorDia.entrySet()) {
            serieVentas.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        chartVentas.getData().add(serieVentas);
        Platform.runLater(() ->{
            if(serieVentas.getNode() != null){
                serieVentas.getNode().setStyle("-fx-stroke: #2c3e50; -fx-stroke-width: 3px;");
            }
            for(XYChart.Data<String, Number> data : serieVentas.getData()){
                if(data.getNode() != null){
                    data.getNode().setStyle("-fx-background-color: #2c3e50, white; -fx-background-radius: 5px; -fx-padding: 5px;");
                }
            }
            chartVentas.lookupAll(".axis").forEach(axis ->
                    axis.setStyle("-fx-tick-label-fill: #111111; -fx-font-weight: bold; -fx-font-size: 13px;")
            );
            chartVentas.lookupAll(".chart-legend-item").forEach(legend ->
                    legend.setStyle("-fx-text-fill: #111111; -fx-font-weight: bold;")
            );
        });

        actualizarFiltrosDinamicos(); // Llenamos los ComboBox con los datos reales encontrados
    }

    @FXML
    void registrarGastoManual(ActionEvent event) {
        DialogoPersonalizado.mostrarDialogo("Gasto Manual", "Registrar un nuevo egreso externo", "Concepto del gasto (Ej: Pago luz):", "")
                .ifPresent(concepto -> {
                    if (concepto.trim().isEmpty()) {
                        AlertaPersonalizada.mostrarAlerta("Error", "Debe ingresar una descripción obligatoria.", Alert.AlertType.WARNING);
                        return;
                    }
                    DialogoPersonalizado.mostrarDialogo("Monto del Gasto", "Monto para: " + concepto, "Ingrese el monto en Bs:", "0.00")
                            .ifPresent(montoStr -> {
                                try {
                                    double monto = Double.parseDouble(montoStr.replace(",", "."));
                                    if (monto <= 0) {
                                        AlertaPersonalizada.mostrarAlerta("Error", "El monto debe ser mayor a 0.", Alert.AlertType.WARNING);
                                        return;
                                    }

                                    // Registrar Gasto
                                    boolean exito = ReportesDAO.registrarEgreso(concepto, monto);

                                    if(exito){
                                        String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                                        ReportesDAO.registrarAuditoria(operador, "Finanzas", "Gasto Manual", "Egreso", monto, concepto);

                                        generarReportes();
                                        AlertaPersonalizada.mostrarAlerta("Éxito", "El gasto manual ha sido registrado.", Alert.AlertType.INFORMATION);
                                    }else{
                                        AlertaPersonalizada.mostrarAlerta("Error", "No se pudo guardar el gasto.", Alert.AlertType.ERROR);
                                    }
                                } catch (NumberFormatException e) {
                                    AlertaPersonalizada.mostrarAlerta("Error", "Debe ingresar un número válido.", Alert.AlertType.ERROR);
                                }
                            });
                });
    }

    private void configurarBotonDobleClic() {
        tablaVentas.setRowFactory(tv -> {
            TableRow<Factura> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    verDetalleFacturaTicket(row.getItem());
                }
            });
            return row;
        });
    }

    private void verDetalleFacturaTicket(Factura seleccionada) {
        try {
            Factura facDetalles = ReportesDAO.obtenerFacturaConDetalles(seleccionada.getNumeroFactura());
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Ticket.fxml"));
            Parent root = loader.load();

            TicketController controller = loader.getController();
            controller.inicializarTicket(seleccionada, "Copia - Sistema", seleccionada.getNumeroFactura());

            Stage dialogStage = new Stage();
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            dialogStage.initModality(Modality.APPLICATION_MODAL);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            dialogStage.setScene(scene);
            scene.setOnKeyPressed(event -> {
                if((event.getCode() == KeyCode.ESCAPE) || event.getCode() == KeyCode.ENTER){
                    dialogStage.close();
                }
            });

            final double[] xOffset = {0};
            final double[] yOffset = {0};
            root.setOnMousePressed(event -> {
                xOffset[0] = event.getSceneX();
                yOffset[0] = event.getSceneY();
            });
            root.setOnMouseDragged(event -> {
                dialogStage.setX(event.getScreenX() - xOffset[0]);
                dialogStage.setY(event.getScreenY() - yOffset[0]);
            });

            dialogStage.showAndWait();
        } catch (Exception e) {
            AlertaPersonalizada.mostrarAlerta("Error", "No se pudo cargar el Ticket.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    void verDetalleEmpleado(ActionEvent event) {
        Empleado seleccionado = tablaEmpleadosReporte.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            AlertaPersonalizada.mostrarAlerta("Selección requerida", "Seleccione un empleado.", Alert.AlertType.WARNING);
            return;
        }

        String detalle = "IDENTIFICACIÓN (CI): " + seleccionado.getId() + "\n"
                + "NOMBRE COMPLETO: " + seleccionado.getNombre() + "\n"
                + "CARGO ASIGNADO: " + seleccionado.getCargo() + "\n"
                + "SUELDO BASE: Bs. " + String.format("%.2f", seleccionado.getSueldo()) + "\n\n";

        if (seleccionado instanceof Usuario) {
            detalle += "--- DATOS DEL SISTEMA ---\n"
                    + "Usuario: " + ((Usuario) seleccionado).getUsername() + "\n"
                    + "Rol: Acceso Permitido\n";
        } else {
            detalle += "--- DATOS DEL SISTEMA ---\n"
                    + "Sin acceso a la caja.\n";
        }
        AlertaPersonalizada.mostrarAlerta("Ficha Técnica - " + seleccionado.getNombre(), detalle, Alert.AlertType.INFORMATION);
    }
}