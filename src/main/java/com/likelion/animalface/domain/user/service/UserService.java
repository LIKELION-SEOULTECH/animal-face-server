package com.likelion.animalface.domain.user.service;

import com.likelion.animalface.domain.user.dto.SignupReq;
import com.likelion.animalface.domain.user.repositry.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class UserService {

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;

    @Transactional
    public void signup(SignupReq req) {
        userRepository.save(req.to(passwordEncoder.encode(req.password())));
    }

    public String getUsername(String phone) {
        return null;
    }

    public String getPassword(String username, String phone) {
        return null;
    }
}
