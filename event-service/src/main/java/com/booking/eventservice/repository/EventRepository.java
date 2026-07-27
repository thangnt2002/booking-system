package com.booking.eventservice.repository;

import com.booking.eventservice.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, String> {

    @Query(value = """
    SELECT TOP (:limit) * FROM event e
    WHERE 
        (:search IS NULL OR :search = '' OR e.name LIKE '%' + :search + '%')
    AND 
        (
            (:createdAt IS NULL OR :id IS NULL)
            OR 
            (e.created_at < :createdAt OR (e.created_at = :createdAt AND e.id < :id))
        )
    ORDER BY e.created_at DESC, e.id DESC
    """, nativeQuery = true)
    List<Event> findCursorPage(
            @Param("id") String id,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("limit") int limit,
            @Param("search") String search
    );
}
