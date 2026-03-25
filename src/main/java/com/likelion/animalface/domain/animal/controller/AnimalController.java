package com.likelion.animalface.domain.animal.controller;

import com.likelion.animalface.domain.animal.dto.AnimalAnalyzeReq;
import com.likelion.animalface.domain.animal.dto.AnimalResultRes;
import com.likelion.animalface.domain.animal.dto.PresignedUrlRes;
import com.likelion.animalface.domain.animal.service.AnimalCommandService;
import com.likelion.animalface.domain.animal.service.AnimalQueryService;
import com.likelion.animalface.domain.user.entity.User;
import com.likelion.animalface.global.dto.ApiResponse;
import com.likelion.animalface.infra.s3.S3Provider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Animal", description = "동물상 분석 API")
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
    @Operation(summary = "S3 업로드용 Presigned URL 발급",
            description = "S3에 이미지를 업로드하기 위한 임시 URL과 imageKey를 반환합니다. 인증 불필요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "URL 발급 성공")
    })
    @GetMapping("/presigned-url")
    public ApiResponse<PresignedUrlRes> getUploadUrl() {
        String key = s3Provider.createPath("animal");
        String url = s3Provider.getPresignedUrlForUpload(key);
        return ApiResponse.ok(new PresignedUrlRes(key, url));
    }

    /**
     * 2. 분석 요청 API (비동기 트리거)
     */
    @Operation(summary = "동물상 분석 요청",
            description = "업로드된 이미지의 S3 키를 전달하면 비동기로 AI 분석을 수행합니다. 인증 필요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "분석 요청 접수 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
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
    @Operation(summary = "내 동물상 결과 목록 조회",
            description = "로그인한 사용자의 전체 분석 결과 목록을 반환합니다. 인증 필요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/results")
    public ApiResponse<List<AnimalResultRes>> getMyResults(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(animalQueryService.getMyResults(user.getId()));
    }
}