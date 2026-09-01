package com.duoc.batch.procesadorf1.listener;

import com.duoc.batch.procesadorf1.service.ReporteService;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;

/**
 * Al finalizar el Job de transacciones, genera el resumen diario.
 */
public class ResumenDiarioJobListener implements JobExecutionListener {

    private final ReporteService reporteService;

    public ResumenDiarioJobListener(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus().isUnsuccessful()) {
            return;
        }
        reporteService.generarResumenDiario();
    }
}
