package com.booking.eventservice.configurations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    private final String[] PUBLIC_ENDPOINT = {"/events/create",
            "/events/update/{id}",
            "/events/{id}",
            "/tickets/create",
            "/tickets/{id}",
            "/internal/**"
    };

    @Autowired
    private CustomJWTDecoder customJWTDecoder;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        //security for api endpoint
        http.authorizeHttpRequests(request ->
                request.requestMatchers(
                        HttpMethod.GET, PUBLIC_ENDPOINT).permitAll()
                        .requestMatchers(
                                HttpMethod.POST, PUBLIC_ENDPOINT).permitAll()
                        .requestMatchers(
                                HttpMethod.PUT, PUBLIC_ENDPOINT).permitAll()
                        .anyRequest().authenticated()
                );

        // security for jwt
        http.oauth2ResourceServer(
                oauth2 -> oauth2.jwt(jwtConfigurer ->
                        jwtConfigurer.decoder(customJWTDecoder)
                                .jwtAuthenticationConverter(converter()))
                        .authenticationEntryPoint(new JWTAuthenticationEntryPoint())
        );
        http.csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    JwtAuthenticationConverter converter (){
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return converter;
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
}
