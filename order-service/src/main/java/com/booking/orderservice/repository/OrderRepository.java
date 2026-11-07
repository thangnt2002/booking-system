package com.booking.orderservice.repository;

import com.booking.orderservice.entities.Order;
import com.booking.orderservice.enums.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Compare-and-set state transition. Returns 1 for the single winner, 0 for everyone
     * else - no distributed lock needed.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Order o
            SET o.status = :to, o.updatedDate = :now, o.version = o.version + 1
            WHERE o.id = :id AND o.status = :from""")
    int transition(@Param("id") UUID id,
                   @Param("from") OrderStatus from,
                   @Param("to") OrderStatus to,
                   @Param("now") Instant now);

    /** Payment is only accepted while the hold is still valid. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Order o
            SET o.status = com.booking.orderservice.enums.OrderStatus.SUCCESS,
                o.updatedDate = :now, o.version = o.version + 1
            WHERE o.id = :id
              AND o.status = com.booking.orderservice.enums.OrderStatus.PENDING
              AND o.expireTime > :now""")
    int markPaid(@Param("id") UUID id, @Param("now") Instant now);

    /**
     * Claims a batch of overdue orders. READPAST lets several instances run the expiry job
     * in parallel on disjoint rows; UPDLOCK blocks a concurrent cancel/pay until commit.
     */
    @Query(value = """
            SELECT TOP (:limit) * FROM orders WITH (UPDLOCK, READPAST, ROWLOCK)
            WHERE status = 'PENDING' AND expire_time < :now
            ORDER BY expire_time""", nativeQuery = true)
    List<Order> lockExpired(@Param("now") Instant now, @Param("limit") int limit);

    @Query("""
            SELECT o FROM Order o
            WHERE o.customerId = :customerId AND o.orderNumber LIKE :search
            ORDER BY o.createdDate DESC, o.id DESC""")
    List<Order> findFirstPage(@Param("customerId") UUID customerId,
                              @Param("search") String search,
                              Pageable pageable);

    /** Keyset pagination: stable and O(limit) regardless of how deep the user scrolls. */
    @Query("""
            SELECT o FROM Order o
            WHERE o.customerId = :customerId AND o.orderNumber LIKE :search
              AND (o.createdDate < :createdDate OR (o.createdDate = :createdDate AND o.id < :id))
            ORDER BY o.createdDate DESC, o.id DESC""")
    List<Order> findPageAfter(@Param("customerId") UUID customerId,
                              @Param("search") String search,
                              @Param("createdDate") Instant createdDate,
                              @Param("id") UUID id,
                              Pageable pageable);
}
