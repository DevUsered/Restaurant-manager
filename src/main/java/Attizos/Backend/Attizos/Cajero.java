package Attizos.Backend.Attizos;



public class Cajero extends Usuario {

    // Constructor para el cajero
    public Cajero(String id, String nombre, double sueldo, String username, String password) {
        // Le pasamos automáticamente el cargo "Cajero" a la clase padre
        super(id, nombre, "Cajero", sueldo, username,password);
    }

    // Menú simplificado y protegido (No puede ver planillas ni editar productos)
    @Override
    public void mostrarMenu() {
        System.out.println("\n========================================");
        System.out.println("          TERMINAL DE CAJERO            ");
        System.out.println("========================================");
        System.out.println(" [1] Atender Caja (Nueva Venta)");
        System.out.println(" [2] Gestionar Reservas de mesas");
        System.out.println(" [3] Gestionar Pedidos (Monitor de Cocina)");
        System.out.println(" [0] CERRAR SESIÓN");
        System.out.println("========================================");
    }
}
