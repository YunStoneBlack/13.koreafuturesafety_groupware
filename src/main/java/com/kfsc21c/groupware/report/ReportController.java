package com.kfsc21c.groupware.report;

import com.kfsc21c.groupware.auth.User;
import com.kfsc21c.groupware.auth.UserRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * "보고서 자동화" 메뉴 — 실제 기능은 별도 프로젝트(12-1. 보고서 작성 자동화 프로그램 웹판)가 보고서 PC(윈도우 + 한글)에서
 * 돌고, nginx가 https://groupware.kfsc21c.com/report/... 를 SSH 통로로 그 PC에 넘긴다(한글 PDF 변환이 리눅스에선 불가라서).
 * 그룹웨어는 그 앞에서 두 가지만 해준다:
 *
 * 1. {@code /internal/report-auth} — nginx {@code auth_request} 전용 "로그인했나?" 확인. 로그인 상태면 204 + 사용자 정보 헤더,
 *    아니면 401(로그인 화면으로 리다이렉트하지 않음 — nginx가 401을 보고 직접 /login 으로 보낸다). 외부에서 직접 부르는 건
 *    nginx가 막는다({@code location /internal/ { return 404; }}). 보고서 서버는 이 헤더로 로그인 없이 사용자를 알아본다.
 * 2. {@code /report-shell/sidebar} — 이 그룹웨어의 사이드바 조각만 렌더링해서 돌려준다. 보고서 화면이 이걸 그대로 끼워 넣어
 *    로고·메뉴·관리자 메뉴·프로필·로그아웃까지 그룹웨어와 똑같이 보인다(메뉴가 바뀌어도 자동으로 따라감).
 *
 * {@code /report} 안내 페이지는 nginx가 /report 를 가로채므로 평소엔 안 쓰이고, nginx 규칙을 되돌렸을 때만 보인다.
 */
@Controller
@RequiredArgsConstructor
public class ReportController {

    private final UserRepository userRepository;

    @GetMapping("/report")
    public String comingSoon() {
        return "report/coming-soon";
    }

    @GetMapping("/report-shell/sidebar")
    public String sidebar() {
        // 뷰 이름에 붙는 조각 인자는 이름을 붙여야 한다(Thymeleaf: 위치 인자면 "must be named" 오류로 렌더 실패)
        return "fragments/appshell :: sidebar(active='report')";
    }

    @GetMapping("/internal/report-auth")
    public ResponseEntity<Void> reportAuth(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || !user.isEnabled()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.noContent()
                .header("X-Gw-User", encode(user.getUsername()))
                .header("X-Gw-Name", encode(user.getDisplayName()))
                .header("X-Gw-Role", user.getRole().name())
                .build();
    }

    /** HTTP 헤더는 ASCII만 안전하므로 한글 이름은 URL 인코딩해서 보낸다(보고서 서버가 디코딩). */
    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
