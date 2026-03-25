package com.likelion.animalface.domain.animal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.animalface.domain.animal.dto.AnimalAnalyzeReq;
import com.likelion.animalface.domain.animal.dto.AnimalResultRes;
import com.likelion.animalface.domain.animal.service.AnimalCommandService;
import com.likelion.animalface.domain.animal.service.AnimalQueryService;
import com.likelion.animalface.domain.user.entity.User;
import com.likelion.animalface.global.config.SecurityConfig;
import com.likelion.animalface.infra.s3.S3Provider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AnimalController 웹 계층 테스트
 *
 * <p>인증이 필요한 엔드포인트는 {@code User} 객체를 SecurityContext에 직접 주입하여
 * {@code @AuthenticationPrincipal User user} 파라미터가 올바르게 바인딩되는지 검증한다.
 */
@WebMvcTest(AnimalController.class)
@Import(SecurityConfig.class)
class AnimalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private S3Provider s3Provider;

    @MockBean
    private AnimalCommandService animalCommandService;

    @MockBean
    private AnimalQueryService animalQueryService;

    /**
     * 테스트용 가상 유저 — id 없이 빌드하며 서비스는 mock이므로 id 값은 무관
     */
    private final User mockUser = User.builder()
            .username("testuser")
            .phone("01012345678")
            .password("encoded_pw")
            .build();

    /**
     * mockUser를 SecurityContext 주인공(principal)으로 세팅하는 인증 객체
     */
    private UsernamePasswordAuthenticationToken userAuth() {
        return new UsernamePasswordAuthenticationToken(mockUser, null, Collections.emptyList());
    }

    // ── GET /api/animal/presigned-url ────────────────────

    @Test
    @DisplayName("getUploadUrl 성공: 인증 유저 → 200 + imageKey·presignedUrl 반환")
    void getUploadUrl_success() throws Exception {
        given(s3Provider.createPath("animal")).willReturn("animal/test-uuid");
        given(s3Provider.getPresignedUrlForUpload("animal/test-uuid"))
                .willReturn("https://s3.example.com/presigned");

        mockMvc.perform(get("/api/animal/presigned-url")
                        .with(authentication(userAuth())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.imageKey").value("animal/test-uuid"))
                .andExpect(jsonPath("$.data.presignedUrl").value("https://s3.example.com/presigned"));
    }

    @Test
    @DisplayName("getUploadUrl 미인증: 403 반환 (form login 미설정 → Http403ForbiddenEntryPoint)")
    void getUploadUrl_unauthorized() throws Exception {
        mockMvc.perform(get("/api/animal/presigned-url"))
                .andExpect(status().isForbidden());
    }

    // ── POST /api/animal/analyze ─────────────────────────

    @Test
    @DisplayName("requestAnalyze 성공: 인증 유저 → 202 + '분석이 시작되었습니다.' 반환")
    void requestAnalyze_success() throws Exception {
        AnimalAnalyzeReq req = new AnimalAnalyzeReq("animal/test-key");
        willDoNothing().given(animalCommandService).analyzeAndSave(any(), eq(req));

        mockMvc.perform(post("/api/animal/analyze")
                        .with(authentication(userAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("분석이 시작되었습니다."));
    }

    @Test
    @DisplayName("requestAnalyze 미인증: 403 반환 (form login 미설정 → Http403ForbiddenEntryPoint)")
    void requestAnalyze_unauthorized() throws Exception {
        AnimalAnalyzeReq req = new AnimalAnalyzeReq("animal/test-key");

        mockMvc.perform(post("/api/animal/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/animal/results ──────────────────────────

    @Test
    @DisplayName("getMyResults 성공: 인증 유저 → 200 + 결과 리스트 반환")
    void getMyResults_success() throws Exception {
        List<AnimalResultRes> mockResults = List.of(
                new AnimalResultRes(1L, "DOG", "https://s3.example.com/dog"),
                new AnimalResultRes(2L, "CAT", "https://s3.example.com/cat")
        );
        given(animalQueryService.getMyResults(any())).willReturn(mockResults);

        mockMvc.perform(get("/api/animal/results")
                        .with(authentication(userAuth())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].animalName").value("DOG"))
                .andExpect(jsonPath("$.data[1].animalName").value("CAT"));
    }

    @Test
    @DisplayName("getMyResults 성공: 결과 없음 → 200 + 빈 배열")
    void getMyResults_empty() throws Exception {
        given(animalQueryService.getMyResults(any())).willReturn(List.of());

        mockMvc.perform(get("/api/animal/results")
                        .with(authentication(userAuth())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("getMyResults 미인증: 403 반환 (form login 미설정 → Http403ForbiddenEntryPoint)")
    void getMyResults_unauthorized() throws Exception {
        mockMvc.perform(get("/api/animal/results"))
                .andExpect(status().isForbidden());
    }
}
