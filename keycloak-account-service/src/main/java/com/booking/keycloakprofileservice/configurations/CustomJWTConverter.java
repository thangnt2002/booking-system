package com.booking.keycloakprofileservice.configurations;

import com.booking.keycloakprofileservice.entities.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class CustomJWTConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        log.info("[JwtConverter] Starting JWT conversion for subject: {}", jwt.getId());

        UUID id = UUID.fromString(jwt.getClaim("sub"));
        String name = jwt.getClaim("name");
        String username = jwt.getClaim("preferred_username");
        String email = jwt.getClaim("email");

        List<String> roles = new ArrayList<>();
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            roles = (List<String>) realmAccess.get("roles");
        }

        Collection<GrantedAuthority> authorities = Stream.concat(
                jwtGrantedAuthoritiesConverter.convert(jwt).stream(),
                extractResourceRoles(jwt).stream()).collect(Collectors.toSet());

        UserPrincipal userPrincipal = new UserPrincipal(id, name, username, email, authorities, roles);
        return new UsernamePasswordAuthenticationToken(userPrincipal, jwt, authorities);
    }

    private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess != null && realmAccess.containsKey("roles")) {
            log.info("[common][SecurityConfig] getting roles from token");

            List<String> roles = (List<String>) realmAccess.get("roles");
            authorities.addAll(roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toSet()));
        }

        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            Map<String, Object> accountRoles = (Map<String, Object>) resourceAccess.get("bookingClient");
            if (accountRoles != null && accountRoles.containsKey("roles")) {
                log.info("[JwtConverter] Getting permissions from token");

                List<String> rolePermissions = (List<String>) accountRoles.get("roles");
                authorities.addAll(rolePermissions.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toSet()));
            } else {
                log.warn("[JwtConverter] No roles found in resource_access");
            }
        }

        return authorities;
    }
}
