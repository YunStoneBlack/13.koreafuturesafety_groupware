package com.kfsc21c.groupware.config;

import com.kfsc21c.groupware.auth.Role;
import com.kfsc21c.groupware.auth.User;
import com.kfsc21c.groupware.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 최초 기동 시 관리자 계정이 하나도 없으면 하나 만든다.
 * 이후 재기동에서는 이미 계정이 있으므로 아무 것도 하지 않는다(비밀번호가 매번 바뀌지 않음).
 *
 * 비밀번호는 GROUPWARE_ADMIN_INITIAL_PASSWORD 환경변수로 지정하거나,
 * 지정하지 않으면 무작위로 생성해 기동 로그에 한 번만 출력한다 — 로그에서 즉시 확인 후
 * 안전한 곳에 옮겨 적어야 한다(현재 버전엔 비밀번호 변경 UI가 없음, 추후 추가 예정).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${GROUPWARE_ADMIN_INITIAL_PASSWORD:}")
    private String configuredInitialPassword;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        String rawPassword = (configuredInitialPassword != null && !configuredInitialPassword.isBlank())
                ? configuredInitialPassword
                : generateRandomPassword();

        User admin = User.builder()
                .username("admin")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .displayName("관리자")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        userRepository.save(admin);

        log.warn("========================================================");
        log.warn("초기 관리자 계정이 생성되었습니다.");
        log.warn("username: admin");
        log.warn("password: {}", rawPassword);
        log.warn("이 비밀번호는 다시 표시되지 않습니다. 지금 안전한 곳에 기록하세요.");
        log.warn("========================================================");
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#%";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
