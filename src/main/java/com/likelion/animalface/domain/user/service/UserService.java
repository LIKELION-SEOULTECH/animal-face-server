package com.likelion.animalface.domain.user.service;

import com.likelion.animalface.domain.user.dto.req.SignupReq;
import com.likelion.animalface.domain.user.dto.res.UserIdRes;
import com.likelion.animalface.domain.user.dto.res.UserPasswordRes;
import com.likelion.animalface.domain.user.entity.User;
import com.likelion.animalface.domain.user.repository.UserRepository;
import com.likelion.animalface.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signup(SignupReq req) {
        // 1. 이미 가입된 아이디인지 중복 체크
        if (userRepository.findByUsername(req.username()).isPresent()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "이미 존재하는 아이디입니다.");
        }

        // 2. 비밀번호 암호화 및 엔티티 변환
        String encodedPassword = passwordEncoder.encode(req.password());
        User user = req.to(encodedPassword);

        // 3. 저장
        userRepository.save(user);
    }

    public UserIdRes getUsername(String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "해당 번호로 가입된 사용자가 없습니다."));
        return UserIdRes.from(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Spring Security 계약상 UsernameNotFoundException 유지 (formLogin 실패 처리에 사용)
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사용자입니다."));
    }

    @Transactional
    public UserPasswordRes getPassword(String username, String phone) {
        User user = userRepository.findByUsernameAndPhone(username, phone)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "일치하는 회원 정보가 없습니다."));

        // 1. UUID 생성 후 하이픈 제거 및 8자리 추출
        String tempPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // 2. 암호화하여 DB 업데이트 (Dirty Checking 활용)
        user.updatePassword(passwordEncoder.encode(tempPassword));

        // 3. 사용자에게 보여줄 응답 객체 반환 (암호화 전 평문 전달)
        return UserPasswordRes.of(tempPassword);
    }
}
