package com.booking.keycloakprofileservice.configurations;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static com.booking.keycloakprofileservice.common.Constant.APP_INTERNAL_HEADER;
import static com.booking.keycloakprofileservice.common.Constant.VALID_INTERNAL_KEY_ATTRIBUTE;

@Slf4j
@Component
public class InternalFeignAuthFilter extends OncePerRequestFilter {

    @Value("${app.internal.key}")
    private String appInternalKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        boolean validInternalKey = false;

        if(!appInternalKey.isEmpty()){
            String requestKey = request.getHeader(APP_INTERNAL_HEADER);
            if (requestKey != null) {
                validInternalKey = MessageDigest.isEqual(appInternalKey.getBytes(StandardCharsets.UTF_8), requestKey.getBytes(StandardCharsets.UTF_8));
            }
        }

        if (validInternalKey) {
            request.setAttribute(VALID_INTERNAL_KEY_ATTRIBUTE, true);
        }
        filterChain.doFilter(request, response);
    }

}
