package com.likelion.animalface.domain.animal.service;

import com.likelion.animalface.domain.animal.dto.AnimalResultRes;
import com.likelion.animalface.domain.animal.entity.AnimalResult;
import com.likelion.animalface.domain.animal.entity.AnimalType;
import com.likelion.animalface.domain.animal.repository.AnimalResultRepository;
import com.likelion.animalface.domain.user.entity.User;
import com.likelion.animalface.infra.s3.S3Provider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * AnimalQueryService 단위 테스트
 *
 * <p>Fetch Join 쿼리 결과에 Presigned URL이 올바르게 붙어 DTO로 변환되는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AnimalQueryServiceTest {

    @Mock
    private AnimalResultRepository animalResultRepository;

    @Mock
    private S3Provider s3Provider;

    @InjectMocks
    private AnimalQueryService animalQueryService;

    private final User mockUser = User.builder()
            .username("testuser")
            .phone("01012345678")
            .password("encoded_pw")
            .build();

    // ── getMyResults ─────────────────────────────────────

    @Test
    @DisplayName("getMyResults: 결과 2건 → imageUrl이 각각 붙은 DTO 리스트 반환")
    void getMyResults_returnsMappedDtos() {
        Long userId = 1L;
        AnimalResult dog = AnimalResult.create(mockUser, "animal/key-dog", AnimalType.DOG, 0.92);
        AnimalResult cat = AnimalResult.create(mockUser, "animal/key-cat", AnimalType.CAT, 0.85);

        given(animalResultRepository.findAllByUserIdWithUser(userId)).willReturn(List.of(dog, cat));
        given(s3Provider.getPresignedUrlForView("animal/key-dog")).willReturn("https://s3.example.com/dog");
        given(s3Provider.getPresignedUrlForView("animal/key-cat")).willReturn("https://s3.example.com/cat");

        List<AnimalResultRes> result = animalQueryService.getMyResults(userId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).animalName()).isEqualTo("DOG");
        assertThat(result.get(0).imageUrl()).isEqualTo("https://s3.example.com/dog");
        assertThat(result.get(1).animalName()).isEqualTo("CAT");
        assertThat(result.get(1).imageUrl()).isEqualTo("https://s3.example.com/cat");
    }

    @Test
    @DisplayName("getMyResults: 결과 없음 → 빈 리스트 반환")
    void getMyResults_empty() {
        Long userId = 1L;
        given(animalResultRepository.findAllByUserIdWithUser(userId)).willReturn(List.of());

        List<AnimalResultRes> result = animalQueryService.getMyResults(userId);

        assertThat(result).isEmpty();
        // 결과가 없으면 S3 호출도 발생하지 않아야 한다
        verify(s3Provider, org.mockito.Mockito.never()).getPresignedUrlForView(org.mockito.ArgumentMatchers.anyString());
    }
}
