package com.kfsc21c.groupware.calendar;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarApiController {

    private final CalendarEventRepository calendarEventRepository;

    /**
     * FullCalendar가 보는 화면(월 뷰)에 걸친 기간의 이벤트만 조회한다.
     * start/end는 프론트에서 "yyyy-MM-dd" 형태로만 보내도록 고정해서
     * 타임존이 섞인 ISO 문자열 파싱을 피한다(templates/calendar/index.html 참고).
     */
    @GetMapping("/events")
    public List<EventDto> events(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return calendarEventRepository.findInRange(start.atStartOfDay(), end.atStartOfDay()).stream()
                .map(EventDto::from)
                .toList();
    }
}
