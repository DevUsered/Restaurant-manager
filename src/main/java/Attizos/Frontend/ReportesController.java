package Attizos.Frontend;

import Attizos.Backend.Api.ApiClient;
import Attizos.Backend.Attizos.*;
import Attizos.Frontend.Network.WebSocketManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.nio.charset.StandardCharsets;
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
    @FXML private Label lblIngresoEfectivo;
    @FXML private Label lblIngresoQR;

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
        WebSocketManager.setAccionReportes(() ->{
            generarReportes();
        });
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
        double totalEfectivo = r.containsKey("totalEfectivo") ? ((Number) r.get("totalEfectivo")).doubleValue() : 0.0;
        double totalQR = r.containsKey("totalQR") ? ((Number) r.get("totalQR")).doubleValue() : 0.0;

        if (lblIngresoEfectivo != null && lblIngresoQR != null) {
            lblIngresoEfectivo.setText(String.format("Bs. %.2f", totalEfectivo));
            lblIngresoQR.setText(String.format("Bs. %.2f", totalQR));
        }

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
            listaFacturas.sort((f1, f2) -> {
                if (f1.getFecha() == null || f2.getFecha() == null) return 0;
                return f2.getFecha().compareTo(f1.getFecha());
            });
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
            listaEgresos.sort((e1, e2) ->{
                if(e1.getDate() == null && e2.getDate() == null) return 0;
                return e2.getDate().compareTo(e1.getDate());
            });
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
            DateTimeFormatter fmtAudit = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            masterLogs.sort((l1, l2) -> {
                if (l1.getFechaHora() == null || l2.getFechaHora() == null) return 0;
                try {
                    LocalDateTime dt1 = LocalDateTime.parse(l1.getFechaHora(), fmtAudit);
                    LocalDateTime dt2 = LocalDateTime.parse(l2.getFechaHora(), fmtAudit);
                    return dt2.compareTo(dt1); // dt2 vs dt1 invierte el orden para poner el último primero
                } catch (Exception e) {
                    return 0;
                }
            });
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
                                    new Thread(() ->{
                                        boolean exito = ApiClient.registrarEgresoEnServidor(concepto, monto);
                                        Platform.runLater(() ->{
                                            if(exito){
                                                String operador = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Admin";
                                                new Thread(() -> ApiClient.registrarAuditoriaEnServidor(operador,"Finanzas","Gasto manual","Egreso",monto,concepto)).start();
                                                generarReportes();
                                                AlertaPersonalizada.mostrarAlerta("Éxito", "El gasto se registro correctamente.", Alert.AlertType.INFORMATION);
                                            }else{
                                                AlertaPersonalizada.mostrarAlerta("Error", "No se pudo guardar el gasto.", Alert.AlertType.ERROR);
                                            }
                                        });
                                    }).start();

                                } catch (NumberFormatException e) {
                                    AlertaPersonalizada.mostrarAlerta("Error", "Debe ingresar un número válido.", Alert.AlertType.ERROR);
                                }
                            });
                });
    }

    private void verDetalleFacturaTicket(Factura seleccionada) {
        new Thread(() ->{
            if(!ApiClient.isServidorDisponible()){
                Platform.runLater(() ->AlertaPersonalizada.mostrarAlerta(
                        "Servidor No disponible",
                        "El desglose exacto de los productos de este ticket se encuentra en el servidor principal. Espere a que el motor arranque completamente o reconecte el sistema.",
                        Alert.AlertType.WARNING
                ));
                return;
            }
            try{
                Factura facDetalles = ApiClient.obtenerFacturaConDetalles(seleccionada.getNumeroFactura());
                Platform.runLater(() ->{
                    if(facDetalles == null || facDetalles.getDetalles().isEmpty()){
                        AlertaPersonalizada.mostrarAlerta("Ticket No Encontrado", "No se pudo obtener el detalle de la factura desde el servidor.", Alert.AlertType.ERROR);
                        return;
                    }
                    mostrarVisorDigitalDeFactura(facDetalles);
                });
            }catch(Exception e){
                Platform.runLater(() ->AlertaPersonalizada.mostrarAlerta("Error", "No se pudo cargar el Ticket.", Alert.AlertType.ERROR));
            }
        }).start();
    }

    private void mostrarVisorDigitalDeFactura(Factura fac) {
        Stage dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.initModality(Modality.APPLICATION_MODAL);

        // Contenedor raíz (fondo completamente transparente para mostrar solo la tarjeta)
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: transparent; -fx-padding: 20;");

        // Tarjeta principal (toda la UI vive aquí)
        VBox tarjeta = new VBox(15);
        tarjeta.setAlignment(Pos.TOP_CENTER);
        tarjeta.getStyleClass().add("tarjeta-dialogo");   // clase CSS

        // Cabecera
        Label lblIcono = new Label("🧾");
        lblIcono.getStyleClass().add("icono-titulo");
        Label lblTitulo = new Label("Detalle de Venta");
        lblTitulo.getStyleClass().add("titulo-dialogo");

        // Grid de información (ya no oscuro)
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(20); infoGrid.setVgap(10);
        infoGrid.getStyleClass().add("grid-info");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String fecha = fac.getFecha() != null ? fac.getFecha().format(fmt) : "N/A";

        Label l1 = new Label("N° Ticket:"); l1.getStyleClass().add("etiqueta");
        Label l2 = new Label(String.valueOf(fac.getNumeroTicket())); l2.getStyleClass().add("valor");
        Label l3 = new Label("Fecha:"); l3.getStyleClass().add("etiqueta");
        Label l4 = new Label(fecha); l4.getStyleClass().add("valor");
        Label l5 = new Label("Cliente:"); l5.getStyleClass().add("etiqueta");
        Label l6 = new Label(fac.getNombreCliente()); l6.getStyleClass().add("valor");
        Label l7 = new Label("Estado:"); l7.getStyleClass().add("etiqueta");

        infoGrid.add(l1, 0, 0); infoGrid.add(l2, 1, 0);
        infoGrid.add(l3, 0, 1); infoGrid.add(l4, 1, 1);
        infoGrid.add(l5, 0, 2); infoGrid.add(l6, 1, 2);
        infoGrid.add(l7, 0, 3);

        Label lblEst = new Label(fac.getEstado());
        lblEst.getStyleClass().add("valor-estado");
        if (fac.getEstado().equals("Anulada")) {
            lblEst.getStyleClass().add("estado-anulada");
        } else {
            lblEst.getStyleClass().add("estado-activa");
        }
        infoGrid.add(lblEst, 1, 3);

        // Tabla de productos
        TableView<DetalleFactura> tablaDetalles = new TableView<>();
        tablaDetalles.setPrefHeight(200);
        tablaDetalles.getStyleClass().add("tabla-productos");

        TableColumn<DetalleFactura, Integer> colCant = new TableColumn<>("Cant.");
        colCant.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colCant.setPrefWidth(50);

        TableColumn<DetalleFactura, String> colProd = new TableColumn<>("Producto");
        colProd.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProducto().getNombre()));
        colProd.setPrefWidth(210);

        TableColumn<DetalleFactura, Double> colSub = new TableColumn<>("Subtotal (Bs)");
        colSub.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        colSub.setPrefWidth(100);

        tablaDetalles.getColumns().addAll(colCant, colProd, colSub);
        tablaDetalles.setItems(FXCollections.observableArrayList(fac.getDetalles()));

        // Footer Total
        HBox boxTotal = new HBox(10);
        boxTotal.setAlignment(Pos.CENTER_RIGHT);
        Label lblTextoTotal = new Label("TOTAL COBRADO:");
        lblTextoTotal.getStyleClass().add("etiqueta-total");
        Label lblMontoTotal = new Label(String.format("Bs. %.2f", fac.getTotal()));
        lblMontoTotal.getStyleClass().add("monto-total");
        boxTotal.getChildren().addAll(lblTextoTotal, lblMontoTotal);

        // Botón Cerrar
        Button btnOk = new Button("Cerrar Visor");
        btnOk.getStyleClass().add("boton-cerrar");
        btnOk.setOnAction(e -> dialogStage.close());

        tarjeta.getChildren().addAll(lblIcono, lblTitulo, infoGrid, tablaDetalles, boxTotal, btnOk);
        root.getChildren().add(tarjeta);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(crearEstiloCremaModerno()); // método que genera el CSS en línea
        dialogStage.setScene(scene);

        scene.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) dialogStage.close(); });


        final double[] xOffset = {0}; final double[] yOffset = {0};
        root.setOnMousePressed(evt -> { xOffset[0] = evt.getSceneX(); yOffset[0] = evt.getSceneY(); });
        root.setOnMouseDragged(evt -> {
            dialogStage.setX(evt.getScreenX() - xOffset[0]);
            dialogStage.setY(evt.getScreenY() - yOffset[0]);
        });

        dialogStage.showAndWait();
    }
    private String crearEstiloCremaModerno() {
        String css = """
        /* === Tema crema moderno con bordes extra redondeados y texto oscuro === */
        .root {
            -fx-background-color: transparent;
        }

        .tarjeta-dialogo {
            -fx-background-color: #FDF6EC;
            -fx-background-radius: 30px;
            -fx-border-radius: 30px;
            -fx-border-color: #EED9C4;
            -fx-border-width: 1.5px;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0, 0, 6);
            -fx-padding: 25px;
        }

        .icono-titulo {
            -fx-font-size: 35px;
            -fx-padding: 0 0 5 0;
        }

        .titulo-dialogo {
            -fx-font-size: 22px;
            -fx-font-weight: bold;
            -fx-text-fill: #3A2E27;
            -fx-padding: 0 0 15 0;
        }

        /* Grid de información */
        .grid-info {
            -fx-background-color: #F8EDE0;
            -fx-background-radius: 24px;
            -fx-padding: 18px;
            -fx-hgap: 20px;
            -fx-vgap: 12px;
        }

        .etiqueta {
            -fx-text-fill: #5A5046;
            -fx-font-size: 13px;
        }

        .valor {
            -fx-text-fill: #1E1E1E;
            -fx-font-weight: bold;
            -fx-font-size: 13px;
        }

        .valor-estado {
            -fx-font-weight: bold;
            -fx-font-size: 13px;
        }

        .estado-anulada {
            -fx-text-fill: #C0392B;
        }

        .estado-activa {
            -fx-text-fill: #2C5F2D;
        }

        /* Tabla */
        .tabla-productos {
            -fx-background-color: #FDF6EC;
            -fx-table-cell-border-color: transparent;
            -fx-table-header-border-color: transparent;
            -fx-padding: 0;
            -fx-background-radius: 24px;
            -fx-border-radius: 24px;
            -fx-border-color: #EED9C4;
            -fx-border-width: 1px;
        }

        .tabla-productos .column-header-background {
            -fx-background-color: #EED9C4;
            -fx-background-radius: 24px 24px 0 0;
        }

        .tabla-productos .column-header {
            -fx-background-color: transparent;
            -fx-text-fill: #000000;          /* ← NEGRO PURO */
            -fx-font-weight: bold;
            -fx-font-size: 13px;
            -fx-padding: 10 5 10 5;
        }
        .tabla-productos .column-header .label {
            -fx-text-fill: #000000;
            -fx-font-weight: bold;
        }

        .tabla-productos .table-row-cell {
            -fx-background-color: #FDF6EC;
            -fx-text-fill: #1E1E1E;
            -fx-cell-size: 35px;
        }

        .tabla-productos .table-row-cell:odd {
            -fx-background-color: #F8EDE0;
            -fx-text-fill: #1E1E1E;
        }

        .tabla-productos .table-row-cell:selected {
            -fx-background-color: #D6B9A0;
            -fx-text-fill: white;
        }

        .tabla-productos .table-cell {
            -fx-border-color: transparent;
            -fx-padding: 0 8 0 8;
            -fx-text-fill: #1E1E1E;
        }

        /* Footer total */
        .etiqueta-total {
            -fx-font-size: 16px;
            -fx-font-weight: bold;
            -fx-text-fill: #1E1E1E;
        }

        .monto-total {
            -fx-font-size: 22px;
            -fx-font-weight: bold;
            -fx-text-fill: #8B5E3C;
        }

        /* Botón */
        .boton-cerrar {
            -fx-background-color: #EED9C4;
            -fx-text-fill: #1E1E1E;
            -fx-font-weight: bold;
            -fx-font-size: 14px;
            -fx-background-radius: 16px;
            -fx-padding: 10 30 10 30;
            -fx-cursor: hand;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);
        }

        .boton-cerrar:hover {
            -fx-background-color: #E1C7AE;
        }

        .boton-cerrar:pressed {
            -fx-background-color: #D4B49B;
        }
    """;

        try {
            return "data:text/css;base64," +
                    Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
        double liquidoNeto = Math.max(0, sueldoBase - totalAdelantos);
        double costoAnualProyectado = (sueldoBase * 12) + sueldoBase;

        StringBuilder sb = new StringBuilder();

        String nombreNegocio = (App.attizos != null && App.attizos.getNombre() != null)
                ? App.attizos.getNombre().toUpperCase() : "REPORTE OFICIAL";

        sb.append("🏢 ").append(nombreNegocio).append("\n");
        sb.append("=== REPORTE FINANCIERO Y LABORAL ===\n\n");

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

        sb.append("📉 HISTORIAL DE ADELANTOS (Filtro Actual)\n");
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
        Stage dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.initModality(Modality.APPLICATION_MODAL);

        // Usaremos el color verde (Information) de tu diseño
        String colorAcento = "#218c4e";

        javafx.scene.layout.VBox cajaDialogo = new javafx.scene.layout.VBox(15);
        cajaDialogo.setAlignment(javafx.geometry.Pos.CENTER);
        cajaDialogo.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20; -fx-border-color: " + colorAcento + "; -fx-border-width: 2; -fx-border-radius: 20; -fx-padding: 30; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0, 0, 5);");

        Label lblIcono = new Label("📋");
        lblIcono.setStyle("-fx-font-size: 40px; -fx-text-fill: " + colorAcento + ";");

        Label lblTitulo = new Label("Ficha Técnica: " + seleccionado.getNombre());
        lblTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #111111;");

        TextArea txtReporte = new TextArea(sb.toString());
        txtReporte.setEditable(false);
        txtReporte.setWrapText(true);
        txtReporte.setPrefWidth(550);
        txtReporte.setPrefHeight(350);
        // Se estiliza el TextArea para que combine con el fondo blanco redondeado
        txtReporte.setStyle("-fx-font-size: 14px; -fx-font-family: 'Segoe UI', Helvetica, Arial, sans-serif; -fx-control-inner-background: #F9F9F9; -fx-background-color: transparent; -fx-border-color: #DDDDDD; -fx-border-radius: 8; -fx-padding: 5;");

        Button btnOk = new Button("Cerrar Ficha");
        btnOk.setStyle("-fx-background-color: " + colorAcento + "; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 10; -fx-padding: 10 25; -fx-cursor: hand;");
        btnOk.setOnAction(e -> dialogStage.close());

        cajaDialogo.getChildren().addAll(lblIcono, lblTitulo, txtReporte, btnOk);

        javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane(cajaDialogo);
        root.setStyle("-fx-background-color: transparent; -fx-padding: 20;");

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialogStage.setScene(scene);

        // Movimiento de la ventana
        final double[] xOffset = {0};
        final double[] yOffset = {0};
        root.setOnMousePressed(eventClick -> {
            xOffset[0] = eventClick.getSceneX();
            yOffset[0] = eventClick.getSceneY();
        });
        root.setOnMouseDragged(eventDrag -> {
            dialogStage.setX(eventDrag.getScreenX() - xOffset[0]);
            dialogStage.setY(eventDrag.getScreenY() - yOffset[0]);
        });

        dialogStage.showAndWait();
    }
}