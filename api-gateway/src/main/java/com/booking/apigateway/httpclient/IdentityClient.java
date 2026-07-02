package com.booking.apigateway.httpclient;


import com.booking.apigateway.dto.ApiResponse;
import com.booking.apigateway.dto.request.IntrospectTokenRequest;
import com.booking.apigateway.dto.response.IntrospectTokenResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;

@HttpExchange
public interface IdentityClient {
    @PostExchange("/auth/introspect")
    Mono<ApiResponse<IntrospectTokenResponse>> introspectToken(
            @RequestBody IntrospectTokenRequest request
    );
}
