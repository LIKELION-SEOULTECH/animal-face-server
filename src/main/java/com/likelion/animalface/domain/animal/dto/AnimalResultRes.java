package com.likelion.animalface.domain.animal.dto;

import com.likelion.animalface.domain.animal.entity.AnimalResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "동물상 분석 결과 응답 DTO")
public record AnimalResultRes(
        @Schema(description = "분석 결과 ID", example = "1")
        Long id,
        @Schema(description = "분석된 동물상 이름", example = "DOG")
        String animalName,
        @Schema(description = "결과 이미지 조회용 Presigned URL")
        String imageUrl
) {
    public static AnimalResultRes from(AnimalResult animalResult, String imageUrl) {
        return new AnimalResultRes(
                animalResult.getId(),
                animalResult.getAnimalType().name(),
                imageUrl
        );
    }
}
