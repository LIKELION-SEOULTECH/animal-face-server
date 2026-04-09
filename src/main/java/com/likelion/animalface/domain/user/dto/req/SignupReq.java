package com.likelion.animalface.domain.user.dto.req;

import com.likelion.animalface.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 요청 DTO")
public record SignupReq(
        @Schema(description = "사용자 아이디", example = "likelion123")
        String username,
        @Schema(description = "비밀번호", example = "password1234!")
        String password,
        @Schema(description = "전화번호 (하이픈 없이)", example = "01012345678")
        String phone
) {
    public User to(String encodedPassword) {
        return User.create(username, encodedPassword, phone);
    }
}

