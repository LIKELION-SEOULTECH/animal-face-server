package com.likelion.animalface.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.animalface.domain.user.dto.req.FindIdReq;
import com.likelion.animalface.domain.user.dto.req.PasswordReq;
import com.likelion.animalface.domain.user.dto.req.SignupReq;
import com.likelion.animalface.domain.user.dto.res.UserIdRes;
import com.likelion.animalface.domain.user.dto.res.UserPasswordRes;
import com.likelion.animalface.domain.user.service.UserService;
import com.likelion.animalface.global.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
 *   <li>/api/user/signup — permitAll (인증 불필요)</li>
 *   <li>/api/user/id, /api/user/password — 인증 필요 (@WithMockUser)</li>
 * </ul>
 */
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    // ── POST /api/user/signup ────────────────────────────

    @Test
    @DisplayName("signup 성공: 200 + success=true + data='회원가입 완료'")
    void signup_success() throws Exception {
        SignupReq req = new SignupReq("newuser", "password1!", "01011111111");
        willDoNothing().given(userService).signup(req);

        mockMvc.perform(post("/api/user/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("회원가입 완료"));
    }

    @Test
    @DisplayName("signup 실패: 중복 아이디 → 전역 ExceptionHandler 없으므로 예외가 직접 전파됨 (Spring Boot 3.x)")
    void signup_duplicate() {
        SignupReq req = new SignupReq("testuser", "password1!", "01011111111");
        willThrow(new IllegalArgumentException("이미 존재하는 아이디입니다."))
                .given(userService).signup(req);

        // @ExceptionHandler 없으면 ServletException으로 감싸져 전파 (Spring 6)
        assertThatThrownBy(() ->
                mockMvc.perform(post("/api/user/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .isInstanceOf(jakarta.servlet.ServletException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("이미 존재하는 아이디입니다.");
    }

    // ── POST /api/user/id ────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("findId 성공: 200 + data.username 반환")
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

    @Test
    @DisplayName("findId 미인증: 403 반환 (form login 미설정 → Http403ForbiddenEntryPoint)")
    void findId_unauthorized() throws Exception {
        FindIdReq req = new FindIdReq("01012345678");

        mockMvc.perform(post("/api/user/id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ── POST /api/user/password ──────────────────────────

    @Test
    @WithMockUser
    @DisplayName("getPassword 성공: 200 + data.tempPassword 8자리 반환")
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
}
