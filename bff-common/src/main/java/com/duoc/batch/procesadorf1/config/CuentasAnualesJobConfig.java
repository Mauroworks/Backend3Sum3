package com.duoc.batch.procesadorf1.config;

import com.duoc.batch.procesadorf1.listener.CountingSkipListener;
import com.duoc.batch.procesadorf1.listener.EstadoCuentaJobListener;
import com.duoc.batch.procesadorf1.model.MovimientoAnual;
import com.duoc.batch.procesadorf1.model.RawCuentaAnual;
import com.duoc.batch.procesadorf1.processor.CuentaAnualProcessor;
import com.duoc.batch.procesadorf1.service.EstadoCuentaService;
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
public class CuentasAnualesJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SynchronizedItemStreamReader<RawCuentaAnual> reader;
    private final ItemWriter<MovimientoAnual> writer;
    private final AsyncTaskExecutor taskExecutor;
    private final ValidacionSkipPolicy skipPolicy;
    private final EstadoCuentaService estadoCuentaService;

    @Value("${batch.chunk.size:50}")
    private int chunkSize;

    public CuentasAnualesJobConfig(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager,
                                   @Qualifier("cuentaAnualCsvReader")
                                   SynchronizedItemStreamReader<RawCuentaAnual> reader,
                                   @Qualifier("movimientoAnualWriter")
                                   ItemWriter<MovimientoAnual> writer,
                                   @Qualifier("batchTaskExecutor") AsyncTaskExecutor taskExecutor,
                                   ValidacionSkipPolicy skipPolicy,
                                   EstadoCuentaService estadoCuentaService) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.reader = reader;
        this.writer = writer;
        this.taskExecutor = taskExecutor;
        this.skipPolicy = skipPolicy;
        this.estadoCuentaService = estadoCuentaService;
    }

    @Bean
    public CountingSkipListener cuentaAnualSkipListener() {
        return new CountingSkipListener();
    }

    @Bean
    public Step stepProcesarCuentasAnuales() {
        CountingSkipListener skipListener = cuentaAnualSkipListener();
        JobExecutionLogger logger = new JobExecutionLogger("stepProcesarCuentasAnuales",
                skipListener);
        return new StepBuilder("stepProcesarCuentasAnuales", jobRepository)
                .<RawCuentaAnual, MovimientoAnual>chunk(chunkSize)
                .transactionManager(transactionManager)
                .reader(reader)
                .processor(new CuentaAnualProcessor())
                .writer(writer)
                .faultTolerant()
                    .skipPolicy(skipPolicy)
                    .listener(skipListener)
                .listener(logger)
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    public Job jobEstadosCuentaAnuales(@Qualifier("stepProcesarCuentasAnuales") Step step) {
        return new JobBuilder("jobEstadosCuentaAnuales", jobRepository)
                .start(step)
                .listener(new EstadoCuentaJobListener(estadoCuentaService))
                .build();
    }
}
