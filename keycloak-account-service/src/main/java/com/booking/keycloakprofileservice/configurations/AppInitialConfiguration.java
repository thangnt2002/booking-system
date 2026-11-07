package com.booking.keycloakprofileservice.configurations;

import com.booking.keycloakprofileservice.dto.requests.UserRequest;
import com.booking.keycloakprofileservice.enums.RoleName;
import com.booking.keycloakprofileservice.services.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Seeds the admin account and a few mock users on startup. Idempotent: existing accounts are only
 * linked to their Keycloak id (if missing) and granted any missing role.
 * Mock users are created only when app.seed.mock-users=true (default true).
 */
@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AppInitialConfiguration {

    static final String DEFAULT_PASSWORD = "123123123";

    UserService userService;

    @Bean
    ApplicationRunner runner(@org.springframework.beans.factory.annotation.Value("${app.seed.mock-users:true}") boolean seedMockUsers) {
        return args -> {
            seed(user("adminx", "bookingadminx@gmail.com", "thang", "nguyen", "Hà Nội", LocalDate.of(1995, 1, 10)),
                    Set.of(RoleName.ADMIN, RoleName.USER));

            if (!seedMockUsers) return;
            List<UserRequest> mocks = List.of(
                    user("user01", "user01@booking.local", "An", "Nguyễn Văn", "Hà Nội", LocalDate.of(1998, 3, 12)),
                    user("user02", "user02@booking.local", "Bình", "Trần Thị", "TP. Hồ Chí Minh", LocalDate.of(2000, 7, 25)),
                    user("user03", "user03@booking.local", "Cường", "Lê Minh", "Đà Nẵng", LocalDate.of(1993, 11, 2)));
            mocks.forEach(m -> seed(m, Set.of(RoleName.USER)));
        };
    }

    private void seed(UserRequest request, Set<RoleName> roles) {
        try {
            userService.ensureUser(request, roles);
            log.info("[Seed] {} ready with roles {}", request.getUsername(), roles);
        } catch (Exception e) {
            // seeding must never stop the service from starting (e.g. Keycloak still booting)
            log.error("[Seed] Could not seed {}", request.getUsername(), e);
        }
    }

    private static UserRequest user(String username, String email, String firstName, String lastName,
                                    String city, LocalDate dob) {
        return UserRequest.builder()
                .username(username)
                .password(DEFAULT_PASSWORD)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .city(city)
                .dob(dob)
                .avatar("https://i.pravatar.cc/150?u=" + username)
                .build();
    }
}
