package com.likelion.animalface.domain.user.dto.res;

import com.likelion.animalface.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "아이디 찾기 응답 DTO")
public record UserIdRes(
        @Schema(description = "조회된 사용자 아이디", example = "likelion123")
        String username
) {
    public static UserIdRes from(User user) {
        return new UserIdRes(user.getUsername());
    }
}
