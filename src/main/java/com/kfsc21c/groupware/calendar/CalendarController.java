package com.kfsc21c.groupware.calendar;

import com.kfsc21c.groupware.auth.User;
import com.kfsc21c.groupware.auth.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalTime;

@Controller
@RequestMapping("/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarEventRepository calendarEventRepository;
    private final UserRepository userRepository;

    private User currentUser(UserDetails principal) {
        return userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("로그인한 사용자를 찾을 수 없습니다"));
    }

    private boolean canManage(CalendarEvent event, UserDetails principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return isAdmin || event.getCreator().getUsername().equals(principal.getUsername());
    }

    @GetMapping
    public String index() {
        return "calendar/index";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           Model model) {
        LocalDate day = date != null ? date : LocalDate.now();
        CalendarEventForm form = new CalendarEventForm();
        form.setStartDate(day);
        form.setStartTime(LocalTime.of(9, 0));
        form.setEndDate(day);
        form.setEndTime(LocalTime.of(10, 0));
        model.addAttribute("form", form);
        return "calendar/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CalendarEventForm form, BindingResult result,
                          @AuthenticationPrincipal UserDetails principal) {
        if (result.hasErrors()) {
            return "calendar/form";
        }
        CalendarEvent event = new CalendarEvent();
        event.setTitle(form.getTitle());
        event.setDescription(form.getDescription());
        event.setStartDateTime(form.startDateTime());
        event.setEndDateTime(form.endDateTime());
        event.setCreator(currentUser(principal));
        calendarEventRepository.save(event);
        return "redirect:/calendar";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal, Model model) {
        CalendarEvent event = calendarEventRepository.findByIdWithCreator(id)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다: " + id));
        if (!canManage(event, principal)) {
            throw new AccessDeniedException("이 일정을 수정할 권한이 없습니다");
        }
        model.addAttribute("form", CalendarEventForm.from(event));
        return "calendar/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") CalendarEventForm form, BindingResult result,
                          @AuthenticationPrincipal UserDetails principal) {
        CalendarEvent event = calendarEventRepository.findByIdWithCreator(id)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다: " + id));
        if (!canManage(event, principal)) {
            throw new AccessDeniedException("이 일정을 수정할 권한이 없습니다");
        }
        if (result.hasErrors()) {
            return "calendar/form";
        }
        event.setTitle(form.getTitle());
        event.setDescription(form.getDescription());
        event.setStartDateTime(form.startDateTime());
        event.setEndDateTime(form.endDateTime());
        calendarEventRepository.save(event);
        return "redirect:/calendar";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        CalendarEvent event = calendarEventRepository.findByIdWithCreator(id)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다: " + id));
        if (!canManage(event, principal)) {
            throw new AccessDeniedException("이 일정을 삭제할 권한이 없습니다");
        }
        calendarEventRepository.deleteById(id);
        return "redirect:/calendar";
    }
}
