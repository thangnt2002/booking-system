package com.booking.eventservice.repository;

import com.booking.eventservice.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    @Query(value = """
    SELECT TOP (:limit) * FROM events e
    WHERE
        (:search IS NULL OR :search = '' OR e.name LIKE '%' + :search + '%')
    AND
        (
            (:createdAt IS NULL OR :id IS NULL)
            OR
            (e.created_date < :createdAt OR (e.created_date = :createdAt AND e.id < :id))
        )
    ORDER BY e.created_date DESC, e.id DESC
    """, nativeQuery = true)
    List<Event> findCursorPage(
            @Param("id") String id,
            @Param("createdAt") Instant createdAt,
            @Param("limit") int limit,
            @Param("search") String search
    );
}
