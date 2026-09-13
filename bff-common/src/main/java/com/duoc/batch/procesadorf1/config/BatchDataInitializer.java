package com.duoc.batch.procesadorf1.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Carga los datos base de cada BFF ejecutando los tres Jobs de Spring Batch
 * al arrancar la aplicacion (reutiliza el pipeline de la SUMATIVA 1).
 *
 * A diferencia del BatchRunner original de la SUMATIVA 1, esta clase NO detiene
 * el pool de hilos ni finaliza la JVM: cada BFF queda escuchando peticiones
 * REST sobre su propia base de datos H2 en memoria.
 */
@Component
public class BatchDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BatchDataInitializer.class);

    private final JobOperator jobOperator;
    private final Job jobReporteTransacciones;
    private final Job jobCalcularIntereses;
    private final Job jobEstadosCuentaAnuales;

    public BatchDataInitializer(JobOperator jobOperator,
                                @Qualifier("jobReporteTransacciones") Job jobReporteTransacciones,
                                @Qualifier("jobCalcularIntereses") Job jobCalcularIntereses,
                                @Qualifier("jobEstadosCuentaAnuales") Job jobEstadosCuentaAnuales) {
        this.jobOperator = jobOperator;
        this.jobReporteTransacciones = jobReporteTransacciones;
        this.jobCalcularIntereses = jobCalcularIntereses;
        this.jobEstadosCuentaAnuales = jobEstadosCuentaAnuales;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("==================== INICIO CARGA DE DATOS (BATCH) BANCO XYZ ====================");

        runJob(jobReporteTransacciones, "JOB 1: REPORTE DE TRANSACCIONES DIARIAS");
        runJob(jobCalcularIntereses, "JOB 2: CALCULO DE INTERESES MENSUALES");
        runJob(jobEstadosCuentaAnuales, "JOB 3: GENERACION DE ESTADOS DE CUENTA ANUALES");

        log.info("==================== FIN CARGA DE DATOS: BFF LISTO PARA SERVIR API =============");
    }

    private void runJob(Job job, String titulo) throws Exception {
        log.info("-------------------------------------------------------------");
        log.info("> EJECUTANDO {} ...", titulo);
        JobParameters params = new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .addString("proceso", job.getName())
                .toJobParameters();
        JobExecution execution = jobOperator.start(job, params);
        log.info("< FIN '{}' => status={}, exitStatus={}",
                job.getName(), execution.getStatus(), execution.getExitStatus());
        log.info("-------------------------------------------------------------");
    }
}