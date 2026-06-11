package Attizos.Frontend;
import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ComboBox;
import javafx.scene.Node;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UtilidadesUI {

    public static void formatearDatePicker(DatePicker dp) {
        if (dp != null) {
            // El -fx-base: #FFFFFF fuerza a que el calendario interno sea blanco
            dp.setStyle("-fx-base: #FFFFFF; -fx-background-color: #F5F5F5; -fx-border-color: #DDDDDD; -fx-border-radius: 5;");

            // Forzamos que el texto escrito también sea oscuro
            dp.getEditor().setStyle("-fx-text-fill: #111111; -fx-background-color: transparent;");
        }
    }
    public static void formatearComboBoxClaro(ComboBox<?> combo) {
        if (combo != null) {
            combo.setStyle("-fx-base: #FFFFFF; -fx-background-color: #F5F5F5; -fx-border-color: #DDDDDD; -fx-border-radius: 5;");
        }
    }
    public static void saltarConEnter(Node campoAc, Node sigCamp){
        campoAc.setOnKeyPressed(event ->{
            switch (event.getCode()) {
                case ENTER:
                    sigCamp.requestFocus();
                    break;
                default:
                    break;
            }
        });
    }
    public static <T> void ejecutarTareaAsincrona(Node nodoRaiz, Supplier<T> tareaFondo, Consumer<T> accionUI){
        Scene scene = nodoRaiz.getScene();
        if(scene != null) scene.setCursor(Cursor.WAIT);
        nodoRaiz.setDisable(true);

        CompletableFuture.supplyAsync(tareaFondo)
                .whenCompleteAsync((resultado, error) ->{
                    if(scene != null) scene.setCursor(Cursor.DEFAULT);
                    nodoRaiz.setDisable(false);

                    if(error != null){
                        AlertaPersonalizada.mostrarAlerta("Error", "Ocurrió un error: " + error.getMessage(), Alert.AlertType.ERROR);
                    }else{
                        accionUI.accept(resultado);
                    }
                }, Platform::runLater);
    }
}
