package com.likelion.animalface.domain.animal.service;

import com.likelion.animalface.domain.animal.dto.AnimalResultRes;
import com.likelion.animalface.domain.animal.entity.AnimalResult;
import com.likelion.animalface.domain.animal.repository.AnimalResultRepository;
import com.likelion.animalface.infra.s3.S3Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Transactional(readOnly = true) // 성능 향상을 위한 읽기 전용 설정
@Service
public class AnimalQueryService {

    private final AnimalResultRepository animalResultRepository;
    private final S3Provider s3Provider;

    /**
     * 내 분석 결과 리스트 조회
     */
    public List<AnimalResultRes> getMyResults(Long userId) {
        // 1. Fetch Join을 사용하여 N+1 없이 데이터 조회
        List<AnimalResult> results = animalResultRepository.findAllByUserIdWithUser(userId);

        // 2. 각 결과물마다 실시간 조회 URL을 입혀서 DTO로 변환
        return results.stream()
                .map(result -> {
                    String viewUrl = s3Provider.getPresignedUrlForView(result.getImageKey());
                    return AnimalResultRes.from(result, viewUrl);
                })
                .toList();
    }
}