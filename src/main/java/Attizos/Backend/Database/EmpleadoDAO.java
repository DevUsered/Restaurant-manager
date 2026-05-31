package Attizos.Backend.Database;

import Attizos.Backend.Attizos.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class EmpleadoDAO {

    public static boolean insertarEmpleado(Empleado empleado) {
        String sql = "INSERT INTO empleados (id_empleado, nombre, cargo, sueldo, username, password_hash, estado) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'Activo')";

        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, empleado.getIdEmpleado());
            ps.setString(2, empleado.getNombre());
            ps.setString(3, empleado.getCargo());
            ps.setDouble(4, empleado.getSueldo());

            // Ya no usamos instanceof. Si tiene datos de acceso, los guardamos; si no, van nulos.
            if (empleado.tieneAccesoSistema()) {
                ps.setString(5, empleado.getUsername());
                ps.setString(6, empleado.getPasswordHash());
            } else {
                ps.setNull(5, java.sql.Types.VARCHAR);
                ps.setNull(6, java.sql.Types.VARCHAR);
            }

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al insertar empleado: " + e.getMessage());
            return false;
        }
    }

    public static ArrayList<Empleado> obtenerEmpleadosActivos() {
        ArrayList<Empleado> empleados = new ArrayList<>();
        String sql = "SELECT * FROM empleados WHERE estado = 'Activo'";

        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("id_empleado");
                String nombre = rs.getString("nombre");
                String cargo = rs.getString("cargo");
                double sueldo = rs.getDouble("sueldo");
                String user = rs.getString("username");
                String pass = rs.getString("password_hash");

                // El código se reduce a una sola línea.
                // Un solo objeto maneja al Admin, al Cajero y al Cocinero por igual.
                Empleado emp = new Empleado(id, nombre, cargo, sueldo, "Activo", user, pass);

                empleados.add(emp);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener empleados: " + e.getMessage());
        }
        return empleados;
    }

    public static boolean actualizarEmpleado(Empleado emp) {
        String sql = "UPDATE empleados SET nombre=?, cargo=?, sueldo=?, username=?, password_hash=? WHERE id_empleado=?";

        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, emp.getNombre());
            ps.setString(2, emp.getCargo());
            ps.setDouble(3, emp.getSueldo());

            if (emp.tieneAccesoSistema()) {
                ps.setString(4, emp.getUsername());
                ps.setString(5, emp.getPasswordHash());
            } else {
                ps.setNull(4, java.sql.Types.VARCHAR);
                ps.setNull(5, java.sql.Types.VARCHAR);
            }

            ps.setString(6, emp.getIdEmpleado()); // La condición WHERE

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error al actualizar empleado en BD: " + e.getMessage());
            return false;
        }
    }

    public static boolean eliminarEmpleado(String idEmpleado) {
        String sql = "UPDATE empleados SET estado = 'Inactivo' WHERE id_empleado = ?";

        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, idEmpleado);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Error al dar de baja al empleado: " + e.getMessage());
            return false;
        }
    }
}