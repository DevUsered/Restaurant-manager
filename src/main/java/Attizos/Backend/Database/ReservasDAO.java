package Attizos.Backend.Database;

import Attizos.Backend.Attizos.Reserva;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReservasDAO {

    public static ObservableList<Reserva> obtenerReservasPendientesYLimpiar(){
        ObservableList<Reserva> lista = FXCollections.observableArrayList();

        String sqlLimpieza = "UPDATE reservas SET estado = 'Expirada' " +
                "WHERE estado = 'Pendiente' AND fecha_hora < (CURRENT_TIMESTAMP - INTERVAL '15 minutes')";
        String sqlSelect = "SELECT id_reserva, nombre_cliente, telefono, cantidad_personas, fecha_hora, observaciones, estado "+
                "FROM reservas WHERE estado = 'Pendiente' ORDER BY fecha_hora ASC";
        try(Connection con = ConexionBD.getConexion()){
            try(Statement st = con.createStatement()){
                st.executeUpdate(sqlLimpieza);
            }
            try(PreparedStatement ps = con.prepareStatement(sqlSelect);
                ResultSet rs = ps.executeQuery()){

                while(rs.next()){
                    String id = rs.getString("id_reserva");
                    String cliente = rs.getString("nombre_cliente");
                    String telefono = rs.getString("telefono");
                    int personas = rs.getInt("cantidad_personas");
                    Timestamp ts = rs.getTimestamp("fecha_hora");
                    String obs = rs.getString("observaciones");

                    LocalDateTime fechaHora = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

                    Reserva r = new Reserva(id, cliente, telefono, personas, fechaHora, obs);
                    r.setEstado(rs.getString("estado"));
                    lista.add(r);
                }
            }

        }catch (SQLException e) {
            System.err.println("❌ Error al obtener reservas: " + e.getMessage());
        }
        return lista;
    }
    public static boolean insertarReserva(Reserva r) {
        String sql = "INSERT INTO reservas (id_reserva, nombre_cliente, telefono, cantidad_personas, fecha_hora, observaciones, estado) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'Pendiente')";

        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, r.getId());
            ps.setString(2, r.getNombreCliente());
            ps.setString(3, r.getTelefono());
            ps.setInt(4, r.getCantidadPersonas());
            ps.setTimestamp(5, Timestamp.valueOf(r.getFecha()));
            ps.setString(6, r.getObservaciones());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Error al insertar reserva: " + e.getMessage());
            return false;
        }
    }
    public static boolean actualizarEstadoReserva(String idReserva, String nuevoEstado) {
        String sql = "UPDATE reservas SET estado = ? WHERE id_reserva = ?";

        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nuevoEstado);
            ps.setString(2, idReserva);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Error al actualizar estado de la reserva: " + e.getMessage());
            return false;
        }
    }
}
