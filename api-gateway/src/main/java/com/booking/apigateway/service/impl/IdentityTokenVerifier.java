package com.booking.apigateway.service.impl;

import com.booking.apigateway.service.IdentityService;
import com.booking.apigateway.service.TokenVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Legacy mode: asks identity-service to introspect its own tokens (app.auth.provider=identity). */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.auth.provider", havingValue = "identity")
public class IdentityTokenVerifier implements TokenVerifier {

    private final IdentityService identityService;

    @Override
    public Mono<Boolean> isValid(String token) {
        return identityService.introspectToken(token)
                .map(r -> r.getData() != null && r.getData().isValid())
                .onErrorReturn(false);
    }
}
