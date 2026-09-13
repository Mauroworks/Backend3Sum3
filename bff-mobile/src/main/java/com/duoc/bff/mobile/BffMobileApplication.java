package com.duoc.bff.mobile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * BFF Movil (Banco XYZ): Backend for Frontend optimizado para aplicaciones
 * moviles. Expone respuestas ligeras (solo datos esenciales) sobre HTTPS en el
 * puerto 8082 para reducir consumo de ancho de banda y mejorar la velocidad.
 * Carga su propia base H2 en memoria con el pipeline batch de bff-common.
 */
@SpringBootApplication(scanBasePackages = "com.duoc")
@EntityScan("com.duoc.batch.procesadorf1.model")
@EnableJpaRepositories(basePackages = "com.duoc.batch.procesadorf1.repository")
public class BffMobileApplication {

    public static void main(String[] args) {
        SpringApplication.run(BffMobileApplication.class, args);
    }
}