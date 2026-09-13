package com.duoc.bff.atm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * BFF Cajeros Automaticos (Banco XYZ): Backend for Frontend seguro y eficiente
 * para operaciones criticas (consultas de saldo y retiros) sobre HTTPS en el
 * puerto 8083. Cada operacion se registra en auditoria. Carga su propia base
 * H2 en memoria con el pipeline batch de bff-common.
 */
@SpringBootApplication(scanBasePackages = "com.duoc")
@EntityScan(basePackages = {"com.duoc.batch.procesadorf1.model", "com.duoc.bff.atm.model"})
@EnableJpaRepositories(basePackages = {"com.duoc.batch.procesadorf1.repository",
        "com.duoc.bff.atm.repository"})
public class BffAtmApplication {

    public static void main(String[] args) {
        SpringApplication.run(BffAtmApplication.class, args);
    }
}