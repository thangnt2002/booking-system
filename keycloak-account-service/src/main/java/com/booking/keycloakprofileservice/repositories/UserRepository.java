package com.booking.keycloakprofileservice.repositories;

import com.booking.keycloakprofileservice.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);

    /** userId = Keycloak user id (JWT sub). */
    Optional<User> findByUserId(String userId);
}