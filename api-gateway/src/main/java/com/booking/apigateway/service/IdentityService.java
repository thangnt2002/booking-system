package com.booking.apigateway.service;


import com.booking.apigateway.dto.ApiResponse;
import com.booking.apigateway.dto.response.IntrospectTokenResponse;
import reactor.core.publisher.Mono;

public interface IdentityService {
    Mono<ApiResponse<IntrospectTokenResponse>> introspectToken(String token);
}
