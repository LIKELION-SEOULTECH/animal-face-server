package com.likelion.animalface.global.infra.ai;

import com.likelion.animalface.domain.animal.entity.AnimalType;
import java.util.Optional;

public record AiAnalyzeRes(String animalType, Double score) {
    /**
     * AI 서버의 문자열 결과를 시스템 Enum으로 안전하게 변환합니다.
     */
    public AnimalType toAnimalType() {
        // 1. Null 또는 공백 데이터 방어
        if (animalType == null || animalType.isBlank()) {
            return AnimalType.UNKNOWN;
        }

        try {
            // 2. trim()을 추가하여 예상치 못한 앞뒤 공백 제거 후 대문자 변환
            return AnimalType.valueOf(this.animalType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // 3. 로그를 남겨서 어떤 정체불명의 타입이 들어왔는지 파악할 수 있게 하면 더 좋습니다.
            // log.error("Unknown animal type from AI server: {}", animalType);
            return AnimalType.UNKNOWN;
        }
    }
}