package com.kfsc21c.groupware;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Locale;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class GroupwareApplication {

    public static void main(String[] args) {
        // 서버(EC2)의 JVM 기본 로케일이 영어라 #temporals.format의 'E'(요일) 패턴이
        // Mon/Tue처럼 영문으로 나왔다 - spring.mvc.locale 설정만으로는 안 바뀌어서
        // JVM 기본 로케일 자체를 여기서 고정한다.
        Locale.setDefault(Locale.KOREA);

        // 서버 OS 시간대가 UTC라서 LocalDate.now()/LocalDateTime.now()(전부 시스템 기본
        // 시간대 기준)가 한국 시간보다 하루 늦게 나오는 경우가 있었다(예: 한국 09.10
        // 오전인데 서버는 아직 09.09 UTC라 "오늘" 계산이 하루 밀림 - D-day 배지에서
        // 실측 확인). 서버 OS 자체를 바꾸는 대신 JVM 기본 시간대를 한국으로 고정.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));

        SpringApplication.run(GroupwareApplication.class, args);
    }
}
