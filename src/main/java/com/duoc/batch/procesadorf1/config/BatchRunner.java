package com.duoc.batch.procesadorf1.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Lanza los tres Jobs de forma controlada y en orden al arrancar la aplicacion,
 * mostrando la evidencia de ejecucion en consola.
 */
@Component
public class BatchRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BatchRunner.class);

    private final JobOperator jobOperator;
    private final Job jobReporteTransacciones;
    private final Job jobCalcularIntereses;
    private final Job jobEstadosCuentaAnuales;
    private final ThreadPoolTaskExecutor taskExecutor;

    public BatchRunner(JobOperator jobOperator,
                       @Qualifier("jobReporteTransacciones") Job jobReporteTransacciones,
                       @Qualifier("jobCalcularIntereses") Job jobCalcularIntereses,
                       @Qualifier("jobEstadosCuentaAnuales") Job jobEstadosCuentaAnuales,
                       @Qualifier("batchTaskExecutor") ThreadPoolTaskExecutor taskExecutor) {
        this.jobOperator = jobOperator;
        this.jobReporteTransacciones = jobReporteTransacciones;
        this.jobCalcularIntereses = jobCalcularIntereses;
        this.jobEstadosCuentaAnuales = jobEstadosCuentaAnuales;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("==================== INICIO PROCESOS BATCH BANCO XYZ ====================");

        runJob(jobReporteTransacciones, "JOB 1: REPORTE DE TRANSACCIONES DIARIAS");
        runJob(jobCalcularIntereses, "JOB 2: CALCULO DE INTERESES MENSUALES");
        runJob(jobEstadosCuentaAnuales, "JOB 3: GENERACION DE ESTADOS DE CUENTA ANUALES");

        log.info("==================== FIN PROCESOS BATCH BANCO XYZ ======================");

        // Detener el pool de hilos para que la aplicacion (batch) pueda terminar sola.
        taskExecutor.shutdown();
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
