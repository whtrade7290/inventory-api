package com.inventory.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리 핸들러
 * IllegalArgumentException(잘못된 요청)과 IllegalStateException(재고 부족 등 비즈니스 규칙 위반)을
 * 400 Bad Request로 변환해 클라이언트에 메시지를 전달한다
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * IllegalArgumentException 처리 — 존재하지 않는 리소스 참조 등
     * 400 Bad Request와 함께 예외 메시지를 반환한다
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    /**
     * IllegalStateException 처리 — 재고 부족 등 비즈니스 규칙 위반
     * 400 Bad Request와 함께 예외 메시지를 반환한다
     * BOM 등록 시 재고 부족이면 트랜잭션이 롤백되고 이 핸들러가 응답을 생성한다
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    /** 에러 응답 DTO */
    public record ErrorResponse(String message) {}
}
