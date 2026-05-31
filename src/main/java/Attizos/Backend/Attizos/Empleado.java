package Attizos.Backend.Attizos;

public class Empleado {
    private String idEmpleado;
    private String nombre;
    private String cargo;
    private double sueldo;
    private String estado;

    private String username;
    private String passwordHash;

    // Constructor
    public Empleado(){}
    public Empleado(String id, String nombre, String cargo, double sueldo, String estado, String username, String passwordHash) {
        this.idEmpleado = id;
        this.nombre = nombre;
        this.cargo = cargo;
        this.sueldo = sueldo;
        this.estado = estado;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public String getIdEmpleado() {
        return idEmpleado;
    }
    public String getNombre() {
        return nombre;
    }
    public String getCargo() {
        return cargo;
    }
    public double getSueldo() {
        return sueldo;
    }
    public String getEstado() {
        return estado;
    }
    public String getUsername() {
        return username;
    }
    public String getPasswordHash() {
        return passwordHash;
    }

    //SETERS
    public void setIdEmpleado(String idEmpleado) {
        this.idEmpleado = idEmpleado;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    public void setCargo(String cargo) {
        this.cargo = cargo;
    }
    public void setSueldo(double sueldo) {        
        this.sueldo = sueldo;
    }
    public void setEstado(String estado) {
        this.estado = estado;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean tieneAccesoSistema(){
        return this.username != null && !this.username.trim().isEmpty();
    }
    @Override
    public String toString(){
        return nombre + " ("+cargo +")";
    }
}
