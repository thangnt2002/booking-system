package com.booking.keycloakprofileservice.configurations;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Supplier;

@Component
public class DynamicAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    @Override
    @Nullable
    public AuthorizationResult authorize(
            Supplier<? extends @Nullable Authentication> authentication,
            RequestAuthorizationContext context
    ) {
        HttpServletRequest request = context.getRequest();

        Authentication auth = authentication.get();

        if (auth == null || Objects.equals(auth.getPrincipal(), "anonymousUser") || !auth.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }

        // TODO check db/redis
        boolean isGranted = customCheckLogic(auth, request);

        return new AuthorizationDecision(isGranted);
    }

    private boolean customCheckLogic(Authentication auth, HttpServletRequest request) {
        // TODO check bussinessRule
        return true;
    }
}
