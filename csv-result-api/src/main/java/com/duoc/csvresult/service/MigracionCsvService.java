package com.duoc.csvresult.service;

import com.duoc.csvresult.model.ResultadoMigracion;
import com.duoc.csvresult.repository.ResultadoMigracionRepository;
import com.duoc.saga.contracts.MigracionDatosCompletadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MigracionCsvService {

    private static final Logger log = LoggerFactory.getLogger(MigracionCsvService.class);

    private final ResultadoMigracionRepository repository;

    public MigracionCsvService(ResultadoMigracionRepository repository) {
        this.repository = repository;
    }

    /**
     * Resuelve la carpeta de CSV de forma tolerante al directorio de trabajo.
     * Con spring-boot:run el proceso arranca en la carpeta del modulo y con el
     * jar empaquetado en la raiz, asi que se prueba la ruta y su padre
     * (mismo criterio que ReadersConfig de bff-common).
     */
    private Path resolverRaiz(String basePath) {
        Path directo = Paths.get(basePath);
        if (Files.isDirectory(directo)) {
            return directo;
        }
        Path padre = Paths.get("..", basePath);
        if (Files.isDirectory(padre)) {
            return padre;
        }
        Path absoluto = Paths.get(basePath).toAbsolutePath();
        if (Files.isDirectory(absoluto)) {
            return absoluto;
        }
        log.warn("[MIGRACION] no se encontro la carpeta de CSV '{}'; se migrara con 0 filas", directo.toAbsolutePath());
        return directo;
    }

    /**
     * Lee los CSV de csvsemana3/, valida cada registro y persiste el resultado.
     * Devuelve el evento que abre la Saga coreografica.
     */
    @Transactional
    public MigracionDatosCompletadaEvent migrar(String basePath) {
        String sagaId = "SAGA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Path raiz = resolverRaiz(basePath);
        log.info("[MIGRACION] sagaId={} initiating CSV migration from {}", sagaId, raiz.toAbsolutePath());

        List<MigracionDatosCompletadaEvent.DetalleArchivo> detalle = new ArrayList<>();
        List<ResultadoMigracion> aGuardar = new ArrayList<>();

        int[] contador = new int[]{0, 0}; // [validas, invalidas]
        for (String archivo : new String[]{"transacciones.csv", "intereses.csv", "cuentas_anuales.csv"}) {
            int[] res = procesarArchivo(sagaId, raiz.resolve(archivo), aGuardar, contador);
            detalle.add(new MigracionDatosCompletadaEvent.DetalleArchivo(archivo, res[0] + res[1], res[0], res[1]));
        }

        repository.saveAll(aGuardar);

        MigracionDatosCompletadaEvent evento = MigracionDatosCompletadaEvent.of(
                sagaId, contador[0], contador[1], detalle);
        log.info("[MIGRACION] sagaId={} filasValidas={} filasRechazadas={} detalle={}",
                sagaId, contador[0], contador[1], detalle);
        return evento;
    }

    /**
     * Nombres de cabecera que contienen el monto, en orden de prioridad.
     * Los tres CSV de csvsemana3/ no comparten el mismo esquema:
     *   transacciones.csv  -> id,fecha,monto,tipo        (monto en la 3ra)
     *   intereses.csv      -> cuenta_id,nombre,saldo,... (saldo en la 3ra)
     *   cuentas_anuales.csv-> cuenta_id,fecha,transaccion,monto,... (monto en la 4ta)
     * Por eso el parseo se resuelve por nombre de columna y no por posicion fija.
     */
    private static final List<String> COLUMNAS_MONTO = List.of("monto", "saldo", "valor", "importe");
    private static final List<String> COLUMNAS_TIPO = List.of("tipo", "transaccion", "descripcion");

    private int[] procesarArchivo(String sagaId, Path archivo, List<ResultadoMigracion> destino, int[] contador) {
        int validas = 0;
        int invalidas = 0;
        if (!Files.exists(archivo)) {
            log.warn("[MIGRACION] archivo no encontrado: {} (se omite)", archivo);
            return new int[]{0, 0};
        }
        try (BufferedReader reader = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            String linea = reader.readLine();
            if (linea == null) {
                return new int[]{0, 0};
            }
            String[] cabecera = linea.split(",");
            int colMonto = indiceColumna(cabecera, COLUMNAS_MONTO);
            int colTipo = indiceColumna(cabecera, COLUMNAS_TIPO);
            log.info("[MIGRACION] {} cabeceras={} colMonto={} colTipo={}",
                    archivo.getFileName(), List.of(cabecera), colMonto, colTipo);

            while ((linea = reader.readLine()) != null) {
                if (linea.isBlank()) {
                    continue;
                }
                String[] cols = linea.split(",", -1);
                ResultadoMigracion vm = construirRegistro(
                        sagaId, archivo.getFileName().toString(), cols, colMonto, colTipo);
                if ("MIGRADO".equals(vm.getEstado())) {
                    validas++;
                } else {
                    invalidas++;
                }
                destino.add(vm);
            }
        } catch (IOException e) {
            log.error("[MIGRACION] error leyendo {}: {}", archivo, e.getMessage());
        }
        contador[0] += validas;
        contador[1] += invalidas;
        return new int[]{validas, invalidas};
    }

    private int indiceColumna(String[] cabecera, List<String> candidatos) {
        for (String buscado : candidatos) {
            for (int i = 0; i < cabecera.length; i++) {
                if (cabecera[i].trim().toLowerCase().startsWith(buscado)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private ResultadoMigracion construirRegistro(String sagaId, String origen, String[] cols,
                int colMonto, int colTipo) {
        ResultadoMigracion vm = new ResultadoMigracion();
        vm.setSagaId(sagaId);
        vm.setArchivoOrigen(origen);
        vm.setRegistro(cols.length > 0 ? cols[0].trim() : "SIN_ID");

        if (cols.length < 2) {
            vm.setMonto(BigDecimal.ZERO);
            vm.setTipo("DESCONOCIDO");
            vm.setEstado("RECHAZADO");
            vm.setDetalle("Registro incompleto: se esperan al menos 2 columnas y llegaron " + cols.length);
            return vm;
        }

        BigDecimal monto = parseMonto(cols, colMonto);
        if (monto == null) {
            vm.setMonto(BigDecimal.ZERO);
            vm.setTipo(tipoEn(cols, colTipo));
            vm.setEstado("RECHAZADO");
            vm.setDetalle("Monto no numerico o ausente en la columna '" + nombreColumna(colMonto) + "'");
            return vm;
        }
        vm.setMonto(monto);

        String tipo = tipoEn(cols, colTipo);
        vm.setTipo(tipo.isEmpty() ? "SIN_TIPO" : tipo);

        if (monto.signum() < 0) {
            vm.setEstado("MIGRADO_CON_ANOMALIA");
            vm.setDetalle("ANOMALIA_NEGATIVO: monto negativo conservado");
        } else if (monto.signum() == 0) {
            vm.setEstado("MIGRADO_CON_ANOMALIA");
            vm.setDetalle("ANOMALIA_CERO: monto en cero");
        } else {
            vm.setEstado("MIGRADO");
            vm.setDetalle("Registro valido");
        }
        return vm;
    }

    private String tipoEn(String[] cols, int colTipo) {
        if (colTipo < 0 || colTipo >= cols.length) {
            return "";
        }
        return cols[colTipo].trim();
    }

    private String nombreColumna(int indice) {
        return indice >= 0 ? "col" + indice : "n/d";
    }

    /**
     * Interpreta como monto la columna indicada por la cabecera. Si esa celda
     * no es numerica se recorren las demas columnas numericas de la fila para
     * no descartar registros validos (saldos con decimales, valores vacios, etc).
     */
    private BigDecimal parseMonto(String[] cols, int colMonto) {
        if (colMonto >= 0 && colMonto < cols.length) {
            BigDecimal porCabecera = aDecimal(cols[colMonto]);
            if (porCabecera != null) {
                return porCabecera;
            }
        }
        for (String c : cols) {
            BigDecimal alt = aDecimal(c);
            if (alt != null) {
                return alt;
            }
        }
        return null;
    }

    private BigDecimal aDecimal(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim().replace("$", "").replace(" ", "");
        if (limpio.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}