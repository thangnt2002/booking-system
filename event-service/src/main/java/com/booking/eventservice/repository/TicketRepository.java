package com.booking.eventservice.repository;

import com.booking.eventservice.entity.Ticket;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    @Modifying
    @Transactional
    @Query("UPDATE Ticket t SET t.updatedAt = CURRENT_TIMESTAMP," +
            "t.stockAvailable = t.stockAvailable - :quantity," +
            "t.reservedStock = t.reservedStock + :quantity " +
            "WHERE t.id = :ticketId AND t.stockAvailable > :quantity ")
    int decreaseStock(@Param("ticketId") UUID ticketId, @Param("quantity") int quantity);

    @Modifying
    @Transactional
    @Query("UPDATE Ticket t SET t.updatedAt = CURRENT_TIMESTAMP," +
            "t.stockAvailable = t.stockAvailable + :quantity " +
            "WHERE t.id = :ticketId")
    boolean restock(@Param("ticketId") UUID ticketId, @Param("quantity") int quantity);
}
