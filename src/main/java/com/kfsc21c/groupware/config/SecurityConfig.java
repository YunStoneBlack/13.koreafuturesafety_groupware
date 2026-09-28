package com.kfsc21c.groupware.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * DB 기반 로그인 설정. 실제 계정 조회는 auth.CustomUserDetailsService가 담당하며,
 * Spring Security가 이 클래스가 등록한 PasswordEncoder 빈과 함께 자동으로 엮는다.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health", "/login", "/css/**", "/js/**", "/img/**").permitAll()
                        // nginx auth_request가 부르는 로그인 확인 — 비로그인 시 /login 리다이렉트가 아니라 401을 돌려줘야 해서
                        // 여기선 열어두고 컨트롤러가 직접 판단한다(외부 직접 접근은 nginx가 막음, ReportController 참고).
                        .requestMatchers("/internal/report-auth").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"));

        return http.build();
    }
}
