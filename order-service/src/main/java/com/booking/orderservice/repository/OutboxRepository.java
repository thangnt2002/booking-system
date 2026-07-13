package com.booking.orderservice.repository;

import com.booking.orderservice.enums.OutboxStatus;
import com.booking.orderservice.outbox.entity.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxMessage, String> {
   List<OutboxMessage> findByStatus(OutboxStatus status);

   @Modifying
   @Transactional
   @Query(value = "UPDATE outbox_message SET published_at = CURRENT_TIMESTAMP," +
           "status = :status WHERE id = :id",
           nativeQuery = true)
   void updateStatus(@Param("id") String id, @Param("status") String status);
}
