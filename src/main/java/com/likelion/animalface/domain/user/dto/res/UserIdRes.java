package com.likelion.animalface.domain.user.dto.res;

import com.likelion.animalface.domain.user.entity.User;

public record UserIdRes(String username) {
    public static UserIdRes from(User user) {
        return new UserIdRes(user.getUsername());
    }
}
