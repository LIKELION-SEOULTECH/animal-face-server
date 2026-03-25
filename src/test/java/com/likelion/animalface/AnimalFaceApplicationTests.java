package com.likelion.animalface;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 전체 애플리케이션 컨텍스트 로드 확인 테스트
 *
 * <p>테스트 환경에서는 실제 AWS·AI 서버가 없으므로 더미 프로퍼티로 빈 생성만 검증한다.
 * S3Presigner는 빌드 시점에 실제 AWS 연결을 맺지 않으므로 더미 자격증명으로도 정상 생성된다.
 */
@SpringBootTest(properties = {
        "cloud.aws.credentials.access-key=test-access-key",
        "cloud.aws.credentials.secret-key=test-secret-key",
        "cloud.aws.region.static=ap-northeast-2",
        "cloud.aws.s3.bucket=test-bucket"
})
class AnimalFaceApplicationTests {

    @Test
    void contextLoads() {
    }
}
