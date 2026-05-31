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
    public static Empleado usuarioLogueado;
    public static boolean modoOffline = true;

    public static void iniciarSistema() {
        attizos = new Restaurante("Pizzería Attizos");

        ConexionSQLite.inicializarTablasLocales();
        System.out.println("Cargando datos locales...");
        cargarEmpleados();
        cargarInventario();
        cargarProductos();
        System.out.println("✅ Datos locales cargados.");

        //Sincronización Invisible segundo plano
        new Thread(() ->{
            try(Connection con = ConexionBD.getConexion()){
                if(con != null && !con.isClosed()){
                    System.out.println("🔄 Sincronizando datos con la Base de Datos...");
                    modoOffline = false;
                    ConexionSQLite.subirAuditoriaPendiente();

                    ConexionSQLite.actualizarCacheCompleta();
                    RecetaDAO.cargarRecetas();
                }
            }catch (SQLException e){
                System.err.println("[Segundo Plano] Sin internet. Attizos sigue funcionando al 100% en modo local.");
            }
        }).start();
    }
    private static void cargarInventario() {
        HashMap<String, Insumo> inventario = ConexionSQLite.obtenerInventarioLocal();
        if (inventario != null && !inventario.isEmpty()) {
            for (Insumo i : inventario.values()) {
                attizos.getInventario().agregarInsumo(i);
            }
            System.out.println("✅ Inventario cargado en RAM con " + inventario.size() + " insumos.");
        } else {
            System.out.println("La BD local esta vacio");
        }
    }

    public static void cargarEmpleados() {
        ArrayList<Empleado> personaDB = ConexionSQLite.obtenerEmpleadosLocal();
        if (personaDB != null && !personaDB.isEmpty()) {
            for (Empleado emp : personaDB) {
                attizos.agregarEmpleado(emp);
            }
            System.out.println("Empleados cargados");
        }
    }

    public static void cargarProductos() {
        ListaDE<Producto> menuDB = ConexionSQLite.obtenerMenuLocal();
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
        Empleado user = ConexionSQLite.autenticarUsuarioLocal(username, pass);
        if (user != null) {
            usuarioLogueado = user;
            return true;
        } else if(!modoOffline){
            user = LoginDAO.autenticarUsuario(username, pass);
            if(user != null){
                usuarioLogueado = user;
                return  true;
            }
        }
        return false;
    }

    public static void registrarAuditoria(String operador, String tipoArea, String nombreItem, String accion, double cantidad, String motivo) {
        if(modoOffline){
            ConexionSQLite.guardarAuditoriaOffline(operador,tipoArea,nombreItem,accion,cantidad,motivo);
        }else {
            boolean guardadoDB = ReportesDAO.registrarAuditoria(operador, tipoArea, nombreItem, accion, cantidad, motivo);

            if (!guardadoDB) {
                ConexionSQLite.guardarAuditoriaOffline(operador, tipoArea, nombreItem, accion, cantidad, motivo);
            }
        }
    }
}