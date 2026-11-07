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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Ticket t SET t.updatedDate = CURRENT_TIMESTAMP," +
            "t.stockAvailable = t.stockAvailable - :quantity," +
            "t.reservedStock = t.reservedStock + :quantity " +
            "WHERE t.id = :ticketId AND t.stockAvailable >= :quantity ")
    int reservedStock(@Param("ticketId") UUID ticketId, @Param("quantity") int quantity);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Ticket t SET t.updatedDate = CURRENT_TIMESTAMP," +
            "t.reservedStock = t.reservedStock - :quantity," +
            "t.sold = t.sold + :quantity " +
            "WHERE t.id = :ticketId AND t.reservedStock >= :quantity ")
    int decreaseStock(@Param("ticketId") UUID ticketId, @Param("quantity") int quantity);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Ticket t SET t.updatedDate = CURRENT_TIMESTAMP," +
            "t.stockAvailable = t.stockAvailable + :quantity, " +
            "t.reservedStock = t.reservedStock - :quantity " +
            "WHERE t.id = :ticketId AND t.reservedStock >= :quantity")
    int restock(@Param("ticketId") UUID ticketId, @Param("quantity") int quantity);
}
