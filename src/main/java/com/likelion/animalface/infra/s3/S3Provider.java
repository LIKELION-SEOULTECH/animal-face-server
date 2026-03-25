package com.likelion.animalface.infra.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class S3Provider {

    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * 1. 업로드용 Presigned URL 발급
     * 클라이언트가 S3에 직접 파일을 올릴 때 사용합니다.
     */
    public String getPresignedUrlForUpload(String key) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("image/png") // 필요에 따라 동적 설정 가능
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10)) // 10분 내 업로드 필수
                .putObjectRequest(putObjectRequest)
                .build();

        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }

    /**
     * 2. 조회용 Presigned URL 발급
     * DB에서 꺼낸 Key를 기반으로 임시 접근 주소를 만듭니다.
     */
    public String getPresignedUrlForView(String key) {
        if (key == null || key.isBlank()) return null;

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60)) // 1시간 동안 유효
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    /**
     * S3에 저장될 유니크한 파일 키 생성 (도메인별 구분용)
     */
    public String createPath(String prefix) {
        return String.format("%s/%s", prefix, UUID.randomUUID());
    }
}