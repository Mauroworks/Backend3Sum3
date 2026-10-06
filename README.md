# Procesador F1 - Microservicios y Seguridad en la Nube

## Objetivo

Proyecto desarrollado para implementar arquitectura de microservicios resilientes con Spring Cloud, incluyendo Config Server centralizado, Service Discovery (Eureka), arquitectura basada en eventos con Kafka, tolerancia a fallos con Resilience4j, seguridad OAuth2.0 y contenerización con Docker.

## Arquitectura y decisiones tomadas

### 1. Configuración centralizada
Se implementa **Spring Cloud Config Server** (`config-server`, puerto 8888) para centralizar configuraciones de los microservicios, permitiendo su gestión unificada y facilitando despliegues en entorno cloud.

### 2. Service Discovery
**Netflix Eureka** (`eureka-server`, puerto 8761) habilita registro y descubrimiento dinámico de microservicios, eliminando hardcoding de URLs y mejorando escalabilidad.

### 3. Microservicios desarrollados

Basados en los datos CSV de semana3, se implementaron 3 microservicios:

- **csv-result-api** (puerto 8085): API REST que expone los resultados de la migración de datos de CSVs (transacciones, intereses, estados de cuenta). Cumple con el requisito de "API con resultado de migración de datos". Implementa CircuitBreaker y Retry con Resilience4j.
- **account-service** (puerto 8091): Gestiona información de cuentas, saldos finales e intereses calculados. Publica eventos de cambios de cuenta hacia el bus de eventos.
- **reporting-service** (puerto 8092): Genera y consolida reportes. Consume eventos asíncronos desde Kafka para procesamiento de reportes de manera resiliente.

### 4. Arquitectura de eventos (Saga Coreográfica)

Se seleccionó **Saga Coreográfica** (Choreographed Saga) porque:
- Cada microservicio publica eventos cuando ocurre un cambio de estado, otros reaccionan independientemente.
- No requiere orquestador central (menor acoplamiento, mayor resiliencia).
- Adecuado para el flujo de validación → cálculo → reporte.
- Se complementa con enfoque event-driven para garantizar desacoplamiento entre servicios.

### 5. Mensajería asíncrona (Kafka vs JMS)

Se eligió **Apache Kafka** por:
- Alto throughput y escalabilidad distribuida.
- Persistencia duradera y capacidad de replay de eventos.
- Integración nativa con Spring Cloud Stream (Kafka Binder).
- Ideal para arquitectura pub/sub y patrones event-driven escalables.
- Mejor adaptación a entornos cloud nativos comparado con JMS.

Implementación: Spring Cloud Stream con bindings para productores/consumidores, orquestando comunicación asíncrona entre account-service (productor) y reporting-service (consumidor).

### 6. Seguridad OAuth2.0

Se implementó **OAuth 2.0** con Spring Security:
- **auth-server** (puerto 9000): Authorization Server (Spring Authorization Server) que emite tokens JWT.
- **Resource Servers**: csv-result-api, account-service y reporting-service configurados como OAuth2 Resource Servers con validación de JWT (issuer-uri: http://localhost:9000).
- Se migró de autenticación básica a OAuth2.0, siguiendo buenas prácticas para microservicios distribuidos.

### 7. Tolerancia a fallos

**Resilience4j** integrado en los microservicios con:
- CircuitBreaker: protege llamadas ante fallos, con fallback a respuestas vacías/controladas.
- Retry: reintentos automáticos ante fallos transitorios.
- Configuración de health indicators para monitoreo.

### 8. Contenerización y Orquestación

- **Dockerfiles** para cada microservicio (Java 21).
- **docker-compose.yml** orquesta todos los componentes: eureka-server, config-server, auth-server, zookeeper, kafka, y los 3 microservicios.
- Red dedicada `f1-network` para comunicación entre contenedores.

## Estructura del proyecto

```
procesadorf1/
├── bff-common/ bff-web/ bff-mobile/ bff-atm/ (existentes)
├── saga-contracts/       # Records compartidos de los eventos de la Saga
├── config-server/        # Config Server centralizado
├── eureka-server/        # Service Discovery (Eureka)
├── auth-server/          # Authorization Server OAuth2.0
├── csv-result-api/       # API resultados migración CSV (microservicio 1)
├── account-service/      # Servicio de cuentas (microservicio 2)
├── reporting-service/    # Servicio de reportes (microservicio 3)
├── csvsemana3/           # CSV de entrada (transacciones, intereses, cuentas_anuales)
├── docker-compose.yml    # Orquestación de contenedores
├── evidenciasS3/         # Evidencias del punto 11 + guion de ejecución
└── InstruccionesSumativa3.txt
```

## Instrucciones para ejecutar

### Opción 1: Ejecución local (modo desarrollo)

El camino más simple y reproducible es el guion de evidencias, que compila,
levanta todo en el orden correcto, ejecuta las pruebas y detiene los servicios:

```powershell
powershell -ExecutionPolicy Bypass -NoProfile -File evidenciasS3\guion-evidencias.ps1
```

Para levantar los servicios manualmente, en este orden:

1. eureka-server (8761)
2. config-server (8888)
3. auth-server (9000)
4. Zookeeper + Kafka (2181/9092)
5. csv-result-api (8085), account-service (8091), reporting-service (8092)

```powershell
# Instalar primero el POM padre y los modulos (spring-boot:run resuelve saga-contracts del repo local)
./mvnw.cmd -N clean install -DskipTests
./mvnw.cmd -pl saga-contracts,config-server,eureka-server,auth-server,csv-result-api,account-service,reporting-service clean install -DskipTests

./mvnw.cmd -pl eureka-server spring-boot:run
./mvnw.cmd -pl config-server spring-boot:run
./mvnw.cmd -pl auth-server spring-boot:run
./mvnw.cmd -pl csv-result-api spring-boot:run
./mvnw.cmd -pl account-service spring-boot:run
./mvnw.cmd -pl reporting-service spring-boot:run
```

### Opción 2: Ejecución con Docker Compose

```bash
docker-compose up --build
```

La evidencia de esta vía está **pendiente**: ver
`evidenciasS3/09-docker-compose-pendiente/PENDIENTE.md`.

## Evidencias de ejecución

Las evidencias del punto 11 están en `evidenciasS3/`, ordenadas por punto de la
rúbrica. Ver `evidenciasS3/README.md` para el detalle de cada carpeta, las
credenciales OAuth2.0 y la coreografía de la Saga.

## Notas técnicas relevantes

- **Config Server no fusiona archivos.** Cada microservicio declara
  `spring.cloud.config.name: comun,<app>` para recibir la configuración
  compartida (`comun.yml`) y la suya; con el nombre por defecto solo recibía
  su propio archivo y le faltaban `issuer-uri`, Eureka y los brokers Kafka.
- **Bindings de Spring Cloud Stream.** El bean de función se llama
  `sagaEvents`, por lo que los bindings son `sagaEvents-out-0` y
  `sagaEvents-in-0` (con guion y camelCase no aplican).
- **Grupos de consumo distintos.** `account-service` usa `cuenta-consumer` y
  `reporting-service` usa `reporte-consumer`. Si compartieran grupo, Kafka
  repartiría los eventos y solo uno de los dos reaccionaría.
- **Payload de Kafka.** Con `contentType: application/json` y un
  `Consumer<Object>` el binder entrega `byte[]`; se decodifica a texto antes de
  mapear a los records de `saga-contracts`.
- **CSV con esquemas distintos.** El monto se resuelve por nombre de columna de
  la cabecera, porque en `transacciones.csv` está en la 3ra posición y en
  `cuentas_anuales.csv` en la 4ta.
- **Spring Boot 4** requiere `resilience4j-spring-boot4`; la variante
  `spring-boot3` falla al crear el `CircuitBreakerRegistry`. Además Spring Boot
  4 ya no arrastra `spring-jcl`, por lo que se añadió `jcl-over-slf4j`.
- **Jackson 3.** El `RestClient` de Spring Boot 4 serializa con
  `tools.jackson`; pedir un `com.fasterxml.jackson.databind.JsonNode` como body
  falla. Se solicita el body como `String` y se parsea con el `ObjectMapper`.

## Tecnologías utilizadas

- Java 21, Spring Boot 4.0.7
- Spring Cloud 2025.1.3 (Config, Eureka, OpenFeign, Stream)
- Spring Authorization Server 1.4.0 (OAuth2.0)
- Resilience4j 2.4.0 (CircuitBreaker, Retry, TimeLimiter)
- Apache Kafka + Zookeeper (Spring Cloud Stream Binder)
- H2 Database (memoria)
- Docker & Docker Compose
