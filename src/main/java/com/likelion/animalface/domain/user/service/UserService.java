package com.likelion.animalface.domain.user.service;

import com.likelion.animalface.domain.user.dto.req.SignupReq;
import com.likelion.animalface.domain.user.dto.res.UserIdRes;
import com.likelion.animalface.domain.user.dto.res.UserPasswordRes;
import com.likelion.animalface.domain.user.entity.User;
import com.likelion.animalface.domain.user.repositry.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signup(SignupReq req) {
        // 1. 이미 가입된 아이디인지 중복 체크 (실무 필수 로직)
        if (userRepository.findByUsername(req.username()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        // 2. 비밀번호 암호화 및 엔티티 변환 (SignupReq 내부에 정의한 to 메서드 활용)
        String encodedPassword = passwordEncoder.encode(req.password());
        User user = req.to(encodedPassword);

        // 3. 저장
        userRepository.save(user);
    }
    public UserIdRes getUsername(String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("해당 번호로 가입된 사용자가 없습니다."));
        return UserIdRes.from(user);
    }

    @Transactional
    public UserPasswordRes getPassword(String username, String phone) {
        User user = userRepository.findByUsernameAndPhone(username, phone)
                .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보가 없습니다."));

        // 1. UUID 생성 후 하이픈 제거 및 8자리 추출 (실무형 랜덤 비번)
        String tempPw = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // 2. 암호화하여 DB 업데이트 (Dirty Checking 활용)
        user.updatePassword(passwordEncoder.encode(tempPw));

        // 3. 사용자에게 보여줄 응답 객체 반환 (암호화 전 평문 전달)
        return UserPasswordRes.of(tempPw);
    }
}