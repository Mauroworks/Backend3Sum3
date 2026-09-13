package com.duoc.batch.procesadorf1.config;

import com.duoc.batch.procesadorf1.model.RawCuentaAnual;
import com.duoc.batch.procesadorf1.model.RawInteres;
import com.duoc.batch.procesadorf1.model.RawTransaccion;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.infrastructure.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.infrastructure.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
public class ReadersConfig {

    private static final String DEFAULT_CSV_DIR = "csvsemana3";

    @Value("${batch.csv.path:" + DEFAULT_CSV_DIR + "}")
    private String csvBasePath;

    /**
     * Resuelve el directorio de los CSV. Como cada BFF es un modulo que se
     * puede ejecutar desde la raiz del proyecto o desde su propio directorio,
     * se soporta tanto 'csvsemana3' como '../csvsemana3'.
     */
    private String resolveCsvDir() {
        if (Files.isDirectory(Paths.get(csvBasePath))) {
            return csvBasePath;
        }
        String parent = new File("..", csvBasePath).getPath();
        if (Files.isDirectory(Paths.get(parent))) {
            return parent;
        }
        return csvBasePath;
    }

    private <T> SynchronizedItemStreamReader<T> syncWrapper(FlatFileItemReader<T> reader) {
        return new SynchronizedItemStreamReaderBuilder<T>()
                .delegate(reader)
                .build();
    }

    private <T> FlatFileItemReader<T> csvReader(String path, Class<T> targetType,
                                                String... names) {
        return new FlatFileItemReaderBuilder<T>()
                .name("reader-" + targetType.getSimpleName())
                .resource(new FileSystemResource(new File(resolveCsvDir(), path).getPath()))
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
