package com.kfsc21c.groupware.calendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * 홈 화면 "다가오는 일정" 목록에 D-day 배지를 붙이기 위한 뷰 전용 래퍼.
 * 색상 규칙: 오늘(D-0)은 빨강 + "today" 표시, 이번 주(일~토) 안이면 진한 주황,
 * 그 외는 파랑 - Thymeleaf에서 날짜 연산을 직접 하는 대신 여기서 미리 계산해 넘긴다.
 */
public record UpcomingEventView(Long id, String title, LocalDateTime startDateTime,
                                 String ddayLabel, String ddayClass) {

    public static UpcomingEventView from(CalendarEvent event) {
        LocalDate today = LocalDate.now();
        LocalDate eventDate = event.getStartDateTime().toLocalDate();
        long daysUntil = ChronoUnit.DAYS.between(today, eventDate);

        String label;
        String cssClass;
        if (daysUntil == 0) {
            label = "today";
            cssClass = "dday-today";
        } else {
            label = "D-" + daysUntil;
            LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
            LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
            boolean withinThisWeek = !eventDate.isBefore(weekStart) && !eventDate.isAfter(weekEnd);
            cssClass = withinThisWeek ? "dday-week" : "dday-later";
        }

        return new UpcomingEventView(event.getId(), event.getTitle(), event.getStartDateTime(), label, cssClass);
    }
}
