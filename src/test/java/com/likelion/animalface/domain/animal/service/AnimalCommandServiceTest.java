package com.likelion.animalface.domain.animal.service;

import com.likelion.animalface.domain.animal.dto.AnimalAnalyzeReq;
import com.likelion.animalface.domain.animal.entity.AnimalResult;
import com.likelion.animalface.domain.animal.entity.AnimalType;
import com.likelion.animalface.domain.animal.repository.AnimalResultRepository;
import com.likelion.animalface.domain.user.entity.User;
import com.likelion.animalface.domain.user.repository.UserRepository;
import com.likelion.animalface.global.infra.ai.AiAnalyzeRes;
import com.likelion.animalface.infra.ai.AiClient;
import com.likelion.animalface.infra.s3.S3Provider;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * AnimalCommandService 단위 테스트
 *
 * <p>@Async는 Spring 컨텍스트 없이 동작하지 않으므로 이 테스트에서는 동기로 실행된다.
 * AI 서버(AiClient) 및 S3(S3Provider)를 Mockito로 대체하여 순수 비즈니스 로직만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AnimalCommandServiceTest {

    @Mock
    private AiClient aiClient;

    @Mock
    private S3Provider s3Provider;

    @Mock
    private AnimalResultRepository animalResultRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AnimalCommandService animalCommandService;

    private final User mockUser = User.builder()
            .username("testuser")
            .phone("01012345678")
            .password("encoded_pw")
            .build();

    // ── analyzeAndSave 성공 ──────────────────────────────

    @Test
    @DisplayName("analyzeAndSave 성공: AI 분석 결과가 AnimalResult로 저장된다")
    void analyzeAndSave_success() {
        Long userId = 1L;
        AnimalAnalyzeReq req = new AnimalAnalyzeReq("animal/test-key");
        AiAnalyzeRes aiRes = new AiAnalyzeRes("DOG", 0.92);

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(s3Provider.getPresignedUrlForView("animal/test-key")).willReturn("https://s3.example.com/view");
        given(aiClient.analyzeAnimalFace("https://s3.example.com/view")).willReturn(aiRes);
        given(animalResultRepository.save(any(AnimalResult.class))).willAnswer(inv -> inv.getArgument(0));

        animalCommandService.analyzeAndSave(userId, req);

        // 저장된 AnimalResult의 내용 검증
        ArgumentCaptor<AnimalResult> captor = ArgumentCaptor.forClass(AnimalResult.class);
        verify(animalResultRepository).save(captor.capture());

        AnimalResult saved = captor.getValue();
        assertThat(saved.getAnimalType()).isEqualTo(AnimalType.DOG);
        assertThat(saved.getImageKey()).isEqualTo("animal/test-key");
        assertThat(saved.getScore()).isEqualTo(0.92);
    }

    @Test
    @DisplayName("analyzeAndSave 성공: AI가 소문자로 동물 타입 반환해도 올바르게 변환")
    void analyzeAndSave_aiResponseLowerCase() {
        Long userId = 1L;
        AnimalAnalyzeReq req = new AnimalAnalyzeReq("animal/cat-key");
        AiAnalyzeRes aiRes = new AiAnalyzeRes("cat", 0.85);  // 소문자로 반환

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(s3Provider.getPresignedUrlForView(anyString())).willReturn("https://s3.example.com/view");
        given(aiClient.analyzeAnimalFace(anyString())).willReturn(aiRes);
        given(animalResultRepository.save(any(AnimalResult.class))).willAnswer(inv -> inv.getArgument(0));

        animalCommandService.analyzeAndSave(userId, req);

        ArgumentCaptor<AnimalResult> captor = ArgumentCaptor.forClass(AnimalResult.class);
        verify(animalResultRepository).save(captor.capture());
        assertThat(captor.getValue().getAnimalType()).isEqualTo(AnimalType.CAT);
    }

    @Test
    @DisplayName("analyzeAndSave 성공: AI가 알 수 없는 동물 반환 시 UNKNOWN으로 저장")
    void analyzeAndSave_unknownAnimalType() {
        Long userId = 1L;
        AnimalAnalyzeReq req = new AnimalAnalyzeReq("animal/unknown-key");
        AiAnalyzeRes aiRes = new AiAnalyzeRes("BEAR", 0.60);  // 정의되지 않은 타입

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(s3Provider.getPresignedUrlForView(anyString())).willReturn("https://s3.example.com/view");
        given(aiClient.analyzeAnimalFace(anyString())).willReturn(aiRes);
        given(animalResultRepository.save(any(AnimalResult.class))).willAnswer(inv -> inv.getArgument(0));

        animalCommandService.analyzeAndSave(userId, req);

        ArgumentCaptor<AnimalResult> captor = ArgumentCaptor.forClass(AnimalResult.class);
        verify(animalResultRepository).save(captor.capture());
        assertThat(captor.getValue().getAnimalType()).isEqualTo(AnimalType.UNKNOWN);
    }

    // ── analyzeAndSave 실패 ──────────────────────────────

    @Test
    @DisplayName("analyzeAndSave 실패: 존재하지 않는 userId → EntityNotFoundException")
    void analyzeAndSave_userNotFound() {
        Long invalidUserId = 999L;
        AnimalAnalyzeReq req = new AnimalAnalyzeReq("animal/test-key");
        given(userRepository.findById(invalidUserId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> animalCommandService.analyzeAndSave(invalidUserId, req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("유효하지 않은 사용자입니다.");

        // 유저가 없으면 AI 호출도, 저장도 발생하지 않아야 한다
        verify(aiClient, never()).analyzeAnimalFace(anyString());
        verify(animalResultRepository, never()).save(any());
    }
}
