package com.kfsc21c.groupware.staff;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 조직도 조회 전용. 생성/수정/삭제는 관리자 메뉴(admin.AdminEmployeeController)에서만
 * 가능하다 - 일반 직원정보 화면에는 관리 기능을 두지 않기로 함(2026-09-09).
 */
@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeRepository employeeRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("employees", employeeRepository.findAll());
        return "staff/list";
    }
}
