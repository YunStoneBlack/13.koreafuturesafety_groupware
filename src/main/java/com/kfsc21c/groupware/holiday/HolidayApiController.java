package com.kfsc21c.groupware.holiday;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class HolidayApiController {

    private final KoreanHolidayService koreanHolidayService;

    @GetMapping
    public List<HolidayEntry> holidays(@RequestParam int year) {
        return koreanHolidayService.forYear(year);
    }
}
