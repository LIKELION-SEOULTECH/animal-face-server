package com.likelion.animalface.domain.user.controller;

import com.likelion.animalface.domain.user.dto.FindIdReq;
import com.likelion.animalface.domain.user.dto.PasswordReq;
import com.likelion.animalface.domain.user.dto.SignupReq;
import com.likelion.animalface.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/user")
@RequiredArgsConstructor
@RestController
public class UserController {
    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupReq req) {
        userService.signup(req);
        return ResponseEntity.ok("회원가입 완료");
    }

    @PostMapping("/id")
    public ResponseEntity<String> findId(@RequestBody FindIdReq req) {
        return ResponseEntity.ok(userService.getUsername(req.phone()));
    }

    @PostMapping("/password")
    public ResponseEntity<String> getPassword(@RequestBody PasswordReq req) {
        return ResponseEntity.ok(userService.getPassword(req.username(), req.phone()));
    }

}
