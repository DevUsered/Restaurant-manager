package Attizos.Backend.Listas;

public class ListaDE <T>{
    private NodoDE<T> cabeza;
    private NodoDE<T> cola;
    private int longitud;

    public ListaDE(){
        cabeza = null;
        cola = null;
        longitud = 0;
    }
    public boolean esVacia(){
        return cabeza == null;
    }
    public void insertarAlInicio(T dato){
        NodoDE<T> nuevo = new NodoDE<>(dato);
        if (esVacia()){
            cabeza = nuevo;
            cola = nuevo;
        }else{
            nuevo.setSiguiente(cabeza);
            cabeza.setAnterior(nuevo);
            cabeza = nuevo;
        }
        longitud++;
    }
    public void insertarAlFinal(T dato){
        NodoDE<T> nuevo = new NodoDE<>(dato);
        if(esVacia()){
            cabeza = nuevo;
            cola = nuevo;
        }else{
            nuevo.setAnterior(cola);
            cola.setSiguiente(nuevo);
            cola = nuevo;
        }
        longitud++;
    }
    public void insertarEnPosicion(T dato, int pos){
        if(pos < 0 || pos > longitud){
            System.out.println("Posición no válida");
            return;
        }
        if(pos == 0){
            insertarAlInicio(dato);
            return; // Cortamos la ejecución aquí
        }else{
            NodoDE<T> ac = cabeza;
            for (int i = 0; i < pos -1 ; i++){
                ac = ac.getSiguiente();
            }
            NodoDE<T> nuevo = new NodoDE<>(dato);
            nuevo.setSiguiente(ac.getSiguiente());
            nuevo.setAnterior(ac);
            ac.setSiguiente(nuevo);
            if (nuevo.getSiguiente() != null){
                nuevo.getSiguiente().setAnterior(nuevo);
            } else {
                cola = nuevo;
            }
            longitud++; // Solo incrementamos si se insertó en el else
        }
    }

    public T eliminarElInicio() {
        if (esVacia()) {
            System.out.println("La lista está vacía");
            return null;
        }

        T datoEliminado = cabeza.getDato(); // Guardamos el dato antes de desconectar

        if (cabeza == cola) { // Si solo hay un elemento
            cabeza = null;
            cola = null;
        } else {
            cabeza = cabeza.getSiguiente();
            cabeza.setAnterior(null);
        }
        longitud--;
        return datoEliminado;
    }

    // 2. ELIMINAR EN POSICIÓN ESPECÍFICA
    public void eliminarEnPosicion(int pos) {
        if (pos < 0 || pos >= longitud) {
            System.out.println("Posición no válida");
            return;
        }

        if (pos == 0) {
            eliminarElInicio();
            return;
        } else {
            NodoDE<T> ac = cabeza;
            // Llegamos al nodo ANTERIOR al que queremos eliminar
            for (int i = 0; i < pos - 1; i++) {
                ac = ac.getSiguiente();
            }

            NodoDE<T> aEliminar = ac.getSiguiente();

            // Puenteamos el nodo a eliminar
            ac.setSiguiente(aEliminar.getSiguiente());

            if (aEliminar.getSiguiente() != null) {
                // Si NO es el último nodo, conectamos el de adelante hacia atrás
                aEliminar.getSiguiente().setAnterior(ac);
            } else {
                // Si SÍ es el último nodo, actualizamos la 'cola'
                cola = ac;
            }
            longitud--;
        }
    }

    // 3. ELIMINAR POR VALOR (Útil para borrar Productos o Pedidos específicos)
    public void eliminarPorValor(T val) {
        if (esVacia()) {
            System.out.println("La lista está vacía");
            return;
        }

        NodoDE<T> ac = cabeza;

        while (ac != null) {
            NodoDE<T> siguiente = ac.getSiguiente(); // Guardamos el siguiente por si borramos 'ac'

            boolean iguales = (ac.getDato() == null) ? (val == null) : ac.getDato().equals(val);

            if (iguales) {
                if (ac == cabeza) {
                    eliminarElInicio(); // Ya reduce la longitud por dentro
                } else if (ac == cola) {
                    // Lógica para borrar el último sin llamar a otro método para evitar errores
                    cola = cola.getAnterior();
                    cola.setSiguiente(null);
                    longitud--;
                } else {

                    ac.getAnterior().setSiguiente(ac.getSiguiente());
                    ac.getSiguiente().setAnterior(ac.getAnterior());
                    longitud--;
                }
                return;
            }
            ac = siguiente;
        }
    }
    public boolean removerPorValor(T val){
        if(esVacia()){
            return false;
        };
        NodoDE<T> ac = cabeza;
        while (ac != null){
            NodoDE<T> siguiente = ac.getSiguiente();
            boolean iguales = (ac.getDato() == null) ? (val == null) : ac.getDato().equals(val);
            if(iguales){
                if(ac == cabeza){
                    eliminarElInicio();
                } else if (ac == cola) {
                    cola = cola.getAnterior();
                    cola.setSiguiente(null);
                    longitud--;
                }else{
                    ac.getAnterior().setSiguiente(ac.getSiguiente());
                    ac.getSiguiente().setAnterior(ac.getAnterior());
                    longitud --;
                }
                return true;
            }
            ac = siguiente;
        }
        return false;
    }


    public boolean buscar(T val){
        NodoDE<T> ac = cabeza;
        while(ac != null){
            boolean iguales = (ac.getDato()) == null ? (val == null) : ac.getDato().equals(val);
            if(iguales){
                return true;
            }
            ac = ac.getSiguiente();
        }
        return false;
    }
    public void modificar(T val, int pos){
        if(pos < 0 || pos >= longitud){
            System.out.println("Posición no válida");
            return;
        }
        NodoDE<T> ac = cabeza;
        for(int i = 0; i < pos; i++){
            ac = ac.getSiguiente();
        }
        ac.setDato(val);
    }
    public void vaciar(){
        cabeza = null;
        cola = null;
        longitud = 0;
    }
    public void invertir(){
        if(esVacia() || cabeza == cola) return;
        NodoDE<T> ac = cabeza;
        NodoDE<T> temp = null;
        while (ac != null){
            temp = ac.getAnterior();
            ac.setAnterior(ac.getSiguiente());
            ac.setSiguiente(temp);
            ac = ac.getAnterior();
        }
        temp = cabeza;
        cabeza = cola;
        cola = temp;
    }
    public void mostrarLista() {
        NodoDE<T> aux = cabeza;
        if (aux == null) {
            System.out.println("La lista está vacía.");
            return;
        }
        while (aux != null) {
            System.out.println(aux.getDato().toString());
            aux = aux.getSiguiente();
        }
    }
    public int getLongitud(){
        return longitud;
    }
    public NodoDE<T> getCabeza(){
        return cabeza;
    }
    public NodoDE<T> getCola(){
        return cola;
    }
}
