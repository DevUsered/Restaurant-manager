package Attizos.Frontend;

import Attizos.Backend.Attizos.*;
import Attizos.Backend.Database.ConexionSQLite;
import Attizos.Backend.Database.FacturaDAO;
import Attizos.Backend.Listas.ListaDE;
import Attizos.Backend.Listas.NodoDE;
import Attizos.Frontend.Cobros.CobroQRController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.*;

public class VentasController {
    @FXML
    private TextField tfBuscar;
    @FXML
    private TextField tfNombreCli;
    @FXML
    private HBox hBCategoria;
    @FXML
    private FlowPane flPProductos;
    @FXML
    private VBox vBoxCarrito;
    @FXML
    private Label lblTotal;

    @FXML
    private HBox hBPromociones;
    @FXML
    private ScrollPane sPPromociones;
    @FXML
    private VBox panelPromociones;

    private Factura facturaActual;
    private String categoriaActiva = "Todos";

    private Producto productoSeleccionadoEnCarrito;
    private HBox filaSeleccionada;

    private ArrayList<Producto> menuRAM;
    private HashMap<String, Insumo> inventarioFrescoBD;

    @FXML
    public void initialize() {
        iniciarNuevaVenta();
        if (tfBuscar != null) {
            tfBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
                mostrarProductosPorCategoria(categoriaActiva);
            });
        }
    }

    private void iniciarNuevaVenta() {
        tfNombreCli.clear();
        if (tfBuscar != null) tfBuscar.clear();
        facturaActual = new Factura(0, "");
        productoSeleccionadoEnCarrito = null;
        filaSeleccionada = null;

        inventarioFrescoBD = App.attizos.getInventario().getInventarioInsumos();
        menuRAM = App.attizos.getMenu();
        cargarCategorias();
        mostrarProductosPorCategoria("Todos");
        cargarPromocionesActivas();
        actualizarVistaCarrito();
    }

    private void cargarCategorias() {
        hBCategoria.getChildren().clear();

        Button btnAll = crearBotonCategoria("Todos");
        hBCategoria.getChildren().add(btnAll);

        Set<String> cats = new HashSet<>();
        if (menuRAM != null) {
            for(Producto p : menuRAM){
                if(p.getEstado() != null && p.getEstado().equals("Activo") && !p.isPromocion() && !p.getCategoria().equalsIgnoreCase("Promocion")){
                    cats.add(p.getCategoria());
                }
            }
        }
        for (String c : cats) {
            hBCategoria.getChildren().add(crearBotonCategoria(c));
        }
    }

    private Button crearBotonCategoria(String nombreCat) {
        Button btn = new Button(nombreCat);
        btn.getStyleClass().add(nombreCat.equals(categoriaActiva) ? "menu-button-active" : "menu-button");
        btn.setOnAction(e -> {
            categoriaActiva = nombreCat;
            cargarCategorias();
            mostrarProductosPorCategoria(categoriaActiva);
        });
        return btn;
    }

    private void mostrarProductosPorCategoria(String categoria) {
        flPProductos.getChildren().clear();
        String busqueda = (tfBuscar != null && tfBuscar.getText() != null) ? tfBuscar.getText().toLowerCase() : "";
        if (menuRAM != null) {
            for(Producto p : App.attizos.getMenu()){
                if(p.getEstado() != null && p.getEstado().equals("Activo") && !p.isPromocion() && !p.getCategoria().equalsIgnoreCase("Promocion")){
                    boolean coincideCategoria = categoria.equals("Todos") || p.getCategoria().equalsIgnoreCase(categoria);
                    boolean coincideBusqueda = p.getNombre().toLowerCase().contains(busqueda);
                    if(coincideCategoria && coincideBusqueda){
                        crearTarjetaProducto(p);
                    }
                }
            }
        }
    }

    private void agregarAlCarrito(Producto p) {
        int disponible = calcularDisponibilidadEnVivo(p);
        if (disponible <= 0) {
            String motivoExacto = obtenerMotivoSinStock(p);
            mostrarAlerta("Sin Stock", "No se puede agregar: " + p.getNombre() + ".\n👉 " + motivoExacto, Alert.AlertType.WARNING);
            return;
        }
        boolean existe = false;
        for(DetalleFactura df : facturaActual.getDetalles()){
            if(df.getProducto().getId() == p.getId()){
                int nuevaCant = df.getCantidad() + 1;
                facturaActual.modificarCantidad(p, nuevaCant);
                existe = true;
                break;
            }
        }
        if (!existe) {
            facturaActual.agregarProducto(p, 1);
        }
        actualizarVistaCarrito();
        mostrarProductosPorCategoria(categoriaActiva);
        cargarPromocionesActivas();
    }
    private String obtenerMotivoSinStock(Producto p) {
        Map<Integer, Integer> prodDirectosAtrapados = new HashMap<>();
        Map<String, Double> insumosAtrapados = new HashMap<>();

        for(DetalleFactura df : facturaActual.getDetalles()){
            Producto prodCart = df.getProducto();
            int cant = df.getCantidad();
            registrarConsumoSimulado(prodCart, cant, prodDirectosAtrapados, insumosAtrapados);
        }
        return diagnosticarFalta(p, prodDirectosAtrapados, insumosAtrapados, 1);
    }
    private String diagnosticarFalta(Producto p, Map<Integer, Integer> prodDirectos, Map<String, Double> insumos, int cantidadRequerida) {
        if (p.isPromocion()) {
            Promocion promo = (Promocion) p;
            for (DetalleCombo dc : promo.getProductosCombo()) {
                int dispInterna = calcularMaximoPosible(dc.getProducto(), prodDirectos, insumos);
                if (dispInterna < dc.getCantidad()) {
                    return diagnosticarFalta(dc.getProducto(), prodDirectos, insumos, dc.getCantidad());
                }
            }
            return "Faltan componentes para el combo.";
        } else if (p.tieneReceta() && p.getReceta() != null) {
            if (p.getReceta().getIngredientes().isEmpty()) {
                int reservado = prodDirectos.getOrDefault(p.getId(), 0);
                if (p.getStock() - reservado < cantidadRequerida) return "Falta stock en vitrina de: " + p.getNombre();
                return "Error de stock.";
            }
            for (Map.Entry<String, Double> entry : p.getReceta().getIngredientes().entrySet()) {
                String cod = entry.getKey();
                double cantNec = entry.getValue() * cantidadRequerida;

                Insumo ins = App.attizos.getInventario().getInventarioInsumos().get(cod);
                if (ins == null || ins.isVencido()) {
                    return "El insumo está vencido o eliminado: " + (ins != null ? ins.getNombre() : cod);
                }

                double stockReal = ins.getStockActual();
                double reservado = insumos.getOrDefault(cod, 0.0);
                double libre = stockReal - reservado;

                if (libre < cantNec) {
                    return "Falta insumo: " + ins.getNombre() + "\n(Solo quedan " + String.format("%.2f", libre) + " " + ins.getUnidad() + " libres)";
                }
            }
            return "Ingredientes insuficientes para: " + p.getNombre();
        } else {
            int reservado = prodDirectos.getOrDefault(p.getId(), 0);
            double libre = p.getStock() - reservado;
            if (libre < cantidadRequerida) {
                return "Producto físico agotado: " + p.getNombre() + "\n(Disponibles: " + (int)Math.max(libre, 0) + ")";
            }
            return "Stock insuficiente.";
        }
    }

    @FXML
    void reducirProducto() {
        if (productoSeleccionadoEnCarrito == null) {
            mostrarAlerta("Atención", "Seleccione un producto del carrito para reducir su cantidad.", Alert.AlertType.WARNING);
            return;
        }
        for(DetalleFactura df : facturaActual.getDetalles()){
            if(df.getProducto().getId() == productoSeleccionadoEnCarrito.getId()){
                int nuevaCant = df.getCantidad() - 1;
                if(nuevaCant > 0){
                    facturaActual.modificarCantidad(productoSeleccionadoEnCarrito, nuevaCant);
                }else{
                    facturaActual.eliminarProducto(productoSeleccionadoEnCarrito);
                    productoSeleccionadoEnCarrito = null;
                    filaSeleccionada = null;
                }
                break;
            }
        }
        actualizarVistaCarrito();
        mostrarProductosPorCategoria(categoriaActiva);
    }

    @FXML
    void quitarProducto() {
        if (productoSeleccionadoEnCarrito == null) {
            mostrarAlerta("Atención", "Seleccione un producto del carrito para eliminarlo.", Alert.AlertType.WARNING);
            return;
        }
        facturaActual.eliminarProducto(productoSeleccionadoEnCarrito);
        productoSeleccionadoEnCarrito = null;
        filaSeleccionada = null;
        actualizarVistaCarrito();
        mostrarProductosPorCategoria(categoriaActiva);
    }

    @FXML
    void finalizarVenta() {
        String nombreCli = tfNombreCli.getText().trim();
        if (nombreCli.isEmpty()) nombreCli = "Sin Nombre";

        if (facturaActual.getTotal() <= 0) {
            mostrarAlerta("Carrito Vacío", "⚠ Agregue productos antes de cobrar.", Alert.AlertType.WARNING);
            return;
        }

        Map<Producto, Integer> carritoDB = new HashMap<>();

        StringBuilder jsonVenta = new StringBuilder();
        jsonVenta.append("{\"cliente\": \"").append(nombreCli).append("\", ");
        jsonVenta.append("\"total\": ").append(facturaActual.getTotal()).append(", ");
        jsonVenta.append("\"detalles\": [");

        boolean primero = true;
        for(DetalleFactura df : facturaActual.getDetalles()){
            carritoDB.put(df.getProducto(), df.getCantidad());
            if (primero) jsonVenta.append(", ");
            jsonVenta.append("{\"id_producto\": ").append(df.getProducto().getId())
                    .append(", \"cantidad\": ").append(df.getCantidad()).append("}");
            primero = false;
        }
        jsonVenta.append("]}");

        int numeroTicketDiario = -1;
        int numeroFacturaGlobal = -1;
        if(facturaActual.requiereCocina()){
            facturaActual.setEstado("En cocina");
        }else{
            facturaActual.setEstado("Finalizada");
        }

        if (!App.modoOffline) {
            int[] resultados = FacturaDAO.registrarVenta(nombreCli, facturaActual.getTotal(), carritoDB, facturaActual.getEstado());
            if (resultados != null) {
                numeroFacturaGlobal = resultados[0];
                numeroTicketDiario = resultados[1];
                ConexionSQLite.actualizarSecuenciaLocal(numeroTicketDiario);
            }
        }
        if (numeroTicketDiario <= 0) {
            numeroTicketDiario = ConexionSQLite.obtenerSiguienteTicketOffline();
            numeroFacturaGlobal = 0;
            boolean guardadoOffline = ConexionSQLite.guardarVentaOffline(jsonVenta.toString());
            if (!guardadoOffline) {
                mostrarAlerta("Error Crítico", "No se pudo registrar la venta localmente.", Alert.AlertType.ERROR);
                return;
            }
        }
        if (numeroTicketDiario > 0) {
            facturaActual.setNumeroFactura(numeroFacturaGlobal);
            facturaActual.setNombreCliente(nombreCli);

            for (Map.Entry<Producto, Integer> entry : carritoDB.entrySet()) {
                Producto p = entry.getKey();
                int cantVendida = entry.getValue();
                if (p.isPromocion()) {
                    Promocion promo = (Promocion) p;
                    for (DetalleCombo itemCombo : promo.getProductosCombo()) {
                        Producto productoInterno = itemCombo.getProducto();
                        int cantidadADescontar = itemCombo.getCantidad() * cantVendida;
                        descontarStockProductoFisico(productoInterno, cantidadADescontar);
                    }
                } else {
                    descontarStockProductoFisico(p, cantVendida);
                }
            }

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Ticket.fxml"));
                Parent nodoTicket = loader.load();

                TicketController controller = loader.getController();
                String nombreCajero = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Caja Principal";

                controller.inicializarTicket(facturaActual, nombreCajero, numeroTicketDiario);
                imprimirEnImpresoraTermica(nodoTicket);

            } catch (Exception e) {
                e.printStackTrace();
                mostrarAlerta("Error", "La venta se registró pero no se pudo mandar al ticket.", Alert.AlertType.ERROR);
            }
            iniciarNuevaVenta();
        }
    }

    private void imprimirEnImpresoraTermica(javafx.scene.Node nodoTicket) {
        javafx.print.PrinterJob printerJob = javafx.print.PrinterJob.createPrinterJob();
        if (printerJob != null) {
            try {
                boolean impreso = printerJob.printPage(nodoTicket);
                if (impreso) {
                    printerJob.endJob();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            mostrarAlerta("Error de Impresora", "No se detectó ninguna impresora instalada en el sistema.", Alert.AlertType.ERROR);
        }
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        AlertaPersonalizada.mostrarAlerta(titulo, mensaje, tipo);
    }

    private void crearTarjetaProducto(Producto p) {
        VBox card = new VBox(8);
        card.getStyleClass().add("sale-product-card");
        card.setAlignment(Pos.CENTER);
        ImageView imgView = new ImageView();
        imgView.setFitHeight(80);
        imgView.setFitWidth(80);
        imgView.setPreserveRatio(true);
        imgView.getStyleClass().add("product-image-view");
        String datoImagen = p.getImagenURL();
        Image imgOptimizada = UtilidadesImagen.obtenerImagenOptimizada(datoImagen);
        imgView.setImage(imgOptimizada);
        Label name = new Label(p.getNombre());
        name.getStyleClass().add("product-name");
        name.setWrapText(true);
        name.setTextAlignment(TextAlignment.CENTER);

        Label price = new Label("Bs. " + String.format("%.2f", p.getPrecio()));
        price.getStyleClass().add("product-price");

        int stock = calcularDisponibilidadEnVivo(p);
        Label lblStock = new Label("Stock: " + stock);
        lblStock.setStyle("-fx-text-fill: " + (stock > 0 ? "#218c4e;" : "#ff4c4c;"));

        card.getChildren().addAll(imgView, name, price, lblStock);

        card.setOnMouseClicked(e -> agregarAlCarrito(p));
        flPProductos.getChildren().add(card);
    }

    private void actualizarVistaCarrito() {
        vBoxCarrito.getChildren().clear();

        double imgSize = 45.0;
        for(DetalleFactura df : facturaActual.getDetalles()){
            Producto p = df.getProducto();
            HBox itemRow = new HBox(12);
            itemRow.getStyleClass().add("cart-item");
            if (p == productoSeleccionadoEnCarrito) {
                itemRow.getStyleClass().add("cart-item-selected");
            }

            ImageView imgView = new ImageView();
            String datoImagen = p.getImagenURL();
            Image imgOptimizada = UtilidadesImagen.obtenerImagenOptimizada(datoImagen);
            imgView.setImage(imgOptimizada);
            imgView.setFitHeight(imgSize);
            imgView.setFitWidth(imgSize);
            imgView.setPreserveRatio(false);

            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(imgSize, imgSize);
            clip.setArcWidth(10);
            clip.setArcHeight(10);
            imgView.setClip(clip);

            VBox textos = new VBox(2);
            textos.setAlignment(Pos.CENTER_LEFT);
            Label name = new Label(p.getNombre());
            name.getStyleClass().add("cart-product-name");
            name.setMaxWidth(110);
            name.setWrapText(true);

            Label qty = new Label("x" + df.getCantidad());
            qty.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #111111;");

            textos.getChildren().addAll(name, qty);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label price = new Label("Bs. " + String.format("%.2f", df.getSubtotal()));
            price.getStyleClass().add("cart-product-price");

            itemRow.getChildren().addAll(imgView, textos, spacer, price);

            itemRow.setOnMouseClicked(e -> {
                productoSeleccionadoEnCarrito = p;
                actualizarVistaCarrito();
            });

            vBoxCarrito.getChildren().add(itemRow);
        }
        lblTotal.setText(String.format("%.2f", facturaActual.getTotal()));
    }

    private int calcularDisponibilidadEnVivo(Producto p) {
        Map<Integer, Integer> prodDirectosAtrapados = new HashMap<>();
        Map<String, Double> insumosAtrapados = new HashMap<>();

        for(DetalleFactura df : facturaActual.getDetalles()){
            Producto prodCart = df.getProducto();
            int cant = df.getCantidad();
            registrarConsumoSimulado(prodCart, cant, prodDirectosAtrapados, insumosAtrapados);
        }
        return calcularMaximoPosible(p, prodDirectosAtrapados, insumosAtrapados);
    }
    private void registrarConsumoSimulado(Producto p, int cantidad, Map<Integer, Integer> prodDirectos, Map<String, Double> insumos) {
        if (p.isPromocion()) {
            Promocion promo = (Promocion) p;
            for (DetalleCombo dc : promo.getProductosCombo()) {
                registrarConsumoSimulado(dc.getProducto(), cantidad * dc.getCantidad(), prodDirectos, insumos);
            }
        } else if (p.tieneReceta() && p.getReceta() != null) {
            for (Map.Entry<String, Double> entry : p.getReceta().getIngredientes().entrySet()) {
                insumos.put(entry.getKey(), insumos.getOrDefault(entry.getKey(), 0.0) + (entry.getValue() * cantidad));
            }
        } else {
            prodDirectos.put(p.getId(), prodDirectos.getOrDefault(p.getId(), 0) + cantidad);
        }
    }
    private int calcularMaximoPosible(Producto p, Map<Integer, Integer> prodDirectos, Map<String, Double> insumos) {
        if (p.isPromocion()) {
            Promocion promo = (Promocion) p;
            int maxCombos = Integer.MAX_VALUE;
            for (DetalleCombo dc : promo.getProductosCombo()) {
                int disp = calcularMaximoPosible(dc.getProducto(), prodDirectos, insumos);
                int posibles = disp / dc.getCantidad();
                if (posibles < maxCombos) maxCombos = posibles;
            }
            return maxCombos == Integer.MAX_VALUE ? 0 : maxCombos;

        } else if (p.tieneReceta() && p.getReceta() != null) {
            int maxPlatos = Integer.MAX_VALUE;
            if (p.getReceta().getIngredientes().isEmpty()) return (int) p.getStock();

            for (Map.Entry<String, Double> entry : p.getReceta().getIngredientes().entrySet()) {
                String cod = entry.getKey();
                double cantNec = entry.getValue();

                double stockReal = 0;
                Insumo ins = App.attizos.getInventario().getInventarioInsumos().get(cod);
                if (ins != null && !ins.isVencido()) stockReal = ins.getStockActual();

                double reservado = insumos.getOrDefault(cod, 0.0);
                double libre = stockReal - reservado;
                if (libre < 0) libre = 0;

                int porciones = (int) Math.floor((libre / cantNec) + 0.0001);
                if (porciones < maxPlatos) maxPlatos = porciones;
            }
            return maxPlatos == Integer.MAX_VALUE ? 0 : maxPlatos;
        } else {
            int reservado = prodDirectos.getOrDefault(p.getId(), 0);
            int libre = (int) p.getStock() - reservado;
            return Math.max(libre, 0);
        }
    }

    @FXML
    void cobrarConQR(ActionEvent event){
        if(facturaActual.getTotal() <= 0){
            mostrarAlerta("Carrito Vacío","⚠ Agregue productos antes de cobrar.",Alert.AlertType.WARNING);
            return;
        }
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ModalCobroQR.fxml"));
            Parent root = loader.load();

            CobroQRController controller = loader.getController();
            controller.inicializarCobro(facturaActual.getTotal());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);

            stage.showAndWait();

            if(controller.isPagoCompletado()){
                finalizarVenta();
                mostrarAlerta("¡Pago Exitoso!", "El pago Qr se realizó con éxito.", Alert.AlertType.INFORMATION);
            }else{
                mostrarAlerta("Operación Cancelada", "El cobro qr fue cancelado. ", Alert.AlertType.WARNING);
            }
        }catch (Exception e){
            e.printStackTrace();
            mostrarAlerta("Error","No se pudo mostrar el QR por el momento.", Alert.AlertType.ERROR);
        }
    }

    private void cargarPromocionesActivas(){
        ArrayList<Promocion> listaPromos = App.attizos.getPromocionesActivas();

        if(listaPromos == null || listaPromos.isEmpty()){
            if(sPPromociones != null){
                sPPromociones.setVisible(false);
                sPPromociones.setManaged(false);
            }
            return;
        }

        if(sPPromociones != null){
            sPPromociones.setVisible(true);
            sPPromociones.setManaged(true);
        }

        hBPromociones.getChildren().clear();
        for(Promocion promo : listaPromos) {
            VBox tarjetaPromo = new VBox(8);
            tarjetaPromo.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            tarjetaPromo.setMinWidth(150);
            tarjetaPromo.setStyle("-fx-background-color: rgba(255,255,255,0.25); -fx-background-radius: 10; -fx-padding: 8 12; -fx-cursor: hand;");

            ImageView imgView = new ImageView();
            String datoImagen = promo.getImagenURL();
            Image imgOptimizada = UtilidadesImagen.obtenerImagenOptimizada(datoImagen);
            imgView.setImage(imgOptimizada);
            imgView.setFitHeight(110);
            imgView.setFitWidth(110);
            imgView.setPreserveRatio(false);

            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(110, 110);
            clip.setArcWidth(15);
            clip.setArcHeight(15);
            imgView.setClip(clip);

            VBox textosPromo = new VBox(4);
            textosPromo.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Label lblNombre = new Label(promo.getNombre());
            lblNombre.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 16px;");

            Label lblPrecio = new Label("Bs. " + String.format("%.2f", promo.getPrecio()));
            lblPrecio.setStyle("-fx-text-fill: #FFEB3B; -fx-font-weight: bold; -fx-font-size: 15px;");
            int stock = calcularDisponibilidadEnVivo(promo);
            Label lblStock = new Label("Stock: " + stock);
            lblStock.setStyle("-fx-text-fill: " + (stock > 0 ? "#FFFFFF;" : "#ff4c4c;"));

            textosPromo.getChildren().addAll(lblNombre, lblPrecio);

            tarjetaPromo.getChildren().addAll(imgView, textosPromo);

            tarjetaPromo.setOnMouseClicked(e -> agregarAlCarrito(promo));

            tarjetaPromo.setOnMouseEntered(e -> tarjetaPromo.setStyle("-fx-background-color: rgba(255,255,255,0.4); -fx-background-radius: 8; -fx-padding: 8 12; -fx-cursor: hand;"));
            tarjetaPromo.setOnMouseExited(e -> tarjetaPromo.setStyle("-fx-background-color: rgba(255,255,255,0.25); -fx-background-radius: 8; -fx-padding: 8 12; -fx-cursor: hand;"));

            hBPromociones.getChildren().add(tarjetaPromo);
        }
    }

    private void descontarStockProductoFisico(Producto p, int cant){
        if(p.tieneReceta() && p.getReceta() != null) {
            for (Map.Entry<String, Double> entry : p.getReceta().getIngredientes().entrySet()) {
                Insumo ins = App.attizos.getInventario().getInventarioInsumos().get(entry.getKey());
                if (ins != null) ins.setStockActual(ins.getStockActual() - (entry.getValue() * cant));
            }
        }else{
            p.reducirStock(cant);
        }
    }
}