package Attizos.Frontend;

import Attizos.Backend.Attizos.*;
import Attizos.Backend.Database.FacturaDAO;
import Attizos.Backend.Database.InsumoDAO;
import Attizos.Backend.Database.ProductoDAO;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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


    private ListaDE<Producto> menuDB;
    //Cache
    private HashMap<String, Insumo> inventarioFrescoBD;

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

        inventarioFrescoBD = InsumoDAO.obtenerInventarioActivo();
        menuDB = ProductoDAO.obtenerMenuCompleto();
        cargarCategorias();
        mostrarProductosPorCategoria("Todos");
        actualizarVistaCarrito();
    }
    private void cargarCategorias() {
        hBCategoria.getChildren().clear();

        Button btnAll = crearBotonCategoria("Todos");
        hBCategoria.getChildren().add(btnAll);

        Set<String> cats = new HashSet<>();
        if(menuDB != null){
            NodoDE<Producto> actualDB = menuDB.getCabeza();
            while (actualDB != null) {
                cats.add(actualDB.getDato().getCategoria());
                actualDB = actualDB.getSiguiente();
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
        if(menuDB != null) {
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
    }
    private void agregarAlCarrito(Producto p) {
        int disponible = calcularDisponibilidadEnVivo(p);
        if(disponible <= 0){
            mostrarAlerta("Sin Stock", p.getNombre() + "esta agotado o falta insumos. ",Alert.AlertType.WARNING);
            return;
        }
        NodoDE<DetalleFactura> ac = facturaActual.getDetalles().getCabeza();
        boolean existe = false;
        while (ac != null) {
            if(ac.getDato().getProducto().getId() == p.getId()){
                int nuevaCant = ac.getDato().getCantidad() + 1;
                facturaActual.modificarCantidad(p, nuevaCant);
                existe = true;
                break;
            }
            ac = ac.getSiguiente();
        }
        if (!existe) {
            boolean agregado = facturaActual.agregarProducto(p, 1);;
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
                        facturaActual.modificarCantidad(productoSeleccionadoEnCarrito, nuevaCant);
                    } else {
                        facturaActual.eliminarProducto(productoSeleccionadoEnCarrito);
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

        if(facturaActual.getTotal() <= 0){
            mostrarAlerta("Carrito Vacío", "⚠ Agregue productos antes de cobrar.", Alert.AlertType.WARNING);
            return;
        }

        Map<Producto, Integer> carritoDB = new HashMap<>();
        NodoDE<DetalleFactura> actual = facturaActual.getDetalles().getCabeza();
        while (actual != null){
            DetalleFactura det = actual.getDato();
            carritoDB.put(det.getProducto(), det.getCantidad());
            actual = actual.getSiguiente();
        }
        int  numeroFactura = FacturaDAO.registrarVenta(nombreCli, facturaActual.getTotal(), carritoDB);
        if(numeroFactura > 0) {
            facturaActual.setNumeroFactura(numeroFactura);
            facturaActual.setNombreCliente(nombreCli);

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
        } else {
            mostrarAlerta("Error Crítico", "No se pudo registrar la venta en la Base de Datos. Revise el stock de cocina o la conexión.", Alert.AlertType.ERROR);
        }
    }

    private void imprimirEnImpresoraTermica(javafx.scene.Node nodoTicket){
        javafx.print.PrinterJob printerJob = javafx.print.PrinterJob.createPrinterJob();
        if (printerJob != null){
            try{
               boolean impreso = printerJob.printPage(nodoTicket);
                if (impreso) {
                    printerJob.endJob();
                }
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
        String datoImagen = p.getImagenURL();
        if (datoImagen != null && !datoImagen.equals("default.png")) {
            Image imgReal = UtilidadesImagen.convertirBase64AImagen(datoImagen);
            if (imgReal != null) {
                imgView.setImage(imgReal);
            } else {
                imgView.setImage(new Image(getClass().getResourceAsStream("/images/default.png")));
            }
        } else {
            imgView.setImage(new Image(getClass().getResourceAsStream("/images/default.png")));
        }
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
            String datoImagen = p.getImagenURL();
            if (datoImagen != null && !datoImagen.equals("default.png")) {
                Image imgReal = UtilidadesImagen.convertirBase64AImagen(datoImagen);
                if (imgReal != null) {
                    imgView.setImage(imgReal);
                } else {
                    imgView.setImage(new Image(getClass().getResourceAsStream("/images/default.png")));
                }
            } else {
                imgView.setImage(new Image(getClass().getResourceAsStream("/images/default.png")));
            }
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
    private int calcularDisponibilidadEnVivo(Producto p){
        if(!p.tieneReceta()){
            int enCarrito = 0;
            NodoDE<DetalleFactura> ac = facturaActual.getDetalles().getCabeza();
            while(ac != null){
                if(ac.getDato().getProducto().getId() == p.getId()){
                    enCarrito += ac.getDato().getCantidad();
                }
                ac = ac.getSiguiente();
            }
            return (int) p.getStock() - enCarrito;
        }else{
            Map<String, Double> insumoAtrapados = new HashMap<>();
            NodoDE<DetalleFactura> ac = facturaActual.getDetalles().getCabeza();
            while(ac != null){
                Producto prodCarrito = ac.getDato().getProducto();
                int cantEnCarrito = ac.getDato().getCantidad();
               if(prodCarrito.tieneReceta()){
                   for(Map.Entry<String, Double> entry : prodCarrito.getReceta().getIngredientes().entrySet()){
                       String codInsumo = entry.getKey();
                       double cantTotalUsada = entry.getValue() * cantEnCarrito;
                       insumoAtrapados.put(codInsumo, insumoAtrapados.getOrDefault(codInsumo,0.0) + cantTotalUsada);

                   }
               }
               ac = ac.getSiguiente();
            }
            int maxPlatosPosibles = Integer.MAX_VALUE;
            Inventario inv = App.attizos.getInventario();

            for(Map.Entry<String, Double> entry : p.getReceta().getIngredientes().entrySet()){
                String codInsumoBase = entry.getKey();
                double cantNecesariaPorPlato = entry.getValue();

                double stockValidoReal = 0;

                Insumo insumoFisico = (inventarioFrescoBD != null) ? inventarioFrescoBD.get(codInsumoBase) : null;
                if(insumoFisico != null && !insumoFisico.isVencido()){
                    stockValidoReal = insumoFisico.getStockActual();
                }
                double stockReservado = insumoAtrapados.getOrDefault(codInsumoBase, 0.0);
                double stockLibre = stockValidoReal - stockReservado;
                if(stockLibre < 0) stockLibre = 0;
                int porciones = (int) (stockLibre / cantNecesariaPorPlato);
                if(porciones < maxPlatosPosibles){
                    maxPlatosPosibles = porciones;
                }
            }
            return maxPlatosPosibles == Integer.MAX_VALUE ? 0 : maxPlatosPosibles;
        }
    }
}