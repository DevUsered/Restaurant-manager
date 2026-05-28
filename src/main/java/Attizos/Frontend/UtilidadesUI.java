package Attizos.Frontend;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ComboBox;
import javafx.scene.Node;
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
}
