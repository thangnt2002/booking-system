package com.booking.identityservice.configuration;


import com.booking.identityservice.entity.Role;
import com.booking.identityservice.entity.User;
import com.booking.identityservice.enums.RoleName;
import com.booking.identityservice.repository.RoleRepository;
import com.booking.identityservice.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AppInitialConfiguration {

    @NonFinal
    String ADMIN = "admin";

    UserRepository userRepository;
    RoleRepository roleRepository;
    PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner runner() {
        return args -> {
            if (!userRepository.existsByUsername(ADMIN)) {

                Role adminRole = roleRepository.findById(RoleName.ADMIN.name())
                        .orElseGet(() -> roleRepository.save(Role.builder()
                                .name(RoleName.ADMIN.name())
                                .description(RoleName.ADMIN.name())
                                .build()));

                Set<Role> roles = new HashSet<>();
                roles.add(adminRole);
                userRepository.save(User.builder()
                        .username(ADMIN)
                        .password(passwordEncoder.encode(ADMIN))
                        .roles(roles)
                        .build());
                log.warn("Admin account was created!!!");
            }
        };
    }

}
