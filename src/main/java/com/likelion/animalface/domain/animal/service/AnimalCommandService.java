package com.likelion.animalface.domain.animal.service;

import com.likelion.animalface.domain.animal.dto.AnimalAnalyzeReq;
import com.likelion.animalface.domain.animal.entity.AnimalResult;
import com.likelion.animalface.domain.animal.repository.AnimalResultRepository;
import com.likelion.animalface.domain.user.entity.User;
import com.likelion.animalface.domain.user.repository.UserRepository;
import com.likelion.animalface.infra.ai.AiClient;
import com.likelion.animalface.infra.s3.S3Provider;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnimalCommandService {

    private final AiClient aiClient;
    private final S3Provider s3Provider;
    private final AnimalResultRepository animalResultRepository;
    private final UserRepository userRepository;
    /**
     * [비동기 분석 실행 및 저장]
     * Java 21 Virtual Threads(asyncExecutor)를 사용하여
     * AI 서버와의 통신 중 발생하는 I/O 블로킹을 효율적으로 처리합니다.
     */
    @Async("asyncExecutor")
    @Transactional
    public void analyzeAndSave(Long userId, AnimalAnalyzeReq req) {

        // 1. [보안 및 정합성] 현재 시점에 유저가 존재하는지 진짜 DB에서 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("유효하지 않은 사용자입니다."));

        String viewUrl = s3Provider.getPresignedUrlForView(req.imageKey());
        com.likelion.animalface.global.infra.ai.AiAnalyzeRes aiRes = aiClient.analyzeAnimalFace(viewUrl);

        // 2. 팩터리 메서드로 조립
        AnimalResult result = AnimalResult.create(
                user,
                req.imageKey(),
                aiRes.toAnimalType(),
                aiRes.score()
        );

        // 3. 저장
        animalResultRepository.save(result);
    }
}