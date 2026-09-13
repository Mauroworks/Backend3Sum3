package com.duoc.bff.atm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Autenticacion y autorizacion del canal Cajero Automatico.
 * - Credencial de maquina de demostracion: atm / atm123 (rol ATM_USER).
 * - Las operaciones criticas (saldo/retiro) ademas requieren el PIN de la
 *   cuenta en el cuerpo de la peticion (validado por AtmService).
 */
@Configuration
public class SecurityConfig {

    @Value("${bff.security.user:atm}")
    private String user;

    @Value("${bff.security.password:atm123}")
    private String password;

    @Value("${bff.security.role:ATM_USER}")
    private String role;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.frameOptions(f -> f.disable()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/api/atm/**").hasRole(role)
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails maquinaAtm = User.withUsername(user)
                .password("{noop}" + password)
                .roles(role)
                .build();
        return new InMemoryUserDetailsManager(maquinaAtm);
    }
}