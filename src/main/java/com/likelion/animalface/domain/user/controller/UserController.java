package com.likelion.animalface.domain.user.controller;

import com.likelion.animalface.domain.user.dto.req.FindIdReq;
import com.likelion.animalface.domain.user.dto.req.PasswordReq;
import com.likelion.animalface.domain.user.dto.req.SignupReq;
import com.likelion.animalface.domain.user.dto.res.UserIdRes;
import com.likelion.animalface.domain.user.dto.res.UserPasswordRes;
import com.likelion.animalface.domain.user.service.UserService;
import com.likelion.animalface.global.dto.ApiResponse; // 공통 응답 객체
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ApiResponse<String> signup(@RequestBody SignupReq req) {
        userService.signup(req);
        return ApiResponse.success("회원가입 완료");
    }

    @PostMapping("/id")
    public ApiResponse<UserIdRes> findId(@RequestBody FindIdReq req) {
        UserIdRes res = userService.getUsername(req.phone());
        return ApiResponse.success(res);
    }

    @PostMapping("/password")
    public ApiResponse<UserPasswordRes> getPassword(@RequestBody PasswordReq req) {
        UserPasswordRes res = userService.getPassword(req.username(), req.phone());
        return ApiResponse.success(res);
    }
}