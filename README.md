# TriageX

Sistema de urgencias y triaje simulado (MVP) desarrollado como proyecto académico de Estructuras de Datos.

TriageX permite registrar pacientes, consultar su información y administrar la atención mediante dos órdenes de organización: prioridad clínica y orden de llegada. La prioridad va de **NIVEL 1** (mayor urgencia) a **NIVEL 5**. El estado del paciente es independiente de la prioridad.

## Integrantes

- Fredy Manuel
- Juan de Dios
- Santiago de León

## Funcionalidades principales

- Registrar y listar pacientes.
- Buscar pacientes por código y consultar su información.
- Validar códigos duplicados sin distinguir mayúsculas/minúsculas ni espacios externos.
- Administrar la cola por prioridad y consultar el orden FIFO de llegada.
- Pasar pacientes por los estados `PENDIENTE`, `EN_ATENCION`, `ATENDIDO` y `CANCELADO`.
- Actualizar pacientes pendientes, cancelar pacientes y finalizar atenciones.

## Estructuras de datos

- **Tabla hash:** búsqueda de pacientes por código.
- **Cola FIFO enlazada:** conserva el orden de llegada.
- **Cola de prioridad con heap binario:** organiza pacientes pendientes por nivel de prioridad y, en caso de empate, por llegada.

PostgreSQL conserva los registros. Al iniciar el backend, las estructuras en memoria se reconstruyen a partir de los pacientes persistidos.

## Stack tecnológico

| Componente | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot 4.1.2 |
| Construcción backend | Gradle Wrapper |
| Persistencia | Spring Data JPA / Hibernate |
| Base de datos | PostgreSQL |
| Frontend | React 19, TypeScript |
| Herramienta frontend | Vite 8 |
| Pruebas backend | JUnit / Spring Boot Test, H2 |

## Requisitos previos

- Git.
- Java JDK 21.
- Node.js 20.19+ o 22.12+ y npm (requisitos de Vite 8).
- PostgreSQL y una herramienta de administración, por ejemplo pgAdmin.
- Una base de datos llamada `triagex`.

Verifica las instalaciones:

```powershell
java -version
node -v
npm -v
git --version
```

El proyecto incluye Gradle Wrapper; no es necesario instalar Gradle globalmente.

## Obtener el proyecto

```powershell
git clone https://github.com/fredy1321/TriageX.git
cd TriageX
```

Si ya tienes el repositorio clonado, actualiza tu copia:

```powershell
git fetch origin
git pull origin main
```

## Configuración de PostgreSQL

1. Inicia el servicio de PostgreSQL.
2. Crea una base de datos llamada `triagex`.
3. Configura las credenciales mediante variables de entorno:
   - `DB_USERNAME`: usuario de PostgreSQL; si no se define, utiliza `postgres`.
   - `DB_PASSWORD`: contraseña del usuario. Es obligatoria y no debe guardarse en el repositorio.

En **PowerShell**, antes de iniciar el backend, define las variables en la terminal que usarás:

```powershell
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "TU_CONTRASENA"
```

Reemplaza `TU_CONTRASENA` por la contraseña local de PostgreSQL. Estas variables permanecen disponibles en esa ventana de PowerShell; si abres otra terminal, debes definirlas allí también. No publiques contraseñas ni las escribas en este README.

La conexión predeterminada está configurada en `backend/src/main/resources/application.properties`:

- URL: `jdbc:postgresql://localhost:5432/triagex`
- Usuario: `DB_USERNAME` o `postgres`
- Contraseña: `DB_PASSWORD`

Si PostgreSQL usa otro puerto, base de datos o usuario, ajusta la configuración antes de ejecutar el backend.

### Migración inicial / actualización del esquema

El script manual está en:

`backend/src/main/resources/db/manual/V2__normalizar_codigos_y_estados.sql`

Para una base existente, realiza primero un respaldo de PostgreSQL. Luego, con el backend detenido, abre pgAdmin, selecciona la base `triagex`, abre **Query Tool**, carga el contenido del archivo SQL y ejecútalo. El script:

- Detiene la ejecución si encuentra códigos duplicados al comparar sin distinguir mayúsculas/minúsculas y espacios externos.
- Normaliza los códigos a mayúsculas y elimina espacios externos.
- Crea un índice único sobre el código normalizado.
- Comprueba los cuatro estados admitidos y agrega la restricción correspondiente.

El script **no elimina pacientes**. Si informa duplicados o estados no reconocidos, revisa y corrige esos datos manualmente antes de volver a ejecutarlo. No ejecutes esta migración contra una base con datos importantes sin respaldo.

## Instalación de dependencias

### Backend

Gradle Wrapper descarga las dependencias necesarias al ejecutar las tareas. No requiere instalación manual de librerías.

### Frontend

Desde una terminal:

```powershell
cd frontend
npm ci
```

`npm ci` instala las dependencias exactas definidas por `package-lock.json`.

## Ejecución del proyecto

El backend y el frontend se ejecutan en **dos terminales diferentes**. Mantén ambas abiertas mientras uses la aplicación.

### 1. Iniciar el backend

Desde la raíz del repositorio, en la primera terminal:

```powershell
# Define primero DB_USERNAME y DB_PASSWORD (ver Configuración de PostgreSQL).
.\backend\gradlew.bat -p backend bootRun
```

El backend inicia en `http://localhost:8080`. Espera el mensaje:

`Started BackendApplication`

Si el backend no inicia, revisa la conexión, el nombre de la base de datos, las credenciales y si ejecutaste la migración V2 cuando corresponda.

### 2. Iniciar el frontend

Abre una segunda terminal desde la raíz del repositorio:

```powershell
cd frontend
npm run dev
```

Vite mostrará la dirección local, normalmente `http://localhost:5173/`. Abre esa dirección en el navegador. La configuración de Vite envía las solicitudes `/api` al backend en `http://localhost:8080`.

## Ejecución de pruebas y validaciones

### Pruebas del backend

Desde la raíz del repositorio:

```powershell
.\backend\gradlew.bat -p backend test
```

Las pruebas utilizan una base H2 en memoria mediante el perfil de pruebas; no necesitan modificar la base PostgreSQL de desarrollo. El resultado esperado es `BUILD SUCCESSFUL`.

### Compilación del frontend

Desde la carpeta `frontend`:

```powershell
npm run build
```

### Análisis estático del frontend

Desde la carpeta `frontend`:

```powershell
npm run lint
```

## Endpoints principales de la API

El backend expone los siguientes endpoints bajo `http://localhost:8080`:

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/pacientes` | Lista los pacientes. |
| POST | `/api/pacientes` | Registra un paciente. |
| GET | `/api/pacientes/{codigo}` | Busca un paciente por código. |
| PUT | `/api/pacientes/{codigo}` | Actualiza un paciente pendiente. |
| POST | `/api/pacientes/{codigo}/cancelar` | Cancela un paciente. |
| DELETE | `/api/pacientes/{codigo}` | Elimina un paciente. |
| GET | `/api/cola` | Consulta la cola de atención por prioridad. |
| GET | `/api/cola/llegada` | Consulta el orden FIFO de llegada. |
| POST | `/api/cola/atender` | Pasa al siguiente paciente a estado `EN_ATENCION`. |
| POST | `/api/cola/{codigo}/finalizar` | Finaliza la atención del paciente. |

## Flujo de estados del paciente

Los estados representan el ciclo de atención y no el nivel de prioridad:

- `PENDIENTE`: registrado y en espera.
- `EN_ATENCION`: su atención comenzó.
- `ATENDIDO`: atención finalizada.
- `CANCELADO`: atención cancelada.

La prioridad se registra por separado como `NIVEL 1` a `NIVEL 5`.

## Solución de problemas frecuentes

- **Error de conexión con PostgreSQL:** confirma que el servicio esté iniciado, que exista la base `triagex`, que el puerto sea el correcto y que `DB_USERNAME` / `DB_PASSWORD` estén definidos en la terminal del backend.
- **El backend informa códigos antiguos sin normalizar:** detén el backend, respalda la base y ejecuta el script V2 en pgAdmin.
- **La migración informa códigos duplicados:** no borres registros automáticamente. Identifica los pacientes que comparten el código normalizado y asigna códigos únicos conservando sus datos.
- **El frontend no conecta con la API:** confirma que el backend esté activo en el puerto 8080 y que Vite se haya iniciado desde la carpeta `frontend`.
- **Falla `npm ci`:** comprueba la versión de Node.js y vuelve a ejecutar el comando desde `frontend`.

## Licencia y uso académico

Proyecto académico desarrollado como MVP para la asignatura de Estructuras de Datos.
