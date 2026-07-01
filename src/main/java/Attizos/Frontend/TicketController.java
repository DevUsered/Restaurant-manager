package Attizos.Frontend;

import Attizos.Backend.Attizos.App;
import Attizos.Backend.Attizos.DetalleFactura;
import Attizos.Backend.Attizos.Factura;
import Attizos.Backend.Listas.NodoDE;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class TicketController {
    @FXML private Label lblNombreNegocio;
    @FXML private Label lblNumFacturaCaja;
    @FXML private Label lblFechaCaja;
    @FXML private Label lblCajero;
    @FXML private Label lblClienteCaja;
    @FXML private VBox vboxProductosCaja;
    @FXML private Label lblTotalCaja;

    @FXML private Label lblTurnoCliente;
    @FXML private Label lblFechaCliente;
    @FXML private Label lblClienteCliente;
    @FXML private VBox vboxProductosCliente;
    @FXML private Label lblTotalCliente;
    @FXML private VBox ticketCocina;
    @FXML private VBox ticketCliente;
    @FXML private Label separadorTijera;

    public void inicializarTicket(Factura factura, String nombreCajero, int turno){
        System.out.println("-ID Global Factura: "+factura.getNumeroFactura());
        System.out.println("- Turno diario: "+turno);
        if(lblNombreNegocio != null && App.attizos != null){
            lblNombreNegocio.setText(App.attizos.getNombre());
        }
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        LocalDateTime fechaSegura = factura.getFecha() != null ? factura.getFecha() : LocalDateTime.now();
        String fechaStr = fechaSegura.format(formato);

        String numFac = String.format("%03d", turno);


        lblNumFacturaCaja.setText("Ticket Nro: " + numFac);
        lblFechaCaja.setText("Fecha: " + fechaStr);
        lblCajero.setText("Usuario: " + nombreCajero);
        lblClienteCaja.setText("Cliente: " + factura.getNombreCliente());
        lblTotalCaja.setText(String.format("Bs. %.2f", factura.getTotal()));

        lblTurnoCliente.setText("#" + String.format("%03d", turno));
        lblFechaCliente.setText("Fecha: " + fechaStr);
        lblClienteCliente.setText("Cliente: " + factura.getNombreCliente());
        lblTotalCliente.setText(String.format("Bs. %.2f", factura.getTotal()));

        vboxProductosCaja.getChildren().clear();
        vboxProductosCliente.getChildren().clear();

        for(DetalleFactura df : factura.getDetalles()){
            vboxProductosCaja.getChildren().add(crearFilaProducto(df));
            vboxProductosCliente.getChildren().add(crearFilaProducto(df));
        }
    }
    private HBox crearFilaProducto(DetalleFactura det) {
        HBox fila = new HBox();
        fila.setSpacing(10);

        Label lblCant = new Label(String.valueOf(det.getCantidad()));
        lblCant.setPrefWidth(35);
        lblCant.setStyle("-fx-font-family: 'Monospaced'; -fx-text-fill: black;");

        Label lblProd = new Label(det.getProducto().getNombre());
        lblProd.setPrefWidth(140);
        lblProd.setWrapText(true);
        lblProd.setStyle("-fx-font-family: 'Monospaced'; -fx-text-fill: black;");

        Label lblSub = new Label(String.format("%.2f", det.getSubtotal()));
        lblSub.setPrefWidth(95);
        lblSub.setAlignment(Pos.CENTER_RIGHT);
        lblSub.setStyle("-fx-font-family: 'Monospaced'; -fx-text-fill: black;");

        fila.getChildren().addAll(lblCant, lblProd, lblSub);
        return fila;
    }
    public Node getContenedorTicketCliente(){

        return ticketCliente;
    }
    public Node getContenedorTicketCocina(){
        return ticketCocina;
    }
    public void ocultarTicketCocina(){
       if(ticketCocina != null) {
           ticketCocina.setVisible(false);
           ticketCocina.setManaged(false);
       }
       if(separadorTijera != null) {
           separadorTijera.setVisible(false);
           separadorTijera.setManaged(false);
       }
    }
    public void mostrarTicketCocina(){
        if(ticketCocina != null) {
            ticketCocina.setVisible(true);
            ticketCocina.setManaged(true);
        }
        if(separadorTijera != null) {
            separadorTijera.setVisible(true);
            separadorTijera.setManaged(true);
        }
    }
    public void ocultarTicketCliente() {
        if (ticketCliente != null) {
            ticketCliente.setVisible(false);
            ticketCliente.setManaged(false);
        }
        if (separadorTijera != null) {
            separadorTijera.setVisible(false);
            separadorTijera.setManaged(false);
        }
    }

    public void mostrarTicketCliente() {
        if (ticketCliente != null) {
            ticketCliente.setVisible(true);
            ticketCliente.setManaged(true);
        }
        if (separadorTijera != null && ticketCocina.isVisible()) {
            separadorTijera.setVisible(true);
            separadorTijera.setManaged(true);
        }
    }
}