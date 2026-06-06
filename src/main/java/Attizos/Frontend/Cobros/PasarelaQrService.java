package Attizos.Frontend;

public interface PasarelaQrService {

    String solicitarQrDinamico(double monto, String idVenta) throws Exception;

    boolean verificarPago(String idVenta) throws Exception;
}
