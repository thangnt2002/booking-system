package com.booking.identityservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "token")
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "jti", nullable = false, unique = true)
    String jti;

    @Column(name = "is_revoked", nullable = false)
    boolean isRevoked;

    @Column(name = "revoked_at")
    Date revokedAt;

    @Column(name = "expiry_time", nullable = false)
    Date expiryTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    Date createdAt;
}
