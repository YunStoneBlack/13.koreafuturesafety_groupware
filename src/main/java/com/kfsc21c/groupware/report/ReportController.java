package com.kfsc21c.groupware.report;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * "12. (주)한국미래안전 보고서 작성 자동화 프로그램" 완성 후 이식될 자리.
 * 지금은 안내 페이지 하나만 둔다 - 완성되면 이 컨트롤러를 실제 기능으로 교체.
 */
@Controller
public class ReportController {

    @GetMapping("/report")
    public String comingSoon() {
        return "report/coming-soon";
    }
}
