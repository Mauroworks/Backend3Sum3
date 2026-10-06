<#
    guion-evidencias.ps1
    ===========================================================================
    Evidencia de ejecucion de la SUMATIVA 3 - punto 11 de InstruccionesSumativa3.txt.

    Levanta la infraestructura y los 3 microservicios en MODO LOCAL, ejecuta
    las pruebas cURL de cada punto de la sumativa y guarda la salida de consola
    de cada prueba en las subcarpetas de evidenciasS3/.

    Uso:
        powershell -ExecutionPolicy Bypass -File evidenciasS3\guion-evidencias.ps1

    Parametros:
        -NoKafka     omite Kafka/Zookeeper (la evidencia 07 queda como PENDIENTE)
        -NoLimpiar   no detiene los servicios al finalizar
        -SoloBuild   solo compila, no levanta servicios
        -Reutilizar  no reconstruye el proyecto (ahorra tiempo en reejecuciones)
#>

[CmdletBinding()]
param(
    [switch]$NoKafka,
    [switch]$NoLimpiar,
    [switch]$SoloBuild,
    [switch]$Reutilizar
)

$ErrorActionPreference = 'Continue'
$root    = Split-Path -Parent $PSScriptRoot
$ev      = $PSScriptRoot
$logDir  = Join-Path $ev 'logs'
$stamp   = Get-Date -Format 'yyyyMMdd-HHmmss'
$mvnw    = Join-Path $root 'mvnw.cmd'
$csvPath = (Join-Path $root 'csvsemana3') -replace '\\','/'
# La ruta del proyecto contiene espacios y acentos: sin codificar, curl
# descarta la URL y la llamada falla en silencio.
$csvPathUrl = [System.Uri]::EscapeDataString($csvPath)

$script:Token       = $null
$script:UltimaSalida = $null
$svc = @{}

$carpetas = @('00-build','01-eureka','02-config-server','03-oauth2','04-csv-result-api',
              '05-account-service','06-reporting-service','07-kafka-eventos',
              '08-resilience4j','09-docker-compose-pendiente','logs')
foreach ($d in $carpetas) {
    $p = Join-Path $ev $d
    if (-not (Test-Path $p)) { New-Item -ItemType Directory -Force -Path $p | Out-Null }
}

function Say($m)  { Write-Host ("[{0}] {1}" -f (Get-Date -Format 'HH:mm:ss'), $m) -ForegroundColor Cyan }
function Ok($m)   { Write-Host ("   OK  " + $m) -ForegroundColor Green }
function Warn($m) { Write-Host ("   !!  " + $m) -ForegroundColor Yellow }
function Sep     { '=' * 78 }

# ---------------------------------------------------------------------------
# Grabar una prueba: escribe el comando usado + su salida en un .txt
# ---------------------------------------------------------------------------
function Save-Proof {
    param(
        [Parameter(Mandatory)][string]$Carpeta,
        [Parameter(Mandatory)][string]$Archivo,
        [Parameter(Mandatory)][string]$Titulo,
        [Parameter(Mandatory)][string]$Comando,
        [string[]]$Params
    )
    $destino = Join-Path (Join-Path $ev $Carpeta) $Archivo
    $cmdTexto = $Comando
    if ($Params) { $cmdTexto = "$Comando $($Params -join ' ')" }

    Say "$Carpeta\$Archivo"
    # Se ejecutan los parametros reales, sustituyendo el marcador <<TOKEN>>
    # por el JWT obtenido; para la evidencia se muestra <<JWT>>.
    $paramsEjecucion = $Params
    if ($script:Token) {
        $paramsEjecucion = $Params | ForEach-Object { $_ -replace '<<TOKEN>>', $script:Token }
    }
    $cmdTexto = $cmdTexto -replace '<<TOKEN>>', '<<JWT>>'
    $salida = & $Comando @paramsEjecucion 2>&1 | Out-String
    $script:UltimaSalida = $salida

    @(
        (Sep)
        "PRUEBA : $Titulo"
        "FECHA  : $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
        "COMANDO: $cmdTexto"
        (Sep)
        ''
        $salida
    ) | Set-Content -Path $destino -Encoding UTF8

    if ($salida.Trim()) { Ok 'ok' } else { Warn 'sin salida' }
}

# ---------------------------------------------------------------------------
# Grabar una prueba a partir de texto ya capturado (logs, filtros, etc.)
# ---------------------------------------------------------------------------
function Save-ProofText {
    param(
        [Parameter(Mandatory)][string]$Carpeta,
        [Parameter(Mandatory)][string]$Archivo,
        [Parameter(Mandatory)][string]$Titulo,
        [AllowEmptyCollection()][AllowNull()][string[]]$Lineas
    )
    $destino = Join-Path (Join-Path $ev $Carpeta) $Archivo
    if ($null -eq $Lineas) { $Lineas = @() }
    @(
        (Sep)
        "PRUEBA : $Titulo"
        "FECHA  : $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
        (Sep)
        ''
        ($Lineas -join "`r`n")
    ) | Set-Content -Path $destino -Encoding UTF8
    Ok "$Carpeta\$Archivo"
}

# ---------------------------------------------------------------------------
# Esperar a que un puerto HTTP responda
# ---------------------------------------------------------------------------
function Wait-Http {
    param([string]$Url, [int]$Segundos = 180, [string]$Nombre = 'servicio')
    $limite = (Get-Date).AddSeconds($Segundos)
    while ((Get-Date) -lt $limite) {
        try {
            $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
            if ($r.StatusCode -lt 500) { Ok "$Nombre respondio ($($r.StatusCode))"; return $true }
        } catch {
            $c = $null
            try { $c = [int]$_.Exception.Response.StatusCode } catch {}
            if ($c -and $c -lt 500) { Ok "$Nombre respondio ($c)"; return $true }
        }
        Start-Sleep -Seconds 3
    }
    Warn "TIMEOUT esperando $Nombre"
    return $false
}

function Stop-Service {
    param([string]$Nombre)
    if ($svc.ContainsKey($Nombre) -and $svc[$Nombre] -and -not $svc[$Nombre].HasExited) {
        Say "deteniendo $Nombre (pid $($svc[$Nombre].Id))"
        Stop-Process -Id $svc[$Nombre].Id -Force -ErrorAction SilentlyContinue
    }
}

# Detiene cualquier java que este escuchando en los puertos de la sumativa.
function Stop-PuertoOcupado {
    param([int]$Puerto, [string]$Que)
    $c = Get-NetTCPConnection -LocalPort $Puerto -State Listen -ErrorAction SilentlyContinue
    if (-not $c) { return }
    $pids = $c | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($id in $pids) {
        if ($id -eq 0) { continue }
        $proc = Get-Process -Id $id -ErrorAction SilentlyContinue
        if ($proc -and ($proc.ProcessName -like '*java*' -or $proc.ProcessName -like '*mvn*')) {
            Say "liberando puerto $Puerto ($Que, pid $id - $($proc.ProcessName))"
            Stop-Process -Id $id -Force -ErrorAction SilentlyContinue
        }
    }
    Start-Sleep -Seconds 2
}

function Start-JavaService {
    param([string]$Nombre, [string]$Modulo)
    $log = Join-Path $logDir "$Nombre.log"
    if (Test-Path $log) { Remove-Item $log -Force }
    Say "iniciando $Nombre (modulo $Modulo)"
    $p = Start-Process -FilePath $mvnw `
        -ArgumentList @('-pl', $Modulo, 'spring-boot:run') `
        -WorkingDirectory $root `
        -RedirectStandardOutput $log `
        -RedirectStandardError "$log.err" `
        -PassThru -NoNewWindow
    $svc[$Nombre] = $p
    return $log
}

function LogPath { param([string]$Nombre) Join-Path $logDir "$Nombre.log" }

Write-Host ''
Write-Host (Sep) -ForegroundColor DarkCyan
Write-Host '  EVIDENCIAS SUMATIVA 3  -  punto 11' -ForegroundColor Cyan
Write-Host '  Modo LOCAL (Docker Compose queda PENDIENTE)' -ForegroundColor Cyan
Write-Host "  Proyecto : $root" -ForegroundColor DarkGray
Write-Host "  Fecha    : $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor DarkGray
Write-Host (Sep) -ForegroundColor DarkCyan
Write-Host ''

# ===========================================================================
# 00 - BUILD
# ===========================================================================
$modulos = 'saga-contracts,config-server,eureka-server,auth-server,csv-result-api,account-service,reporting-service'

if ($Reutilizar) {
    Say 'Reutilizar: se omite la compilación'
} else {
    Say 'Compilando los módulos de la sumativa 3 (clean install)...'
    # 1) instalar el POM padre (no recursivo) para que los módulos lo resuelvan
    $padre = cmd /c "`"$mvnw`" -N clean install -DskipTests 2>&1"
    # 2) instalar los módulos; se usa install y no package porque
    #    spring-boot:run de cada microservicio resuelve saga-contracts
    #    desde el repositorio local de Maven.
    $salidaBuild = cmd /c "`"$mvnw`" -pl $modulos clean install -DskipTests 2>&1"
    ($padre + $salidaBuild) | Set-Content -Path (Join-Path $ev '00-build\maven-package.txt') -Encoding UTF8
    if ($LASTEXITCODE -eq 0) {
        ($salidaBuild | Select-String 'BUILD SUCCESS|Total time|Building ') |
            Set-Content -Path (Join-Path $ev '00-build\build-resumen.txt') -Encoding UTF8
        Ok 'BUILD SUCCESS'
    } else {
        Warn "BUILD con errores (exit $LASTEXITCODE) - revisar 00-build\maven-package.txt"
    }
}

if ($SoloBuild) { Say 'SoloBuild: finalizando.'; exit 0 }

# ===========================================================================
# Limpieza previa: liberar puertos de la sumativa
# ===========================================================================
Say 'Liberando puertos de ejecuciones anteriores...'
foreach ($p in @(8761, 8888, 9000, 8085, 8091, 8092)) { Stop-PuertoOcupado -Puerto $p -Que "puerto $p" }

# ===========================================================================
# Infraestructura de eventos: Zookeeper + Kafka
# ===========================================================================
if (-not $NoKafka) {
    Say 'Levantando Zookeeper + Kafka (bus de eventos de la Saga)...'
    docker rm -f f1-kafka f1-zookeeper *>$null
    docker run -d --name f1-zookeeper -p 2181:2181 `
        -e ZOOKEEPER_CLIENT_PORT=2181 -e ZOOKEEPER_TICK_TIME=2000 `
        confluentinc/cp-zookeeper:7.5.0 *>$null
    docker run -d --name f1-kafka -p 9092:9092 --link f1-zookeeper:f1-zookeeper `
        -e KAFKA_BROKER_ID=1 `
        -e KAFKA_ZOOKEEPER_CONNECT=f1-zookeeper:2181 `
        -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 `
        -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT `
        -e KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT `
        -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 `
        -e KAFKA_AUTO_CREATE_TOPICS_ENABLE=true `
        confluentinc/cp-kafka:7.5.0 *>$null
    Start-Sleep -Seconds 30
    $ps = (docker ps --format '{{.Names}} | {{.Status}}' 2>&1 | Out-String)
    Save-ProofText '07-kafka-eventos' '00-kafka-contenedores.txt' `
        'Punto 5 - Zookeeper y Kafka levantados' ($ps -split "`r?`n")
}

# ===========================================================================
# 01 - EUREKA SERVER  (punto 2)
# ===========================================================================
Say 'Arrancando eureka-server (8761)...'
Start-JavaService 'eureka-server' 'eureka-server' | Out-Null
Wait-Http 'http://localhost:8761/actuator/health' 180 'eureka-server' | Out-Null
Start-Sleep -Seconds 5

$logEureka = Get-Content (LogPath 'eureka-server') -Raw
Save-ProofText '01-eureka' '01-eureka-arranque.txt' `
    'Punto 2 - Service Discovery: Eureka Server arriba (log de arranque)' `
    (($logEureka -split "`r?`n") | Select-String 'Started EurekaServerApplication|Tomcat started|Netty started|port\(s\)|BUILD|ERROR' | ForEach-Object { $_.Line })

Save-Proof '01-eureka' '02-eureka-apps.json' `
    'Punto 2 - aplicaciones registradas en Eureka' `
    'curl.exe' @('-s', '-H', 'Accept: application/json', 'http://localhost:8761/eureka/apps')

# ===========================================================================
# 02 - CONFIG SERVER  (punto 1)
# ===========================================================================
Say 'Arrancando config-server (8888)...'
Start-JavaService 'config-server' 'config-server' | Out-Null
Wait-Http 'http://localhost:8888/actuator/health' 180 'config-server' | Out-Null
Start-Sleep -Seconds 4

$logConfig = Get-Content (LogPath 'config-server') -Raw
Save-ProofText '02-config-server' '01-config-server-arranque.txt' `
    'Punto 1 - Config Server centralizado arriba (log de arranque)' `
    (($logConfig -split "`r?`n") | Select-String 'Started ConfigServerApplication|Tomcat started|Netty started|port\(s\)|BUILD|ERROR' | ForEach-Object { $_.Line })

foreach ($app in @('csv-result-api', 'account-service', 'reporting-service')) {
    Save-Proof '02-config-server' "02-config-$app.json" `
        "Punto 1 - configuración centralizada servida para $app" `
        'curl.exe' @('-s', '-H', 'Accept: application/json', "http://localhost:8888/$app/default")
}

# ===========================================================================
# 03 - AUTH SERVER / OAuth2.0  (punto 6)
# ===========================================================================
Say 'Arrancando auth-server (9000)...'
Start-JavaService 'auth-server' 'auth-server' | Out-Null
Wait-Http 'http://localhost:9000/.well-known/oauth-authorization-server' 180 'auth-server' | Out-Null
Start-Sleep -Seconds 4

$logAuth = Get-Content (LogPath 'auth-server') -Raw
Save-ProofText '03-oauth2' '01-auth-server-arranque.txt' `
    'Punto 6 - Authorization Server OAuth2.0 arriba (log de arranque)' `
    (($logAuth -split "`r?`n") | Select-String 'Started AuthServerApplication|Tomcat started|Netty started|port\(s\)|BUILD|ERROR' | ForEach-Object { $_.Line })

Save-Proof '03-oauth2' '02-metadata-oauth2.json' `
    'Punto 6 - metadata del Authorization Server (RFC 8414)' `
    'curl.exe' @('-s', 'http://localhost:9000/.well-known/oauth-authorization-server')

Save-Proof '03-oauth2' '03-jwks.json' `
    'Punto 6 - llaves publicas JWKS para validar el JWT' `
    'curl.exe' @('-s', 'http://localhost:9000/oauth2/jwks')

# --- token para el usuario de pruebas -----------------------------------
Say 'Obteniendo token OAuth2.0 (client_credentials, scope=read)...'
$tokenRaw = & curl.exe -s -X POST 'http://localhost:9000/oauth2/token' `
    -H 'Content-Type: application/x-www-form-urlencoded' `
    -u 'client-app:secret' `
    -d 'grant_type=client_credentials' -d 'scope=read' 2>&1 | Out-String

@(
    (Sep)
    'PRUEBA : Punto 6 - solicitud de token OAuth2.0'
    "FECHA  : $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    'COMANDO: curl -X POST http://localhost:9000/oauth2/token -u client-app:secret -d grant_type=client_credentials -d scope=read'
    (Sep)
    ''
    $tokenRaw
) | Set-Content -Path (Join-Path $ev '03-oauth2\04-token-response.json') -Encoding UTF8

try {
    $j = $tokenRaw | ConvertFrom-Json
    $script:Token = $j.access_token
    if ($script:Token) {
        Ok ("token obtenido: " + $script:Token.Substring(0,30) + "...  expires_in=$($j.expires_in)s")

        # decodificacion del payload del JWT (sin verificar firma)
        $payload = $script:Token.Split('.')[1].Replace('-','+').Replace('_','/')
        while ($payload.Length % 4) { $payload += '=' }
        $json = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($payload))
        Save-ProofText '03-oauth2' '05-token-jwt-decodificado.json' `
            'Punto 6 - claims del JWT emitido (firma no verificada, solo lectura)' `
            (($json | ConvertFrom-Json | ConvertTo-Json -Depth 5) -split "`r?`n")
    } else {
        Warn "el auth-server no devolvio access_token: $tokenRaw"
    }
} catch {
    Warn "no se pudo leer la respuesta del token: $tokenRaw"
}

# ===========================================================================
# 04 / 05 / 06 - LOS 3 MICROSERVICIOS  (punto 3)
# ===========================================================================
Say 'Arrancando csv-result-api (8085)...'
Start-JavaService 'csv-result-api' 'csv-result-api' | Out-Null
Wait-Http 'http://localhost:8085/actuator/health' 200 'csv-result-api' | Out-Null

Say 'Arrancando account-service (8091)...'
Start-JavaService 'account-service' 'account-service' | Out-Null
Wait-Http 'http://localhost:8091/actuator/health' 200 'account-service' | Out-Null

Say 'Arrancando reporting-service (8092)...'
Start-JavaService 'reporting-service' 'reporting-service' | Out-Null
Wait-Http 'http://localhost:8092/actuator/health' 200 'reporting-service' | Out-Null

# dar tiempo a que los 3 se registren en Eureka
Start-Sleep -Seconds 20

foreach ($s in @(
    @{ n='csv-result-api';    c='04-csv-result-api'   },
    @{ n='account-service';   c='05-account-service'  },
    @{ n='reporting-service'; c='06-reporting-service'})) {
    $contenido = Get-Content (LogPath $s.n) -Raw
    Save-ProofText $s.c '01-arranque.txt' `
        "Punto 3 - $($s.n) arriba (consume config-server y se registra en Eureka)" `
        (($contenido -split "`r?`n") | Select-String `
            'Started .*Application|Tomcat started|Netty started|port\(s\)|DiscoveryClient_BANDSWITCH|Registering service|registration status UP|Registered instance|ERROR' |
            Select-Object -First 45 | ForEach-Object { $_.Line })
}

# --- registro en Eureka (punto 2) ---------------------------------------
Save-Proof '01-eureka' '03-eureka-apps-final.json' `
    'Punto 2 - Eureka con los 3 microservicios registrados' `
    'curl.exe' @('-s', '-H', 'Accept: application/json', 'http://localhost:8761/eureka/apps')

$reg = @()
foreach ($s in @('csv-result-api', 'account-service', 'reporting-service')) {
    $f = LogPath $s
    if (Test-Path $f) {
        $reg += "--- $s ---"
        $reg += (Select-String -Path $f -Pattern 'REGISTERED|Registering service|registration status UP|Registered instance|DiscoveryClient' |
                 Select-Object -First 10 | ForEach-Object { $_.Line })
        $reg += ''
    }
}
Save-ProofText '01-eureka' '04-registro-eureka-logs.txt' `
    'Punto 2 - trazas de registro en Eureka de los 3 microservicios' $reg

# --- 04 csv-result-api: API con resultado de la migracion ---------------
Say 'Ejecutando la migracion de los CSV (dispara la Saga)...'
Save-Proof '04-csv-result-api' '02-post-migracion.json' `
    'Punto 3 - POST /api/migracion/ejecutar (resultado de la migracion de CSV)' `
    'curl.exe' @('-s', '-X', 'POST', '-H', 'Authorization: Bearer <<TOKEN>>',
                 "http://localhost:8085/api/migracion/ejecutar?ruta=$csvPathUrl")

$sagaId = ''
if ($script:UltimaSalida -match '(SAGA-[A-Z0-9]+)') { $sagaId = $Matches[1]; Ok "sagaId detectado: $sagaId" }
else { Warn 'no se pudo detectar el sagaId en la respuesta' }

Save-Proof '04-csv-result-api' '03-get-resultados.json' `
    'Punto 3 - GET /api/migracion/resultados (filas migradas del saga)' `
    'curl.exe' @('-s', '-H', 'Authorization: Bearer <<TOKEN>>',
                 "http://localhost:8085/api/migracion/resultados?sagaId=$sagaId")

Save-Proof '04-csv-result-api' '04-get-resumen.json' `
    'Punto 3 - GET /api/migracion/resumen (totales migrados / rechazados)' `
    'curl.exe' @('-s', '-H', 'Authorization: Bearer <<TOKEN>>',
                 'http://localhost:8085/api/migracion/resumen')

Save-Proof '04-csv-result-api' '05-rechazados-transacciones.json' `
    'Punto 3 - registros de transacciones.csv descartados por validacion' `
    'curl.exe' @('-s', '-H', 'Authorization: Bearer <<TOKEN>>',
                 'http://localhost:8085/api/migracion/resultados?archivo=transacciones.csv')

# --- 05 account-service -------------------------------------------------
Start-Sleep -Seconds 15
Save-Proof '05-account-service' '02-get-cuentas.json' `
    'Punto 3 - GET /api/accounts (saldo con interes aplicado por el saga)' `
    'curl.exe' @('-s', '-H', 'Authorization: Bearer <<TOKEN>>',
                 "http://localhost:8091/api/accounts?sagaId=$sagaId")

Save-Proof '05-account-service' '03-get-resumen.json' `
    'Punto 3 - GET /api/accounts/resumen' `
    'curl.exe' @('-s', '-H', 'Authorization: Bearer <<TOKEN>>', 'http://localhost:8091/api/accounts/resumen')

# --- 06 reporting-service -----------------------------------------------
Start-Sleep -Seconds 15
Save-Proof '06-reporting-service' '02-get-reportes.json' `
    'Punto 3 - GET /api/reports?sagaId (reporte del saga)' `
    'curl.exe' @('-s', '-H', 'Authorization: Bearer <<TOKEN>>',
                 "http://localhost:8092/api/reports?sagaId=$sagaId")

Save-Proof '06-reporting-service' '03-get-todos.json' `
    'Punto 3 - GET /api/reports (todos los reportes generados)' `
    'curl.exe' @('-s', '-H', 'Authorization: Bearer <<TOKEN>>', 'http://localhost:8092/api/reports')

# ===========================================================================
# 07 - SAGA COREOGRAFICA SOBRE KAFKA  (puntos 4 y 5)
# ===========================================================================
if (-not $NoKafka) {
    $ev7 = @('=========== account-service (productor + consumidor) ===========')
    $fAcc = LogPath 'account-service'
    if (Test-Path $fAcc) {
        $ev7 += (Select-String -Path $fAcc -Pattern '\[OAUTH2\]|\[SAGA-RECIBIDO\]|\[SAGA-PASO\]|\[SAGA-PUB\]|\[INTERES\]|\[CLIENTE\]|\[RESILIENCIA\]' |
                 ForEach-Object { $_.Line })
    }
    $ev7 += ''
    $ev7 += '=========== reporting-service (consumidor) ==========='
    $fRep = LogPath 'reporting-service'
    if (Test-Path $fRep) {
        $ev7 += (Select-String -Path $fRep -Pattern '\[SAGA-RECIBIDO\]|\[SAGA-PUB\]|\[REPORTE\]|\[SAGA-CIERRE\]' |
                 ForEach-Object { $_.Line })
    }
    $ev7 += ''
    $ev7 += '=========== csv-result-api (inicio de la saga) ==========='
    $fCsv = LogPath 'csv-result-api'
    if (Test-Path $fCsv) {
        $ev7 += (Select-String -Path $fCsv -Pattern '\[MIGRACION\]|\[SAGA-PUB\]' | ForEach-Object { $_.Line })
    }
    Save-ProofText '07-kafka-eventos' '01-saga-coreografica.txt' `
        'Puntos 4 y 5 - Saga coreografica: MIGRACION -> INTERESES -> REPORTE' $ev7

    Say 'Describiendo el topic saga.banco.xyz...'
    $topic = & docker exec f1-kafka kafka-topics --bootstrap-server localhost:9092 `
                --describe --topic saga.banco.xyz 2>&1 | Out-String
    Save-ProofText '07-kafka-eventos' '02-topic-kafka.txt' `
        'Punto 5 - topic saga.banco.xyz' ($topic -split "`r?`n")

    Say 'Leyendo los eventos publicados en saga.banco.xyz...'
    # kafka-console-consumer devuelve codigo != 0 al agotar --timeout-ms y
    # escribe un TimeoutException por stderr; se separa del contenido util.
    $msgs = (& docker exec f1-kafka kafka-console-consumer --bootstrap-server localhost:9092 `
                --topic saga.banco.xyz --from-beginning --timeout-ms 15000 2>$null) -join "`n"
    Save-ProofText '07-kafka-eventos' '03-eventos-consumidos.txt' `
        'Punto 5 - eventos leidos del topic saga.banco.xyz' ($msgs -split "`r?`n")
} else {
    Save-ProofText '07-kafka-eventos' 'PENDIENTE.md' `
        'Punto 5 - Evidencia de Kafka no generada' @(
        'No se genero porque el script se ejecuto con -NoKafka.',
        'Para obtenerla: ejecutar el script sin ese parametro.')
}

# ===========================================================================
# 08 - RESILIENCE4J  (punto 3)
# ===========================================================================
Save-Proof '08-resilience4j' '01-health-csv-result-api.json' `
    'Punto 3 - Actuator health con Resilience4j (circuit breaker UP)' `
    'curl.exe' @('-s', 'http://localhost:8085/actuator/health')

Save-Proof '08-resilience4j' '02-circuitbreakers-csv-result-api.json' `
    'Punto 3 - estado de los circuit breakers de csv-result-api' `
    'curl.exe' @('-s', 'http://localhost:8085/actuator/circuitbreakers')

Save-Proof '08-resilience4j' '03-circuitbreakers-account-service.json' `
    'Punto 3 - estado de los circuit breakers de account-service' `
    'curl.exe' @('-s', 'http://localhost:8091/actuator/circuitbreakers')

Save-Proof '08-resilience4j' '04-metricas-resilience4j.json' `
    'Punto 3 - metricas de resilience4j (circuit breaker / retry)' `
    'curl.exe' @('-s', 'http://localhost:8091/actuator/metrics/resilience4j.circuitbreaker.calls')

Say 'Demostrando tolerancia a fallos: se detiene csv-result-api y se vuelve a consultar...'
Stop-Service 'csv-result-api'
Start-Sleep -Seconds 8

Save-Proof '08-resilience4j' '05-sin-csv-result-api-fallback.json' `
    'Punto 3 - account-service con csv-result-api CAIDO (fallback / circuit breaker)' `
    'curl.exe' @('-s', '-m', '90', '-H', 'Authorization: Bearer <<TOKEN>>',
                 "http://localhost:8091/api/accounts?sagaId=$sagaId")

if (Test-Path $fAcc) {
    Save-ProofText '08-resilience4j' '06-log-resiliencia-account-service.txt' `
        'Punto 3 - trazas de retry y circuit breaker en account-service' `
        ((Select-String -Path $fAcc -Pattern '\[RESILIENCIA\]|\[CLIENTE\]|\[OAUTH2\]' | ForEach-Object { $_.Line }) -join "`r`n")
}

Save-Proof '08-resilience4j' '07-health-account-service.json' `
    'Punto 3 - health de account-service tras la caida de su dependencia' `
    'curl.exe' @('-s', 'http://localhost:8091/actuator/health')

# ===========================================================================
# 09 - DOCKER / DOCKER-COMPOSE  (puntos 7 y 8) - PENDIENTE
# ===========================================================================
Save-ProofText '09-docker-compose-pendiente' 'PENDIENTE.md' `
    'Puntos 7 y 8 - Evidencia de Docker / docker-compose PENDIENTE' @(
    'ESTADO: PENDIENTE (por decision: solo se generaron evidencias en modo local).',
    '',
    'Lo que YA esta implementado en el proyecto:',
    '  - Dockerfile en cada microservicio (config-server, eureka-server, auth-server,',
    '    csv-result-api, account-service, reporting-service).',
    '  - docker-compose.yml con los 8 servicios: eureka-server, config-server,',
    '    auth-server, zookeeper, kafka, csv-result-api, account-service, reporting-service.',
    '',
    'Como completar esta evidencia manualmente:',
    '  1. docker compose up --build -d',
    '  2. docker compose ps                                  > 01-contenedores.txt',
    '  3. docker compose logs eureka-server                  > 02-log-eureka.txt',
    '  4. docker compose logs csv-result-api                 > 03-log-csv-result-api.txt',
    '  5. curl http://localhost:8085/actuator/health         > 04-health.txt',
    '  6. docker compose down                               (para detener)')

# ===========================================================================
# INDICE
# ===========================================================================
$indice = @()
foreach ($d in Get-ChildItem $ev -Directory | Sort-Object Name) {
    $indice += "## $($d.Name)"
    $indice += ''
    foreach ($f in Get-ChildItem $d.FullName -File | Sort-Object Name) {
        $indice += "- $($f.Name)"
    }
    $indice += ''
}
Save-ProofText '.' '00-indice.txt' 'Indice de evidencias generadas' $indice

# ===========================================================================
# LIMPIEZA
# ===========================================================================
if (-not $NoLimpiar) {
    Write-Host ''
    Say 'Deteniendo microservicios...'
    foreach ($n in @('csv-result-api','account-service','reporting-service',
                     'auth-server','config-server','eureka-server')) {
        Stop-Service $n
    }
    foreach ($p in @(8085, 8091, 8092, 9000, 8888, 8761)) { Stop-PuertoOcupado -Puerto $p -Que "puerto $p" }
    if (-not $NoKafka) {
        Say 'Deteniendo Kafka y Zookeeper...'
        docker rm -f f1-kafka f1-zookeeper *>$null
    }
}

Write-Host ''
Write-Host (Sep) -ForegroundColor Green
Write-Host '  EVIDENCIAS GENERADAS' -ForegroundColor Green
Write-Host "  Carpeta: $ev" -ForegroundColor Green
Write-Host "  sagaId de la corrida: $sagaId" -ForegroundColor Green
Write-Host (Sep) -ForegroundColor Green