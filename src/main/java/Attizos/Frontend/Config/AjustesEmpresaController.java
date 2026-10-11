package Attizos.Frontend.Config;

import Attizos.Backend.Api.ApiClient;
import Attizos.Backend.Attizos.App;
import Attizos.Frontend.AlertaPersonalizada;
import Attizos.Frontend.DialogoPersonalizado;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class AjustesEmpresaController implements Initializable {
    @FXML private TextField txtNombreNegocio;
    @FXML private TextField txtIpServidor;

    @FXML private CheckBox chkTieneCocina;
    @FXML private CheckBox chkImpresion;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cargarDatosDesdeConfigApp();
    }

    private void cargarDatosDesdeConfigApp() {
        ConfigurationApp.cargarConfiguracion(); // Nos aseguramos de tener lo último en RAM

        txtNombreNegocio.setText(ConfigurationApp.getNombreRestaurante());
        txtIpServidor.setText(ConfigurationApp.getIpServidor());

        // Cargamos los estados booleanos
        chkTieneCocina.setSelected(ConfigurationApp.isTieneCocina());
        chkImpresion.setSelected(ConfigurationApp.isImpresionActivada());

        // Disparamos la actualización visual de textos
        actualizarTextosChecks(null);
    }

    // ✨ EVENTO DINÁMICO QUE CAMBIA LOS TEXTOS AL HACER CLIC
    @FXML
    private void actualizarTextosChecks(ActionEvent event) {
        if (chkTieneCocina.isSelected()) {
            chkTieneCocina.setText("👨‍🍳 Módulo de Cocina: ACTIVADO");
        } else {
            chkTieneCocina.setText("👨‍🍳 Módulo de Cocina: DESACTIVADO");
        }

        if (chkImpresion.isSelected()) {
            chkImpresion.setText("🖨️ Impresión de Tickets: ACTIVADA");
        } else {
            chkImpresion.setText("🖨️ Impresión de Tickets: DESACTIVADA");
        }
    }

    @FXML
    private void confirmarYGuardar(ActionEvent event) {
        String nuevoNombre = txtNombreNegocio.getText().trim();
        if (nuevoNombre.isEmpty()) {
            AlertaPersonalizada.mostrarAlerta("Campo Obligatorio", "El nombre del restaurante no puede quedar vacío.", Alert.AlertType.WARNING);
            return;
        }

        String nuevaIp = txtIpServidor.getText().trim();
        if (nuevaIp.isEmpty()) nuevaIp = "localhost";

        boolean nuevaCocina = chkTieneCocina.isSelected();
        boolean nuevaImpresion = chkImpresion.isSelected();

        StringBuilder cambios = new StringBuilder();
        boolean hayCambios = false;

        if (!nuevoNombre.equals(ConfigurationApp.getNombreRestaurante())) {
            cambios.append("• Nombre: ").append(ConfigurationApp.getNombreRestaurante()).append(" ➔ ").append(nuevoNombre).append("\n");
            hayCambios = true;
        }
        if (!nuevaIp.equals(ConfigurationApp.getIpServidor())) {
            cambios.append("• IP Servidor: ").append(ConfigurationApp.getIpServidor()).append(" ➔ ").append(nuevaIp).append("\n");
            hayCambios = true;
        }
        if (nuevaCocina != ConfigurationApp.isTieneCocina()) {
            cambios.append("• Módulo Cocina: ").append(nuevaCocina ? "Habilitado" : "Deshabilitado").append("\n");
            hayCambios = true;
        }
        if (nuevaImpresion != ConfigurationApp.isImpresionActivada()) {
            cambios.append("• Impresión Tickets: ").append(nuevaImpresion ? "Habilitada" : "Deshabilitada").append("\n");
            hayCambios = true;
        }

        // Si no tocó nada, evitamos guardar innecesariamente
        if (!hayCambios) {
            AlertaPersonalizada.mostrarAlerta("Sin Modificaciones", "No has realizado ningún cambio en la configuración.", Alert.AlertType.INFORMATION);
            return;
        }

        // DIÁLOGO DINÁMICO
        Optional<ButtonType> respuesta = DialogoPersonalizado.mostrarDialogoConfirmacion(
                "Confirmar Modificaciones",
                "¿Estás seguro de aplicar los siguientes cambios?",
                cambios.toString().trim()
        );

        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            ejecutarGuardadoEnNubeYLocal(nuevoNombre, nuevaIp, nuevaCocina, nuevaImpresion);
        }
    }

    private void ejecutarGuardadoEnNubeYLocal(String nuevoNombre, String nuevaIp, boolean nuevaCocina, boolean nuevaImpresion) {

        if (!nuevaIp.equals(ConfigurationApp.getIpServidor()) && !nuevaIp.equalsIgnoreCase("localhost")) {
            System.out.println("🔄 Verificando nueva IP del servidor: " + nuevaIp);
            if (!ValidadorCredenciales.probarServidorCentral(nuevaIp)) {
                AlertaPersonalizada.mostrarAlerta(
                        "IP Inválida o Inaccesible",
                        "La nueva dirección IP (" + nuevaIp + ") no responde.\nNo se puede guardar esta IP porque la terminal perdería conexión con la base de datos.",
                        Alert.AlertType.ERROR
                );
                return;
            }
        }

        ConfigurationApp.guardarConfiguracionNueva(nuevaCocina, nuevaImpresion, nuevaIp, nuevoNombre);

        App.setNombre(nuevoNombre);
        if (App.attizos != null) {
            App.getAttizos().setNombre(nuevoNombre);
        }
        ApiClient.configurarIpServidor(nuevaIp);

        AlertaPersonalizada.mostrarAlerta(
                "Ajustes Guardados",
                "La configuración del sistema se actualizó correctamente.",
                Alert.AlertType.INFORMATION
        );

        cerrarVentana(null);
    }

    @FXML
    private void cerrarVentana(ActionEvent event) {
        Stage stage = (Stage) txtNombreNegocio.getScene().getWindow();
        stage.close();
    }
}