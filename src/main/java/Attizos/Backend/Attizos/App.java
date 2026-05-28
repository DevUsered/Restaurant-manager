package Attizos.Backend.Attizos;

import Attizos.Backend.Database.*;
import Attizos.Backend.Listas.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;

public class App {
    public static Restaurante attizos;
    public static Usuario usuarioLogueado;
    public static void iniciarSistema(){
        attizos = new Restaurante("Pizzería Attizos");

        try (Connection con = ConexionBD.getConexion()) {
            if (con != null && !con.isClosed()) {
                System.out.println("✅ Base de Datos conectada. Descargando datos a la memoria RAM...");
                cargarEmpleados();
                cargarInventario();
                cargarProductos();
                RecetaDAO.cargarRecetas();


            }
        } catch (SQLException e) {
            System.err.println("❌ Falla crítica: No se pudo conectar a PostgreSQL. " + e.getMessage());
        }
    }
    private static void cargarInventario(){
        HashMap<String, Insumo> inventario = InsumoDAO.obtenerInventarioActivo();
        if(!inventario.isEmpty()){
            for(Insumo i : inventario.values()){
                attizos.getInventario().agregarInsumo(i);
            }
            System.out.println("✅ Inventario cargado en RAM con " + inventario.size() + " insumos.");
        }else{
            System.out.println("La BD esta vacio");
        }
    }
    public static void cargarEmpleados(){
        ArrayList<Empleado> personaDB = EmpleadoDAO.obtenerEmpleadosActivos();
        if(!personaDB.isEmpty()){
            for(Empleado emp : personaDB){
                attizos.agregarEmpleado(emp);
            }
        }
    }
    public static void cargarProductos(){
        ListaDE<Producto> menuDB = ProductoDAO.obtenerMenuCompleto();
        if(menuDB != null && !menuDB.esVacia()){
            NodoDE<Producto> actual = menuDB.getCabeza();
            while(actual != null) {
                attizos.agregarProducto(actual.getDato());
                actual = actual.getSiguiente();
            }
            System.out.println("✅ Menú cargado en RAM con " + menuDB.getLongitud() + " productos.");
            RecetaDAO.cargarRecetas();
        }else{
            System.out.println("⚠️ La tabla de productos está vacía en la BD");
        }
    }
    public static boolean autenticarUsuario(String username, String pass){
        Usuario user = LoginDAO.autenticarUsuario(username, pass);
        if(user != null){
            usuarioLogueado = user;
            return true;
        }else{
            return false;
        }
    }
    public static void registrarAuditoria(String operador, String tipoArea, String nombreItem, String accion, double cantidad, String motivo){
        boolean guardadoDB = ReportesDAO.registrarAuditoria(operador, tipoArea, nombreItem, accion, cantidad, motivo);
        
        if (!guardadoDB) {
            System.err.println("⚠️ Error: No se pudo guardar la auditoría en la Base de Datos.");
        }
    }
   /* public static Restaurante attizos;
    public static Usuario usuarioLogueado;

    public static void cargarDatosEnRAM() {
        System.out.println("Cargando el sistema [EP]::Core para Pizzería Attizos en RAM...");
        attizos = new Restaurante("Pizzería Attizos");

        // CREACIÓN DE EMPLEADOS
        Admin jefe = new Admin("1", "Edgar Perez", "admin", "admin123", 12000);
        Cajero cajero1 = new Cajero("2", "Fulanito (Cajero)", 3500.0, "caja1", "123");
        Cocinero cocinero = new Cocinero("3", "Vinícius Jr.", 5000, "cocina1","abc");
        Empleado mesero1 = new Empleado("4", "Cristiano Ronaldo", "Mesero", 3300);
        Empleado limpieza = new Empleado("5", "Doña María", "Limpieza", 2500);

        attizos.agregarEmpleado(jefe);
        attizos.agregarEmpleado(cajero1);
        attizos.agregarEmpleado(cocinero);
        attizos.agregarEmpleado(mesero1);
        attizos.agregarEmpleado(limpieza);

        Inventario almacen = attizos.getInventario();
        LocalDate fechaBuena = LocalDate.now().plusMonths(6);
        LocalDate fechaCasiVencida = LocalDate.now().plusDays(3);
        LocalDate fechaVencida = LocalDate.now().minusDays(5);
        LocalDate fechaLejana = LocalDate.now().plusYears(1);

        almacen.agregarInsumo(new Insumo("I001", "Harina de trigo 000", "Masas", "kg", 50.0, 10.0, 100.0, fechaBuena));
        almacen.agregarInsumo(new Insumo("I002", "Levadura Fresca", "Masas", "gr", 500.0, 100.0, 1000.0, fechaCasiVencida));
        almacen.agregarInsumo(new Insumo("I003", "Aceite de Oliva", "Líquidos", "lt", 15.0, 3.0, 30.0, fechaLejana));

        // Lácteos y Salsas
        almacen.agregarInsumo(new Insumo("I006", "Salsa de tomate", "Salsas", "lt", 12.0, 5.0, 25.0, fechaBuena)); // Para mostrar la alerta roja
        almacen.agregarInsumo(new Insumo("I012", "Queso Mozzarella", "Lácteos", "kg", 20.0, 5.0, 40.0, fechaCasiVencida)); // Alerta amarilla
        almacen.agregarInsumo(new Insumo("I013", "Crema de Leche", "Lácteos", "lt", 10.0, 2.0, 20.0, fechaBuena));

        // Carnes y Fiambres
        almacen.agregarInsumo(new Insumo("I018", "Pepperoni", "Carnes", "kg", 8.0, 2.0, 15.0, fechaBuena));
        almacen.agregarInsumo(new Insumo("I019", "Jamón Ahumado", "Carnes", "kg", 10.0, 3.0, 20.0, fechaBuena));
        almacen.agregarInsumo(new Insumo("I020", "Tocino", "Carnes", "kg", 5.0, 1.5, 10.0, fechaBuena));
        almacen.agregarInsumo(new Insumo("I021", "Carne Molida", "Carnes", "kg", 15.0, 4.0, 30.0, fechaBuena));

        // Vegetales y Aderezos
        almacen.agregarInsumo(new Insumo("I025", "Aceitunas Negras", "Aderezos", "kg", 6.0, 2.0, 15.0, fechaLejana));
        almacen.agregarInsumo(new Insumo("I026", "Piña en Almíbar", "Aderezos", "lt", 8.0, 2.0, 20.0, fechaLejana));
        almacen.agregarInsumo(new Insumo("I027", "Champiñones Frescos", "Vegetales", "kg", 4.0, 1.0, 10.0, fechaCasiVencida));

        Receta rPepperoni = new Receta();
        rPepperoni.agregarIngrediente("I001", 0.30);
        rPepperoni.agregarIngrediente("I012", 0.25);
        rPepperoni.agregarIngrediente("I006", 0.15);
        rPepperoni.agregarIngrediente("I018", 0.20);

        Receta rHawaiana = new Receta();
        rHawaiana.agregarIngrediente("I001", 0.30);
        rHawaiana.agregarIngrediente("I012", 0.20);
        rHawaiana.agregarIngrediente("I006", 0.15);
        rHawaiana.agregarIngrediente("I019", 0.15);
        rHawaiana.agregarIngrediente("I026", 0.20);

        Receta rCarnivora = new Receta();
        rCarnivora.agregarIngrediente("I001", 0.40);
        rCarnivora.agregarIngrediente("I012", 0.30);
        rCarnivora.agregarIngrediente("I006", 0.20);
        rCarnivora.agregarIngrediente("I018", 0.10);
        rCarnivora.agregarIngrediente("I019", 0.10);
        rCarnivora.agregarIngrediente("I020", 0.15);
        rCarnivora.agregarIngrediente("I021", 0.20);

        Receta rPastaBol = new Receta();
        rPastaBol.agregarIngrediente("I001", 0.25);
        rPastaBol.agregarIngrediente("I006", 0.20);
        rPastaBol.agregarIngrediente("I021", 0.30);

        Receta rPastaAlf = new Receta();
        rPastaAlf.agregarIngrediente("I001", 0.25);
        rPastaAlf.agregarIngrediente("I013", 0.20);
        rPastaAlf.agregarIngrediente("I027", 0.15);

        Receta rCalzone = new Receta();
        rCalzone.agregarIngrediente("I001", 0.25);
        rCalzone.agregarIngrediente("I012", 0.20);
        rCalzone.agregarIngrediente("I006", 0.10);
        rCalzone.agregarIngrediente("I019", 0.15);


        // PIZZAS
        Pizza pPepp = new Pizza(101, "Pizza Pepperoni", 60.0, "Pizzas", "pizza_peperoni.png", "Pequeño", "Queso, salsa, pepperoni", false);
        pPepp.setReceta(rPepperoni);
        attizos.agregarProducto(pPepp);

        Pizza pHawaiana = new Pizza(102, "Pizza Hawaiana", 55.0, "Pizzas", "pizza.png", "Grande", "Queso, salsa, hawaiana", false);
        pHawaiana.setReceta(rHawaiana);
        attizos.agregarProducto(pHawaiana);

        Pizza pCarnivora = new Pizza(103, "Pizza Carnívora", 70.0, "Pizzas", "pizza_carnivora.png", "Mediana", "Queso, salsa, carnivora", false);
        pCarnivora.setReceta(rCarnivora);
        pCarnivora.setImagenURL("pizza_carnivora.png");
        attizos.agregarProducto(pCarnivora);

        Pasta pastaBol = new Pasta(201, "Pasta Boloñesa", 50.0, "Pastas", "pasta_bolonesa.png", "Pasta fresca casera, carne molida","Roja(Tomate)" );
        pastaBol.setReceta(rPastaBol);
        attizos.agregarProducto(pastaBol);

        Pasta pastaAlf = new Pasta(202, "Pasta Alfredo", 50.0, "Pastas", "fettuccine_alf.png", "Pasta fresca casera, champiñones frescos","Blanca(Crema)" );
        pastaAlf.setReceta(rPastaAlf);
        attizos.agregarProducto(pastaAlf);


        Calzone calzoneClas = new Calzone(301, "Calzone Italiano", 35.0, "Calzones","calzone_ita.png" ,"Masa crujiente, queso fundido, jamón, salsa base");
        calzoneClas.setReceta(rCalzone);
        attizos.agregarProducto(calzoneClas);

        Bebida b1 = new Bebida(401, "Coca Cola 2L", 15.0, "Bebidas", 24, "coca_2L.png",TamanoBebida.DOS_LITROS, "Gaseosa");
        attizos.agregarProducto(b1);

        Bebida b2 = new Bebida(402, "Coca Cola Personal", 3, "Bebidas", 24, "coca_personal.png",TamanoBebida.PERSONAL, "Gaseosa");
        attizos.agregarProducto(b2);

        Bebida b3 = new Bebida(403, "Sprite 2L", 12, "Bebidas", 24, "sprite_2L.png",TamanoBebida.MEDIO_LITRO, "Agua");
        attizos.agregarProducto(b3);

        Bebida b4 = new Bebida(404, "Agua Mineral", 5, "Bebidas", 24, "agua_min.png",TamanoBebida.PERSONAL, "Agua");
        attizos.agregarProducto(b4);

        Bebida b5 = new Bebida(405, "Cerveza Taquiña", 8, "Bebidas", 24, "cerveza_taqu.png",TamanoBebida.MEDIO_LITRO, "Alcohólica");
        attizos.agregarProducto(b5);
        System.out.println("✅ Datos cargados exitosamente.");
    }*/
}