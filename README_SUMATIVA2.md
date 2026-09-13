# Procesador F1 — SUMATIVA 2: Backend for Frontend (BFF) Banco XYZ

> Documento nuevo de la **SUMATIVA 2**. El `README.md` original (procesos batch de
> la SUMATIVA 1) se mantiene intacto; este describe la evolución a arquitectura BFF.

---

## 1. Objetivo

Sobre el pipeline Spring Batch ya construido (SUMATIVA 1), se implementa el patrón
**Backend for Frontend (BFF)**: un **backend dedicado por cada canal de cliente**
del Banco XYZ, de modo que cada frontend (web, móvil y cajeros automáticos) recibe
exactamente lo que necesita, de forma segura y eficiente.

- Cada BFF tiene **su propio proceso, puerto, base de datos H2 y credenciales**.
- Todos se alimentan de los mismos datos de `csvsemana3/` (transacciones, intereses
  y cuentas anuales) usando el batch de la SUMATIVA 1 al arrancar.
- Se cumple HTTPS, autenticación y autorización **específica por canal**.

---

## 2. Análisis y estrategia BFF elegida

| Opción | Descripción | Decisión |
|--------|-------------|----------|
| A. Monolito con 3 BFF internos | Un solo `.jar` con controladores `/api/web`, `/api/mobile`, `/api/atm` | Rechazada: no hay separación real de despliegue ni de equipos. |
| B. Perfiles por canal | Un `.jar` con perfiles `web/mobile/atm` | Rechazada: complica ejecución simultánea de los 3 canales. |
| **C. Multi-módulo: un backend por cliente** | **Módulo Maven por cada BFF, cada uno con su proceso, puerto, BD, HTTPS y seguridad propios** | **ELEGIDA** |

**Por qué la opción C:**
- Es el BFF "puro": un backend mantenido y desplegado independientemente por cada
  frontend (cada canal evoluciona sin afectar a los otros).
- Permite servir **contratos de datos distintos**:
  - **Web**: datos completos y enriquecidos.
  - **Móvil**: payloads reducidos (mínimo ancho de banda).
  - **Cajeros**: operaciones críticas validadas con PIN y auditadas.
- Aísla la seguridad por canal (usuarios, roles y PIN de cajero) y permite HTTPS
  con certificado propio por BFF.

---

## 3. Arquitectura

```
                              +----------------------------+
                              |  csvsemana3/ (datos base)  |
                              |  transacciones.csv         |
                              |  intereses.csv             |
                              |  cuentas_anuales.csv       |
                              +-------------+--------------+
                                            |
                    (cada BFF carga su propia BD al arrancar
                            con los 3 Jobs de Spring Batch)
                                            |
        +-----------------------+-----------+------------------------+
        |                       |                                   |
+-------+--------+     +--------+----------+     +------------------+------+
|  BFF WEB       |     |  BFF MOBILE      |     |  BFF CAJEROS     |      |
|  HTTPS :8081   |     |  HTTPS :8082     |     |  HTTPS :8083     |      |
|  datos completos|     |  respuestas ligeras|  |  saldo / retiro  |      |
|  H2 en memoria |     |  H2 en memoria    |     |  PIN + auditoría |      |
|  rol WEB_USER  |     |  rol MOBILE_USER  |     |  rol ATM_USER    |      |
+-------+--------+     +--------+----------+     +---------+--------+
        |                       |                           |
   navegadores           apps Android/iOS            cajeros automaticos
```

### Estructura del código

```
procesadorf1/
├── pom.xml                      # Agregador Maven (packaging=pom)
├── csvsemana3/                  # Datos base compartidos
├── README.md                    # SUMATIVA 1 (batch) - intacto
├── README_SUMATIVA2.md          # Este documento
├── evidencia/                   # Salidas curl + logs (pruebas de ejecucion)
│
├── bff-common/                  # Modulo compartido (libreria)
│   └── src/main/java/com/duoc/batch/procesadorf1/
│       ├── config/              # Jobs, readers, writers, skip policy + BatchDataInitializer
│       ├── model/               # Entidades JPA (Transaccion, CuentaSaldoFinal, ...)
│       ├── repository/          # Repositorios Spring Data JPA
│       ├── processor/           # ItemProcessors (validacion/correccion)
│       ├── service/             # ReporteService, InteresService, EstadoCuentaService
│       ├── listener/            # Listeners de Job/Step/skip
│       ├── exception/, util/    # DataValidationException, DateParser
│
├── bff-web/                     # BFF WEB (puerto 8081)
│   └── src/main/java/com/duoc/bff/web/  (BffWebApplication, config, service, controller, dto)
├── bff-mobile/                  # BFF MOVIL (puerto 8082)
│   └── src/main/java/com/duoc/bff/mobile/
├── bff-atm/                     # BFF CAJEROS (puerto 8083)
│   └── src/main/java/com/duoc/bff/atm/   (model AtmOperacion, service AtmService, controller,...)
└── logs/                        # salidas de consola de las 3 apps en ejecucion
```

---

## 4. Los tres BFF y sus APIs

### 4.1 BFF Web — `https://localhost:8081`

Optimizado para navegadores: JSON completo, enriquecido y apto para dashboards.

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/web/dashboard` | Resumen diario + totales de cuentas/movimientos |
| GET | `/api/web/transacciones` | Todas las transacciones (campos completos) |
| GET | `/api/web/transacciones/{id}` | Transacción por id |
| GET | `/api/web/cuentas` | Saldos finales por cuenta (nombre, tipo, interés) |
| GET | `/api/web/cuentas/{id}` | Detalle: saldo + intereses aplicados + estado anual |
| GET | `/api/web/estados-cuenta` | Estados de cuenta anuales consolidados |
| GET | `/api/web/cuentas/{id}/movimientos` | Movimientos anuales del período |
| GET | `/api/web/estadisticas` | Conteos por tipo/estado para gráficos |

### 4.2 BFF Móvil — `https://localhost:8082`

Optimizado para apps móviles: respuestas sin campos pesados (sin descripciones ni
auditoría), con `limit` configurable para reducir ancho de banda.

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/mobile/resumen` | `{total, creditos, debitos, monto_total, anomalias}` |
| GET | `/api/mobile/saldos` | `[{cuenta_id, saldo, tipo}]` |
| GET | `/api/mobile/transacciones?limit=` | Últimas transacciones (campos esenciales) |
| GET | `/api/mobile/cuenta/{id}/resumen` | Saldo + últimos movimientos (compacto) |
| GET | `/api/mobile/cuenta/{id}/movimientos?limit=` | Últimos movimientos (solo fecha/tipo/monto) |

### 4.3 BFF Cajeros Automáticos — `https://localhost:8083`

Interfaz segura para operaciones críticas. Cada operación queda registrada en
`AtmOperacion` (auditoría), aprobada o rechazada.

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/atm/estado` | Estado operativo del cajero |
| GET | `/api/atm/billetes` | Inventario de billetes |
| POST | `/api/atm/saldo` | Consulta de saldo con PIN → `{cuenta_id, nombre, saldo, ticket}` |
| POST | `/api/atm/retiro` | Retiro con PIN y monto → debita la cuenta y entrega ticket |
| GET | `/api/atm/operaciones?cuentaId=` | Auditoría de operaciones del cajero |

**Cuerpo de `saldo`/`retiro`:**
```json
{ "cuentaId": 137, "pin": "1234", "monto": 5000 }
```

| Código HTTP | Motivo | Descripción |
|-------------|--------|-------------|
| 200 | — | Operación aprobada (cuenta 137, retiro de 5.000 → saldo nuevo 95.535) |
| 400 | `MONTO_INVALIDO` | Monto nulo, cero o negativo |
| 401 | `PIN_INVALIDO` | PIN incorrecto |
| 404 | `CUENTA_NO_EXISTE` | La cuenta no existe en la base |
| 409 | `SALDO_INSUFICIENTE` | Monto mayor que el saldo disponible |

> El PIN de demostración es `1234` (configurable en `bff-atm/src/main/resources/application.properties` → `atm.pin.default`).

---

## 5. Seguridad (HTTPS, autenticación y autorización)

- **HTTPS**: cada BFF usa un keystore **PKCS12 autofirmado** (`bff-web.p12`,
  `bff-mobile.p12`, `bff-atm.p12`) con password `banco123` y SAN
  `localhost + 127.0.0.1`. En entornos productivos se instalarían certificados de una CA.
- **Autenticación**: HTTP Basic por canal. Cada BFF solo conoce a **su** usuario:
  - Web: `web` / `web123` (rol `WEB_USER`)
  - Móvil: `mobile` / `mobile123` (rol `MOBILE_USER`)
  - Cajero: `atm` / `atm123` (rol `ATM_USER`)
- **Autorización**: cada `SecurityFilterChain` solo habilita su propio sub-path
  (`/api/web/**`, `/api/mobile/**`, `/api/atm/**`) para su rol; cualquier otra
  ruta exige autenticación y las peticiones sin token devuelven **401**.
  Al aislar las credenciales por BFF, las credenciales de un canal **no sirven
  en otro** (se comprobó en la evidencia: `sec-04` y `sec-05` → 401).
- Además, las operaciones críticas del cajero exigen **PIN** del cliente en el body.

---

## 6. Datos

Los tres BFF leen los archivos de `csvsemana3/`:

- `transacciones.csv` → Job 1 (reporte diario, anomalías, skip de inválidos).
- `intereses.csv` → Job 2 (cálculo de interés y saldo final por cuenta).
- `cuentas_anuales.csv` → Job 3 (estados de cuenta anuales).

Cada BFF levanta una **H2 en memoria propia** y en el arranque ejecuta los 3 Jobs
(`BatchDataInitializer`, en `bff-common`). La ruta de los CSV es configurable
(`batch.csv.path=csvsemana3`) y acepta `../csvsemana3` para cuando el BFF se ejecuta
desde su propio directorio de módulo.

---

## 7. Cómo ejecutar

Requisitos: JDK 21+ (probado con JDK 25) y Maven (wrapper incluido).

```powershell
# 1) Compilar todos los módulos (desde la raiz procesadorf1/)
.\mvnw.cmd clean package

# 2) Levantar cada BFF (cada uno en su terminal, desde la raiz del proyecto)
java -jar bff-web\target\bff-web-0.0.1-SNAPSHOT.jar        # https://localhost:8081
java -jar bff-mobile\target\bff-mobile-0.0.1-SNAPSHOT.jar  # https://localhost:8082
java -jar bff-atm\target\bff-atm-0.0.1-SNAPSHOT.jar        # https://localhost:8083
```

> En cada arranque se ve la evidencia de la carga batch (leidos/escritos/saltados
> por Job). Los certificados son autofirmados, por lo que en `curl` se usa `-k`.

### Ejemplos con curl

```powershell
# BFF Web
curl.exe -sk -u web:web123 https://localhost:8081/api/web/dashboard
curl.exe -sk -u web:web123 https://localhost:8081/api/web/cuentas/101

# BFF Movil
curl.exe -sk -u mobile:mobile123 https://localhost:8082/api/mobile/resumen
curl.exe -sk -u mobile:mobile123 "https://localhost:8082/api/mobile/transacciones?limit=5"

# BFF Cajeros: consulta de saldo
curl.exe -sk -u atm:atm123 -H "Content-Type: application/json" `
  -d '{\"cuentaId\":137,\"pin\":\"1234\"}' https://localhost:8083/api/atm/saldo

# BFF Cajeros: retiro exitoso
curl.exe -sk -u atm:atm123 -H "Content-Type: application/json" `
  -d '{\"cuentaId\":137,\"pin\":\"1234\",\"monto\":5000}' https://localhost:8083/api/atm/retiro

# BFF Cajeros: retiro con PIN incorrecto (401) / saldo insuficiente (409)
curl.exe -sk -u atm:atm123 -H "Content-Type: application/json" `
  -d '{\"cuentaId\":137,\"pin\":\"9999\",\"monto\":1000}' https://localhost:8083/api/atm/retiro
```

> En PowerShell, pasar el JSON con `\"` dentro del comando puede provocar errores de
> parseo; la alternativa robusta es usar `curl.exe -d "@archivo.json"`.

---

## 8. Evidencia de ejecución

Todas las salidas (JSON de cada endpoint con su código HTTP, tiempo y tamaño, más
los logs de carga batch) están en la carpeta **`evidencia/`** (ver
`evidencia/00-indice-evidencia.txt`).

### 8.1 Carga de datos (consola de cada BFF)

```
Tomcat started on port 8081 (https) with context path '/'
> EJECUTANDO JOB 1: REPORTE DE TRANSACCIONES DIARIAS ...
===== FIN STEP 'stepProcesarTransacciones' => leidos=1000, escritos=491, saltados=509, status=COMPLETED =====
Anomalias detectadas: 90
< FIN 'jobReporteTransacciones' => status=COMPLETED
> EJECUTANDO JOB 2: CALCULO DE INTERESES MENSUALES ...
> EJECUTANDO JOB 3: GENERACION DE ESTADOS DE CUENTA ANUALES ...
==================== FIN CARGA DE DATOS: BFF LISTO PARA SERVIR API =============
```

### 8.2 BFF Web

```
GET /api/web/dashboard -> 200
{"bff":"web","datos":{"resumen_diario":{"totalTransacciones":491,"totalCreditos":250,
  "totalDebitos":241,"montoTotal":512400.00,"totalAnomalias":90},
  "total_cuentas":50,"movimientos_anuales":1000}, ...}

GET /api/web/cuentas/101 -> 200  (cuenta 101, Diana Prince, saldo 126.968, interes 1.968)

GET /api/web/estadisticas -> 200
"por_tipo": {"credito":250,"debito":241}
"por_estado": {"OK":401,"ANOMALIA_NEGATIVO":81,"ANOMALIA_CERO":9}
```

### 8.3 BFF Móvil

```
GET /api/mobile/resumen -> 200
{"total":491,"creditos":250,"debitos":241,"monto_total":512400.00,"anomalias":90}

GET /api/mobile/cuenta/101/resumen -> 200
{"cuenta_id":101,"saldo":126968.00,"interes_acumulado":1968.00,"movimientos":[ ... ]}

GET /api/mobile/transacciones?limit=10 -> 200  (solo id/fecha/monto/tipo)
```

### 8.4 BFF Cajeros

```
POST /api/atm/saldo   {cuentaId:137, pin:1234} -> 200
{"tipo":"SALDO","aprobado":true,"cuenta_id":137,"nombre":"Bob Johnson",
 "saldo":100535.00,"ticket":"TKT-ATM-001-SL-..."}

POST /api/atm/retiro  {cuentaId:137, pin:1234, monto:5000} -> 200
{"tipo":"RETIRO","aprobado":true,"monto_retirado":5000,
 "saldo_anterior":100535.00,"saldo_nuevo":95535.00,"transaccion_id":1001,
 "ticket":"TKT-ATM-001-RT-..."}

POST /api/atm/retiro  {pin:9999}          -> 401 {"aprobado":false,"motivo":"PIN_INVALIDO"}
POST /api/atm/retiro  {monto:9999999}     -> 409 {"aprobado":false,"motivo":"SALDO_INSUFICIENTE"}
POST /api/atm/retiro  {monto:-500}        -> 400 {"aprobado":false,"motivo":"MONTO_INVALIDO"}

GET /api/atm/operaciones -> 200  (auditoria de todas las operaciones, con saldo posterior)
```

### 8.5 Seguridad

```
GET /api/web/dashboard (sin token)            -> 401
GET /api/mobile/resumen (sin token)           -> 401
GET /api/atm/estado (sin token)               -> 401
GET /api/web/dashboard con credencial MOVIL   -> 401  (el usuario no existe en el BFF Web)
GET /api/mobile/saldos con credencial CAJERO  -> 401  (aislamiento de credenciales por canal)
```

### 8.6 Tamaño de payload (móvil vs web)

| Caso | BFF Web | BFF Móvil |
|------|---------|-----------|
| Dashboard/resumen | 419 bytes (enriquecido) | 135 bytes (compacto) |
| Saldos de todas las cuentas | 6.914 bytes | 2.691 bytes |
| Movimientos (5) | 6.318 bytes (con descripción/estado) | 337 bytes (fecha/tipo/monto) |

El BFF Móvil entrega **≈50–60% menos bytes**, cumpliendo el objetivo de reducir el
consumo de ancho de banda.

---

## 9. Notas finales

- El `README.md` original no fue modificado.
- Las credenciales, puertos, PIN y contraseñas del keystore son de **demostración**;
  en producción se usarían secretos por variable de entorno y certificados de una CA.
- El módulo `bff-common` no se empaqueta como aplicación (es una librería); solo los
  tres BFF son ejecutables.