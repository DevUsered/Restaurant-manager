package Attizos.Backend.AI;

public class AuditoriaLocal {
    public static String auditarIngreso(String nombre, String categoria, double cantidad, String unidad, long diasVencimiento){
        String alertaBasura = detectarTextoBasura(nombre, categoria);
        if(alertaBasura !=  null) return alertaBasura;

        String alertaCantidad = validarLimitesCantidad(cantidad, unidad);
        if(alertaBasura != null) return alertaCantidad;

        String alertaBiologica = validarTiempoDeVida(nombre, categoria, diasVencimiento);
        if(alertaBasura != null) return alertaBiologica;

        return "OK";
    }
    public static String auditarCreacion(String nombre, String categoria, String unidad, double min, double max, long diasVencimiento){
        if(min > max && max > 0){
            return "ALERTA: Incoherencia matemática. El stock mínimo (" + min + "( no puede ser mayor al stock máximo (" + max + ").";
        }
            String alertaBasura = detectarTextoBasura(nombre, categoria);
            if (alertaBasura != null) return alertaBasura;

            String alertaFisica = validarSentidoFisico(nombre, unidad);
            if (alertaFisica != null) return alertaFisica;

            String alertaMaximo = validarLimitesCantidad(max, unidad);
            if (alertaMaximo != null) return alertaMaximo;

            String alertaBiologia = validarTiempoDeVida(nombre, categoria, diasVencimiento);
            if (alertaBiologia != null) return alertaBiologia;

            return "OK";
    }
    private static String detectarTextoBasura(String nombre, String categoria){
        String texto = (nombre + " " + categoria).toLowerCase();
        if(texto.matches(".*([a-z])\\1{3,}.*")){
            return "ALERTA: El nombre contiene caracteres repetidos.";
        }
        String[] palabras = texto.split("\\s+");
        for (String palabra : palabras) {
            if (palabra.length() >= 4 && !palabra.matches(".*[aeiouáéíóú].*")) {
                return "ALERTA: El nombre '" + palabra + "' parece texto basura generado al azar.";
            }
        }
        return null;
    }
    private static String validarSentidoFisico(String nombre, String unidad) {
        String nom = nombre.toLowerCase();
        String uni = unidad.toLowerCase();

        boolean esLiquido = nom.contains("agua") || nom.contains("aceite") || nom.contains("leche") ||
                nom.contains("jugo") || nom.contains("refresco") || nom.contains("salsa") || nom.contains("vinagre");

        boolean esSolido = nom.contains("arroz") || nom.contains("pollo") || nom.contains("carne") ||
                nom.contains("papa") || nom.contains("sal") || nom.contains("azucar") || nom.contains("harina");

        if (esLiquido && (uni.equals("kg") || uni.equals("g") || uni.equals("lb"))) {
            return "ALERTA: Está asignando una unidad de peso sólido (" + uni + ") a un producto que parece líquido (" + nombre + ").";
        }
        if (esSolido && (uni.equals("lt") || uni.equals("ml"))) {
            return "ALERTA: Está asignando una unidad de volumen líquido (" + uni + ") a un producto que parece sólido (" + nombre + ").";
        }
        return null;
    }
    private static String validarLimitesCantidad(double cantidad, String unidad) {
        String uni = unidad.toLowerCase();
        if ((uni.equals("kg") || uni.equals("lt")) && cantidad > 5000) {
            return "ALERTA: La cantidad (" + cantidad + " " + uni + ") parece exageradamente alta para un local comercial estándar.";
        }
        if ((uni.equals("g") || uni.equals("ml")) && cantidad > 5000000) { // 5 Toneladas
            return "ALERTA: La cantidad en gramos/ml es ridículamente masiva. Verifique los ceros digitados.";
        }
        if (uni.equals("und") && cantidad > 20000) {
            return "ALERTA: Registrar más de 20,000 unidades de golpe es inusual. Verifique la cantidad.";
        }
        return null;
    }
    private static String validarTiempoDeVida(String nombre, String categoria, long diasVencimiento) {
        if (diasVencimiento > 20000) {
            return null; // Es "No caduca" (Año 2099+)
        }

        String texto = (nombre + " " + categoria).toLowerCase();

        if (texto.contains("carne") || texto.contains("pollo") || texto.contains("pescado") || texto.contains("res") || texto.contains("cerdo") || texto.contains("carne")) {
            if (diasVencimiento > 365) return "ALERTA: Es biológicamente inusual que un producto cárnico tenga más de un año de vida útil, incluso congelado.";
        }
        if (texto.contains("verdura") || texto.contains("fruta") || texto.contains("tomate") || texto.contains("lechuga") || texto.contains("cebolla")) {
            if (diasVencimiento > 90) return "ALERTA: Los productos vegetales frescos rara vez duran más de 90 días.";
        }
        if (texto.contains("lacteo") || texto.contains("queso") || texto.contains("leche") || texto.contains("crema")) {
            if (diasVencimiento > 180) return "ALERTA: Los lácteos suelen caducar en menos de 6 meses. Verifique el empaque.";
        }
        if (diasVencimiento > 3650) { // 10 años
            return "ALERTA: El producto tiene más de 10 años de caducidad. Si no caduca, marque la casilla 'No Caduca'.";
        }
        return null;
    }
}
