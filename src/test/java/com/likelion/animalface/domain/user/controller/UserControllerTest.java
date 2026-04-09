package com.likelion.animalface.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.animalface.domain.user.dto.req.FindIdReq;
import com.likelion.animalface.domain.user.dto.req.PasswordReq;
import com.likelion.animalface.domain.user.dto.req.SignupReq;
import com.likelion.animalface.domain.user.dto.res.UserIdRes;
import com.likelion.animalface.domain.user.dto.res.UserPasswordRes;
import com.likelion.animalface.domain.user.entity.User;
import com.likelion.animalface.domain.user.service.UserService;
import com.likelion.animalface.global.advice.GlobalExceptionHandler;
import com.likelion.animalface.global.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserController 웹 계층 테스트
 *
 * <p>MockMvc로 HTTP 요청/응답 흐름, 응답 JSON 구조, Security 설정을 검증한다.
 * <ul>
 *   <li>/api/user/signup, /api/user/id, /api/user/password — permitAll (인증 불필요)</li>
 *   <li>/api/user/login  — Spring Security formLogin 처리</li>
 *   <li>/api/user/logout — 인증된 사용자만 허용</li>
 * </ul>
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UserService userService;

    // ── POST /api/user/signup ────────────────────────────

    @Test
    @DisplayName("signup 성공: 200 + success=true + message='회원가입이 완료되었습니다.'")
    void signup_success() throws Exception {
        SignupReq req = new SignupReq("newuser", "password1!", "01011111111");
        willDoNothing().given(userService).signup(req);

        mockMvc.perform(post("/api/user/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."));
    }

    @Test
    @DisplayName("signup 실패: 중복 아이디 → 400 + success=false + message='이미 존재하는 아이디입니다.'")
    void signup_duplicate() throws Exception {
        SignupReq req = new SignupReq("testuser", "password1!", "01011111111");
        willThrow(new IllegalArgumentException("이미 존재하는 아이디입니다."))
                .given(userService).signup(req);

        mockMvc.perform(post("/api/user/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 존재하는 아이디입니다."));
    }

    // ── POST /api/user/id (permitAll) ────────────────────

    @Test
    @DisplayName("findId 성공: 인증 없이도 200 + data.username 반환 (permitAll)")
    void findId_success() throws Exception {
        FindIdReq req = new FindIdReq("01012345678");
        given(userService.getUsername("01012345678")).willReturn(new UserIdRes("testuser"));

        mockMvc.perform(post("/api/user/id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    // ── POST /api/user/password (permitAll) ──────────────

    @Test
    @DisplayName("getPassword 성공: 인증 없이도 200 + data.tempPassword 8자리 반환 (permitAll)")
    void getPassword_success() throws Exception {
        PasswordReq req = new PasswordReq("testuser", "01012345678");
        given(userService.getPassword("testuser", "01012345678"))
                .willReturn(UserPasswordRes.of("abcd1234"));

        mockMvc.perform(post("/api/user/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tempPassword").value("abcd1234"))
                .andExpect(jsonPath("$.data.message").value("임시 비밀번호가 발급되었습니다."));
    }

    // ── POST /api/user/login (formLogin) ─────────────────

    @Test
    @DisplayName("login 성공: 올바른 자격증명 → 200 + message='로그인 성공'")
    void login_success() throws Exception {
        // given
        String encoded = passwordEncoder.encode("rawPassword1!");
        User loginUser = User.create("testuser", encoded, "01012345678");
        given(userService.loadUserByUsername("testuser")).willReturn(loginUser);

        // when & then
        mockMvc.perform(post("/api/user/login")
                        .param("username", "testuser")
                        .param("password", "rawPassword1!"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그인 성공"));
    }

    @Test
    @DisplayName("login 실패: 잘못된 비밀번호 → 401 + message='아이디 또는 비밀번호가 올바르지 않습니다.'")
    void login_wrongPassword() throws Exception {
        // given
        String encoded = passwordEncoder.encode("rawPassword1!");
        User loginUser = User.create("testuser", encoded, "01012345678");
        given(userService.loadUserByUsername("testuser")).willReturn(loginUser);

        // when & then
        mockMvc.perform(post("/api/user/login")
                        .param("username", "testuser")
                        .param("password", "wrongPassword"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    @DisplayName("login 실패: 존재하지 않는 아이디 → 401 + message='아이디 또는 비밀번호가 올바르지 않습니다.'")
    void login_userNotFound() throws Exception {
        // given
        given(userService.loadUserByUsername("ghost"))
                .willThrow(new UsernameNotFoundException("존재하지 않는 사용자입니다."));

        // when & then
        mockMvc.perform(post("/api/user/login")
                        .param("username", "ghost")
                        .param("password", "rawPassword1!"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    // ── POST /api/user/logout ────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("logout 성공: 인증된 사용자 → 200 + message='로그아웃 성공'")
    void logout_success() throws Exception {
        mockMvc.perform(post("/api/user/logout"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그아웃 성공"));
    }
}
