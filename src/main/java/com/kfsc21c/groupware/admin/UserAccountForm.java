package com.kfsc21c.groupware.admin;

import com.kfsc21c.groupware.auth.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 계정 생성/수정 폼 전용 객체. User 엔티티를 폼 바인딩에 직접 쓰지 않는 이유:
 * 엔티티는 passwordHash(암호화된 값)만 갖고 있는데, 폼에서는 평문 비밀번호를
 * 입력받아야 하고, 수정 화면에서는 "비워두면 기존 비밀번호 유지"가 필요하기 때문.
 */
@Getter
@Setter
@NoArgsConstructor
public class UserAccountForm {
    private String username;
    private String password;
    private String displayName;
    private Role role = Role.USER;
    private boolean enabled = true;
    private Long employeeId;
}
