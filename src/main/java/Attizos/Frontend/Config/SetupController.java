package Attizos.Frontend.Config;

import Attizos.Backend.Attizos.App;
import Attizos.Frontend.AlertaPersonalizada;
import Attizos.Frontend.ServicioNube; // <-- Import para subir a Cloudinary
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.ResourceBundle;

public class SetupController implements Initializable {

    @FXML private RadioButton rbServidorPrincipal;
    @FXML private RadioButton rbTerminalSucursal;
    @FXML private ToggleGroup tgModoOperacion;

    @FXML private TextField txtNombreNegocio;
    @FXML private HBox boxLogo;
    @FXML private Button btnSeleccionarLogo;
    @FXML private Label lblRutaLogo;
    @FXML private ImageView imgLogoPreview;

    @FXML private TextField txtDbUrl;
    @FXML private TextField txtDbUser;
    @FXML private PasswordField txtDbPassword;

    // Contenedor de la sección 4 para ocultarlo entero en Sucursal
    @FXML private VBox boxModuloCocina;
    @FXML private TextField txtCloudinaryUrl;
    @FXML private CheckBox chkTieneCocina;
    @FXML private Button btnGuardar;

    private final String RUTA_BASE = System.getenv("APPDATA") + File.separator + "Attizos";
    private final String RUTA_CONFIG = RUTA_BASE + File.separator + "configuracion.properties";
    private final String RUTA_IMAGENES = RUTA_BASE + File.separator + "images";

    private File archivoLogoTemporal = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarListeners();
    }

    private void configurarListeners() {
        tgModoOperacion.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            boolean esSucursal = rbTerminalSucursal.isSelected();

            // En Sucursal ocultamos Branding, Logo y todo el módulo de Cocina/Cloudinary
            txtNombreNegocio.setVisible(!esSucursal);
            txtNombreNegocio.setManaged(!esSucursal);
            boxLogo.setVisible(!esSucursal);
            boxLogo.setManaged(!esSucursal);

            if (boxModuloCocina != null) {
                boxModuloCocina.setVisible(!esSucursal);
                boxModuloCocina.setManaged(!esSucursal);
            } else {
                txtCloudinaryUrl.setVisible(!esSucursal);
                txtCloudinaryUrl.setManaged(!esSucursal);
                chkTieneCocina.setVisible(!esSucursal);
                chkTieneCocina.setManaged(!esSucursal);
            }

            if (esSucursal) {
                txtDbUrl.setPromptText("IP de la Base de Datos Central (ej. 192.168.1.100)");
            } else {
                txtDbUrl.setPromptText("URL PostgreSQL (ej. localhost:5432)");
            }
        });
    }

    @FXML
    private void seleccionarLogo(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Logo del Negocio");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Archivos de Imagen", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) btnSeleccionarLogo.getScene().getWindow();
        File seleccionado = fileChooser.showOpenDialog(stage);

        if (seleccionado != null) {
            archivoLogoTemporal = seleccionado;
            lblRutaLogo.setText(seleccionado.getName());
            imgLogoPreview.setImage(new Image(seleccionado.toURI().toString()));
        }
    }

    @FXML
    private void guardarConfiguracion(ActionEvent event) {
        if (!validarCampos()) {
            AlertaPersonalizada.mostrarAlerta("Campos Incompletos", "Por favor, completa todos los campos requeridos para continuar.", Alert.AlertType.WARNING);
            return;
        }

        // ==========================================
        // 🛡️ VERIFICACIÓN ESTRICTA EN VIVO
        // ==========================================
        if (rbServidorPrincipal.isSelected()) {
            // A. Prueba REAL en PostgreSQL
            String ipPostgres = txtDbUrl.getText().trim();
            String user = txtDbUser.getText().trim();
            String pass = txtDbPassword.getText().trim();

            if (!ValidadorCredenciales.probarPostgreSQLReal(ipPostgres, user, pass)) {
                AlertaPersonalizada.mostrarAlerta(
                        "Credenciales Incorrectas (BD)",
                        "¡No se pudo iniciar sesión en PostgreSQL!\nVerifica que la base de datos 'attizos_db' esté creada y que el usuario y la contraseña sean exactamente los correctos.\n\nEl sistema no guardará cambios hasta que sean válidos.",
                        Alert.AlertType.ERROR
                );
                return; // 🚫 PROHIBIDO GUARDAR
            }

            // B. Prueba REAL en Cloudinary
            String urlCloudinary = txtCloudinaryUrl.getText().trim();
            if (!urlCloudinary.isEmpty() && !ValidadorCredenciales.probarCloudinaryReal(urlCloudinary)) {
                AlertaPersonalizada.mostrarAlerta(
                        "Credenciales Incorrectas (Cloudinary)",
                        "¡Los servidores de Cloudinary rechazaron las credenciales!\nRevisa que el API Key, API Secret y el Cloud Name sean correctos.\n\nNo se guardará la configuración.",
                        Alert.AlertType.ERROR
                );
                return;
            }
        } else {
            String ipCentral = txtDbUrl.getText().trim();
            if (!ValidadorCredenciales.probarServidorCentral(ipCentral)) {
                AlertaPersonalizada.mostrarAlerta(
                        "Servidor No Encontrado",
                        "No existe ningún servidor Attizos encendido en la IP: " + ipCentral + "\nVerifica que la máquina principal esté encendida y conectada a la red WiFi/LAN.",
                        Alert.AlertType.ERROR
                );
                return;
            }
        }
        Properties propsFrontend = new Properties();
        propsFrontend.setProperty("app.modo", rbServidorPrincipal.isSelected() ? "SERVIDOR" : "SUCURSAL");
        propsFrontend.setProperty("app.modulo.cocina", String.valueOf(chkTieneCocina.isSelected()));

        if(rbServidorPrincipal.isSelected()){
            propsFrontend.setProperty("app.negocio.nombre", txtNombreNegocio.getText().trim());

            if (archivoLogoTemporal != null) {
                try {
                    lblRutaLogo.setText("Subiendo a la nube...");
                    String urlCloudinary = ServicioNube.subirImagen(archivoLogoTemporal);
                    if (urlCloudinary != null && !urlCloudinary.isEmpty()) {
                        propsFrontend.setProperty("app.negocio.logo", urlCloudinary);
                    } else {
                        String rutaLocal = guardarLogoFisicamente(archivoLogoTemporal);
                        if (rutaLocal != null) propsFrontend.setProperty("app.negocio.logo", rutaLocal);
                    }
                } catch (Exception e) {
                    String rutaLocal = guardarLogoFisicamente(archivoLogoTemporal);
                    if (rutaLocal != null) propsFrontend.setProperty("app.negocio.logo", rutaLocal);
                }
            }
            propsFrontend.setProperty("app.servidor.ip", "localhost");
        } else {
            propsFrontend.setProperty("app.servidor.ip", txtDbUrl.getText().trim());
        }

        Properties propsBackend = new Properties();
        if (rbServidorPrincipal.isSelected()) {
            propsBackend.setProperty("server.port", "8080");
            propsBackend.setProperty("spring.datasource.url", "jdbc:postgresql://" + txtDbUrl.getText().trim() + "/attizos_db");
            propsBackend.setProperty("spring.datasource.username", txtDbUser.getText().trim());
            propsBackend.setProperty("spring.datasource.password", txtDbPassword.getText().trim());
            propsBackend.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
            propsBackend.setProperty("spring.jpa.hibernate.ddl-auto", "none");
            propsBackend.setProperty("spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            propsBackend.setProperty("spring.jpa.show-sql", "true");

            String cloudinaryInput = txtCloudinaryUrl.getText().trim();
            desglosarCloudinary(cloudinaryInput, propsBackend);

            propsBackend.setProperty("gemini.api-key", "AQ.Ab8RN6IIu1a1FViogSvXftSrRPmq1_gCHFvGjyXs3fqf_dldKw");
        }

        guardarArchivos(propsFrontend, propsBackend);
    }
    private boolean probarConexionPostgreSQL(String url, String user, String pass) {
        String jdbcUrl = "jdbc:postgresql://" + url + "/attizos_db";
        System.out.println("🔄 Probando credenciales en: " + jdbcUrl);
        try {
            // Seteamos 3 segundos de timeout para que no congele la pantalla si la IP no existe
            java.sql.DriverManager.setLoginTimeout(3);
            try (java.sql.Connection conn = java.sql.DriverManager.getConnection(jdbcUrl, user, pass)) {
                return conn.isValid(3);
            }
        } catch (Exception e) {
            System.err.println("❌ Falló la prueba de base de datos: " + e.getMessage());
            return false;
        }
    }

    /**
     * Valida que la URL de Cloudinary tenga la sintaxis correcta.
     */
    private boolean validarFormatoCloudinary(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        return url.trim().matches("^cloudinary://[^:]+:[^@]+@[a-zA-Z0-9_-]+$");
    }

    /**
     * Verifica mediante un Socket si el servidor principal está vivo en el puerto 8080.
     */
    private boolean probarConexionSucursal(String ipCentral) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(ipCentral, 8080), 2500); // 2.5 seg timeout
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void desglosarCloudinary(String url, Properties propsBackend) {
        try {
            if (url != null && url.startsWith("cloudinary://")) {
                String base = url.substring(13);
                String[] partesArroba = base.split("@");
                String cloudName = partesArroba[1];
                String[] credenciales = partesArroba[0].split(":");

                propsBackend.setProperty("cloudinary.api-key", credenciales[0]);
                propsBackend.setProperty("cloudinary.api-secret", credenciales[1]);
                propsBackend.setProperty("cloudinary.cloud-name", cloudName);
            } else {
                propsBackend.setProperty("cloudinary.api-key", "");
                propsBackend.setProperty("cloudinary.api-secret", "");
                propsBackend.setProperty("cloudinary.cloud-name", "");
            }
        } catch (Exception e) {
            System.err.println("Error procesando URL de Cloudinary: " + e.getMessage());
        }
    }

    private void guardarArchivos(Properties propsF, Properties propsB){
        File dirBase = new File(RUTA_BASE);
        File dirBackend = new File(RUTA_BASE + File.separator + "backend");

        if(!dirBase.exists()) dirBase.mkdirs();
        if(!dirBackend.exists() && rbServidorPrincipal.isSelected()) dirBackend.mkdirs();

        try{
            try(FileOutputStream outFront = new FileOutputStream(RUTA_CONFIG)){
                propsF.store(outFront, "Configuración de POS - Frontend");
            }
            if(rbServidorPrincipal.isSelected()){
                String rutaBackendProps = dirBackend.getAbsolutePath() + File.separator + "application.properties";
                try(FileOutputStream outBack = new FileOutputStream(rutaBackendProps)){
                    propsB.store(outBack, "Configuración de POS - Backend");
                }
            }

            // 1. ENCENDEMOS EL SERVIDOR SPRING BOOT PRIMERO
            App.iniciarBackend();

            // 2. BLOQUEAMOS EL BOTÓN Y DAMOS FEEDBACK CLARO
            btnGuardar.setText("⏳ Iniciando Servidor y Base de Datos...");
            btnGuardar.setDisable(true);

            // 3. HILO DE ESPERA ACTIVA: Ahora esperamos hasta 90 SEGUNDOS (1 minuto y medio)
            new Thread(() -> {
                    int intentos = 0;
                    boolean servidorListo = false;

                    // 90 intentos = 90 segundos máximos de espera
                    while (intentos < 90 && !servidorListo) {
                        try {
                            Thread.sleep(1000); // Esperamos 1 segundo entre cada toque de puerta
                        } catch (InterruptedException e) {}

                        // Consultamos si el puerto 8080 ya respondió
                        servidorListo = Attizos.Backend.Api.ApiClient.isServidorDisponible();
                        intentos++;
                        // Cada 5 segundos actualizamos el texto del botón para calmar al usuario
                        int finalIntentos = intentos;
                        javafx.application.Platform.runLater(() -> {
                            if (finalIntentos == 15) {
                                btnGuardar.setText("⏳ Creando tablas en PostgreSQL (Espera un momento)...");
                            } else if (finalIntentos == 35) {
                                btnGuardar.setText("⏳ Configurando motor de base de datos...");
                            } else if (finalIntentos == 60) {
                                btnGuardar.setText("⏳ Ya casi listo, finalizando arranque del servidor...");
                            }
                        });
                    }

                boolean finalServidorListo = servidorListo;
                int tiempoTotal = intentos;

                // 4. REGRESAMOS AL HILO DE JAVAFX PARA ABRIR LA PANTALLA
                javafx.application.Platform.runLater(() -> {
                    if (finalServidorListo) {
                        System.out.println("✅ ¡Servidor detectado en línea tras " + tiempoTotal + " segundos! Sincronizando...");
                    } else if (rbServidorPrincipal.isSelected()) {
                        AlertaPersonalizada.mostrarAlerta(
                                "Tiempo de Espera Agotado",
                                "El servidor tardó más de 90 segundos en arrancar.\nEs posible que tu máquina esté muy saturada o que el puerto 8080 esté bloqueado.\n\nEl sistema intentará iniciar en modo local.",
                                Alert.AlertType.WARNING
                        );
                    }

                    // AHORA SÍ: Como el servidor ya despertó (aunque haya tardado 50 segs),
                    // al iniciarSistema() descargará tus usuarios y productos reales de PostgreSQL a SQLite
                    App.iniciarSistema();

                    try {
                        Stage stage = (Stage) btnGuardar.getScene().getWindow();
                        stage.close();

                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Home.fxml"));
                        Parent root = loader.load();

                        Stage homeStage = new Stage();
                        homeStage.initStyle(StageStyle.TRANSPARENT);
                        Scene scene = new Scene(root);
                        scene.setFill(Color.TRANSPARENT);

                        homeStage.setScene(scene);
                        homeStage.setResizable(false);
                        homeStage.centerOnScreen();
                        homeStage.setOnCloseRequest(e -> System.exit(0));
                        homeStage.show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }).start();

        } catch (IOException e){
            e.printStackTrace();
            AlertaPersonalizada.mostrarAlerta("Error Crítico", "No se pudo guardar la configuración. Por favor, verifica los permisos de escritura.", Alert.AlertType.ERROR);
        }
    }

    private boolean validarCampos() {
        if (txtDbUrl.getText().trim().isEmpty() ||
                txtDbUser.getText().trim().isEmpty() ||
                txtDbPassword.getText().trim().isEmpty()) {
            return false;
        }
        if (rbServidorPrincipal.isSelected()) {
            return !txtNombreNegocio.getText().trim().isEmpty();
        }
        return true;
    }

    private String guardarLogoFisicamente(File archivoOrigen) {
        File dirImagenes = new File(RUTA_IMAGENES);
        if (!dirImagenes.exists()) dirImagenes.mkdirs();

        String nombreOriginal = archivoOrigen.getName();
        String extension = "";
        int i = nombreOriginal.lastIndexOf('.');
        if (i > 0) extension = nombreOriginal.substring(i);

        String nombreNuevo = "logo" + extension;
        Path rutaDestino = Paths.get(RUTA_IMAGENES, nombreNuevo);

        try {
            Files.copy(archivoOrigen.toPath(), rutaDestino, StandardCopyOption.REPLACE_EXISTING);
            return rutaDestino.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @FXML
    private void cerrarApp(ActionEvent event){
        System.exit(0);
    }
}