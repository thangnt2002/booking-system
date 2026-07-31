package com.booking.apigateway.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRouteConfiguration {

    @Value("${app.api-prefix}")
    String apiPrefix;

    @Value("${app.service.identity}")
    String identityHost;

    @Value("${app.service.profile}")
    String profileHost;

    @Value("${app.service.notification}")
    String notificationHost;

    @Value("${app.service.event}")
    String eventHost;

    @Value("${app.service.order}")
    String orderHost;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("identity_service", r -> r.path(apiPrefix+"/identity/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri(identityHost))
                .route("profile_service", r -> r.path(apiPrefix+"/profile/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri(profileHost))
                .route("notification_service", r -> r.path(apiPrefix+"/notification/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri(notificationHost))
                .route("event_service", r -> r.path(apiPrefix+"/event/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri(eventHost))
                .route("order_service", r -> r.path(apiPrefix+"/order/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri(orderHost))
                .build();
    }
}
