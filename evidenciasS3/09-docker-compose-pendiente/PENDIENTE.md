==============================================================================
PRUEBA : Puntos 7 y 8 - Evidencia de Docker / docker-compose PENDIENTE
FECHA  : 2026-10-05 23:00:10
==============================================================================

ESTADO: PENDIENTE (por decision: solo se generaron evidencias en modo local).

Lo que YA esta implementado en el proyecto:
  - Dockerfile en cada microservicio (config-server, eureka-server, auth-server,
    csv-result-api, account-service, reporting-service).
  - docker-compose.yml con los 8 servicios: eureka-server, config-server,
    auth-server, zookeeper, kafka, csv-result-api, account-service, reporting-service.

Como completar esta evidencia manualmente:
  1. docker compose up --build -d
  2. docker compose ps                                  > 01-contenedores.txt
  3. docker compose logs eureka-server                  > 02-log-eureka.txt
  4. docker compose logs csv-result-api                 > 03-log-csv-result-api.txt
  5. curl http://localhost:8085/actuator/health         > 04-health.txt
  6. docker compose down                               (para detener)
