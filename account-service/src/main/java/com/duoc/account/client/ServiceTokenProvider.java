package com.duoc.account.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * Cliente OAuth2 para llamadas service-to-machine (grant_type=client_credentials).
 *
 * account-service es un Resource Server protegido: cuando necesita invocar a
 * csv-result-api (que tambien exige SCOPE_read) debe presentar un token propio.
 * Este cliente obtiene ese token y lo cachea hasta 30s antes de su expiracion.
 */
@Service
public class ServiceTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(ServiceTokenProvider.class);

    private final RestClient restClient;

    @Value("${clients.oauth2.token-uri:http://localhost:9000/oauth2/token}")
    private String tokenUri;

    @Value("${clients.oauth2.client-id:service-account}")
    private String clientId;

    @Value("${clients.oauth2.client-secret:service-secret}")
    private String clientSecret;

    @Value("${clients.oauth2.scope:read}")
    private String scope;

    private String tokenCache;
    private Instant expiraEn;

    public ServiceTokenProvider(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    /** Devuelve un token de servicio valido, renovandolo si esta por expirar. */
    public synchronized String token() {
        if (tokenCache != null && expiraEn != null && Instant.now().isBefore(expiraEn)) {
            log.debug("[OAUTH2] se reutiliza token de servicio cacheado");
            return tokenCache;
        }
        return renovar();
    }

    private String renovar() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("scope", scope);

        String credenciales = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> respuesta = restClient.post()
                    .uri(tokenUri)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + credenciales)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            if (respuesta == null || respuesta.get("access_token") == null) {
                log.warn("[OAUTH2] auth-server no devolvio access_token");
                return null;
            }
            tokenCache = String.valueOf(respuesta.get("access_token"));
            long expiraEnSegundos = respuesta.get("expires_in") == null
                    ? 60L : Long.parseLong(String.valueOf(respuesta.get("expires_in")));
            expiraEn = Instant.now().plusSeconds(Math.max(expiraEnSegundos - 30, 10));
            log.info("[OAUTH2] token de servicio obtido para clientId={} scope={} expiraEn={}s",
                    clientId, scope, expiraEnSegundos);
            return tokenCache;
        } catch (Exception e) {
            log.warn("[OAUTH2] no se pudo obtener token de servicio: {}", e.getMessage());
            return null;
        }
    }
}