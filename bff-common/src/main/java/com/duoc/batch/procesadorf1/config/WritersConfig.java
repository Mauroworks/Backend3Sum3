package com.duoc.batch.procesadorf1.config;

import com.duoc.batch.procesadorf1.model.CuentaInteres;
import com.duoc.batch.procesadorf1.model.MovimientoAnual;
import com.duoc.batch.procesadorf1.model.Transaccion;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JpaItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class WritersConfig {

    private final EntityManagerFactory entityManagerFactory;

    public WritersConfig(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    private <T> ItemWriter<T> jpaWriter() {
        return new JpaItemWriterBuilder<T>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    @Bean
    public ItemWriter<Transaccion> transaccionWriter() {
        return jpaWriter();
    }

    @Bean
    public ItemWriter<CuentaInteres> interesWriter() {
        return jpaWriter();
    }

    @Bean
    public ItemWriter<MovimientoAnual> movimientoAnualWriter() {
        return jpaWriter();
    }
}
