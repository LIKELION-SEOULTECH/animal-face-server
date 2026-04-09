package com.likelion.animalface.global.advice;

import com.likelion.animalface.global.dto.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 엔티티를 찾을 수 없는 경우 (404)
     * 예: 없는 userId로 조회
     */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(EntityNotFoundException.class)
    public ApiResponse<String> handleEntityNotFound(EntityNotFoundException e) {
        return ApiResponse.error(e.getMessage());
    }

    /**
     * 비즈니스 규칙 위반 (400)
     * 예: 중복 아이디, 정보 불일치
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<String> handleIllegalArgument(IllegalArgumentException e) {
        return ApiResponse.error(e.getMessage());
    }
}
