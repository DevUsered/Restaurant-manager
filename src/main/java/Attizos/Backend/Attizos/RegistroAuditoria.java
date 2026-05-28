package Attizos.Backend.Attizos;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
public class RegistroAuditoria {
    private String fechaHora;
    private String operador;
    private String tipoArea;
    private String nombreItem;
    private String accion;
    private double cantidad;
    private String motivo;

    public RegistroAuditoria(String operador, String tipoArea, String nombreItem, String accion, double cantidad, String motivo) {
        this.fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
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
    public void setDate(String fechaHora) {
        this.fechaHora = fechaHora;
    }
}
