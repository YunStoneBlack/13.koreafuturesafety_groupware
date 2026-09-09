package com.kfsc21c.groupware.calendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    @Query("SELECT e FROM CalendarEvent e JOIN FETCH e.creator " +
            "WHERE e.startDateTime <= :end AND e.endDateTime >= :start " +
            "ORDER BY e.startDateTime")
    List<CalendarEvent> findInRange(LocalDateTime start, LocalDateTime end);

    @Query("SELECT e FROM CalendarEvent e JOIN FETCH e.creator WHERE e.id = :id")
    Optional<CalendarEvent> findByIdWithCreator(Long id);
}
