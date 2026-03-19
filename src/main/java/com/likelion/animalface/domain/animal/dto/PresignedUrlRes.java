package com.likelion.animalface.domain.animal.dto;

/**
 * S3 업로드를 위한 Presigned URL 응답 DTO
 * @param imageKey S3에 저장될 파일의 고유 키 (DB 저장용)
 * @param presignedUrl 프론트엔드에서 파일을 업로드할 임시 URL
 */
public record PresignedUrlRes(
        String imageKey,
        String presignedUrl
) {
}