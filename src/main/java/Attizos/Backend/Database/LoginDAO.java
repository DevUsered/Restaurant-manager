package Attizos.Backend.Database;
import Attizos.Backend.Attizos.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
public class LoginDAO {
    public static Empleado autenticarUsuario(String username, String password){
        String sql = "SELECT id_empleado, nombre, cargo, sueldo, username FROM empleados WHERE username = ?  AND password_hash = ? AND estado = 'Activo'";

        try(Connection con = ConexionBD.getConexion()){
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1,  username);
            ps.setString(2, password);

            try(ResultSet rs = ps.executeQuery()){
               if(rs.next()){
                   String id = rs.getString("id_empleado");
                   String nombre = rs.getString("nombre");
                   String cargo = rs.getString("cargo");
                   double sueldo = rs.getDouble("sueldo");
                   String user = rs.getString("username");
                   String estado = rs.getString("estado");

                   return new Empleado(id, nombre,cargo, sueldo,estado, user, password);
               }
            }
        }catch (SQLException e){
            System.out.println("Error al autenticar usuario: " + e.getMessage());
        }
        return null;
    }
}
