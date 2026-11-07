package com.booking.common.configurations;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
public class ClientAuthenRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        // no request bound when Feign is called from schedulers / Kafka listeners
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return;
        }
        String authHeader = attributes.getRequest().getHeader("Authorization");
        if (StringUtils.hasText(authHeader)) {
            // never log the header itself: it is a bearer credential
            requestTemplate.header("Authorization", authHeader);
        }
    }
}
