package com.kfsc21c.groupware.calendar;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 날짜(input type=date)와 시간(input type=time)을 분리된 필드로 받는 폼 전용 객체.
 * datetime-local 하나로 합쳐서 받으면 브라우저 네이티브 위젯이 로케일에 따라
 * 12시간(오전/오후) 표기로 바뀌는 문제가 있어(핵심기술.md 참고), date+time을 각각
 * 받아 서버에서 LocalDateTime으로 합친다 - 사용자가 시간을 직접 타이핑하기도 더 쉽다.
 */
public class CalendarEventForm {

    private Long id;
    private String title;
    private String description;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public static CalendarEventForm from(CalendarEvent event) {
        CalendarEventForm form = new CalendarEventForm();
        form.setId(event.getId());
        form.setTitle(event.getTitle());
        form.setDescription(event.getDescription());
        form.setStartDate(event.getStartDateTime().toLocalDate());
        form.setStartTime(event.getStartDateTime().toLocalTime());
        form.setEndDate(event.getEndDateTime().toLocalDate());
        form.setEndTime(event.getEndDateTime().toLocalTime());
        return form;
    }

    public LocalDateTime startDateTime() {
        return LocalDateTime.of(startDate, startTime);
    }

    public LocalDateTime endDateTime() {
        return LocalDateTime.of(endDate, endTime);
    }
}
