package com.kfsc21c.groupware.payroll;

import com.kfsc21c.groupware.auth.User;
import com.kfsc21c.groupware.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * 급여는 민감정보라, 여기(직원용)에서는 로그인한 사용자 본인에게 연결된
 * Employee의 급여 내역만 보여준다. 전체 직원 급여를 등록/수정/삭제하는 건
 * admin.AdminPayrollController(/admin/payroll, ADMIN 전용)에서만 가능하다.
 */
@Controller
@RequestMapping("/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollRecordRepository payrollRecordRepository;
    private final UserRepository userRepository;

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userRepository.findByUsernameWithEmployee(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("로그인한 사용자를 찾을 수 없습니다"));

        if (user.getEmployee() == null) {
            model.addAttribute("records", List.<PayrollRecord>of());
            model.addAttribute("noEmployeeLink", true);
        } else {
            model.addAttribute("records",
                    payrollRecordRepository.findByEmployeeOrderByPayYearDescPayMonthDesc(user.getEmployee()));
            model.addAttribute("noEmployeeLink", false);
        }
        return "payroll/list";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userRepository.findByUsernameWithEmployee(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("로그인한 사용자를 찾을 수 없습니다"));
        PayrollRecord record = payrollRecordRepository.findByIdWithEmployee(id)
                .orElseThrow(() -> new IllegalArgumentException("급여 내역을 찾을 수 없습니다: " + id));

        if (user.getEmployee() == null || !user.getEmployee().getId().equals(record.getEmployee().getId())) {
            throw new AccessDeniedException("본인의 급여 내역만 조회할 수 있습니다");
        }
        model.addAttribute("record", record);
        return "payroll/view";
    }
}
