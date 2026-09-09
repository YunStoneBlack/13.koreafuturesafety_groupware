package com.kfsc21c.groupware.common;

import com.kfsc21c.groupware.approval.ApprovalRequest;
import com.kfsc21c.groupware.approval.ApprovalRequestRepository;
import com.kfsc21c.groupware.approval.ApprovalStatus;
import com.kfsc21c.groupware.board.Post;
import com.kfsc21c.groupware.board.PostRepository;
import com.kfsc21c.groupware.calendar.CalendarEvent;
import com.kfsc21c.groupware.calendar.CalendarEventRepository;
import com.kfsc21c.groupware.calendar.UpcomingEventView;
import com.kfsc21c.groupware.staff.Employee;
import com.kfsc21c.groupware.staff.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final PostRepository postRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final EmployeeRepository employeeRepository;

    @GetMapping("/")
    public String home(Model model) {
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();
        List<ApprovalRequest> approvals = approvalRequestRepository.findAllWithUsers();
        List<Employee> employees = employeeRepository.findAll();

        LocalDate today = LocalDate.now();
        List<CalendarEvent> upcoming = calendarEventRepository.findInRange(
                today.atStartOfDay(), today.plusDays(30).atStartOfDay());

        List<ApprovalRequest> pending = approvals.stream()
                .filter(a -> a.getStatus() == ApprovalStatus.SUBMITTED)
                .toList();

        long weekPosts = posts.stream()
                .filter(p -> p.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7)))
                .count();

        long todayEvents = upcoming.stream()
                .filter(e -> e.getStartDateTime().toLocalDate().equals(today))
                .count();

        Map<String, Long> byDept = employees.stream()
                .collect(Collectors.groupingBy(Employee::getDepartment, LinkedHashMap::new, Collectors.counting()));

        model.addAttribute("recentPosts", posts.stream().limit(3).toList());
        model.addAttribute("pendingApprovals", pending.stream().limit(3).toList());
        model.addAttribute("upcomingEvents", upcoming.stream().limit(3).map(UpcomingEventView::from).toList());
        model.addAttribute("pendingCount", pending.size());
        model.addAttribute("todayEventCount", todayEvents);
        model.addAttribute("weekPostCount", weekPosts);
        model.addAttribute("employeeCount", employees.size());
        model.addAttribute("deptCounts", byDept);

        return "home";
    }
}
