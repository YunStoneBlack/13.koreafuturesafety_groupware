package com.kfsc21c.groupware;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Locale;

@SpringBootApplication
@EnableScheduling
public class GroupwareApplication {

    public static void main(String[] args) {
        // 서버(EC2)의 JVM 기본 로케일이 영어라 #temporals.format의 'E'(요일) 패턴이
        // Mon/Tue처럼 영문으로 나왔다 - spring.mvc.locale 설정만으로는 안 바뀌어서
        // JVM 기본 로케일 자체를 여기서 고정한다.
        Locale.setDefault(Locale.KOREA);
        SpringApplication.run(GroupwareApplication.class, args);
    }
}
