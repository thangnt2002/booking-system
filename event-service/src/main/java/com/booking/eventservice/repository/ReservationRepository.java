package com.booking.eventservice.repository;

import com.booking.eventservice.entity.Reservation;
import com.booking.eventservice.enums.CancelReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Optional<Reservation> findByOrderId(UUID orderId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Reservation r
        SET r.status = 'CONFIRMED', r.confirmedAt = :now, r.updatedDate = :now
        WHERE r.orderId = :orderId AND r.status = 'RESERVED'""")
    int confirm(@Param("orderId") UUID orderId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Reservation r
        SET r.status = 'CANCELLED', r.cancelReason = :reason, r.cancelledAt = :now, r.updatedDate = :now
        WHERE r.orderId = :orderId AND r.status = 'RESERVED'""")
    int cancel(@Param("orderId") UUID orderId, @Param("reason") CancelReason reason, @Param("now") Instant now);
}