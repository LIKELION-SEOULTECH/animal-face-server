package com.likelion.animalface.domain.animal.controller;

import com.likelion.animalface.domain.animal.dto.AnimalAnalyzeReq;
import com.likelion.animalface.domain.animal.dto.AnimalResultRes;
import com.likelion.animalface.domain.animal.dto.PresignedUrlRes;
import com.likelion.animalface.domain.animal.service.AnimalCommandService;
import com.likelion.animalface.domain.animal.service.AnimalQueryService;
import com.likelion.animalface.domain.user.entity.User;
import com.likelion.animalface.global.dto.ApiResponse;
import com.likelion.animalface.infra.s3.S3Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/animal")
@RequiredArgsConstructor
public class AnimalController {

    private final S3Provider s3Provider;
    private final AnimalCommandService animalCommandService;
    private final AnimalQueryService animalQueryService;

    /**
     * 1. 업로드용 URL 발급 API
     */
    @GetMapping("/presigned-url")
    public ApiResponse<PresignedUrlRes> getUploadUrl() {
        String key = s3Provider.createPath("animal");
        String url = s3Provider.getPresignedUrlForUpload(key);
        return ApiResponse.ok(new PresignedUrlRes(key, url));
    }

    /**
     * 2. 분석 요청 API (비동기 트리거)
     */
    @PostMapping("/analyze")
    public ApiResponse<String> requestAnalyze(
            @AuthenticationPrincipal User user, // 세션에서 인증 정보 가져오기
            @RequestBody AnimalAnalyzeReq req
    ) {
        // 세션에서 꺼낸 ID를 서비스에 전달 (출처가 세션이므로 보안 안전)
        animalCommandService.analyzeAndSave(user.getId(), req);

        return ApiResponse.ok("분석이 시작되었습니다.");
    }

    /**
     * 3. 결과 리스트 조회 API (N+1 고려)
     */
    @GetMapping("/results")
    public ApiResponse<List<AnimalResultRes>> getMyResults(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(animalQueryService.getMyResults(user.getId()));
    }
}