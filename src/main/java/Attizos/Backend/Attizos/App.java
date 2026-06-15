package Attizos.Backend.Attizos;

import Attizos.Backend.Api.ApiClient;
import Attizos.Backend.Database.*;
import Attizos.Backend.Listas.*;
import Attizos.Frontend.ServicioNube;

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
    private static ScheduledExecutorService temporizadorSincronizacion;

    public static void iniciarSincronizacionAutomatica() {
        temporizadorSincronizacion = Executors.newScheduledThreadPool(1, r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        });

        temporizadorSincronizacion.scheduleAtFixedRate(() -> {
            try {
                if(ApiClient.isServidorDisponible()){
                        System.out.println("⏳ [Auto-Sync] Buscando actualizaciones en la red...");
                        modoOffline = false;

                        ConexionSQLite.subirAuditoriaPendiente();
                        ServicioNube.sincronizarImagenesPendientes();
                        ConexionSQLite.actualizarCacheCompleta();
                        ArrayList<Producto> menuActualizado = ConexionSQLite.obtenerMenuLocal();
                        attizos.setMenu(menuActualizado);
                        ArrayList<Promocion> promocionDB = ConexionSQLite.obtenerPromocionesLocal(menuActualizado);
                        attizos.setPromocionesActivas(promocionDB);
                        System.out.println("✅ [Auto-Sync] Sistema actualizado con éxito.");
                    }else{
                    throw new RuntimeException("Servidor inalcanzable");
                }
            } catch (Exception e) {
                System.out.println("⚠️ [Auto-Sync] Modo offline detectado, reintentando en 5 min.");
                modoOffline = true;
            }
        }, 5, 5, TimeUnit.MINUTES);
    }

    public static void iniciarSistema() {
        attizos = new Restaurante("Pizzería Attizos");

        ConexionSQLite.inicializarTablasLocales();
        System.out.println("Cargando datos locales...");
        cargarEmpleados();
        cargarInventario();
        cargarProductos();
        System.out.println("✅ Datos locales cargados.");

        Thread hiloInicial = new Thread(() ->{
            try(Connection con = ConexionBD.getConexion()){
                if(con != null && !con.isClosed()){
                    System.out.println("🔄 Sincronizando datos con la Base de Datos...");
                    modoOffline = false;
                    ConexionSQLite.subirAuditoriaPendiente();
                    ServicioNube.sincronizarImagenesPendientes();
                    ConexionSQLite.actualizarCacheCompleta();

                    ArrayList<Insumo> stockRealNube = ApiClient.obtenerInsumoDelServidor();
                    if (stockRealNube != null && !stockRealNube.isEmpty()) {
                        attizos.getInventario().getInventarioInsumos().clear();
                        for (Insumo ins : stockRealNube) {
                            attizos.getInventario().getInventarioInsumos().put(ins.getCodigo(), ins);
                        }
                        System.out.println("✅ RAM actualizada con el stock real de la nube.");
                    }
                    ArrayList<Producto> menuActualizado = ConexionSQLite.obtenerMenuLocal();
                    attizos.setMenu(menuActualizado);
                    RecetaDAO.cargarRecetas();
                    ArrayList<Promocion> promocionDB = ConexionSQLite.obtenerPromocionesLocal(menuActualizado);
                    attizos.setPromocionesActivas(promocionDB);
                }
            }catch (SQLException e){
                System.err.println("[Segundo Plano] Sin internet. Attizos sigue funcionando al 100% en modo local.");
            }
        });
        hiloInicial.setDaemon(true);
        hiloInicial.start();
        iniciarSincronizacionAutomatica();
    }
    public static void cargarInventario() {
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