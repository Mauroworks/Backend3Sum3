package com.duoc.batch.procesadorf1.config;

import com.duoc.batch.procesadorf1.listener.CountingSkipListener;
import com.duoc.batch.procesadorf1.listener.ResumenDiarioJobListener;
import com.duoc.batch.procesadorf1.model.RawTransaccion;
import com.duoc.batch.procesadorf1.model.Transaccion;
import com.duoc.batch.procesadorf1.processor.TransaccionProcessor;
import com.duoc.batch.procesadorf1.service.ReporteService;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class TransaccionesJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SynchronizedItemStreamReader<RawTransaccion> reader;
    private final ItemWriter<Transaccion> writer;
    private final AsyncTaskExecutor taskExecutor;
    private final ValidacionSkipPolicy skipPolicy;
    private final ReporteService reporteService;

    @Value("${batch.chunk.size:50}")
    private int chunkSize;

    public TransaccionesJobConfig(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  @Qualifier("transaccionCsvReader")
                                  SynchronizedItemStreamReader<RawTransaccion> reader,
                                  @Qualifier("transaccionWriter") ItemWriter<Transaccion> writer,
                                  @Qualifier("batchTaskExecutor") AsyncTaskExecutor taskExecutor,
                                  ValidacionSkipPolicy skipPolicy,
                                  ReporteService reporteService) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.reader = reader;
        this.writer = writer;
        this.taskExecutor = taskExecutor;
        this.skipPolicy = skipPolicy;
        this.reporteService = reporteService;
    }

    @Bean
    public CountingSkipListener transaccionSkipListener() {
        return new CountingSkipListener();
    }

    @Bean
    public Step stepProcesarTransacciones() {
        CountingSkipListener skipListener = transaccionSkipListener();
        JobExecutionLogger logger = new JobExecutionLogger("stepProcesarTransacciones",
                skipListener);
        return new StepBuilder("stepProcesarTransacciones", jobRepository)
                .<RawTransaccion, Transaccion>chunk(chunkSize)
                .transactionManager(transactionManager)
                .reader(reader)
                .processor(new TransaccionProcessor())
                .writer(writer)
                .faultTolerant()
                    .skipPolicy(skipPolicy)
                    .listener(skipListener)
                .listener(logger)
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    public Job jobReporteTransacciones(@Qualifier("stepProcesarTransacciones") Step step) {
        return new JobBuilder("jobReporteTransacciones", jobRepository)
                .start(step)
                .listener(new ResumenDiarioJobListener(reporteService))
                .build();
    }
}
