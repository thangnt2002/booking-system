package com.booking.keycloakprofileservice.configurations;

import com.booking.keycloakprofileservice.dto.requests.UserRequest;
import com.booking.keycloakprofileservice.repositories.UserRepository;
import com.booking.keycloakprofileservice.services.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AppInitialConfiguration {

    UserRepository userRepository;

    UserService userService;

    @Bean
    ApplicationRunner runner() {
        return args -> {
            if (!userRepository.existsByUsername("adminx")) {
                userService.create(UserRequest.builder()
                        .username("adminx")
                        .password("123123123")
                        .email("bookingadminx@gmail.com")
                        .firstName("thang")
                        .lastName("nguyen")
                        .build());
                log.warn("Admin account was created!!!");
            }
        };
    }

}
