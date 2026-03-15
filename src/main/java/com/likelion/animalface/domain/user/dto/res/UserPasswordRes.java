package com.likelion.animalface.domain.user.dto.res;

public record UserPasswordRes(String message, String tempPassword) {
    public static UserPasswordRes of(String tempPassword) {
        return new UserPasswordRes("임시 비밀번호가 발급되었습니다.", tempPassword);
    }
}