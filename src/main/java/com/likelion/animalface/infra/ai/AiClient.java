package com.likelion.animalface.global.infra.ai;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ai-server-client", url = "${ai.server.url}")
public interface AiClient {

    /**
     * AI 서버에 이미지 경로를 전달하여 분석 결과를 받아옵니다.
     * I/O Bound 작업이므로 가상 스레드 환경에서 매우 효율적으로 동작합니다.
     */
    @PostMapping("/analyze")
    AiAnalyzeRes analyzeAnimalFace(@RequestParam("imageUrl") String imageUrl);

    // AI 서버 응답을 담을 내부 Record
    record AiAnalyzeRes(String animalType, Double score) {}
}