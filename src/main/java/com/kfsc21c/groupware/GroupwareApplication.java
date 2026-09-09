package com.kfsc21c.groupware;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GroupwareApplication {

    public static void main(String[] args) {
        SpringApplication.run(GroupwareApplication.class, args);
    }
}
