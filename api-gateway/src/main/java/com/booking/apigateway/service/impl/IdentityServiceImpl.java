package com.booking.apigateway.service.impl;


import com.booking.apigateway.dto.ApiResponse;
import com.booking.apigateway.dto.request.IntrospectTokenRequest;
import com.booking.apigateway.dto.response.IntrospectTokenResponse;
import com.booking.apigateway.httpclient.IdentityClient;
import com.booking.apigateway.service.IdentityService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class IdentityServiceImpl implements IdentityService {

    IdentityClient identityClient;

    public Mono<ApiResponse<IntrospectTokenResponse>> introspectToken(String token){
        return identityClient.introspectToken(IntrospectTokenRequest.builder()
                .token(token)
                .build());
    }
}
