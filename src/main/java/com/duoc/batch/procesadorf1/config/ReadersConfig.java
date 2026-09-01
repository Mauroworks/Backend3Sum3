package com.duoc.batch.procesadorf1.config;

import com.duoc.batch.procesadorf1.model.RawCuentaAnual;
import com.duoc.batch.procesadorf1.model.RawInteres;
import com.duoc.batch.procesadorf1.model.RawTransaccion;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.infrastructure.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.infrastructure.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ReadersConfig {

    private static final String BASE_DIR = "csvsemana3/";

    private <T> SynchronizedItemStreamReader<T> syncWrapper(FlatFileItemReader<T> reader) {
        return new SynchronizedItemStreamReaderBuilder<T>()
                .delegate(reader)
                .build();
    }

    private <T> FlatFileItemReader<T> csvReader(String path, Class<T> targetType,
                                                String... names) {
        return new FlatFileItemReaderBuilder<T>()
                .name("reader-" + targetType.getSimpleName())
                .resource(new FileSystemResource(BASE_DIR + path))
                .linesToSkip(1)
                .delimited()
                .delimiter(DelimitedLineTokenizer.DELIMITER_COMMA)
                .names(names)
                .targetType(targetType)
                .build();
    }

    @Bean
    public SynchronizedItemStreamReader<RawTransaccion> transaccionCsvReader() {
        return syncWrapper(csvReader("transacciones.csv", RawTransaccion.class,
                "id", "fecha", "monto", "tipo"));
    }

    @Bean
    public SynchronizedItemStreamReader<RawInteres> interesCsvReader() {
        return syncWrapper(csvReader("intereses.csv", RawInteres.class,
                "cuenta_id", "nombre", "saldo", "edad", "tipo"));
    }

    @Bean
    public SynchronizedItemStreamReader<RawCuentaAnual> cuentaAnualCsvReader() {
        return syncWrapper(csvReader("cuentas_anuales.csv", RawCuentaAnual.class,
                "cuenta_id", "fecha", "transaccion", "monto", "descripcion"));
    }
}
