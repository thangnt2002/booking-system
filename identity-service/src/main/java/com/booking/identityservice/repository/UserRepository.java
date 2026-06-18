package com.booking.identityservice.repository;

import com.booking.identityservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    boolean existsById(String id);
    User findByUsername(String username);
    boolean existsByUsername(String username);

}
