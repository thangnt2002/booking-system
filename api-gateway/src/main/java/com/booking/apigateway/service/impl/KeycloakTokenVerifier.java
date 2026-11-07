package com.booking.apigateway.service.impl;

import com.booking.apigateway.service.TokenVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Verifies Keycloak access tokens locally: signature against the realm JWKS (cached by Nimbus),
 * expiry/not-before, and issuer. The downstream services validate the same token again.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.auth.provider", havingValue = "keycloak", matchIfMissing = true)
public class KeycloakTokenVerifier implements TokenVerifier {

    private final ReactiveJwtDecoder decoder;

    public KeycloakTokenVerifier(@Value("${app.keycloak.base-url}") String baseUrl,
                                 @Value("${app.keycloak.realm}") String realm) {
        String issuer = baseUrl + "/realms/" + realm;
        NimbusReactiveJwtDecoder nimbus = NimbusReactiveJwtDecoder
                .withJwkSetUri(issuer + "/protocol/openid-connect/certs")
                .build();
        nimbus.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        this.decoder = nimbus;
        log.info("[Gateway] Validating Keycloak tokens issued by {}", issuer);
    }

    @Override
    public Mono<Boolean> isValid(String token) {
        return decoder.decode(token)
                .map(jwt -> true)
                .onErrorResume(e -> {
                    log.debug("[Gateway] Rejected token: {}", e.getMessage());
                    return Mono.just(false);
                });
    }
}
