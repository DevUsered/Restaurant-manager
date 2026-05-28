package Attizos.Backend.Database;
import Attizos.Backend.Attizos.Admin;
import Attizos.Backend.Attizos.Cajero;
import Attizos.Backend.Attizos.Cocinero;
import Attizos.Backend.Attizos.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
public class LoginDAO {
    public static Usuario autenticarUsuario(String username, String password){
        String sql = "SELECT id_empleado, nombre, cargo, sueldo, username FROM empleados WHERE username = ?  AND password_hash = ?";

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

                   if(cargo.equalsIgnoreCase("Administrador")){
                       return new Admin(id, nombre, user, password, sueldo);
                   }else if(cargo.equalsIgnoreCase("Cajero")){
                       return new Cajero(id, nombre, sueldo, user, password);
                   }else if(cargo.equalsIgnoreCase("Cocinero")){
                       return new Cocinero(id, nombre, sueldo, user, password);
                   }
               }
            }
        }catch (SQLException e){
            System.out.println("Error al autenticar usuario: " + e.getMessage());
        }
        return null;
    }
}
