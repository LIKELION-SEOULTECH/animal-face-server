package com.likelion.animalface.global.infra.ai;

import com.likelion.animalface.domain.animal.entity.AnimalType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AiAnalyzeRes.toAnimalType() 단위 테스트
 *
 * <p>AI 서버가 반환하는 문자열을 AnimalType Enum으로 안전하게 변환하는 로직을 검증한다.
 * 대소문자, 공백, null, 미정의 타입 등 엣지 케이스를 포함한다.
 */
class AiAnalyzeResTest {

    // ── 정상 변환 ─────────────────────────────────────────

    @Test
    @DisplayName("toAnimalType: 'DOG' → AnimalType.DOG")
    void toAnimalType_dog() {
        assertThat(new AiAnalyzeRes("DOG", 0.92).toAnimalType()).isEqualTo(AnimalType.DOG);
    }

    @Test
    @DisplayName("toAnimalType: 'CAT' → AnimalType.CAT")
    void toAnimalType_cat() {
        assertThat(new AiAnalyzeRes("CAT", 0.85).toAnimalType()).isEqualTo(AnimalType.CAT);
    }

    @Test
    @DisplayName("toAnimalType: 'FOX' → AnimalType.FOX")
    void toAnimalType_fox() {
        assertThat(new AiAnalyzeRes("FOX", 0.71).toAnimalType()).isEqualTo(AnimalType.FOX);
    }

    // ── 대소문자 처리 ──────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"dog", "Dog", "dOg", "DOG"})
    @DisplayName("toAnimalType: 대소문자 무관 → AnimalType.DOG")
    void toAnimalType_caseInsensitive(String input) {
        assertThat(new AiAnalyzeRes(input, 0.9).toAnimalType()).isEqualTo(AnimalType.DOG);
    }

    // ── 공백 처리 ─────────────────────────────────────────

    @Test
    @DisplayName("toAnimalType: 앞뒤 공백 포함 ' DOG ' → AnimalType.DOG")
    void toAnimalType_withTrimming() {
        assertThat(new AiAnalyzeRes("  DOG  ", 0.9).toAnimalType()).isEqualTo(AnimalType.DOG);
    }

    // ── UNKNOWN 변환 ──────────────────────────────────────

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("toAnimalType: null 또는 빈 문자열 → AnimalType.UNKNOWN")
    void toAnimalType_nullOrEmpty(String input) {
        assertThat(new AiAnalyzeRes(input, 0.0).toAnimalType()).isEqualTo(AnimalType.UNKNOWN);
    }

    @Test
    @DisplayName("toAnimalType: 공백만 있는 문자열 → AnimalType.UNKNOWN")
    void toAnimalType_blank() {
        assertThat(new AiAnalyzeRes("   ", 0.0).toAnimalType()).isEqualTo(AnimalType.UNKNOWN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"BEAR", "RABBIT", "UNKNOWN_ANIMAL", "123"})
    @DisplayName("toAnimalType: 미정의 동물 타입 → AnimalType.UNKNOWN")
    void toAnimalType_undefined(String input) {
        assertThat(new AiAnalyzeRes(input, 0.5).toAnimalType()).isEqualTo(AnimalType.UNKNOWN);
    }
}
