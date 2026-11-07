package com.booking.apigateway.service;

import reactor.core.publisher.Mono;

/** Decides whether a bearer token may pass the gateway. Selected by {@code app.auth.provider}. */
public interface TokenVerifier {

    Mono<Boolean> isValid(String token);
}
