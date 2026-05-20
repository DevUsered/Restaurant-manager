package Attizos.Frontend;

import Attizos.Backend.Attizos.*;
import Attizos.Backend.Listas.NodoDE;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

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

    // Listas observables para las tablas
    private ObservableList<Factura> listaFacturas = FXCollections.observableArrayList();
    private ObservableList<Egreso> listaEgresos = FXCollections.observableArrayList();
    private ObservableList<Empleado> listaEmpleados = FXCollections.observableArrayList();
    private ObservableList<RegistroAuditoria> listaLogs = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTablas();
        configurarBotonDobleClic();
        generarReportes();
    }

    private void configurarTablas() {
        // --- 1. Tabla Ventas ---
        colNroFac.setCellValueFactory(new PropertyValueFactory<>("numeroFactura"));
        colClienteFac.setCellValueFactory(new PropertyValueFactory<>("nombreCliente"));
        colTotalFac.setCellValueFactory(new PropertyValueFactory<>("total"));
        colFechaFac.setCellValueFactory(cellData -> {
            LocalDateTime fecha = cellData.getValue().getFecha();
            return new SimpleStringProperty(fecha != null ? fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A");
        });
        tablaVentas.setItems(listaFacturas);

        // --- 2. Tabla Egresos ---
        // Asumiendo que Egreso tiene getFecha(), getMotivo/getConcepto(), getMonto()
        colConceptoEgreso.setCellValueFactory(new PropertyValueFactory<>("concepto"));
        colMontoEgreso.setCellValueFactory(new PropertyValueFactory<>("monto"));
        colFechaEgreso.setCellValueFactory(cellData -> {
            LocalDate fecha = cellData.getValue().getDate();
            return new SimpleStringProperty(fecha != null ? fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        });
        tablaEgresos.setItems(listaEgresos);

        // --- 3. Tabla RRHH ---
        colEmpId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEmpNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colEmpCargo.setCellValueFactory(new PropertyValueFactory<>("cargo"));
        colEmpSueldo.setCellValueFactory(new PropertyValueFactory<>("sueldo"));
        tablaEmpleadosReporte.setItems(listaEmpleados);

        // --- 4. Tabla Trazabilidad (Seguidor) ---
        colFechaLog.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));
        colUsuarioLog.setCellValueFactory(new PropertyValueFactory<>("operador"));
        colTipoItemLog.setCellValueFactory(new PropertyValueFactory<>("tipoArea"));
        colItemLog.setCellValueFactory(new PropertyValueFactory<>("nombreItem"));
        colAccionLog.setCellValueFactory(new PropertyValueFactory<>("accion"));
        colCantidadLog.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colJustificacionLog.setCellValueFactory(new PropertyValueFactory<>("motivo"));
        tablaLogSeguimiento.setItems(listaLogs);
    }

    private void configurarBotonDobleClic() {
        // En lugar de un botón "Ver Factura", usamos doble clic en la fila
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

    private void generarReportes() {
        double ingresosTotales = 0.0;
        double egresosTotales = 0.0;
        int contadorVentas = 0;

        // 1. CARGAR VENTAS E INGRESOS
        listaFacturas.clear();
        Map<String, Double> ventasPorDia = new TreeMap<>(); // Para el gráfico

        if (App.attizos != null && App.attizos.getHistorialVentas() != null) {
            NodoDE<Factura> actual = App.attizos.getHistorialVentas().getCabeza();
            while (actual != null) {
                Factura f = actual.getDato();
                listaFacturas.add(f);
                ingresosTotales += f.getTotal();
                contadorVentas++;

                // Agrupar para el gráfico
                if (f.getFecha() != null) {
                    String dia = f.getFecha().format(DateTimeFormatter.ofPattern("dd/MM"));
                    ventasPorDia.put(dia, ventasPorDia.getOrDefault(dia, 0.0) + f.getTotal());
                }
                actual = actual.getSiguiente();
            }
        }

        // 2. CARGAR EGRESOS
        listaEgresos.clear();
        if (App.attizos != null && App.attizos.getExpenseHistory() != null) {
            NodoDE<Egreso> actual = App.attizos.getExpenseHistory().getCabeza();
            while (actual != null) {
                Egreso e = actual.getDato();
                listaEgresos.add(e);
                egresosTotales += e.getTotalAmount();
                actual = actual.getSiguiente();
            }
        }

        // 3. CARGAR EMPLEADOS Y PLANILLA
        listaEmpleados.clear();
        double sumaSueldos = 0.0;
        if (App.attizos != null && App.attizos.getEmpleados() != null) {
            for (Empleado emp : App.attizos.getEmpleados()) {
                listaEmpleados.add(emp);
                sumaSueldos += emp.getSueldo();
            }
        }

        // Sumamos los sueldos a los egresos totales (Gasto fijo)
        egresosTotales += sumaSueldos;

        // 4. ACTUALIZAR KPIS (FINANZAS)
        double balance = ingresosTotales - egresosTotales;
        double ticketPromedio = (contadorVentas > 0) ? (ingresosTotales / contadorVentas) : 0.0;

        lblIngresos.setText("Bs. " + String.format("%.2f", ingresosTotales));
        lblEgresos.setText("Bs. " + String.format("%.2f", egresosTotales));
        lblBalance.setText("Bs. " + String.format("%.2f", balance));
        lblTicketProm.setText("Bs. " + String.format("%.2f", ticketPromedio));
        lblTotalSueldos.setText("Bs. " + String.format("%.2f", sumaSueldos));

        // Colorear el balance (Verde si hay ganancia, Rojo si hay pérdida)
        if (balance >= 0) {
            lblBalance.setStyle("-fx-text-fill: #218c4e;"); // Verde
        } else {
            lblBalance.setStyle("-fx-text-fill: #c0392b;"); // Rojo
        }

        // 5. ACTUALIZAR GRÁFICO
        chartVentas.getData().clear();
        XYChart.Series<String, Number> serieVentas = new XYChart.Series<>();
        serieVentas.setName("Ingresos por Día");
        for (Map.Entry<String, Double> entry : ventasPorDia.entrySet()) {
            serieVentas.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        chartVentas.getData().add(serieVentas);
    }

    // --- MÉTODO ANTIGUO RESCATADO Y MEJORADO ---
    @FXML
    void verDetalleEmpleado(ActionEvent event) {
        Empleado seleccionado = tablaEmpleadosReporte.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarAlerta("Selección requerida", "Por favor, seleccione un empleado de la tabla para ver su detalle.");
            return;
        }

        String detalle = "IDENTIFICACIÓN (CI): " + seleccionado.getId() + "\n"
                + "NOMBRE COMPLETO: " + seleccionado.getNombre() + "\n"
                + "CARGO ASIGNADO: " + seleccionado.getCargo() + "\n"
                + "SUELDO BASE: Bs. " + String.format("%.2f", seleccionado.getSueldo()) + "\n\n";

        if (seleccionado instanceof Usuario) {
            detalle += "--- DATOS DEL SISTEMA ---\n"
                    + "Usuario: " + ((Usuario) seleccionado).getUsername() + "\n"
                    + "Rol en Sistema: Acceso Permitido\n";
        } else {
            detalle += "--- DATOS DEL SISTEMA ---\n"
                    + "Sin acceso a la caja o computadora.\n";
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Ficha Técnica del Empleado");
        alert.setHeaderText("Detalles de Planilla: " + seleccionado.getNombre());
        alert.setContentText(detalle);
        aplicarEstiloClaro(alert);
        alert.showAndWait();
    }

    // --- MÉTODO ANTIGUO RESCATADO (AHORA POR DOBLE CLIC) ---
    private void verDetalleFacturaTicket(Factura seleccionada) {
        String textoTicket = seleccionada.generarTicket(); // Método de tu clase Factura

        Alert alertaTicket = new Alert(Alert.AlertType.INFORMATION);
        alertaTicket.setTitle("Detalle de Venta");
        alertaTicket.setHeaderText("Copia de Ticket - Factura N° " + seleccionada.getNumeroFactura());

        // Formato letra de cajero térmica
        TextArea areaTicket = new TextArea(textoTicket);
        areaTicket.setEditable(false);
        areaTicket.setFont(javafx.scene.text.Font.font("Monospaced", 14));
        areaTicket.setPrefSize(340, 500);

        alertaTicket.getDialogPane().setContent(areaTicket);
        alertaTicket.showAndWait();
    }

    // --- ESTILOS VISUALES PARA LAS ALERTAS (Combina con la estética Crema/Negro) ---
    private void aplicarEstiloClaro(Dialog<?> dialog) {
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #FDF6E3; -fx-border-color: #111111; -fx-border-width: 2px;");
        dialogPane.lookupAll(".label").forEach(node -> ((Label) node).setStyle("-fx-text-fill: #111111; -fx-font-weight: bold;"));
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        aplicarEstiloClaro(alert);
        alert.showAndWait();
    }

    // =========================================================================
    // CLASE INTERNA PARA LA TABLA DE AUDITORÍA / TRAZABILIDAD (EL SEGUIDOR)
    // =========================================================================
    // Esta clase permite que tu tabla visual funcione ya mismo.
    // Te avisaré cuando necesitemos vincularla a tu clase Restaurante / Inventario.
    public static class RegistroAuditoria {
        private String fechaHora;
        private String operador;
        private String tipoArea;
        private String nombreItem;
        private String accion;
        private double cantidad;
        private String motivo;

        public RegistroAuditoria(String fechaHora, String operador, String tipoArea, String nombreItem, String accion, double cantidad, String motivo) {
            this.fechaHora = fechaHora;
            this.operador = operador;
            this.tipoArea = tipoArea;
            this.nombreItem = nombreItem;
            this.accion = accion;
            this.cantidad = cantidad;
            this.motivo = motivo;
        }

        public String getFechaHora() { return fechaHora; }
        public String getOperador() { return operador; }
        public String getTipoArea() { return tipoArea; }
        public String getNombreItem() { return nombreItem; }
        public String getAccion() { return accion; }
        public double getCantidad() { return cantidad; }
        public String getMotivo() { return motivo; }
    }
}