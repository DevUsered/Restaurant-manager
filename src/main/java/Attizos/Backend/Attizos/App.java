package Attizos.Backend.Attizos;

import Attizos.Backend.Api.ApiClient;
import Attizos.Backend.Database.*;
import Attizos.Backend.Listas.*;
import Attizos.Frontend.Network.WebSocketManager;
import Attizos.Frontend.ServicioNube;
import javafx.application.Platform;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App {
    public static Restaurante attizos;
    public static Empleado usuarioLogueado;
    public static boolean modoOffline = true;

    public static void iniciarSistema() {
        attizos = new Restaurante("Pollos CRX", "FAST_FOOD");
        ApiClient.cargarCredencialesDelServidor();
        ConexionSQLite.inicializarTablasLocales();

        System.out.println("Cargando datos locales...");
        cargarEmpleados();
        cargarInventario();
        cargarProductos();
        System.out.println("✅ Datos locales cargados.");

        Thread hiloInicial = new Thread(() ->{
            if(ApiClient.isServidorDisponible()){
                    System.out.println("🔄 Sincronizando datos con la Base de Datos...");
                    modoOffline = false;
                    ConexionSQLite.subirAuditoriaPendiente();
                    ServicioNube.sincronizarImagenesPendientes();
                    sincronizarDatosDesdeServidor();

                    String ipServidor = ApiClient.getIpServidor();
                   WebSocketManager.conectarAlServidor(ipServidor);
            }else{
                System.err.println("[Segundo Plano] Sin internet. Attizos sigue funcionando al 100% en modo local.");
                modoOffline = true;
            }
        });
        hiloInicial.setDaemon(true);
        hiloInicial.start();
    }
    public static void sincronizarDatosDesdeServidor() {
        try {
            ConexionSQLite.actualizarCacheCompleta();

            ArrayList<Insumo> stockRealNube = ApiClient.obtenerInsumoDelServidor();
            ArrayList<Producto> menuActualizado = ConexionSQLite.obtenerMenuLocal();
            ArrayList<Promocion> promocionDB = ConexionSQLite.obtenerPromocionesLocal(menuActualizado);

            Platform.runLater(() -> {
                if (stockRealNube != null && !stockRealNube.isEmpty()) {
                    attizos.getInventario().getInventarioInsumos().clear();
                    for (Insumo ins : stockRealNube) {
                        attizos.getInventario().getInventarioInsumos().put(ins.getCodigo(), ins);
                    }
                }
                attizos.setMenu(menuActualizado);
                attizos.setPromocionesActivas(promocionDB);
                System.out.println("✅ RAM actualizada con los datos frescos del servidor.");
            });
        } catch (Exception e) {
            System.err.println("❌ Error al sincronizar datos desde el servidor: " + e.getMessage());
        }
    }
    public static void cargarInventario() {
        HashMap<String, Insumo> inventario = ConexionSQLite.obtenerInventarioLocal();
        if (inventario != null && !inventario.isEmpty()) {
            for (Insumo i : inventario.values()) {
                System.out.println("DEBUG: Cargando insumo " + i.getNombre() + " con stock: " + i.getStockActual());
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
        ArrayList<Producto> menuDB = ConexionSQLite.obtenerMenuLocal();
        if (menuDB != null && !menuDB.isEmpty()) {
            for (Producto p : menuDB) {
                attizos.getMenu().add(p);
            }
            ArrayList<Promocion> promocionDB = ConexionSQLite.obtenerPromocionesLocal(menuDB);
            attizos.setPromocionesActivas(promocionDB);
            System.out.println("Promociones cargados. ");
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
            user = ApiClient.autenticarUsuarioEnServidor(username, pass);
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
            boolean guardadoDB = ApiClient.registrarAuditoriaEnServidor(operador, tipoArea, nombreItem, accion, cantidad, motivo);

            if (!guardadoDB) {
                ConexionSQLite.guardarAuditoriaOffline(operador, tipoArea, nombreItem, accion, cantidad, motivo);
            }
        }
    }
}