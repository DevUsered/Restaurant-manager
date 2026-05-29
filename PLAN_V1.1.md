# Hoja de Ruta: Sistema Attizos v1.1

## Objetivo de la versión
Implementar una arquitectura **Offline-First** con caché local (SQLite) y optimizar el rendimiento del servidor principal (PostgreSQL), garantizando que el sistema siga operativo sin conexión a internet para los módulos no críticos.

---

## 📅 Fase 1: Preparación del entorno local (Día 1)

- [ ] Descargar la librería `sqlite-jdbc.jar` y agregarla a las dependencias del proyecto (IDE).
- [ ] Modificar la lógica de directorios usando `System.getenv("APPDATA")` para establecer una ruta de escritura permitida en Windows.
- [ ] Programar la creación automática de la carpeta `Attizos` dentro de `%APPDATA%` si no existe en el cliente.
- [ ] Crear la clase `ConexionSQLite.java` con métodos para generar/conectar el archivo local `attizos_cache.db` en esa ruta.

## 🗃️ Fase 2: Caché de datos maestros (Días 2–3)

- [ ] Generar tablas en SQLite: `empleados`, `insumos_catalogo`, `productos` (estructuras simplificadas, solo lectura).
- [ ] Programar rutina de sincronización al iniciar el sistema:
    - Si hay conexión a PostgreSQL → vaciar tablas locales (`TRUNCATE`) y recargar con datos frescos del servidor.
- [ ] Modificar los DAO (`LoginDAO`, `ProductoDAO`, etc.) para que la interfaz gráfica lea **exclusivamente desde SQLite** (usando listas/Map en memoria).
- [ ] **Prueba:** Desconectar internet, iniciar sesión y verificar que el menú principal cargue con datos del último sincronizado.

## 🛒 Fase 3: Módulo de ventas offline (Días 4–5)

- [ ] Crear tabla `ventas_pendientes` en SQLite para almacenar facturas y detalles temporalmente.
- [ ] Modificar el bloque de cobro en caja:
    - Envolver el `INSERT` a PostgreSQL en un `try-catch`.
    - Si ocurre `SQLException` (falta de red) → guardar la venta en SQLite y mostrar notificación: *"Venta guardada en modo offline"*.
- [ ] Desarrollar un **hilo en segundo plano** (o botón manual de “Sincronizar”) que:
    - Lea registros de `ventas_pendientes`.
    - Los envíe a PostgreSQL cuando vuelva la conexión.
    - Los elimine de SQLite tras confirmación exitosa.
- [ ] Asegurar que el ID de venta offline se gestione sin conflictos (usar `id_local` + `id_remoto` o UUID).

## ⚡ Fase 4: Optimización y despliegue (Día 6)

- [ ] Ejecutar `EXPLAIN ANALYZE` en pgAdmin para identificar consultas lentas.
- [ ] Crear índices en PostgreSQL para columnas críticas:
    - `fecha_emision` (facturas)
    - `id_producto` (recetas, movimientos de inventario)
- [ ] Empaquetar la librería SQLite dentro del nuevo archivo `.exe`.
- [ ] Actualizar `AppVersion=1.1` en el script de Inno Setup y compilar el instalador final.

---

## ⚠️ Regla de desarrollo (importante)

> Los módulos que afectan el **inventario físico** (descuento de lotes en tiempo real) y la **cola de cocina** **no deben pasar por caché local**.  
> Deben intentar conectarse siempre a PostgreSQL para evitar desfases operativos. Si no hay red, se debe denegar la operación y notificar al usuario.

---

## 📊 Criterios de aceptación de la versión 1.1

| Criterio | Estado esperado |
|----------|----------------|
| El sistema arranca sin conexión a internet | ✅ OK |
| Los datos maestros (empleados, productos, insumos) se muestran correctamente offline | ✅ OK |
| Una venta registrada sin red se guarda localmente y se sincroniza automáticamente al recuperar la conexión | ✅ OK |
| Las operaciones de inventario y cocina requieren conexión activa | ✅ OK |
| Tiempo de respuesta de consultas críticas en PostgreSQL mejora en al menos un 30% | ✅ OK |

---

## 📌 Notas para el equipo

- Usar siempre `SQLite` como **fuente de verdad para consultas de solo lectura**.
- La sincronización de ventas pendientes debe ser **idempotente** (evitar duplicados).
- Incluir logging para eventos de conmutación online/offline.
- Actualizar el `README.md` del repositorio con instrucciones para empaquetar `sqlite-jdbc`.