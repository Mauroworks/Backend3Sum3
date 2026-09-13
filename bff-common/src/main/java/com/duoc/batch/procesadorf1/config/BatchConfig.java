package com.duoc.batch.procesadorf1.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuracion compartida de Spring Batch.
 *
 * Estrategia de escalamiento elegida (segura para una sola instancia con H2):
 *  - Chunk-oriented processing con un TaskExecutor de pool de hilos
 *    (componente del mismo step). El numero de hilos y el throttleLimit
 *    definen el grado de paralelismo. Es mas sencillo y seguro que
 *    particionar porque no replica lectores/escritores en particiones.
 *
 * Parametros configurables en application.properties:
 *  - batch.threads.core, batch.threads.max, batch.threads.queue.
 */
@Configuration
public class BatchConfig {

    @Value("${batch.threads.core:4}")
    private int core;

    @Value("${batch.threads.max:8}")
    private int max;

    @Value("${batch.threads.queue:100}")
    private int queue;

    @Value("${batch.chunk.size:50}")
    private int chunkSize;

    @Value("${batch.skip.max:100000}")
    private int skipMax;

    @Bean
    public AsyncTaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setThreadNamePrefix("batch-worker-");
        executor.initialize();
        return executor;
    }

    @Bean
    public ValidacionSkipPolicy validacionSkipPolicy() {
        return new ValidacionSkipPolicy(skipMax);
    }

    public int getChunkSize() {
        return chunkSize;
    }
}
