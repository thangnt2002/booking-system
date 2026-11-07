package com.booking.eventservice.repository;

import com.booking.eventservice.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * All stock mutations are single conditional UPDATEs: the row lock taken by the UPDATE plus
 * the WHERE guard make them atomic without SELECT ... FOR UPDATE or optimistic retries.
 * Invariant: stock_available + reserved_stock + sold = stock_initial.
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    @Query("SELECT t.stockAvailable FROM Ticket t WHERE t.id = :ticketId")
    Optional<Integer> findStockAvailable(@Param("ticketId") UUID ticketId);

    /** available -> reserved (order placed) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Ticket t SET t.updatedDate = CURRENT_TIMESTAMP," +
            "t.stockAvailable = t.stockAvailable - :quantity," +
            "t.reservedStock = t.reservedStock + :quantity " +
            "WHERE t.id = :ticketId AND t.stockAvailable >= :quantity ")
    int reservedStock(@Param("ticketId") UUID ticketId, @Param("quantity") int quantity);

    /** reserved -> sold (order paid) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Ticket t SET t.updatedDate = CURRENT_TIMESTAMP," +
            "t.reservedStock = t.reservedStock - :quantity," +
            "t.sold = t.sold + :quantity " +
            "WHERE t.id = :ticketId AND t.reservedStock >= :quantity ")
    int decreaseStock(@Param("ticketId") UUID ticketId, @Param("quantity") int quantity);

    /** reserved -> available (order cancelled / expired) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Ticket t SET t.updatedDate = CURRENT_TIMESTAMP," +
            "t.stockAvailable = t.stockAvailable + :quantity, " +
            "t.reservedStock = t.reservedStock - :quantity " +
            "WHERE t.id = :ticketId AND t.reservedStock >= :quantity")
    int restock(@Param("ticketId") UUID ticketId, @Param("quantity") int quantity);
}
