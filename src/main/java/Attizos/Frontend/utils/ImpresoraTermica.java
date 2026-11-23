package Attizos.Frontend.utils;

import Attizos.Backend.Attizos.*;

import javax.print.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ImpresoraTermica {

    // Ancho en caracteres por línea según el rollo de papel:
    // 58mm = 32 caracteres | 80mm = 42 o 48 caracteres (42 es el estándar seguro en 80mm)
    public static final int ANCHO_58MM = 32;
    public static final int ANCHO_80MM = 42;

    // Comandos ESC/POS estándar (Epson, Xprinter, 3nStar, Bixolon, genéricas)
    private static final byte[] ESC_INIT        = {0x1B, 0x40};       // Reset impresora
    private static final byte[] ESC_CP850       = {0x1B, 0x74, 0x02}; // Tabla caracteres CP850 (español)
    private static final byte[] ALIGN_LEFT      = {0x1B, 0x61, 0x00};
    private static final byte[] ALIGN_CENTER    = {0x1B, 0x61, 0x01};
    private static final byte[] ALIGN_RIGHT     = {0x1B, 0x61, 0x02};
    private static final byte[] BOLD_ON         = {0x1B, 0x45, 0x01};
    private static final byte[] BOLD_OFF        = {0x1B, 0x45, 0x00};
    private static final byte[] SIZE_NORMAL     = {0x1D, 0x21, 0x00}; // 1x1
    private static final byte[] SIZE_DOUBLE_H   = {0x1D, 0x21, 0x01}; // 1x ancho, 2x alto
    private static final byte[] SIZE_DOUBLE_WH  = {0x1D, 0x21, 0x11}; // 2x ancho, 2x alto
    private static final byte[] SIZE_TURNO      = {0x1D, 0x21, 0x22}; // 3x ancho, 3x alto (para #001)
    private static final byte[] CUT_PARTIAL     = {0x1D, 0x56, 0x42, 0x00}; // Avanza y corta papel

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Imprime el ticket de cliente (equivalente a ticketCliente en Ticket.fxml).
     */
    public static boolean imprimirTicketCliente(Factura factura, int turno, String metodoPago, String nombreImpresora, int ancho) {
        try {
            byte[] data = construirBytesCliente(factura, turno, metodoPago, ancho);
            return enviarAImpresora(data, nombreImpresora);
        } catch (Exception e) {
            System.err.println("❌ Error al imprimir ticket cliente: " + e.getMessage());
            return false;
        }
    }

    /**
     * Imprime la comanda de cocina/caja (equivalente a ticketCocina en Ticket.fxml).
     */
    public static boolean imprimirTicketCocina(Factura factura, String nombreCajero, int turno, String nombreImpresora, int ancho) {
        try {
            byte[] data = construirBytesCocina(factura, nombreCajero, turno, ancho);
            return enviarAImpresora(data, nombreImpresora);
        } catch (Exception e) {
            System.err.println("❌ Error al imprimir ticket cocina: " + e.getMessage());
            return false;
        }
    }

    /**
     * Útil para depurar el alineado exacto en consola sin gastar papel térmico.
     */
    public static String vistaPreviaConsola(Factura factura, String nombreCajero, int turno, String metodoPago, int ancho) {
        StringBuilder sb = new StringBuilder();
        String sep = "-".repeat(ancho);
        String nombreNegocio = App.getNombre() != null ? App.getNombre().toUpperCase() : "ATTIZOS";
        String fechaStr = obtenerFechaFormateada(factura);

        sb.append("=== VISTA PREVIA CLIENTE (").append(ancho).append(" cols) ===\n");
        sb.append(centrarTexto(nombreNegocio, ancho)).append("\n");
        sb.append(centrarTexto("SU TURNO:", ancho)).append("\n");
        sb.append(centrarTexto("#" + String.format("%03d", turno), ancho)).append("\n");
        sb.append("Fecha:   ").append(fechaStr).append("\n");
        sb.append("Cliente: ").append(factura.getNombreCliente()).append("\n");
        sb.append("Pago:    ").append(metodoPago).append("\n");
        sb.append(sep).append("\n");
        sb.append(formatearEncabezadoTabla(ancho)).append("\n");
        sb.append(sep).append("\n");
        for (DetalleFactura df : factura.getDetalles()) {
            for (String linea : formatearFilaProducto(df.getCantidad(), df.getProducto().getNombreConTamano(), df.getSubtotal(), ancho)) {
                sb.append(linea).append("\n");
            }
        }
        sb.append(sep).append("\n");
        sb.append(alinearIzquierdaDerecha("TOTAL PAGADO:", String.format("Bs. %.2f", factura.getTotal()), ancho)).append("\n\n");
        sb.append(centrarTexto("¡Gracias por su preferencia!", ancho)).append("\n");
        return sb.toString();
    }

    private static byte[] construirBytesCliente(Factura factura, int turno, String metodoPago, int ancho) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String sep = "-".repeat(ancho);
        String nombreNegocio = App.getNombre() != null ? App.getNombre().toUpperCase() : "ATTIZOS";
        String fechaStr = obtenerFechaFormateada(factura);

        out.write(ESC_INIT);
        out.write(ESC_CP850);

        // Encabezado centrado
        out.write(ALIGN_CENTER);
        out.write(BOLD_ON);
        out.write(SIZE_DOUBLE_H);
        escribirLinea(out, nombreNegocio);
        out.write(SIZE_NORMAL);
        out.write(BOLD_OFF);
        escribirLinea(out, "");

        escribirLinea(out, "SU TURNO:");
        out.write(BOLD_ON);
        out.write(SIZE_TURNO);
        escribirLinea(out, "#" + String.format("%03d", turno));
        out.write(SIZE_NORMAL);
        out.write(BOLD_OFF);
        escribirLinea(out, "");

        // Datos del cliente
        out.write(ALIGN_LEFT);
        escribirLinea(out, "Fecha:   " + fechaStr);
        escribirLinea(out, "Cliente: " + limpiarTexto(factura.getNombreCliente()));
        if (metodoPago != null && !metodoPago.isEmpty()) {
            escribirLinea(out, "Pago:    " + metodoPago);
        }

        escribirLinea(out, sep);
        out.write(BOLD_ON);
        escribirLinea(out, formatearEncabezadoTabla(ancho));
        out.write(BOLD_OFF);
        escribirLinea(out, sep);

        // Detalle de productos (con tamaño)
        for (DetalleFactura df : factura.getDetalles()) {
            String nombreProd = df.getProducto().getNombreConTamano();
            for (String linea : formatearFilaProducto(df.getCantidad(), nombreProd, df.getSubtotal(), ancho)) {
                escribirLinea(out, linea);
            }
        }

        escribirLinea(out, sep);

        // Total
        out.write(BOLD_ON);
        out.write(SIZE_DOUBLE_H);
        escribirLinea(out, alinearIzquierdaDerecha("TOTAL PAGADO:", String.format("Bs. %.2f", factura.getTotal()), ancho));
        out.write(SIZE_NORMAL);
        out.write(BOLD_OFF);

        // Pie de página
        escribirLinea(out, "");
        out.write(ALIGN_CENTER);
        escribirLinea(out, "¡Gracias por su preferencia!");
        escribirLinea(out, "");
        escribirLinea(out, "");
        escribirLinea(out, "");

        out.write(CUT_PARTIAL);
        return out.toByteArray();
    }

    private static byte[] construirBytesCocina(Factura factura, String nombreCajero, int turno, int ancho) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String sep = "-".repeat(ancho);
        String nombreNegocio = App.getNombre() != null ? App.getNombre().toUpperCase() : "ATTIZOS";
        String fechaStr = obtenerFechaFormateada(factura);

        out.write(ESC_INIT);
        out.write(ESC_CP850);

        // Encabezado cocina
        out.write(ALIGN_CENTER);
        out.write(BOLD_ON);
        out.write(SIZE_DOUBLE_H);
        escribirLinea(out, nombreNegocio);
        out.write(SIZE_NORMAL);
        escribirLinea(out, "** COMANDA DE COCINA **");
        out.write(SIZE_DOUBLE_WH);
        escribirLinea(out, "TICKET #" + String.format("%03d", turno));
        out.write(SIZE_NORMAL);
        out.write(BOLD_OFF);
        escribirLinea(out, "");

        // Metadatos
        out.write(ALIGN_LEFT);
        escribirLinea(out, "Fecha:   " + fechaStr);
        escribirLinea(out, "Usuario: " + limpiarTexto(nombreCajero));
        escribirLinea(out, "Cliente: " + limpiarTexto(factura.getNombreCliente()));

        escribirLinea(out, sep);
        out.write(BOLD_ON);
        escribirLinea(out, formatearEncabezadoTabla(ancho));
        out.write(BOLD_OFF);
        escribirLinea(out, sep);

        // Productos en cocina (destacados y con desglose de combos)
        for (DetalleFactura df : factura.getDetalles()) {
            Producto p = df.getProducto();
            out.write(BOLD_ON);
            for (String linea : formatearFilaProducto(df.getCantidad(), p.getNombreConTamano(), df.getSubtotal(), ancho)) {
                escribirLinea(out, linea);
            }
            out.write(BOLD_OFF);

            // Si es promoción/combo, imprimir lo que cocina debe preparar adentro
            if (p.isPromocion() && p instanceof Promocion promo && promo.getProductosCombo() != null) {
                for (DetalleCombo dc : promo.getProductosCombo()) {
                    int cantTotalItem = dc.getCantidad() * df.getCantidad();
                    String detalleInterno = "  -> " + cantTotalItem + "x " + dc.getProducto().getNombreConTamano();
                    for (String subLinea : envolverTexto(detalleInterno, ancho)) {
                        escribirLinea(out, subLinea);
                    }
                }
            }
        }

        escribirLinea(out, sep);

        out.write(BOLD_ON);
        escribirLinea(out, alinearIzquierdaDerecha("TOTAL A PAGAR:", String.format("Bs. %.2f", factura.getTotal()), ancho));
        out.write(BOLD_OFF);

        escribirLinea(out, "");
        escribirLinea(out, "");
        escribirLinea(out, "");

        out.write(CUT_PARTIAL);
        return out.toByteArray();
    }

    /**
     * Busca la impresora por nombre parcial (ej. "POS-80", "Xprinter") o usa la predeterminada del SO.
     */
    private static boolean enviarAImpresora(byte[] rawBytes, String nombreImpresora) throws PrintException {
        PrintService servicio = buscarImpresora(nombreImpresora);
        if (servicio == null) {
            System.err.println("❌ No se encontró ninguna impresora térmica disponible.");
            return false;
        }
        DocPrintJob job = servicio.createPrintJob();
        Doc doc = new SimpleDoc(rawBytes, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
        job.print(doc, null);
        return true;
    }

    public static PrintService buscarImpresora(String nombreParcial) {
        if (nombreParcial != null && !nombreParcial.trim().isEmpty()) {
            PrintService[] servicios = PrintServiceLookup.lookupPrintServices(null, null);
            String filtro = nombreParcial.trim().toLowerCase();
            for (PrintService ps : servicios) {
                if (ps.getName().toLowerCase().contains(filtro)) {
                    return ps;
                }
            }
        }
        return PrintServiceLookup.lookupDefaultPrintService();
    }

    public static List<String> listarImpresorasInstaladas() {
        List<String> nombres = new ArrayList<>();
        for (PrintService ps : PrintServiceLookup.lookupPrintServices(null, null)) {
            nombres.add(ps.getName());
        }
        return nombres;
    }

    // ==================== FORMATEO DE COLUMNAS ====================

    private static String formatearEncabezadoTabla(int ancho) {
        int colCant = 4;
        int colSub = 9;
        int colProd = Math.max(10, ancho - colCant - colSub - 2);
        return String.format("%-" + colCant + "s %-" + colProd + "s %" + colSub + "s", "CANT", "PRODUCTO", "SUBTOTAL");
    }

    private static List<String> formatearFilaProducto(int cantidad, String nombre, double subtotal, int ancho) {
        int colCant = 4;
        int colSub = 9;
        int colProd = Math.max(10, ancho - colCant - colSub - 2);

        String cantStr = String.valueOf(cantidad);
        String subStr = String.format("%.2f", subtotal);
        List<String> lineasNombre = envolverTexto(limpiarTexto(nombre), colProd);
        List<String> resultado = new ArrayList<>();

        for (int i = 0; i < lineasNombre.size(); i++) {
            if (i == 0) {
                resultado.add(String.format("%-" + colCant + "s %-" + colProd + "s %" + colSub + "s",
                        cantStr, lineasNombre.get(i), subStr));
            } else {
                resultado.add(String.format("%-" + colCant + "s %-" + colProd + "s",
                        "", lineasNombre.get(i)));
            }
        }
        return resultado;
    }

    private static String alinearIzquierdaDerecha(String izq, String der, int ancho) {
        int espacios = ancho - izq.length() - der.length();
        if (espacios < 1) {
            return izq + " " + der;
        }
        return izq + " ".repeat(espacios) + der;
    }

    private static String centrarTexto(String texto, int ancho) {
        if (texto.length() >= ancho) return texto;
        int pad = (ancho - texto.length()) / 2;
        return " ".repeat(pad) + texto;
    }

    private static List<String> envolverTexto(String texto, int maxAncho) {
        List<String> lineas = new ArrayList<>();
        if (texto == null || texto.isEmpty()) {
            lineas.add("");
            return lineas;
        }
        String[] palabras = texto.trim().split("\\s+");
        StringBuilder actual = new StringBuilder();

        for (String palabra : palabras) {
            if (palabra.length() > maxAncho) {
                if (actual.length() > 0) {
                    lineas.add(actual.toString());
                    actual.setLength(0);
                }
                int idx = 0;
                while (idx < palabra.length()) {
                    int fin = Math.min(idx + maxAncho, palabra.length());
                    lineas.add(palabra.substring(idx, fin));
                    idx = fin;
                }
            } else if (actual.length() == 0) {
                actual.append(palabra);
            } else if (actual.length() + 1 + palabra.length() <= maxAncho) {
                actual.append(" ").append(palabra);
            } else {
                lineas.add(actual.toString());
                actual.setLength(0);
                actual.append(palabra);
            }
        }
        if (actual.length() > 0) {
            lineas.add(actual.toString());
        }
        return lineas;
    }

    private static String obtenerFechaFormateada(Factura factura) {
        LocalDateTime fechaSegura = factura.getFecha() != null ? factura.getFecha() : LocalDateTime.now();
        return fechaSegura.format(FORMATO_FECHA);
    }

    private static String limpiarTexto(String s) {
        return s == null ? "" : s.replace("\n", " ").replace("\r", " ").trim();
    }

    private static void escribirLinea(ByteArrayOutputStream out, String texto) throws IOException {
        out.write(codificarCP850(texto));
        out.write('\n');
    }

    /**
     * Codificador CP850 sin dependencia de jdk.charsets (seguro para JREs empaquetados).
     */
    private static byte[] codificarCP850(String texto) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(texto.length());
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case 'á' -> buf.write(0xA0);
                case 'é' -> buf.write(0x82);
                case 'í' -> buf.write(0xA1);
                case 'ó' -> buf.write(0xA2);
                case 'ú' -> buf.write(0xA3);
                case 'Á' -> buf.write(0xB5);
                case 'É' -> buf.write(0x90);
                case 'Í' -> buf.write(0xD6);
                case 'Ó' -> buf.write(0xE0);
                case 'Ú' -> buf.write(0xE9);
                case 'ñ' -> buf.write(0xA4);
                case 'Ñ' -> buf.write(0xA5);
                case 'ü' -> buf.write(0x81);
                case 'Ü' -> buf.write(0x9A);
                case '¡' -> buf.write(0xAD);
                case '¿' -> buf.write(0xA8);
                case '°' -> buf.write(0xF8);
                default  -> buf.write(c < 128 ? c : '?');
            }
        }
        return buf.toByteArray();
    }
}