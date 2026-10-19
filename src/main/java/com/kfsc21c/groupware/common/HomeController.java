package com.kfsc21c.groupware.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 홈 = 보고서 자동화·서류 자동화 입구 카드 두 개만(2026-10-20 사용자). 공지·결재·일정·직원 칸은 왼쪽 메뉴 각 화면에서 보므로
 * 여기서는 아무 자료도 읽지 않는다(인사말의 currentUser는 CurrentUserAdvice가 채움).
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "home";
    }
}
