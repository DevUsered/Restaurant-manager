package Attizos.Frontend.Cobros;

public class BcpQrServiceImpl implements PasarelaQrService{
    @Override
    public String solicitarQrDinamico(double monto, String idVenta) throws Exception {
        System.out.println("🔗 Conectando a la API del BCP...");
        // Aquí irá la lógica HTTP específica del Banco de Crédito
        return "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=Simulacion_BCP_Attizos_" + monto;
    }

    @Override
    public boolean verificarPago(String idVenta) throws Exception {
        // Lógica para revisar el pago en el BCP
        return false;
    }
}
