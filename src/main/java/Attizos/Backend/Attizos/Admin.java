package Attizos.Backend.Attizos;


public class Admin extends Usuario {

    // Constructor que recibe los datos y los envía a la clase padre (Usuario)
    public Admin(String id, String nombre, String username, String password, double sueldo) {
        // Llamada al constructor de Usuario con los parámetros correctos
        super(id, nombre, "Administrador", sueldo, username, password);
    }
    
    @Override
    public void mostrarMenu() {
        System.out.println("\n========================================");
        System.out.println("      PANEL DE CONTROL - ADMINISTRADOR  ");
        System.out.println("========================================");
        System.out.println(" [1] Atender Caja (Nueva Venta)");
        System.out.println(" [2] Registrar nuevo producto al menú");
        System.out.println(" [3] Ver menú completo (Todos los productos)");
        System.out.println(" [4] Editar precio o stock de un producto");
        System.out.println(" [5] Eliminar un producto del menú");
        System.out.println(" [6] Gestionar empleados");
        System.out.println(" [7] Gestionar Reservas de mesas");
        System.out.println(" [8] Gestionar Pedidos (Monitor de Cocina)");
        System.out.println(" [9] Gestionar Almacén e Insumos");
        System.out.println(" [10] Generar Reportes (Diarios/Mensuales)");
        System.out.println(" [0] CERRAR SESIÓN");
        System.out.println("========================================");
    }
}