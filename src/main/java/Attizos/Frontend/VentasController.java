package Attizos.Frontend;

import Attizos.Backend.Attizos.*;
import Attizos.Backend.Listas.ListaDE;
import Attizos.Backend.Listas.NodoDE;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class VentasController {
    @FXML private TextField tfBuscar;
    @FXML private TextField tfNombreCli;
    @FXML private HBox hBCategoria;
    @FXML private FlowPane flPProductos;
    @FXML private VBox vBoxCarrito;
    @FXML private Label lblTotal;

    private Factura facturaActual;
    private String categoriaActiva = "Todos";

    private Producto productoSeleccionadoEnCarrito;
    private HBox filaSeleccionada;

    @FXML
    public void initialize() {
        iniciarNuevaVenta();
        if(tfBuscar != null){
            tfBuscar.textProperty().addListener((observable, oldValue, newValue) ->{
                mostrarProductosPorCategoria(categoriaActiva);
            });
        }
    }

    private void iniciarNuevaVenta() {
        tfNombreCli.clear();
        if(tfBuscar != null) tfBuscar.clear();
        facturaActual = new Factura(0, "");
        productoSeleccionadoEnCarrito = null;
        filaSeleccionada = null;

        cargarCategorias();
        mostrarProductosPorCategoria("Todos");
        actualizarVistaCarrito();
    }
    private void cargarCategorias() {
        hBCategoria.getChildren().clear();

        Button btnAll = crearBotonCategoria("Todos");
        hBCategoria.getChildren().add(btnAll);

        Set<String> cats = new HashSet<>();
        NodoDE<Producto> actual = App.attizos.getMenu().getCabeza();
        while (actual != null) {
            cats.add(actual.getDato().getCategoria());
            actual = actual.getSiguiente();
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
        NodoDE<Producto> actual = App.attizos.getMenu().getCabeza();
        while (actual != null) {
            Producto p = actual.getDato();
            boolean coincideCategoria = categoria.equals("Todos") || p.getCategoria().equalsIgnoreCase(categoria);
            boolean coincideBusqueda = p.getNombre().toLowerCase().contains(busqueda);
            if (coincideCategoria && coincideBusqueda) {
                crearTarjetaProducto(p);
            }
            actual = actual.getSiguiente();
        }
    }
    private void agregarAlCarrito(Producto p) {
        int disponible = p.calcularDisponibilidad(App.attizos.getInventario());
        if(disponible <= 0){
            mostrarAlerta("Sin Stock", p.getNombre() + "esta agotado o falta insumos. ",Alert.AlertType.WARNING);
            return;
        }
        NodoDE<DetalleFactura> ac = facturaActual.getDetalles().getCabeza();
        boolean existe = false;
        while (ac != null) {
            if(ac.getDato().getProducto().getId() == p.getId()){
                int nuevaCant = ac.getDato().getCantidad() + 1;
                facturaActual.modificarCantidad(p, nuevaCant, App.attizos.getInventario());
                existe = true;
                break;
            }
            ac = ac.getSiguiente();
        }
        if (!existe) {
            boolean agregado = facturaActual.agregarProducto(p, 1, App.attizos.getInventario());
            if (!agregado) {
                mostrarAlerta("Error de Stock", "No hay ingredientes suficientes para: " + p.getNombre(), Alert.AlertType.ERROR);
            }
        }
        actualizarVistaCarrito();
        mostrarProductosPorCategoria(categoriaActiva);
    }
    private void seleccionarItemCarrito(HBox row, Producto p) {
        if (filaSeleccionada != null) {
            filaSeleccionada.setStyle("-fx-border-color: transparent");
        }

        filaSeleccionada = row;
        productoSeleccionadoEnCarrito = p;
        filaSeleccionada.setStyle("-fx-background-color: rgba(218, 165, 32, 0.15); -fx-background-radius: 10; -fx-border-color: #daa520; -fx-border-radius: 10;");
    }

    @FXML
    void reducirProducto() {
        if (productoSeleccionadoEnCarrito == null) {
            mostrarAlerta("Atención", "Seleccione un producto del carrito para reducir su cantidad.", Alert.AlertType.WARNING);
            return;
        }
            NodoDE<DetalleFactura> actual = facturaActual.getDetalles().getCabeza();
            while (actual != null) {
                if (actual.getDato().getProducto().getId() == productoSeleccionadoEnCarrito.getId()) {
                    int nuevaCant = actual.getDato().getCantidad() - 1;
                    if (nuevaCant > 0) {
                        facturaActual.modificarCantidad(productoSeleccionadoEnCarrito, nuevaCant, App.attizos.getInventario());
                    } else {
                        facturaActual.eliminarProducto(productoSeleccionadoEnCarrito, App.attizos.getInventario());
                        productoSeleccionadoEnCarrito = null;
                        filaSeleccionada = null;
                    }
                    break;
                }
                actual = actual.getSiguiente();
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
            facturaActual.eliminarProducto(productoSeleccionadoEnCarrito, App.attizos.getInventario());
        productoSeleccionadoEnCarrito = null;
        filaSeleccionada = null;
            actualizarVistaCarrito();
            mostrarProductosPorCategoria(categoriaActiva);
    }
    @FXML
    void finalizarVenta() {
        String nombreCli = tfNombreCli.getText().trim();
        if (nombreCli.isEmpty()) nombreCli = "Sin Nombre";

        if(facturaActual.getTotal() <= 0){
            mostrarAlerta("Carrito Vacío", "⚠ Agregue productos antes de cobrar.", Alert.AlertType.WARNING);
            return;
        }
        int nroFactura = App.attizos.generarNumeroFactura();
        facturaActual.setNumeroFactura(nroFactura);
        facturaActual.setNombreCliente(nombreCli);

        ListaDE<DetalleFactura> productosParaCocina = new ListaDE<>();
        NodoDE<DetalleFactura> actual = facturaActual.getDetalles().getCabeza();
        boolean requiereCocina = false;

        while (actual != null){
            if(actual.getDato().getProducto().tieneReceta()){
                productosParaCocina.insertarAlFinal(actual.getDato());
                requiereCocina = true;
            }
            actual = actual.getSiguiente();
        }

        if(requiereCocina){
            Pedido nuevoPedido = new Pedido(facturaActual.getNumeroFactura(), nombreCli, productosParaCocina, facturaActual.getTotal());
            App.attizos.agregarPedido(nuevoPedido);
        }

        App.attizos.registrarVentaFinalizada(facturaActual);

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/Ticket.fxml"));
            javafx.scene.Parent nodoTicket = loader.load();

            TicketController controller = loader.getController();
            String nombreCajero = (App.usuarioLogueado != null) ? App.usuarioLogueado.getUsername() : "Caja Principal";

            controller.inicializarTicket(facturaActual, nombreCajero, facturaActual.getNumeroFactura());
            imprimirEnImpresoraTermica(nodoTicket);

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "La venta se registró pero no se pudo mandar al ticket.", Alert.AlertType.ERROR);
        }
        iniciarNuevaVenta();
    }

    private void imprimirEnImpresoraTermica(javafx.scene.Node nodoTicket){
        javafx.print.PrinterJob printerJob = javafx.print.PrinterJob.createPrinterJob();
        if (printerJob != null){
            try{
                // ========================================================
                // OPCIÓN 1: MOSTRAR PANTALLA DE IMPRESIÓN DE WINDOWS
                // ========================================================
               /*boolean procede = printerJob.showPrintDialog(null);
                if(procede){
                    boolean impreso = printerJob.printPage(nodoTicket);
                    if (impreso) {
                        printerJob.endJob();
                        System.out.println("✅ Ticket enviado a la impresora exitosamente.");
                    }
                }*/

               boolean impreso = printerJob.printPage(nodoTicket);
                if (impreso) {
                    printerJob.endJob();
                }
                // ========================================================


            } catch(Exception e) {
                e.printStackTrace();
            }
        } else {
            mostrarAlerta("Error de Impresora", "No se detectó ninguna impresora instalada en el sistema.", Alert.AlertType.ERROR);
        }
    }
    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        AlertaPersonalizada.mostrarAlerta(titulo,mensaje,tipo);
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
        try {
           String url = p.getImagenURL();
           String rutaF = url.startsWith("/") ? url : "/images/Productos/"+url;
           try(InputStream streamImg = getClass().getResourceAsStream(rutaF)){
               if(streamImg != null){
                   imgView.setImage(new Image(streamImg));
               }else{
                   try (InputStream streamDefault = getClass().getResourceAsStream("/images/default.png")) {
                       if (streamDefault != null) imgView.setImage(new Image(streamDefault));
                   }
               }
           }catch (Exception e){}
        } catch (Exception e) {
        }

        Label name = new Label(p.getNombre());
        name.getStyleClass().add("product-name");
        name.setWrapText(true);
        name.setTextAlignment(TextAlignment.CENTER);

        Label price = new Label("Bs. " + String.format("%.2f", p.getPrecio()));
        price.getStyleClass().add("product-price");

        int stock = p.calcularDisponibilidad(App.attizos.getInventario());
        Label lblStock = new Label("Stock: " + stock);
        lblStock.setStyle("-fx-text-fill: " + (stock > 0 ? "#218c4e;" : "#ff4c4c;"));

        card.getChildren().addAll(imgView, name, price, lblStock);

        card.setOnMouseClicked(e -> agregarAlCarrito(p));
        flPProductos.getChildren().add(card);
    }
    private void actualizarVistaCarrito() {
        vBoxCarrito.getChildren().clear();

        double imgSize = 45.0;

        NodoDE<DetalleFactura> actual = facturaActual.getDetalles().getCabeza();

        while (actual != null) {
            DetalleFactura det = actual.getDato();
            Producto p = det.getProducto();

            HBox itemRow = new HBox(12);
            itemRow.getStyleClass().add("cart-item");
            if (p == productoSeleccionadoEnCarrito) {
                itemRow.getStyleClass().add("cart-item-selected");
            }

            ImageView imgView = new ImageView();
            try {
                String archivoImagen = p.getImagenURL();
                String rutaFinal = archivoImagen.startsWith("/") ? archivoImagen : "/images/Productos/" + archivoImagen;
                java.io.InputStream streamImg = getClass().getResourceAsStream(rutaFinal);

                if (streamImg != null) {
                    imgView.setImage(new Image(streamImg));
                } else {
                    imgView.setImage(new Image(getClass().getResourceAsStream("/images/Productos/default.png")));
                }
            } catch (Exception e) {}

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

            Label qty = new Label("x" + det.getCantidad());
            qty.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #111111;");

            textos.getChildren().addAll(name, qty);


            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label price = new Label("Bs. " + String.format("%.2f", det.getSubtotal()));
            price.getStyleClass().add("cart-product-price");

            itemRow.getChildren().addAll(imgView, textos, spacer, price);


            itemRow.setOnMouseClicked(e -> {
                productoSeleccionadoEnCarrito = p;
                actualizarVistaCarrito();
            });

            vBoxCarrito.getChildren().add(itemRow);
            actual = actual.getSiguiente();
        }
        lblTotal.setText(String.format("%.2f", facturaActual.getTotal()));
    }
}