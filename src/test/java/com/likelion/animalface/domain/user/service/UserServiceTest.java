package com.likelion.animalface.domain.user.service;

import com.likelion.animalface.domain.user.dto.req.SignupReq;
import com.likelion.animalface.domain.user.dto.res.UserIdRes;
import com.likelion.animalface.domain.user.dto.res.UserPasswordRes;
import com.likelion.animalface.domain.user.entity.User;
import com.likelion.animalface.domain.user.repository.UserRepository;
import com.likelion.animalface.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * UserService 단위 테스트
 *
 * <p>실제 DB 없이 Mockito로 의존성을 대체하여 비즈니스 로직만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private final User mockUser = User.create("testuser", "encoded_pw", "01012345678");

    // ── signup ──────────────────────────────────────────

    @Test
    @DisplayName("signup 성공: 중복 없으면 암호화 후 저장")
    void signup_success() {
        SignupReq req = new SignupReq("newuser", "rawPassword1!", "01011111111");
        given(userRepository.findByUsername("newuser")).willReturn(Optional.empty());
        given(passwordEncoder.encode("rawPassword1!")).willReturn("encoded_pw");
        given(userRepository.save(any(User.class))).willReturn(mockUser);

        userService.signup(req);

        verify(passwordEncoder).encode("rawPassword1!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("signup 실패: 중복 username → IllegalArgumentException")
    void signup_duplicateUsername() {
        SignupReq req = new SignupReq("testuser", "rawPassword1!", "01011111111");
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> userService.signup(req))
                .isInstanceOf(BusinessException.class)
                .hasMessage("이미 존재하는 아이디입니다.");

        // 중복이면 저장이 호출되어선 안 된다
        verify(userRepository, never()).save(any());
    }

    // ── getUsername ──────────────────────────────────────

    @Test
    @DisplayName("getUsername 성공: phone으로 username 반환")
    void getUsername_success() {
        given(userRepository.findByPhone("01012345678")).willReturn(Optional.of(mockUser));

        UserIdRes res = userService.getUsername("01012345678");

        assertThat(res.username()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("getUsername 실패: 미등록 phone → IllegalArgumentException")
    void getUsername_notFound() {
        given(userRepository.findByPhone("01099999999")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUsername("01099999999"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("해당 번호로 가입된 사용자가 없습니다.");
    }

    // ── loadUserByUsername ───────────────────────────────

    @Test
    @DisplayName("loadUserByUsername 성공: username으로 UserDetails 반환")
    void loadUserByUsername_success() {
        // given
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(mockUser));

        // when
        UserDetails result = userService.loadUserByUsername("testuser");

        // then
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("loadUserByUsername 실패: 존재하지 않는 username → UsernameNotFoundException")
    void loadUserByUsername_notFound() {
        // given
        given(userRepository.findByUsername("ghost")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("존재하지 않는 사용자입니다.");
    }

    // ── getPassword ──────────────────────────────────────

    @Test
    @DisplayName("getPassword 성공: username + phone 일치 시 8자리 임시 비밀번호 반환")
    void getPassword_success() {
        given(userRepository.findByUsernameAndPhone("testuser", "01012345678"))
                .willReturn(Optional.of(mockUser));
        given(passwordEncoder.encode(anyString())).willReturn("new_encoded_pw");

        UserPasswordRes res = userService.getPassword("testuser", "01012345678");

        assertThat(res.message()).isEqualTo("임시 비밀번호가 발급되었습니다.");
        assertThat(res.tempPassword()).hasSize(8);
        // DB에 암호화된 값이 저장되는지 검증
        verify(passwordEncoder).encode(anyString());
    }

    @Test
    @DisplayName("getPassword 실패: 정보 불일치 → IllegalArgumentException")
    void getPassword_notFound() {
        given(userRepository.findByUsernameAndPhone("wrong", "01012345678"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getPassword("wrong", "01012345678"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("일치하는 회원 정보가 없습니다.");
    }
}
