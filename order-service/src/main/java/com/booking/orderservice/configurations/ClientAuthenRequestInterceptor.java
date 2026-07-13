package com.booking.orderservice.configurations;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
public class ClientAuthenRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            String authHeader = servletAttrs.getRequest().getHeader("Authorization");
            log.info("Auth header: {}", authHeader);
            if (StringUtils.hasText(authHeader)) {
                requestTemplate.header("Authorization", authHeader);
            }
        } else {
            log.debug("No servlet request context available, skipping Authorization header forwarding");
        }
    }
}
