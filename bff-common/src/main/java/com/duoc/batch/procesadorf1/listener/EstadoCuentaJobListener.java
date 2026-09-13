package com.duoc.batch.procesadorf1.listener;

import com.duoc.batch.procesadorf1.service.EstadoCuentaService;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;

/**
 * Al finalizar el Job de estados de cuenta anuales, consolida y persiste
 * el reporte por cuenta para auditoria.
 */
public class EstadoCuentaJobListener implements JobExecutionListener {

    private final EstadoCuentaService estadoCuentaService;

    public EstadoCuentaJobListener(EstadoCuentaService estadoCuentaService) {
        this.estadoCuentaService = estadoCuentaService;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus().isUnsuccessful()) {
            return;
        }
        estadoCuentaService.generarEstadosAnuales();
    }
}
