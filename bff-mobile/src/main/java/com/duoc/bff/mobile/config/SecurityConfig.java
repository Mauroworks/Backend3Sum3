package com.duoc.bff.mobile.config;

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
 * Autenticacion y autorizacion del canal Movil.
 * - Usuario de demostracion: mobile / mobile123 (rol MOBILE_USER).
 * - Solo el API /api/mobile/** es accesible con ese rol.
 */
@Configuration
public class SecurityConfig {

    @Value("${bff.security.user:mobile}")
    private String user;

    @Value("${bff.security.password:mobile123}")
    private String password;

    @Value("${bff.security.role:MOBILE_USER}")
    private String role;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.frameOptions(f -> f.disable()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/api/mobile/**").hasRole(role)
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails clienteMovil = User.withUsername(user)
                .password("{noop}" + password)
                .roles(role)
                .build();
        return new InMemoryUserDetailsManager(clienteMovil);
    }
}