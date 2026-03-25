package com.likelion.animalface.domain.animal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "동물상 분석 요청 DTO")
public record AnimalAnalyzeReq(
        @Schema(description = "S3에 업로드된 이미지의 키", example = "animal/550e8400-e29b-41d4-a716-446655440000")
        String imageKey
) {}