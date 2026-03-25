package com.likelion.animalface.domain.user.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "임시 비밀번호 발급 응답 DTO")
public record UserPasswordRes(
        @Schema(description = "안내 메시지", example = "임시 비밀번호가 발급되었습니다.")
        String message,
        @Schema(description = "발급된 8자리 임시 비밀번호", example = "a1b2c3d4")
        String tempPassword
) {
    public static UserPasswordRes of(String tempPassword) {
        return new UserPasswordRes("임시 비밀번호가 발급되었습니다.", tempPassword);
    }
}