# Evidencias Sumativa 3 — Punto 11

Modo **LOCAL**: los microservicios se ejecutan desde el codigo fuente con Maven.
La evidencia de ejecucion completa con `docker-compose.yml` queda marcada como
**PENDIENTE** en `09-docker-compose-pendiente/PENDIENTE.md`.

Docker se usa unicamente como infraestructura local para levantar
Zookeeper + Kafka (el bus de eventos de la Saga coreografica).

## Como reproducir

```powershell
powershell -ExecutionPolicy Bypass -NoProfile -File evidenciasS3\guion-evidencias.ps1
```

Parametros utiles:

| Parametro      | Efecto                                                            |
|----------------|-------------------------------------------------------------------|
| `-Reutilizar`  | Omite la compilacion y reutiliza los artefactos ya instalados.     |
| `-NoKafka`     | No levanta Kafka; la evidencia 07 queda como `PENDIENTE.md`.       |

El script es **autocontenido**: compila, levanta todo en orden, espera cada
servicio, ejecuta las pruebas `curl`, guarda cada salida como evidencia,
registra el `sagaId` de la corrida y detiene todo al terminar.

Requisitos: Docker Desktop en ejecucion (solo para Kafka), Java 21 y
`mvnw.cmd` incluido en el proyecto.

## Servicios y puertos

| Servicio           | Puerto | Rol                                                |
|--------------------|--------|----------------------------------------------------|
| `eureka-server`    | 8761   | Registro y descubrimiento de servicios              |
| `config-server`    | 8888   | Configuracion centralizada                          |
| `auth-server`      | 9000   | OAuth2.0 / OIDC (Spring Authorization Server)       |
| `csv-result-api`   | 8085   | Migracion de CSV, publica `MIGRACION_DATOS_COMPLETADA` |
| `account-service`  | 8091   | Calcula intereses, publica `INTERESES_CALCULADOS`  |
| `reporting-service`| 8092   | Consolida y publica `REPORTE_GENERADO`              |
| Kafka              | 9092   | Topic `saga.banco.xyz`                              |

## Credenciales OAuth2.0

| Cliente           | Secret          | Uso                                    |
|-------------------|-----------------|----------------------------------------|
| `client-app`      | `secret`        | Token para las llamadas `curl` de evidencia |
| `service-account` | `service-secret`| Token service-to-service de `account-service` a `csv-result-api` |

Flujo: `grant_type=client_credentials`, `scope=read`, `POST /oauth2/token`.

## Contenido de las evidencias

| Carpeta                          | Punto  | Qué demuestra                                                        |
|----------------------------------|--------|-----------------------------------------------------------------------|
| `00-build`                       | 1      | `clean install` de los 7 módulos de la sumativa 3 (BUILD SUCCESS)     |
| `01-eureka`                      | 2      | Eureka arriba, registro de los 3 microservicios                      |
| `02-config-server`               | 2      | Configuracion servida por app: `comun` + config especifica            |
| `03-oauth2`                      | 3      | Metadata, JWKS, token JWT y su payload decodificado                  |
| `04-csv-result-api`              | 3      | Migracion de los 3 CSV, filas migradas, resumen y rechazos           |
| `05-account-service`             | 3      | Cuentas con interés aplicado y resumen por saga                      |
| `06-reporting-service`           | 3      | Reportes generados por el saga                                       |
| `07-kafka-eventos`               | 4 y 5  | Contenedores, coreografia y eventos leidos del topic                 |
| `08-resilience4j`                | 6      | Circuit breakers, metricas y fallback con `csv-result-api` detenido  |
| `09-docker-compose-pendiente`    | 7      | Pendiente: ejecucion 100% con Docker Compose                         |
| `logs`                           | —      | Log completo de arranque de cada servicio                            |

## Resultado de la ultima corrida

`sagaId = SAGA-F9BF7DD5`

- Migracion: **2575 filas migradas**, 425 rechazadas
  - `transacciones.csv`: 1000 filas, 841 validas, 159 invalidas
  - `intereses.csv`: 1000 filas, 1000 validas
  - `cuentas_anuales.csv`: 1000 filas, 734 validas, 266 invalidas
- `account-service`: `sagasProcesadas = 1`, `totalCuentas = 1000`
- `reporting-service`: reporte `RESUMEN_MIGRACION` y `ESTADO_CUENTA_ANUAL`
  con `saldoTotal = 145411.05`
- Eventos en `saga.banco.xyz`: `MIGRACION_DATOS_COMPLETADA` (1),
  `INTERESES_CALCULADOS` (1), `REPORTE_GENERADO` (2)

## Coreografia de la Saga

```
POST /api/migracion/ejecutar   (csv-result-api, 8085)
        |
        v
  MIGRACION_DATOS_COMPLETADA  -> topic saga.banco.xyz
        |
        v
  account-service (8091): consulta resultados migrados con token
  service-to-service, calcula intereses y persiste cuentas
        |
        v
  INTERESES_CALCULADOS        -> topic saga.banco.xyz
        |
        v
  reporting-service (8092): consolida reporte
        |
        v
  REPORTE_GENERADO            -> topic saga.banco.xyz
```

## Notas de implementacion relevantes

- El Config Server **no fusiona** archivos: cada microservicio pide
  `spring.cloud.config.name: comun,<app>` para recibir la configuracion
  compartida y la suya.
- El bean de funcion se llama `sagaEvents`, por lo que los bindings usan
  `sagaEvents-out-0` / `sagaEvents-in-0`.
- Cada consumidor usa su propio `group` (`cuenta-consumer` y
  `reporte-consumer`); si compartieran grupo, Kafka repartiria los eventos.
- Los CSV se localizan por nombre de columna de la cabecera, porque los tres
  archivos no comparten esquema (el monto esta en la 3ra o 4ra posicion).
- Spring Boot 4 requiere `resilience4j-spring-boot4`; la variante
  `spring-boot3` no crea el `CircuitBreakerRegistry`.