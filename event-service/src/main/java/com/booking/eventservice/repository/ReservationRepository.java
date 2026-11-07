package com.booking.eventservice.repository;

import com.booking.common.events.CancelReason;
import com.booking.eventservice.entity.Reservation;
import com.booking.eventservice.enums.ReservationStatus;
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

    /** RESERVED -> CONFIRMED; returns 1 only for the first (winning) caller. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Reservation r
            SET r.status = :confirmed, r.confirmedAt = :now, r.updatedDate = :now
            WHERE r.orderId = :orderId AND r.status = :reserved""")
    int transitionToConfirmed(@Param("orderId") UUID orderId,
                              @Param("reserved") ReservationStatus reserved,
                              @Param("confirmed") ReservationStatus confirmed,
                              @Param("now") Instant now);

    /** RESERVED -> CANCELLED; returns 1 only for the first (winning) caller. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Reservation r
            SET r.status = :cancelled, r.cancelReason = :reason, r.cancelledAt = :now, r.updatedDate = :now
            WHERE r.orderId = :orderId AND r.status = :reserved""")
    int transitionToCancelled(@Param("orderId") UUID orderId,
                              @Param("reserved") ReservationStatus reserved,
                              @Param("cancelled") ReservationStatus cancelled,
                              @Param("reason") CancelReason reason,
                              @Param("now") Instant now);

    default int confirm(UUID orderId, Instant now) {
        return transitionToConfirmed(orderId, ReservationStatus.RESERVED, ReservationStatus.CONFIRMED, now);
    }

    default int cancel(UUID orderId, CancelReason reason, Instant now) {
        return transitionToCancelled(orderId, ReservationStatus.RESERVED, ReservationStatus.CANCELLED, reason, now);
    }
}
