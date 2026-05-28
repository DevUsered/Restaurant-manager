package Attizos.Backend.Attizos;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Egreso {
    private String description;
    private double totalAmount;
    private LocalDate date;

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Egreso(String description, double totalAmount) {
        this.description = description;
        this.totalAmount = totalAmount;
        this.date = LocalDate.now();
    }

    public String getDescription() {
        return description;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public LocalDate getDate() {
        return date;
    }
}
