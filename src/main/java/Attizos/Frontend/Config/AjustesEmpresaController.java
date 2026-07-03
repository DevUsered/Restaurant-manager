package Attizos.Frontend.Config;

import Attizos.Backend.Api.ApiClient;
import Attizos.Backend.Attizos.App;
import Attizos.Frontend.AlertaPersonalizada;
import Attizos.Frontend.DialogoPersonalizado;
import Attizos.Frontend.ServicioNube;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class AjustesEmpresaController implements Initializable {
    @FXML private TextField txtNombreNegocio;
    @FXML private ImageView imgLogoActual;
    @FXML private Label lblRutaLogo;
    @FXML private TextField txtIpServidor;
    @FXML private CheckBox chkTieneCocina;

    private File nuevoLogoSeleccionado = null;
    private String urlLogoActual = "";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cargarDatosDesdeConfigApp();
    }

    private void cargarDatosDesdeConfigApp() {
        ConfigurationApp.cargarConfiguracion(); // Nos aseguramos de tener lo último en RAM

        txtNombreNegocio.setText(ConfigurationApp.getNombreRestaurante());
        txtIpServidor.setText(ConfigurationApp.getIpServidor());
        chkTieneCocina.setSelected(ConfigurationApp.isTieneCocina());

        urlLogoActual = ConfigurationApp.getRutaLogo();
        lblRutaLogo.setText(urlLogoActual.startsWith("http") ? "Sincronizado en Cloudinary" : "Logo local/defecto");

        try {
            if (urlLogoActual != null && !urlLogoActual.isEmpty() && !urlLogoActual.equals("default_logo.png")) {
                if (urlLogoActual.startsWith("http")) {
                    imgLogoActual.setImage(new Image(urlLogoActual, true));
                } else {
                    File imgFile = new File(urlLogoActual);
                    if (imgFile.exists()) {
                        imgLogoActual.setImage(new Image(imgFile.toURI().toString()));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("No se pudo cargar la vista previa del logo: " + e.getMessage());
        }
    }

    @FXML
    private void cambiarLogo(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Nuevo Logo para el Negocio");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Archivos de Imagen", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) txtNombreNegocio.getScene().getWindow();
        File seleccionado = fileChooser.showOpenDialog(stage);

        if (seleccionado != null) {
            nuevoLogoSeleccionado = seleccionado;
            lblRutaLogo.setText("Listo para subir a Cloudinary...");
            imgLogoActual.setImage(new Image(seleccionado.toURI().toString()));
        }
    }

    /**
     * Evalúa qué campos cambiaron exactamente y muestra un diálogo dinámico.
     */
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

        // 1. DETECTOR DINÁMICO DE CAMBIOS
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
        if (nuevoLogoSeleccionado != null) {
            cambios.append("• Logotipo: Nueva imagen lista para subir a Cloudinary\n");
            hayCambios = true;
        }

        // Si no tocó nada, evitamos guardar innecesariamente
        if (!hayCambios) {
            AlertaPersonalizada.mostrarAlerta("Sin Modificaciones", "No has realizado ningún cambio en la configuración.", Alert.AlertType.INFORMATION);
            return;
        }

        // 2. DIÁLOGO DINÁMICO CON TU TARJETA DORADA
        Optional<ButtonType> respuesta = DialogoPersonalizado.mostrarDialogoConfirmacion(
                "Confirmar Modificaciones",
                "¿Estás seguro de aplicar los siguientes cambios?",
                cambios.toString().trim()
        );

        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            ejecutarGuardadoEnNubeYLocal(nuevoNombre, nuevaIp, nuevaCocina);
        }
    }

    private void ejecutarGuardadoEnNubeYLocal(String nuevoNombre, String nuevaIp, boolean nuevaCocina) {

        // ==========================================
        // 🛡️ VERIFICACIÓN DE RED DESDE EL DASHBOARD
        // ==========================================
        // Si cambió la IP del servidor, verificamos que esa nueva IP realmente exista y esté respondiendo
        if (!nuevaIp.equals(ConfigurationApp.getIpServidor()) && !nuevaIp.equalsIgnoreCase("localhost")) {
            System.out.println("🔄 Verificando nueva IP del servidor: " + nuevaIp);
            if (!ValidadorCredenciales.probarServidorCentral(nuevaIp)) {
                AlertaPersonalizada.mostrarAlerta(
                        "IP Inválida o Inaccesible",
                        "La nueva dirección IP (" + nuevaIp + ") no responde.\nNo se puede guardar esta IP porque la terminal perdería conexión con la base de datos.",
                        Alert.AlertType.ERROR
                );
                return; // 🚫 PROHIBIDO GUARDAR CAMBIOS
            }
        }

        String rutaLogoFinal = urlLogoActual;

        if (nuevoLogoSeleccionado != null) {
            try {
                lblRutaLogo.setText("Subiendo a la nube...");
                String urlSubida = ServicioNube.subirImagen(nuevoLogoSeleccionado);
                if (urlSubida != null && !urlSubida.isEmpty()) {
                    rutaLogoFinal = urlSubida;
                } else {
                    AlertaPersonalizada.mostrarAlerta("Advertencia de Red", "No se pudo subir a Cloudinary. Se mantendrá el logo anterior.", Alert.AlertType.WARNING);
                }
            } catch (Exception e) {
                System.err.println("Error subiendo logo: " + e.getMessage());
                AlertaPersonalizada.mostrarAlerta("Error de Conexión", "Fallo al conectar con Cloudinary. Se mantendrá el logo actual.", Alert.AlertType.ERROR);
                return;
            }
        }

        // Guardar configuración usando nuestra clase estandarizada solo tras pasar las pruebas
        ConfigurationApp.guardarConfiguracionNueva(nuevaCocina, nuevaIp, nuevoNombre, rutaLogoFinal);

        if (App.attizos != null) {
            App.setNombre(nuevoNombre);
        }
        ApiClient.configurarIpServidor(nuevaIp);
        App.cargarCacheLogo();

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