package com.booking.identityservice.configuration;

import com.booking.identityservice.dto.ApiResponse;
import com.booking.identityservice.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JWTAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        ErrorCode error = ErrorCode.UN_AUTHORIZE;
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .success(false)
                .code(error.getCode())
                .message(error.getMessage())
                .build();
        ObjectMapper mapper = new ObjectMapper();

        response.setStatus(error.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(mapper.writeValueAsString(apiResponse));
        response.flushBuffer();
    }
}
