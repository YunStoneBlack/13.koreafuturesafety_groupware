package com.kfsc21c.groupware.common;

import com.kfsc21c.groupware.auth.User;
import com.kfsc21c.groupware.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 로그인한 사용자의 표시이름/권한을 모든 화면(사이드바 하단 프로필 영역)에서 쓸 수 있도록
 * 매 요청마다 모델에 "currentUser"로 채워 넣는다. UserDetails(Spring Security 원칙)에는
 * displayName이 없어서 실제 User 엔티티를 다시 조회해 넣는다.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class CurrentUserAdvice {

    private final UserRepository userRepository;

    @ModelAttribute("currentUser")
    public User currentUser(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return null;
        }
        return userRepository.findByUsername(principal.getUsername()).orElse(null);
    }
}
