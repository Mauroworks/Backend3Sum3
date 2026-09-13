package com.duoc.batch.procesadorf1.listener;

import com.duoc.batch.procesadorf1.service.InteresService;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;

/**
 * Al finalizar el Job de intereses, actualiza el saldo final en la base
 * de datos y muestra la evidencia por cuenta.
 */
public class InteresJobListener implements JobExecutionListener {

    private final InteresService interesService;

    public InteresJobListener(InteresService interesService) {
        this.interesService = interesService;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus().isUnsuccessful()) {
            return;
        }
        interesService.actualizarSaldoFinal();
    }
}
