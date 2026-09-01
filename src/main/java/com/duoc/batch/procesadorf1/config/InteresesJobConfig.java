package com.duoc.batch.procesadorf1.config;

import com.duoc.batch.procesadorf1.listener.CountingSkipListener;
import com.duoc.batch.procesadorf1.listener.InteresJobListener;
import com.duoc.batch.procesadorf1.model.CuentaInteres;
import com.duoc.batch.procesadorf1.model.RawInteres;
import com.duoc.batch.procesadorf1.processor.InteresProcessor;
import com.duoc.batch.procesadorf1.service.InteresService;
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
public class InteresesJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SynchronizedItemStreamReader<RawInteres> reader;
    private final ItemWriter<CuentaInteres> writer;
    private final AsyncTaskExecutor taskExecutor;
    private final ValidacionSkipPolicy skipPolicy;
    private final InteresService interesService;

    @Value("${batch.chunk.size:50}")
    private int chunkSize;

    public InteresesJobConfig(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              @Qualifier("interesCsvReader")
                              SynchronizedItemStreamReader<RawInteres> reader,
                              @Qualifier("interesWriter") ItemWriter<CuentaInteres> writer,
                              @Qualifier("batchTaskExecutor") AsyncTaskExecutor taskExecutor,
                              ValidacionSkipPolicy skipPolicy,
                              InteresService interesService) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.reader = reader;
        this.writer = writer;
        this.taskExecutor = taskExecutor;
        this.skipPolicy = skipPolicy;
        this.interesService = interesService;
    }

    @Bean
    public CountingSkipListener interesSkipListener() {
        return new CountingSkipListener();
    }

    @Bean
    public Step stepCalcularIntereses() {
        CountingSkipListener skipListener = interesSkipListener();
        JobExecutionLogger logger = new JobExecutionLogger("stepCalcularIntereses",
                skipListener);
        return new StepBuilder("stepCalcularIntereses", jobRepository)
                .<RawInteres, CuentaInteres>chunk(chunkSize)
                .transactionManager(transactionManager)
                .reader(reader)
                .processor(new InteresProcessor())
                .writer(writer)
                .faultTolerant()
                    .skipPolicy(skipPolicy)
                    .listener(skipListener)
                .listener(logger)
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    public Job jobCalcularIntereses(@Qualifier("stepCalcularIntereses") Step step) {
        return new JobBuilder("jobCalcularIntereses", jobRepository)
                .start(step)
                .listener(new InteresJobListener(interesService))
                .build();
    }
}
