package Attizos.Frontend;

import Attizos.Backend.Attizos.App;
import Attizos.Backend.Attizos.Reserva;
import Attizos.Backend.Listas.NodoDE;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ReservasController {

    @FXML private TableView<Reserva> tablaReservas;
    @FXML private TableColumn<Reserva, String> colId, colCliente, colTelefono, colEstado, colObservaciones, colFechaHora;
    @FXML private TableColumn<Reserva, Integer> colPersonas;

    @FXML private TextField txtCliente, txtTelefono, txtPersonas;
    @FXML private TextArea txtObservaciones;
    @FXML private DatePicker dpFecha;
    @FXML private ComboBox<String> cmbHora;

    private ObservableList<Reserva> listaVisible;

    @FXML
    public void initialize() {
        configurarColumnas();
        generarSelectorDeHoras();
        cargarReservas();

        cargarReservas();
    }
    private void configurarColumnas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("nombreCliente"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colPersonas.setCellValueFactory(new PropertyValueFactory<>("cantidadPersonas"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colObservaciones.setCellValueFactory(new PropertyValueFactory<>("observaciones"));

        colFechaHora.setCellValueFactory(cellData -> {
            LocalDateTime fecha = cellData.getValue().getFecha();
            return new SimpleStringProperty(fecha.format(DateTimeFormatter.ofPattern("dd/MM HH:mm")));
        });

        listaVisible = FXCollections.observableArrayList();
        tablaReservas.setItems(listaVisible);
    }
    private void generarSelectorDeHoras() {
        ObservableList<String> opcionesHoras = FXCollections.observableArrayList();
        LocalTime tiempo = LocalTime.of(12, 0);

        while (!tiempo.equals(LocalTime.MIN)) {
            opcionesHoras.add(tiempo.format(DateTimeFormatter.ofPattern("HH:mm")));
            tiempo = tiempo.plusMinutes(15);
            if (tiempo.equals(LocalTime.of(0, 0))) break;
        }
        cmbHora.setItems(opcionesHoras);
    }

    private void cargarReservas() {
        listaVisible.clear();
        LocalDateTime ahora = LocalDateTime.now();

        NodoDE<Reserva> actual = App.attizos.getReservas().getCabeza();

        while (actual != null) {
            Reserva r = actual.getDato();
            NodoDE<Reserva> siguiente = actual.getSiguiente();

            // REGLA 1: Borrar si pasó la tolerancia de 15 min y no fue atendida
            if (r.toleranciaExcedida() && r.getEstado().equalsIgnoreCase("Pendiente")) {
                App.attizos.getReservas().eliminarPorValor(r); // Borramos físicamente del backend
            }
            else if (r.getEstado().equalsIgnoreCase("Pendiente")) {
                listaVisible.add(r);
            }
            actual = siguiente;
        }
        tablaReservas.refresh();
    }

    @FXML
    void guardarReserva(ActionEvent event) {
        try {
            String cliente = txtCliente.getText().trim();
            String telf = txtTelefono.getText().trim();
            String obs = txtObservaciones.getText().trim();
            LocalDate f = dpFecha.getValue();
            String h = cmbHora.getValue();

            if (cliente.isEmpty() || f == null || h == null) {
                mostrarAlerta("Error", "Datos básicos faltantes.");
                return;
            }

            LocalDateTime fechaHora = LocalDateTime.of(f, LocalTime.parse(h));
            if(fechaHora.isBefore(LocalDateTime.now())){
                mostrarAlerta("Error","No se puede reservar en el pasado");
            }
            int pax = Integer.parseInt(txtPersonas.getText());

            String id = App.attizos.generarIdReserva(fechaHora);
            Reserva nueva = new Reserva(id, cliente, telf, pax, fechaHora, obs);


            App.attizos.agregarReserva(nueva);
            cargarReservas();
            limpiarFormulario();

        } catch (Exception e) {
            mostrarAlerta("Error", "Verifique que 'Personas' y 'Mesa' sean números.");
        }
    }
    @FXML
    void finalizarReservaSeleccionada(ActionEvent event) {
        Reserva sel = tablaReservas.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        sel.setEstado("Atendida");

        mostrarExito("Éxito", "Reserva procesada. Desaparecerá de la lista de pendientes.");
        cargarReservas();
    }
    @FXML
    void cancelarReservaSeleccionada(ActionEvent event) {
        Reserva sel = tablaReservas.getSelectionModel().getSelectedItem();
        if (sel != null) {
            sel.setNumeroMesa(0);
            App.attizos.getReservas().eliminarPorValor(sel);
            cargarReservas();
        }
    }

    private void limpiarFormulario() {
        txtCliente.clear();
        txtTelefono.clear();
        txtPersonas.clear();
        txtObservaciones.clear();
        dpFecha.setValue(null);
        cmbHora.getSelectionModel().clearSelection();
    }

    private void mostrarAlerta(String t, String m) {
        AlertaPersonalizada.mostrarAlerta(t, m, Alert.AlertType.WARNING);
    }
    private void mostrarExito(String t, String m) {
        AlertaPersonalizada.mostrarAlerta(t,m, Alert.AlertType.INFORMATION);
    }
}