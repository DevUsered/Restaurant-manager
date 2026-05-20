package Attizos.Backend.Attizos;


public abstract class Usuario extends Empleado {
    protected String username;
    protected String password;

    public Usuario(String id,String nombre, String cargo, double sueldo, String username, String password) {
        super(id, nombre, cargo, sueldo);
        this.username = username;
        this.password = password;
    }
    public boolean login(String user, String pass){
        return this.username.equals(user) && this.password.equals(pass);
    }
    public abstract void mostrarMenu();
    public String getUsername(){return username;}

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
