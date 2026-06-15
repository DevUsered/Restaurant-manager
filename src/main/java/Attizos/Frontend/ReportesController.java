package Attizos.Frontend;

import Attizos.Backend.Api.ApiClient;
import Attizos.Backend.Attizos.*;
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
import java.util.*;

public class ReportesController {

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

    @FXML private Label lblTotalSueldos;
    @FXML private TableView<Empleado> tablaEmpleadosReporte;
    @FXML private TableColumn<Empleado, String> colEmpId;
    @FXML private TableColumn<Empleado, String> colEmpNombre;
    @FXML private TableColumn<Empleado, String> colEmpCargo;
    @FXML private TableColumn<Empleado, Double> colEmpSueldo;

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

    @FXML private LineChart<String, Number> chartVentas;

    private ObservableList<Factura> listaFacturas = FXCollections.observableArrayList();
    private ObservableList<Egreso> listaEgresos = FXCollections.observableArrayList();
    private ObservableList<Empleado> listaEmpleados = FXCollections.observableArrayList();
    private ObservableList<RegistroAuditoria> masterLogs = FXCollections.observableArrayList();
    private FilteredList<RegistroAuditoria> filteredLogs;

    @FXML
    public void initialize() {
        configurarTablas();
        configurarBotonDobleClic();
        configurarFiltrosTrazabilidad();
        UtilidadesUI.formatearDatePicker(dpInicio);
        UtilidadesUI.formatearDatePicker(dpFin);

        generarReportes();
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
            return new SimpleStringProperty(fecha != null ? fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
        });
        tablaEgresos.setItems(listaEgresos);

        colEmpId.setCellValueFactory(new PropertyValueFactory<>("idEmpleado"));
        colEmpNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colEmpCargo.setCellValueFactory(new PropertyValueFactory<>("cargo"));
        colEmpSueldo.setCellValueFactory(new PropertyValueFactory<>("sueldo"));
        tablaEmpleadosReporte.setItems(listaEmpleados);

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

        if (inicio != null && fin != null && (inicio.isAfter(fin) || inicio.isEqual(fin))) {
            AlertaPersonalizada.mostrarAlerta("Rango Inválido", "La fecha de inicio debe ser anterior a la fecha de fin.", Alert.AlertType.WARNING);
            return;
        }
        generarReportes();
    }

    private void generarReportes() {
        String fechaIni = dpInicio.getValue() != null ? dpInicio.getValue().toString() : null;
        String fechaFin = dpFin.getValue() != null ? dpFin.getValue().toString() : null;

        new Thread(() -> {
            Map<String, Object> reporte = ApiClient.obtenerReporteConsolidado(fechaIni, fechaFin);

            if (reporte != null) {
                Platform.runLater(() -> procesarDatosDelServidor(reporte));
            } else {
                Platform.runLater(() -> AlertaPersonalizada.mostrarAlerta("Sin Conexión", "No se pudo cargar el reporte del servidor.", Alert.AlertType.WARNING));
            }
        }).start();
    }

    @SuppressWarnings("unchecked")
    private void procesarDatosDelServidor(Map<String, Object> r) {
        double ingresos = ((Number) r.get("totalIngresos")).doubleValue();
        double egresos = ((Number) r.get("totalEgresos")).doubleValue();
        double sueldos = ((Number) r.get("totalSueldos")).doubleValue();
        int ventas = (Integer) r.get("cantidadVentas");
        double balance = ingresos - egresos;

        lblIngresos.setText(String.format("Bs. %.2f", ingresos));
        lblEgresos.setText(String.format("Bs. %.2f", egresos));
        lblTotalSueldos.setText(String.format("Bs. %.2f", sueldos));
        lblTicketProm.setText(String.valueOf(ventas));
        lblBalance.setText(String.format("Bs. %.2f", balance));
        lblBalance.setStyle(balance >= 0 ? "-fx-text-fill: #218c4e;" : "-fx-text-fill: #ff4c4c;");

        listaFacturas.clear();
        List<Map<String, Object>> facturasDB = (List<Map<String, Object>>) r.get("facturas");
        if (facturasDB != null) {
            for (Map<String, Object> map : facturasDB) {
                Factura f = new Factura((Integer) map.get("numero_factura"), (String) map.get("nombre_cliente"));
                f.setNumeroTicket((Integer) map.get("numero_ticket"));
                f.setTotal(((Number) map.get("total")).doubleValue());
                f.setEstado((String) map.get("estado"));
                try {
                    String fechaStr = map.get("fecha_hora").toString().replace("T", " ");
                    f.setFecha(LocalDateTime.parse(fechaStr.substring(0, 19), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                } catch (Exception e) {}
                listaFacturas.add(f);
            }
        }

        listaEgresos.clear();
        List<Map<String, Object>> egresosDB = (List<Map<String, Object>>) r.get("egresos");
        if(egresosDB != null) {
            for (Map<String, Object> map : egresosDB) {
                Egreso e = new Egreso();
                e.setDescription((String) map.get("concepto"));
                e.setTotalAmount(((Number) map.get("monto")).doubleValue());
                try { e.setDate(LocalDate.parse(map.get("fecha").toString().substring(0, 10))); } catch (Exception ex) {}
                listaEgresos.add(e);
            }
        }

        listaEmpleados.clear();
        List<Map<String, Object>> empDB = (List<Map<String, Object>>) r.get("empleados");
        if(empDB != null) {
            for (Map<String, Object> map : empDB) {
                Empleado emp = new Empleado();
                emp.setIdEmpleado((String) map.get("id_empleado"));
                emp.setNombre((String) map.get("nombre"));
                emp.setCargo((String) map.get("cargo"));
                emp.setSueldo(((Number) map.get("sueldo")).doubleValue());
                emp.setUsername((String) map.get("username"));
                listaEmpleados.add(emp);
            }
        }

        masterLogs.clear();
        List<Map<String, Object>> auditDB = (List<Map<String, Object>>) r.get("auditoria");
        if(auditDB != null) {
            for(Map<String, Object> map : auditDB) {
                RegistroAuditoria log = new RegistroAuditoria(
                        (String) map.get("operador"),
                        (String) map.get("tipo_area"),
                        (String) map.get("nombre_item"),
                        (String) map.get("accion"),
                        ((Number) map.get("cantidad")).doubleValue(),
                        (String) map.get("motivo")
                );
                try {
                    String fechaStr = map.get("fecha_hora").toString().replace("T", " ");
                    LocalDateTime ldt = LocalDateTime.parse(fechaStr.substring(0, 19), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    log.setDate(ldt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
                } catch (Exception e) {}
                masterLogs.add(log);
            }
        }

        chartVentas.getData().clear();
        XYChart.Series<String, Number> serieVentas = new XYChart.Series<>();
        serieVentas.setName("Ingresos por Día");

        Map<String, Double> ventasXDia = (Map<String, Double>) r.get("ventasPorDia");
        if (ventasXDia != null) {
            for (Map.Entry<String, Double> entry : ventasXDia.entrySet()) {
                String diaLabel = entry.getKey().substring(5); // "MM-dd"
                serieVentas.getData().add(new XYChart.Data<>(diaLabel, entry.getValue()));
            }
        }
        chartVentas.getData().add(serieVentas);

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

        actualizarFiltrosDinamicos();
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
                                    boolean exito = ApiClient.registrarEgresoEnServidor(concepto, monto);

                                    if(exito){
                                        String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                                        ApiClient.registrarAuditoriaEnServidor(operador, "Finanzas", "Gasto Manual", "Egreso", monto, concepto);

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

    private void verDetalleFacturaTicket(Factura seleccionada) {
        try {
            Factura facDetalles = ApiClient.obtenerFacturaConDetalles(seleccionada.getNumeroFactura());

            if (facDetalles == null) {
                AlertaPersonalizada.mostrarAlerta("Error", "No se pudo obtener el detalle de la factura desde el servidor.", Alert.AlertType.ERROR);
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Ticket.fxml"));
            Parent root = loader.load();

            TicketController controller = loader.getController();
            controller.inicializarTicket(seleccionada, "Copia - Sistema", facDetalles.getNumeroTicket());

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

        // 1. Cálculo de Antigüedad
        String antiguedad = "Desconocida";
        if (seleccionado.getFechaContrato() != null) {
            java.time.Period periodo = java.time.Period.between(seleccionado.getFechaContrato(), LocalDate.now());
            int agnos = periodo.getYears();
            int meses = periodo.getMonths();
            int dias = periodo.getDays();

            if (agnos == 0 && meses == 0) {
                antiguedad = dias + " días";
            } else if (agnos == 0) {
                antiguedad = meses + " meses y " + dias + " días";
            } else {
                antiguedad = agnos + " años y " + meses + " meses";
            }
        }

        double totalAdelantos = 0.0;
        StringBuilder desgloseAdelantos = new StringBuilder();
        int contadorAdelantos = 0;

        String nombreBuscado = seleccionado.getNombre().toLowerCase();

        for (Egreso e : listaEgresos) {
            if (e.getDescription() != null) {
                String desc = e.getDescription().toLowerCase();

                if (desc.contains("adelanto") && desc.contains(nombreBuscado)) {
                    totalAdelantos += e.getTotalAmount();
                    contadorAdelantos++;

                    String fechaStr = e.getDate() != null ? e.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "S/F";
                    desgloseAdelantos.append("   • ").append(fechaStr)
                            .append(" -> Bs. ").append(String.format("%.2f", e.getTotalAmount()))
                            .append(" (").append(e.getDescription()).append(")\n");
                }
            }
        }

        if (contadorAdelantos == 0) {
            desgloseAdelantos.append("   No se registran adelantos tomados en este periodo.\n");
        }

        // 3. Estado de pago actual
        String estadoPago = "Sin registros de pago completo";
        if (seleccionado.getFechaUltimoPago() != null) {
            if (seleccionado.getFechaUltimoPago().getMonth() == LocalDate.now().getMonth() &&
                    seleccionado.getFechaUltimoPago().getYear() == LocalDate.now().getYear()) {
                estadoPago = "✅ Sueldo Mensual Cancelado (" + seleccionado.getFechaUltimoPago().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")";
            } else {
                estadoPago = "❌ Sueldo del mes pendiente";
            }
        }

        double sueldoBase = seleccionado.getSueldo();
        double liquidoNeto = Math.max(0, sueldoBase - totalAdelantos); // Evita números negativos por si se sobregira
        double costoAnualProyectado = (sueldoBase * 12) + sueldoBase; // 12 sueldos + 1 Aguinaldo

        StringBuilder sb = new StringBuilder();
        sb.append("👤 PERFIL DEL EMPLEADO\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("Nombre: ").append(seleccionado.getNombre()).append("\n");
        sb.append("CI / ID: ").append(seleccionado.getIdEmpleado()).append("\n");
        sb.append("Cargo: ").append(seleccionado.getCargo()).append("\n\n");

        sb.append("⏱ TIEMPO EN LA EMPRESA\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        String fechaContratoStr = seleccionado.getFechaContrato() != null ? seleccionado.getFechaContrato().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "No registrada";
        sb.append("Fecha Ingreso: ").append(fechaContratoStr).append("\n");
        sb.append("Antigüedad: ").append(antiguedad).append("\n\n");

        sb.append("📉 HISTORIAL DE ADELANTOS (Filtro de Fechas Actual)\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(desgloseAdelantos.toString());
        sb.append("Total Retirado: Bs. ").append(String.format("%.2f", totalAdelantos)).append("\n\n");

        sb.append("💰 CONTROL DE PLANILLA Y LIQUIDACIÓN\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("Sueldo Mensual Fijo:  Bs. ").append(String.format("%.2f", sueldoBase)).append("\n");
        sb.append("Descuento Adelantos: -Bs. ").append(String.format("%.2f", totalAdelantos)).append("\n");
        sb.append("Líquido Neto a Pagar:  Bs. ").append(String.format("%.2f", liquidoNeto)).append("\n");
        sb.append("Estado de Cuenta:     ").append(estadoPago).append("\n");
        sb.append("Costo Anual Proyectado: Bs. ").append(String.format("%.2f", costoAnualProyectado)).append("\n\n");

        sb.append("🔐 ACCESO AL SISTEMA\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        if (seleccionado.getUsername() != null && !seleccionado.getUsername().trim().isEmpty()) {
            sb.append("Usuario de caja: ").append(seleccionado.getUsername()).append("\n");
            sb.append("Estado: Habilitado para operar\n");
        } else {
            sb.append("Estado: Sin acceso al sistema (Solo operativo)\n");
        }
        Alert ficha = new Alert(Alert.AlertType.INFORMATION);
        ficha.setTitle("Ficha Técnica");
        ficha.setHeaderText("Reporte Financiero y Laboral: " + seleccionado.getNombre());

        TextArea txtReporte = new TextArea(sb.toString());
        txtReporte.setEditable(false);
        txtReporte.setWrapText(true);
        txtReporte.setPrefWidth(600);
        txtReporte.setPrefHeight(350);
        txtReporte.setStyle("-fx-font-size: 14px; -fx-font-family: 'Segoe UI', Helvetica, Arial, sans-serif;");

        ficha.getDialogPane().setContent(txtReporte);
        ficha.showAndWait();
    }
}