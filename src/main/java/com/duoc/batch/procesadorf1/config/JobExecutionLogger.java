package com.duoc.batch.procesadorf1.config;

import com.duoc.batch.procesadorf1.listener.CountingSkipListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;

/**
 * Loggea el inicio y el fin de cada Step y el total de registros saltados.
 */
public class JobExecutionLogger implements StepExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(JobExecutionLogger.class);

    private final String stepName;
    private final CountingSkipListener skipListener;

    public JobExecutionLogger(String stepName, CountingSkipListener skipListener) {
        this.stepName = stepName;
        this.skipListener = skipListener;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("===== INICIO STEP '{}' =====", stepName);
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        long read = stepExecution.getReadCount();
        long written = stepExecution.getWriteCount();
        long skipped = stepExecution.getSkipCount();
        log.info("===== FIN STEP '{}' => leidos={}, escritos={}, saltados={}, status={} =====",
                stepName, read, written, skipped, stepExecution.getStatus());
        return stepExecution.getExitStatus();
    }
}
