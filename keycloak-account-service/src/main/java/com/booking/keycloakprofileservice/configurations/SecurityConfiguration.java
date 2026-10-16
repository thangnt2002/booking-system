package com.booking.keycloakprofileservice.configurations;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.HashMap;
import java.util.Map;

import static com.booking.keycloakprofileservice.common.Constant.VALID_INTERNAL_KEY_ATTRIBUTE;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfiguration {

    private final String[] PUBLIC_ENDPOINT = {
            "/auth/login",
            "/auth/token",
            "/auth/introspect",
            "/auth/logout",
            "/users/registrations"
    };

    private final String[] INTERNAL_ENDPOINT = {
            "/internal/**",
            "/schedule/**"
    };

    @Autowired
    private CustomJWTConverter customJWTConverter;

    @Autowired
    private InternalFeignAuthFilter internalFeignAuthFilter;

    @Autowired
    private DynamicAuthorizationManager dynamicAuthorizationManager;

    @Autowired
    private JWTAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Value("${spring.security.keycloak.base-url}")
    private String keycloakBaseUrl;

    @Value("${spring.app-name}")
    private String appName;

    @Bean
    @Order(1)
    public SecurityFilterChain publicEndpointFilterChain(HttpSecurity http) {

        http
                .securityMatcher(PUBLIC_ENDPOINT)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .securityContext(AbstractHttpConfigurer::disable)
                .sessionManagement(session
                        -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(OAuth2ResourceServerConfigurer::disable);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain internalEndpointFilterChain(HttpSecurity http) {
        Map<String, AuthenticationManager> authenticationManagers = new HashMap<>();
        JwtIssuerAuthenticationManagerResolver authenticationManagerResolver =
                new JwtIssuerAuthenticationManagerResolver(authenticationManagers::get);

        String issuerUri = String.format("%s/realms/%s", keycloakBaseUrl, appName);
        String jwkSetUri = String.format("%s/realms/%s/protocol/openid-connect/certs", keycloakBaseUrl, appName);
        addManager(authenticationManagers, issuerUri, jwkSetUri);

        http
                .securityMatcher(INTERNAL_ENDPOINT)
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .securityContext(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterAfter(internalFeignAuthFilter, SecurityContextHolderFilter.class)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationManagerResolver(authenticationManagerResolver)
                        .accessDeniedHandler(customAccessDeniedHandler)
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest()
                        .access((authSupplier, context) -> {
                            // Allow if internal key is valid OR user is authenticated (JWT)
                            boolean hasInternalKey = Boolean.TRUE.equals(context.getRequest()
                                    .getAttribute(VALID_INTERNAL_KEY_ATTRIBUTE));
                            boolean isAuthenticated = authSupplier.get().isAuthenticated();
                            return new AuthorizationDecision(hasInternalKey || isAuthenticated);
                        })
                ).exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler(customAccessDeniedHandler)
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                );
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain securityFilterChain(HttpSecurity http ) {
        Map<String, AuthenticationManager> authenticationManagers = new HashMap<>();
        JwtIssuerAuthenticationManagerResolver authenticationManagerResolver =
                new JwtIssuerAuthenticationManagerResolver(authenticationManagers::get);

        String issuerUri = String.format("%s/realms/%s", keycloakBaseUrl, appName);
        String jwkSetUri = String.format("%s/realms/%s/protocol/openid-connect/certs", keycloakBaseUrl, appName);
        addManager(authenticationManagers, issuerUri, jwkSetUri);

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().access(dynamicAuthorizationManager)
                )
                .addFilterAfter(internalFeignAuthFilter, SecurityContextHolderFilter.class)
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationManagerResolver(authenticationManagerResolver)
                        .accessDeniedHandler(customAccessDeniedHandler)
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                ).exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler(customAccessDeniedHandler)
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                );
        return http.build();
    }

    @Bean
    CorsFilter corsFilter(){
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("http://localhost:3000");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource basedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        basedCorsConfigurationSource.registerCorsConfiguration("/**", configuration);
        return new CorsFilter(basedCorsConfigurationSource);
    }

    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder(10);
    }

    public void addManager(Map<String, AuthenticationManager> authenticationManagers, String issuerUri, String jwkSetUri) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
        JwtAuthenticationProvider authenticationProvider = new JwtAuthenticationProvider(decoder);
        authenticationProvider.setJwtAuthenticationConverter(customJWTConverter);
        authenticationManagers.put(issuerUri, authenticationProvider::authenticate);
    }
}
