package com.duoc.bff.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * BFF Web (Banco XYZ): Backend for Frontend optimizado para navegadores.
 * Expone el API completo (datos ricos y detallados) sobre HTTPS en el puerto 8081.
 * Levanta su propia base H2 en memoria y la carga con el pipeline batch de
 * com.duoc.batch.procesadorf1 (bff-common).
 */
@SpringBootApplication(scanBasePackages = "com.duoc")
@EntityScan("com.duoc.batch.procesadorf1.model")
@EnableJpaRepositories(basePackages = "com.duoc.batch.procesadorf1.repository")
public class BffWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(BffWebApplication.class, args);
    }
}