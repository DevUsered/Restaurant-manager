package Attizos.Backend.Api;

import Attizos.Backend.Attizos.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiClient {
    private static String ipServidor = "localhost";
    private static  String BASE_URL = "http://" + ipServidor + ":8080/api";
    public static java.util.Map<String, String> credenciales = new java.util.HashMap<>();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    public static String getIpServidor(){
        return ipServidor;
    }
    public static void configurarIpServidor(String nuevaIp) {
        if (nuevaIp != null && !nuevaIp.trim().isEmpty()) {
            ipServidor = nuevaIp.trim();
            BASE_URL = "http://" + ipServidor + ":8080/api";
            System.out.println("🌐 Apuntando a nuevo servidor: " + BASE_URL);
        }
    }
    public static void cargarCredencialesDelServidor() {
        try {
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(BASE_URL + "/config/credenciales"))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                credenciales = mapper.readValue(response.body(), new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, String>>() {});
                System.out.println("✅ Credenciales cargadas desde el servidor de forma segura.");
            }
        } catch (Exception e) {
            System.err.println("❌ Error al descargar credenciales: " + e.getMessage());
        }
    }
    public static ArrayList<Empleado> obtenerEmpleadosDelServidor(){
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/empleados"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String json = response.body();
                return mapper.readValue(json, new TypeReference<ArrayList<Empleado>>() {});
            } else {
                System.err.println("Error del servidor Spring Boot: Código " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Falla de conexión con el backend: " + e.getMessage());
        }

        return new ArrayList<>();
    }

    public static boolean guardarEmpleadoEnServidor(Empleado empleado) {
        try {
            String jsonEmpleado = mapper.writeValueAsString(empleado);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/empleados"))
                    .header("Content-Type", "application/json") // Avisamos que enviamos JSON
                    .POST(HttpRequest.BodyPublishers.ofString(jsonEmpleado))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                return true;
            } else {
                System.err.println("Error al guardar en el servidor. Código: " + response.statusCode());
                System.err.println("Respuesta: " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Falla de conexión al intentar guardar: " + e.getMessage());
            return false;
        }
    }
    public static boolean actualizarEmpleadoEnServidor(Empleado empleado) {
        try {
            String jsonEmpleado = mapper.writeValueAsString(empleado);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/empleados"))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonEmpleado)) // Usamos PUT
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code >= 200 && code < 300) {
                return true;
            } else {
                System.err.println("Error al actualizar. Código: " + code + ". Razón: " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Falla al actualizar en servidor: " + e.getMessage());
            return false;
        }
    }

    public static boolean inactivarEmpleadoEnServidor(String idEmpleado) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/empleados/" + idEmpleado + "/inactivar"))
                    .PUT(HttpRequest.BodyPublishers.noBody()) // No enviamos JSON, solo la orden en la URL
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code >= 200 && code < 300) {
                return true;
            } else {
                System.err.println("Error al inactivar. Código: " + code + ". Razón: " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Falla al inactivar en servidor: " + e.getMessage());
            return false;
        }
    }

    public static boolean registrarPagoEnServidor(String idEmpleado) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/empleados/" + idEmpleado + "/pago"))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            // En tu método registrarPagoEnServidor:
            int code = response.statusCode();
            if (code >= 200 && code < 300) {
                return true;
            } else {
                System.err.println("Error al registrar pago. Código: " + code + ". Razón: " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Falla al registrar pago en servidor: " + e.getMessage());
            return false;
        }
    }
    public static ArrayList<Insumo> obtenerInsumoDelServidor(){
        try{
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/insumos"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String json = response.body();
                return mapper.readValue(json, new TypeReference<ArrayList<Insumo>>() {});
            } else {
                System.err.println("Error del servidor Spring Boot: Código " + response.statusCode());
            }
        }catch(Exception e){
            System.out.println("Falla de conexión: "+ e.getMessage());
        }
        return new ArrayList<>();
    }
    public static String obtenerSiguienteCodigoInsumo() {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/insumos/siguiente-codigo")).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) return response.body();
        } catch (Exception e) { System.err.println("Error código: " + e.getMessage()); }
        return "INS-001";
    }

    public static boolean guardarInsumoEnServidor(Insumo insumo, double costoInicial) {
        try {
            String json = mapper.writeValueAsString(insumo);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/insumos?costoInicial=" + costoInicial))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) { return false; }
    }

    public static boolean inactivarInsumoEnServidor(String codigo) {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/insumos/" + codigo + "/inactivar")).PUT(HttpRequest.BodyPublishers.noBody()).build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) { return false; }
    }

    public static boolean registrarLoteEnServidor(String codigo, double cantidad, double costo, LocalDate vencimiento) {
        try {
            String url = String.format("%s/insumos/%s/lotes?cantidad=%s&costo=%s&vencimiento=%s", BASE_URL, codigo, cantidad, costo, vencimiento.toString());
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).POST(HttpRequest.BodyPublishers.noBody()).build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) { return false; }
    }
    public static boolean descontarStockFEFOEnServidor(String codigo, double cantidad) {
        try {
            String url = String.format("%s/insumos/%s/descontar?cantidad=%s", BASE_URL, codigo, cantidad);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            int code = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
            return code >= 200 && code < 300;
        } catch (Exception e) {
            System.err.println("Falla al descontar stock FEFO: " + e.getMessage());
            return false;
        }
    }
    public static double darDeBajaLotesVencidosEnServidor(String codigo) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/insumos/" + codigo + "/vencidos"))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int code = response.statusCode();
            if (code >= 200 && code < 300) {

                return Double.parseDouble(response.body());
            } else {
                System.err.println("Error al limpiar lotes vencidos. Código: " + code + ". Razón: " + response.body());
                return -1;
            }
        } catch (Exception e) {
            System.err.println("Falla de conexión al limpiar vencidos: " + e.getMessage());
            return -1;
        }
    }
    public static ArrayList<Producto> obtenerProductosDelServidor() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/menu/productos"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return mapper.readValue(response.body(), new com.fasterxml.jackson.core.type.TypeReference<ArrayList<Producto>>() {});
            } else {
                System.err.println("Error al obtener menú: Código " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Falla de conexión al obtener productos: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    public static boolean guardarProductoEnServidor(Producto producto) {
        try {
            String json = mapper.writeValueAsString(producto);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/menu/productos"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                Producto productoGuardado = mapper.readValue(response.body(), Producto.class);
                producto.setId(productoGuardado.getId());
                
                return true;
            } else {
                System.err.println("Error al guardar producto. Código: " + response.statusCode());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error al guardar producto: " + e.getMessage());
            return false;
        }
    }

    public static boolean actualizarProductoEnServidor(Producto producto) {
        try {
            String json = mapper.writeValueAsString(producto);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/menu/productos"))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Error al actualizar producto: " + e.getMessage());
            return false;
        }
    }

    public static boolean inactivarProductoEnServidor(int idProducto) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/menu/productos/" + idProducto + "/inactivar"))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 200 && response.body().trim().equals("true");
        } catch (Exception e) {
            System.err.println("Error al inactivar producto: " + e.getMessage());
            return false;
        }
    }

    public static ArrayList<Promocion> obtenerPromocionesDelServidor() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/menu/promociones"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return mapper.readValue(response.body(), new com.fasterxml.jackson.core.type.TypeReference<ArrayList<Promocion>>() {});
            }
        } catch (Exception e) {
            System.err.println("Falla de conexión al obtener combos: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    public static boolean guardarPromocionEnServidor(Promocion promocion) {
        try {
            String json = mapper.writeValueAsString(promocion);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/menu/promociones"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Error al guardar promoción: " + e.getMessage());
            return false;
        }
    }
    public static boolean actualizarPromocionServidor(Promocion promocion) {
        try {
            String json = mapper.writeValueAsString(promocion);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/menu/promociones"))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Error al actualizar promoción: " + e.getMessage());
            return false;
        }
    }
    public static boolean guardarRecetaEnServidor(int idProducto, Receta receta) {
        try {
            String json = mapper.writeValueAsString(receta);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/menu/productos/" + idProducto + "/receta"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Error al guardar receta: " + e.getMessage());
            return false;
        }
    }
    public static int verificarCaducidadPromociones() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/menu/promociones/verificar-caducidad"))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return Integer.parseInt(response.body());
            } else {
                System.err.println("Error al verificar caducidad de promociones. Código: " + response.statusCode());
                return 0;
            }
        } catch (Exception e) {
            System.err.println("Falla de conexión al verificar caducidad: " + e.getMessage());
            return 0;
        }
    }

    //Facturacion
    public static int[] registrarVenta(String nombreCliente, double total, Map<Producto, Integer> carrito, String estadoFactura, String metodoPago){
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("nombreCliente", nombreCliente);
            requestBody.put("total", total);
            requestBody.put("estado", estadoFactura);
            requestBody.put("metodoPago", metodoPago);

            java.util.List<java.util.Map<String, Object>> itemsList = new java.util.ArrayList<>();
            for (java.util.Map.Entry<Producto, Integer> item : carrito.entrySet()) {
                java.util.Map<String, Object> i = new java.util.HashMap<>();
                i.put("idProducto", item.getKey().getId());
                i.put("cantidad", item.getValue());
                i.put("precio", item.getKey().getPrecio());
                i.put("tieneReceta", item.getKey().tieneReceta());
                itemsList.add(i);
            }
            requestBody.put("items", itemsList);

            String jsonVenta = mapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/ventas"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonVenta))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Integer> respuesta = mapper.readValue(response.body(), new TypeReference<Map<String, Integer>>() {});
                return new int[]{respuesta.get("numeroFactura"), respuesta.get("numeroTicket")};
            } else {
                System.err.println("Error al registrar venta en servidor. Código: " + response.statusCode());
                return null;
            }
        } catch (Exception e) {
            System.err.println("Falla al enviar venta: " + e.getMessage());
            return null;
        }
    }
    public static boolean anularVenta(int numeroFactura) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/ventas/" + numeroFactura + "/anular"))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 && response.body().trim().equals("true");
        } catch (Exception e) {
            System.err.println("Falla al anular venta: " + e.getMessage());
            return false;
        }
    }
    //Modulo comandas / cocina
    public static ArrayList<Pedido> obtenerPedidosPendientes() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/cocina/pendientes"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                java.util.List<java.util.Map<String, Object>> listaMaps = mapper.readValue(response.body(), new TypeReference<List<Map<String, Object>>>() {});
                java.util.ArrayList<Pedido> pedidos = new java.util.ArrayList<>();

                for (java.util.Map<String, Object> map : listaMaps) {
                    Pedido p = new Pedido();
                    p.setIdPedido((Integer) map.get("idPedido"));
                    p.setNumeroTicket((Integer) map.get("numeroTicket"));
                    p.setEstado((String) map.get("estado"));
                    p.setCliente((String) map.get("cliente"));
                    pedidos.add(p);
                }
                return pedidos;
            }
        } catch (Exception e) {
            System.err.println("Falla al obtener pedidos de cocina: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    public static ArrayList<String> obtenerDetallesParaCocina(int idPedido) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/cocina/pedidos/" + idPedido + "/detalles"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return mapper.readValue(response.body(), new TypeReference<ArrayList<String>>() {});
            }
        } catch (Exception e) {
            System.err.println("Falla al obtener detalles de pedido: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    public static boolean eliminarPedidoDespachado(int idPedido) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/cocina/pedidos/" + idPedido + "/despachar"))
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 && response.body().trim().equals("true");
        } catch (Exception e) {
            System.err.println("Falla al despachar pedido: " + e.getMessage());
            return false;
        }
    }

    public static boolean cancelarPedidoEnServidor(int idPedido) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/cocina/pedidos/" + idPedido + "/cancelar"))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 && response.body().trim().equals("true");
        } catch (Exception e) {
            System.err.println("Falla al cancelar pedido: " + e.getMessage());
            return false;
        }
    }

    public static java.util.ArrayList<Reserva> obtenerReservasPendientes() {
        try {
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(BASE_URL + "/reservas/pendientes"))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return mapper.readValue(response.body(), new com.fasterxml.jackson.core.type.TypeReference<java.util.ArrayList<Reserva>>() {});
            }
        } catch (Exception e) {
            System.err.println("Falla al obtener reservas: " + e.getMessage());
        }
        return new java.util.ArrayList<>();
    }

    public static boolean guardarReservaEnServidor(Reserva reserva) {
        try {
            String json = mapper.writeValueAsString(reserva);
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(BASE_URL + "/reservas"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .build();

            java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 && response.body().trim().equals("true");
        } catch (Exception e) {
            System.err.println("Falla al guardar reserva: " + e.getMessage());
            return false;
        }
    }

    public static boolean actualizarEstadoReservaEnServidor(String idReserva, String nuevoEstado) {
        try {
            String url = BASE_URL + "/reservas/" + idReserva + "/estado?estado=" + nuevoEstado.replace(" ", "%20");
            
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .PUT(java.net.http.HttpRequest.BodyPublishers.noBody())
                    .build();

            java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 && response.body().trim().equals("true");
        } catch (Exception e) {
            System.err.println("Falla al actualizar reserva: " + e.getMessage());
            return false;
        }
    }
    public static Map<String, Object> obtenerReporteConsolidado(String fechaInicio, String fechaFin) {
        try {
            String url = BASE_URL + "/reportes/consolidado";
            if (fechaInicio != null && fechaFin != null) {
                url += "?inicio=" + fechaInicio + "&fin=" + fechaFin;
            }

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return mapper.readValue(response.body(), new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
            }
        } catch (Exception e) {
            System.err.println("Falla al obtener reportes: " + e.getMessage());
        }
        return null;
    }
    public static boolean registrarEgresoEnServidor(String descripcion, double monto) {
        try {
            String url = String.format("%s/reportes/egreso?descripcion=%s&monto=%s", BASE_URL, descripcion.replace(" ", "%20"), monto);
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url)).POST(java.net.http.HttpRequest.BodyPublishers.noBody()).build();

            return httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Error al registrar egreso: " + e.getMessage());
            return false;
        }
    }
    public static Factura obtenerFacturaConDetalles(int numeroFactura) {
        Factura factura = null;
        try {
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(BASE_URL + "/ventas/" + numeroFactura))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                java.util.Map<String, Object> mapaFac = mapper.readValue(response.body(), new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});

                factura = new Factura(numeroFactura, (String) mapaFac.get("nombre_cliente"));
                factura.setNumeroTicket((Integer) mapaFac.get("numero_ticket"));
                factura.setTotal(((Number) mapaFac.get("total")).doubleValue());
                factura.setEstado((String) mapaFac.get("estado"));

                try {
                    String fechaStr = mapaFac.get("fecha_hora").toString().replace("T", " ");
                    factura.setFecha(java.time.LocalDateTime.parse(fechaStr.substring(0, 19), java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                } catch (Exception e) {}

                List<java.util.Map<String, Object>> detalles = (List<java.util.Map<String, Object>>) mapaFac.get("detalles");
                if (detalles != null) {
                    for (java.util.Map<String, Object> det : detalles) {
                        Producto p = new Producto();
                        p.setId((Integer) det.get("id_producto"));
                        p.setNombre((String) det.get("nombre"));
                        p.setPrecio(((Number) det.get("precio")).doubleValue());

                        factura.agregarProducto(p, (Integer) det.get("cantidad"));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Falla al obtener detalles de la factura: " + e.getMessage());
        }
        return factura;
    }

    public static boolean existeEgresoPorConcepto(String concepto) {
        try {
            String url = BASE_URL + "/reportes/egreso/existe?concepto=" + concepto.replace(" ", "%20");
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 && response.body().trim().equals("true");
        } catch (Exception e) {
            System.err.println("Falla al verificar egreso: " + e.getMessage());
            return false;
        }
    }
    public static boolean registrarAuditoriaEnServidor(String operador, String tipoArea, String nombreItem, String accion, double cantidad, String motivo) {
        try {
            String url = String.format("%s/reportes/auditoria?operador=%s&tipoArea=%s&nombreItem=%s&accion=%s&cantidad=%s&motivo=%s",
                    BASE_URL,
                    operador.replace(" ", "%20"),
                    tipoArea.replace(" ", "%20"),
                    nombreItem.replace(" ", "%20"),
                    accion.replace(" ", "%20"),
                    cantidad,
                    motivo.replace(" ", "%20"));

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .POST(java.net.http.HttpRequest.BodyPublishers.noBody())
                    .build();

            java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 || response.statusCode() == 201;
        } catch (Exception e) {
            System.err.println("Error subiendo auditoría al servidor: " + e.getMessage());
            return false;
        }
    }
    public static boolean isServidorDisponible() {
        try {
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(BASE_URL + "/empleados"))
                    .timeout(Duration.ofSeconds(2)) // Si no responde en 2s, asumimos que no hay internet
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public static Empleado autenticarUsuarioEnServidor(String username, String password) {
        try {
            String url = BASE_URL + "/empleados/login?username=" + username + "&password=" + password;
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && !response.body().isEmpty()) {
                return mapper.readValue(response.body(), Empleado.class);
            }
        } catch (Exception e) {
            System.err.println("Error de autenticación en la nube: " + e.getMessage());
        }
        return null;
    }
    public static boolean enviarVentaOfflineAServidor(String jsonVentaOffline) {
        try {
            // 1. Leemos el JSON offline desde SQLite y lo convertimos a un Mapa
            java.util.Map<String, Object> mapaOffline = mapper.readValue(jsonVentaOffline, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});

            java.util.Map<String, Object> requestBody = new java.util.HashMap<>();

            // 2. Mapeamos los nombres ("cliente" del offline hacia "nombreCliente" del backend)
            String nombreCli = (String) mapaOffline.getOrDefault("nombreCliente", mapaOffline.get("cliente"));
            requestBody.put("nombreCliente", nombreCli != null ? nombreCli : "Cliente Offline");

            Number total = (Number) mapaOffline.get("total");
            requestBody.put("total", total != null ? total.doubleValue() : 0.0);

            String estado = (String) mapaOffline.getOrDefault("estado", "Finalizada");
            requestBody.put("estado", estado);

            String metodoPago = (String) mapaOffline.getOrDefault("metodoPago", mapaOffline.get("metodo_pago"));
            requestBody.put("metodoPago", metodoPago != null ? metodoPago : "Efectivo");

            // 3. Mapeamos la lista de productos ("detalles" del offline hacia "items" del backend)
            java.util.List<java.util.Map<String, Object>> itemsLista = new java.util.ArrayList<>();
            java.util.List<java.util.Map<String, Object>> detallesGuardados = (java.util.List<java.util.Map<String, Object>>) mapaOffline.getOrDefault("items", mapaOffline.get("detalles"));

            if (detallesGuardados != null) {
                for (java.util.Map<String, Object> det : detallesGuardados) {
                    java.util.Map<String, Object> item = new java.util.HashMap<>();

                    int idProd = ((Number) det.getOrDefault("idProducto", det.get("id_producto"))).intValue();
                    int cant = ((Number) det.get("cantidad")).intValue();

                    item.put("idProducto", idProd);
                    item.put("cantidad", cant);

                    // Buscamos el precio y si tiene receta en la memoria RAM para completar el DTO
                    double precio = 0.0;
                    boolean receta = false;
                    if (Attizos.Backend.Attizos.App.attizos != null && Attizos.Backend.Attizos.App.attizos.getMenu() != null) {
                        for (Attizos.Backend.Attizos.Producto p : Attizos.Backend.Attizos.App.attizos.getMenu()) {
                            if (p.getId() == idProd) {
                                precio = p.getPrecio();
                                receta = p.tieneReceta();
                                break;
                            }
                        }
                    }
                    item.put("precio", det.getOrDefault("precio", precio));
                    item.put("tieneReceta", det.getOrDefault("tieneReceta", receta));

                    itemsLista.add(item);
                }
            }
            requestBody.put("items", itemsLista);

            String jsonFinalParaServidor = mapper.writeValueAsString(requestBody);

            // 5. Lo enviamos por POST a la ruta oficial /ventas de tu Spring Boot
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/ventas"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonFinalParaServidor))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                System.out.println("☁️ ✅ ¡Venta offline rescatada y guardada en PostgreSQL exitosamente!");
                return true;
            } else {
                System.err.println("❌ El servidor rechazó la venta offline. Código: " + response.statusCode() + " - " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Error al intentar subir venta offline al servidor: " + e.getMessage());
            return false;
        }
    }

}
