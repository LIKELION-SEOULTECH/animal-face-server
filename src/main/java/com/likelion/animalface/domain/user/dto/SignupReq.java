package com.likelion.animalface.domain.user.dto;

import com.likelion.animalface.domain.user.entity.User;

public record SignupReq(String username, String password, String phone) {
    public User to(String encodedPassword) {
        return User.builder()
                .username(username)
                .password(encodedPassword)
                .phone(phone)
                .build();
    }
}

