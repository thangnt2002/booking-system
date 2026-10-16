package com.booking.keycloakprofileservice.configurations;

import jakarta.ws.rs.client.Client;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakConfiguration {

    @Value("${keycloak.auth-server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.admin.username}")
    private String username;

    @Value("${keycloak.admin.password}")
    private String password;

    @Bean
    public Keycloak keycloak() {
        ResteasyClientBuilderImpl clientBuilder = new ResteasyClientBuilderImpl();

        // Register the custom ObjectMapper
        Client resteasyClient = clientBuilder
                .register(new CustomObjectMapperProvider())
                .build();

        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .clientId(clientId)
                .grantType(OAuth2Constants.PASSWORD)
                .username(username)
                .password(password)
                .resteasyClient(resteasyClient)
                .build();
    }
}
