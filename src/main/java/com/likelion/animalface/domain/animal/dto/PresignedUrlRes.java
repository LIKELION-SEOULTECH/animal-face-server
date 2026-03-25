package com.likelion.animalface.domain.animal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * S3 업로드를 위한 Presigned URL 응답 DTO
 * @param imageKey S3에 저장될 파일의 고유 키 (DB 저장용)
 * @param presignedUrl 프론트엔드에서 파일을 업로드할 임시 URL
 */
@Schema(description = "S3 업로드용 Presigned URL 응답 DTO")
public record PresignedUrlRes(
        @Schema(description = "S3에 저장될 파일의 고유 키 (분석 요청 시 사용)", example = "animal/550e8400-e29b-41d4-a716-446655440000")
        String imageKey,
        @Schema(description = "프론트엔드에서 직접 PUT 요청으로 파일을 업로드할 임시 URL (10분 유효)")
        String presignedUrl
) {
}