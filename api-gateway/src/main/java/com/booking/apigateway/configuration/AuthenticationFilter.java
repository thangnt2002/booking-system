package com.booking.apigateway.configuration;

import com.booking.apigateway.dto.ApiResponse;
import com.booking.apigateway.service.IdentityService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationFilter implements GlobalFilter, Ordered {

    ObjectMapper objectMapper;
    IdentityService identityService;

    @NonFinal
    @Value("${app.api-prefix}")
    String apiPrefix;

    @NonFinal
    String[] publicEndpoints = {"/identity/auth/login",
            "/identity/users/registrations",
            "/notification/email/send",
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("Enter gateway authentication filter");
        if(isPublicEndpoint(exchange.getRequest())){
            return chain.filter(exchange);
        }
        List<String> authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);
        if(CollectionUtils.isEmpty(authHeader)){
            return unAuthenticate(exchange.getResponse());
        }
        String token = authHeader.getFirst().replace("Bearer ", "");
        return identityService.introspectToken(token).flatMap(introspectResponse -> {
            if (introspectResponse.getData().isValid())
                return chain.filter(exchange);
            else
                return unAuthenticate(exchange.getResponse());
        }).onErrorResume(throwable -> unAuthenticate(exchange.getResponse()));
    }

    private boolean isPublicEndpoint(ServerHttpRequest request){
        return Arrays.stream(publicEndpoints).anyMatch(s ->
           request.getURI().getPath().matches(apiPrefix + s)
        );
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private Mono<Void> unAuthenticate(ServerHttpResponse response){
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(401)
                .success(false)
                .message("unauthenticate")
                .build();
        try {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            return response.writeWith(
              Mono.just(response.bufferFactory()
                      .wrap(objectMapper.writeValueAsString(apiResponse)
                              .getBytes()))
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
