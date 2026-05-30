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

    public static void iniciarSistema() {
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

    private static void cargarInventario() {
        HashMap<String, Insumo> inventario = InsumoDAO.obtenerInventarioActivo();
        if (!inventario.isEmpty()) {
            for (Insumo i : inventario.values()) {
                attizos.getInventario().agregarInsumo(i);
            }
            System.out.println("✅ Inventario cargado en RAM con " + inventario.size() + " insumos.");
        } else {
            System.out.println("La BD esta vacio");
        }
    }

    public static void cargarEmpleados() {
        ArrayList<Empleado> personaDB = EmpleadoDAO.obtenerEmpleadosActivos();
        if (!personaDB.isEmpty()) {
            for (Empleado emp : personaDB) {
                attizos.agregarEmpleado(emp);
            }
        }
    }

    public static void cargarProductos() {
        ListaDE<Producto> menuDB = ProductoDAO.obtenerMenuCompleto();
        if (menuDB != null && !menuDB.esVacia()) {
            NodoDE<Producto> actual = menuDB.getCabeza();
            while (actual != null) {
                attizos.agregarProducto(actual.getDato());
                actual = actual.getSiguiente();
            }
            System.out.println("✅ Menú cargado en RAM con " + menuDB.getLongitud() + " productos.");
            RecetaDAO.cargarRecetas();
        } else {
            System.out.println("⚠️ La tabla de productos está vacía en la BD");
        }
    }

    public static boolean autenticarUsuario(String username, String pass) {
        Usuario user = LoginDAO.autenticarUsuario(username, pass);
        if (user != null) {
            usuarioLogueado = user;
            return true;
        } else {
            return false;
        }
    }

    public static void registrarAuditoria(String operador, String tipoArea, String nombreItem, String accion, double cantidad, String motivo) {
        boolean guardadoDB = ReportesDAO.registrarAuditoria(operador, tipoArea, nombreItem, accion, cantidad, motivo);

        if (!guardadoDB) {
            System.err.println("⚠️ Error: No se pudo guardar la auditoría en la Base de Datos.");
        }
    }
}