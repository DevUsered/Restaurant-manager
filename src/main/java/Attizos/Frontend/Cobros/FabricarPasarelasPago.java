package Attizos.Frontend.Cobros;

public class FabricarPasarelasPago {
    public static PasarelaQrService obtenerPasarelaActiva() {
        System.out.println("✅ Cargando pasarela de pagos activa: BANCO ECONÓMICO");
        return new QrServiceImpl();
    }
}