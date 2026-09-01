# Procesador F1 — Sistema de Migración de Procesos Batch (Banco XYZ)

## Objetivo

Modernizar los procesos batch del sistema legacy de **Banco XYZ** usando **Spring Batch**.
Se re-crean **tres procesos clave** que antes se ejecutaban de forma manual/legacy:

1. **Reporte de Transacciones Diarias** — lee el CSV de transacciones, valida los datos,
   detecta anomalías y genera un resumen diario.
2. **Cálculo de Intereses Mensuales** — aplica tasas de interés sobre cuentas de ahorro,
   préstamos e hipotecas, y actualiza el **saldo final** por cuenta.
3. **Generación de Estados de Cuenta Anuales** — compila los movimientos anuales por cuenta
   y genera un informe detallado para auditorías.

Todos los procesos leen archivos CSV de la carpeta `csvsemana3/`, procesan/validan los datos
con un `ItemProcessor`, y escriben el resultado en una **base de datos relacional H2 en memoria**.

---

## Tecnologías

| Componente        | Versión / Detalle |
|-------------------|-------------------|
| Java              | 25                 |
| Maven / Wrapper   | 3.9.16 / `mvnw.cmd`|
| Spring Boot       | 4.0.7              |
| Spring Framework  | 7.0.8              |
| Spring Batch      | 6.0.4              |
| Spring Data JPA   | con Hibernate 7.2  |
| Base de datos     | H2 (en memoria)    |
| Persistencia      | `jakarta.persistence` |

> Importante: Spring Batch 6 movió varios paquetes. Todo el código usa los paquetes nuevos,
> por ejemplo `org.springframework.batch.infrastructure.item.*` (item), `org.springframework.batch.core.job.*`
> (Job), `org.springframework.batch.core.step.*` (Step), y `jakarta.persistence`.

---

## Estructura del código

```
procesadorf1/
├── pom.xml
├── InstruccionesSumativa1.txt
├── csvsemana3/
│   ├── transacciones.csv
│   ├── intereses.csv
│   └── cuentas_anuales.csv
└── src/main/java/com/duoc/batch/procesadorf1/
    ├── Procesadorf1Application.java
    ├── config/
    │   ├── BatchConfig.java              # pool de threads + skip policy + chunk size
    │   ├── ReadersConfig.java            # FlatFileItemReaders (CSV) + Synchronized wrappers
    │   ├── WritersConfig.java            # JpaItemWriters
    │   ├── BatchRunner.java              # lanza los 3 Jobs en orden (CommandLineRunner)
    │   ├── ValidacionSkipPolicy.java     # política personalizada de tolerancia a fallos
    │   ├── JobExecutionLogger.java       # log de inicio/fin de cada Step
    │   ├── TransaccionesJobConfig.java   # Job 1
    │   ├── InteresesJobConfig.java       # Job 2
    │   └── CuentasAnualesJobConfig.java  # Job 3
    ├── model/                            # entidades JPA + DTOs crudos (String)
    ├── processor/                        # ItemProcessors (validación y corrección)
    ├── service/                          # lógica de resumen/agregación post-proceso
    ├── repository/                       # repositorios Spring Data JPA
    ├── listener/                         # listeners de Job y de skip
    ├── exception/                        # DataValidationException
    └── util/                             # DateParser (multi-formato)
```

---

## Los tres Jobs

### 1) Reporte de Transacciones Diarias
- Lee `transacciones.csv` (`id, fecha, monto, tipo`).
- Validaciones en `TransaccionProcessor`:
  - `id` numérico, `fecha` válida (múltiples formatos), `monto` numérico, `tipo` = `credito`/`debito`.
  - Registros inválidos (`invalid`, `desconocido`, `-1`, `2024-13-01`, montos vacíos, etc.)
    lanzan `DataValidationException` → **skip** (se continúa con el resto).
  - Montos negativos/cero se conservan pero se marcan como anomalía
    (`ANOMALIA_NEGATIVO` / `ANOMALIA_CERO`).
- Al terminar, `ResumenDiarioJobListener` genera el **resumen diario**
  (totales crédito/débito, montos y número de anomalías) y lo persiste.

### 2) Cálculo de Intereses Mensuales
- Lee `intereses.csv` (`cuenta_id, nombre, saldo, edad, tipo`).
- `InteresProcessor` aplica tasas por tipo:
  ahorro 1.5%, préstamo 2.5%, hipoteca 1.2%.
- Correcciones automáticas: saldo/edad vacíos o fuera de rango → 0; nombre vacío → `SIN_NOMBRE`.
- Tipos inválidos (`-1`, `unknown`, vacíos) → skip.
- Al terminar, `InteresJobListener` agrega por cuenta y actualiza el **saldo final** en BD.

### 3) Estados de Cuenta Anuales
- Lee `cuentas_anuales.csv` (`cuenta_id, fecha, transaccion, monto, descripcion`).
- `CuentaAnualProcessor` normaliza (`depósito` → `deposito`), corrige montos vacíos → 0
  y rechaza transacciones con datos inconsistentes.
- Al terminar, `EstadoCuentaJobListener` consolida por cuenta
  (depósitos, retiros, compras, pagos y saldo final del período).

---

## Manejo de errores y tolerancia a fallos

- `DataValidationException` se usa para datos inconsistentes y es **skipeable**.
- `ValidacionSkipPolicy` (personalizada) decide qué excepciones se saltan:

  ```java
  public boolean shouldSkip(Throwable t, long skipCount) {
      if (t instanceof DataValidationException) {           // se salta
          if (skipCount >= maxSkip) throw new SkipLimitExceededException(maxSkip, t);
          return true;
      }
      return false;                                          // cualquier otra: falla el step
  }
  ```

- El step se configura con `.faultTolerant().skipPolicy(...)`, por lo que un registro
  inválido no detiene el procesamiento del resto.
- `CountingSkipListener` registra en consola cada registro saltado y su motivo
  (`[SKIP-PROCESS] DataValidationException - ...`).
- Límite de saltos configurable: `batch.skip.max` (por defecto 100.000; muy alto a propósito
  para que un archivo "sucio" no aborte la corrida).

---

## Estrategia de escalamiento (decisión)

Se eligió la opción **más segura**: **chunk-oriented con multi-threading dentro del Step**
(en lugar de particiones).

- `TaskExecutor` (`ThreadPoolTaskExecutor`, bean `batchTaskExecutor`) con hilos
  **core=4, max=8, queue=100**.
- Cada Step procesa en chunks (by default `batch.chunk.size=50`).
- Los lectores CSV se envuelven con `SynchronizedItemStreamReader` para que el
  `FlatFileItemReader` sea seguro en un entorno multi-hilo.
- Parámetros configurables en `application.properties`:
  - `batch.threads.core`, `batch.threads.max`, `batch.threads.queue`
  - `batch.chunk.size`
  - `batch.skip.max`

Razones: no replica lectores/escritores por partición (evita complicar el repositorio y el
escritor JPA), suficiente para una sola instancia con H2, y fácil de ajustar solo con
parámetros.

---

## Configuración

`src/main/resources/application.properties`:

```properties
# H2 en memoria
spring.datasource.url=jdbc:h2:mem:procesadorf1;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false

# Spring Batch: no auto-ejecutar trabajos al inicio (se lanzan con BatchRunner)
spring.batch.job.enabled=false
# crear esquema de metadatos de Batch en H2
spring.batch.jdbc.initialize-schema=always

# Parámetros de escalamiento / errores
batch.chunk.size=50
batch.threads.core=4
batch.threads.max=8
batch.threads.queue=100
batch.skip.max=100000
```

---

## Cómo ejecutar

Desde la raíz del proyecto (`procesadorf1/`):

```powershell
# Opción 1: compilar y ejecutar con Maven wrapper
& .\mvnw.cmd package -DskipTests
& java -jar target\procesadorf1-0.0.1-SNAPSHOT.jar

# Opción 2: spring-boot:run
& .\mvnw.cmd spring-boot:run
```

La aplicación es una `CommandLineRunner`: al arrancar ejecuta los **tres Jobs en orden**
y muestra la evidencia en consola, finalizando con código de salida **0**.

---

## Evidencia de ejecución

Salida de consola real (resumen de cada Job):

```
> EJECUTANDO JOB 1: REPORTE DE TRANSACCIONES DIARIAS ...
===== FIN STEP 'stepProcesarTransacciones' => leidos=1000, escritos=491, saltados=509, status=COMPLETED
===== RESUMEN DE TRANSACCIONES DIARIAS =====
Total transacciones : 491
Anomalias detectadas: 90
< FIN 'jobReporteTransacciones' => status=COMPLETED

> EJECUTANDO JOB 2: CALCULO DE INTERESES MENSUALES ...
===== FIN STEP 'stepCalcularIntereses' => leidos=1000, escritos=669, saltados=331, status=COMPLETED
Cuenta 101 (Diana Prince) -> saldoFinal=126968.00 interesAcumulado=1968.00
...
< FIN 'jobCalcularIntereses' => status=COMPLETED

> EJECUTANDO JOB 3: GENERACION DE ESTADOS DE CUENTA ANUALES ...
===== FIN STEP 'stepProcesarCuentasAnuales' => leidos=1000, escritos=1000, saltados=0, status=COMPLETED
Cuenta 101 -> movimientos=46 depositos=36000.00 retiros=8200.00 compras=11300.00 pagos=1400.00 saldoFinal=17900.00
...
< FIN 'jobEstadosCuentaAnuales' => status=COMPLETED

==================== FIN PROCESOS BATCH BANCO XYZ ======================   (exit code 0)
```

Cada registro inválido se reporta con `[SKIP-PROCESS] DataValidationException - ...`
(motivo concreto), demostrando la tolerancia a fallos.
