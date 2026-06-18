package com.booking.identityservice.controller;

import com.nimbusds.jose.JOSEException;
import com.booking.identityservice.dto.ApiResponse;
import com.booking.identityservice.dto.request.AuthenticationRequest;
import com.booking.identityservice.dto.request.IntrospectTokenRequest;
import com.booking.identityservice.dto.request.LogoutRequest;
import com.booking.identityservice.dto.request.RefreshRequest;
import com.booking.identityservice.dto.response.AuthenticationResponse;
import com.booking.identityservice.dto.response.IntrospectTokenResponse;
import com.booking.identityservice.service.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {

    AuthenticationService authenticationService;

    @PostMapping("/login")
    private ApiResponse<AuthenticationResponse> login(@RequestBody AuthenticationRequest request){
        return ApiResponse.<AuthenticationResponse>builder()
                .success(true)
                .data(AuthenticationResponse
                        .builder()
                        .isAuthenticated(true)
                        .token(authenticationService.authenticate(request))
                        .build())
                .build();
    }

    @PostMapping("/introspect")
    private ApiResponse<IntrospectTokenResponse> introspect(@RequestBody IntrospectTokenRequest request)
            throws ParseException, JOSEException {
        var res = IntrospectTokenResponse.builder()
                .isValid(authenticationService.introspectToken(request))
                .build();
        return ApiResponse.<IntrospectTokenResponse>builder()
                .success(true)
                .code(200)
                .data(res)
                .build();
    }

    @PostMapping("/logout")
    private ApiResponse<?> logout(@RequestBody LogoutRequest request)
            throws ParseException, JOSEException {
        authenticationService.logout(request);
        return ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .build();
    }

    @PostMapping("/refresh")
    private ApiResponse<AuthenticationResponse> refresh(@RequestBody RefreshRequest request)
            throws ParseException, JOSEException {
        return ApiResponse.<AuthenticationResponse>builder()
                .success(true)
                .data(authenticationService.refresh(request))
                .build();
    }

}
