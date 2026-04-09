package com.likelion.animalface.domain.user.controller;

import com.likelion.animalface.domain.user.dto.req.FindIdReq;
import com.likelion.animalface.domain.user.dto.req.PasswordReq;
import com.likelion.animalface.domain.user.dto.req.SignupReq;
import com.likelion.animalface.domain.user.dto.res.UserIdRes;
import com.likelion.animalface.domain.user.dto.res.UserPasswordRes;
import com.likelion.animalface.domain.user.service.UserService;
import com.likelion.animalface.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "회원 관리 API")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 1. 회원가입 API
     */
    @Operation(summary = "회원가입", description = "아이디, 비밀번호, 전화번호로 회원가입합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 존재하는 아이디")
    })
    @PostMapping("/signup")
    public ApiResponse<Void> signup(@RequestBody SignupReq req) {
        userService.signup(req);
        return ApiResponse.message("회원가입이 완료되었습니다.");
    }

    /**
     * 2. 아이디 찾기 API
     */
    @Operation(summary = "아이디 찾기", description = "전화번호로 등록된 아이디를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "아이디 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "해당 번호로 가입된 사용자 없음")
    })
    @PostMapping("/id")
    public ApiResponse<UserIdRes> findId(@RequestBody FindIdReq req) {
        return ApiResponse.ok(userService.getUsername(req.phone()));
    }

    /**
     * 3. 임시 비밀번호 발급 API
     */
    @Operation(summary = "임시 비밀번호 발급", description = "아이디와 전화번호가 일치하면 임시 비밀번호를 발급하고 DB를 갱신합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "임시 비밀번호 발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "일치하는 회원 정보 없음")
    })
    @PostMapping("/password")
    public ApiResponse<UserPasswordRes> getPassword(@RequestBody PasswordReq req) {
        return ApiResponse.ok(userService.getPassword(req.username(), req.phone()));
    }
}