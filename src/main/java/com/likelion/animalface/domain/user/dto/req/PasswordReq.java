package com.likelion.animalface.domain.user.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "임시 비밀번호 발급 요청 DTO")
public record PasswordReq(
        @Schema(description = "사용자 아이디", example = "likelion123")
        String username,
        @Schema(description = "가입 시 등록한 전화번호 (하이픈 없이)", example = "01012345678")
        String phone
) {}
