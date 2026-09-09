package com.kfsc21c.groupware.common;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 컨트롤러들이 "대상을 못 찾음"을 표현하는 데 공통으로 IllegalArgumentException을
 * 쓰고 있는데(findById().orElseThrow(...)), 별도 처리가 없으면 500(스프링 기본
 * 에러 화면)으로 나간다. 여기서 404로 바꾸고 우리 디자인에 맞는 페이지를 보여준다.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public String notFound(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return "error/404";
    }
}
