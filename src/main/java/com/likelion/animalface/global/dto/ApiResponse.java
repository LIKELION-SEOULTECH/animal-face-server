package com.likelion.animalface.global.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiResponse<T>(
        boolean success,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        T data,
        String message
) {
    // 1. 데이터와 함께 성공 응답을 보낼 때 (기존과 동일)
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, "요청이 성공적으로 처리되었습니다.");
    }

    // 2. [수정] 모든 클래스를 받을 수 있는 ok 메서드
    // 반환 타입을 ApiResponse<T>로 지정하고 제네릭 메서드로 선언합니다.
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, "성공");
    }

    // 3. 단순 메시지만 보내고 싶을 때 (데이터는 null)
    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>(true, null, message);
    }

    public static ApiResponse<String> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}