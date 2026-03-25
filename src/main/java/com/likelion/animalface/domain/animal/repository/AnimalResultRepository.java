package com.likelion.animalface.domain.animal.repository;

import com.likelion.animalface.domain.animal.entity.AnimalResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnimalResultRepository extends JpaRepository<AnimalResult, Long> {

    /**
     * [성능 최적화: JPQL Fetch Join]
     * User 엔티티가 LAZY 로딩 설정되어 있으므로, 리스트 조회 시 N+1 문제가 발생합니다.
     * JOIN FETCH를 통해 한 번의 쿼리로 User 정보까지 가져옵니다.
     */
    @Query("SELECT ar FROM AnimalResult ar JOIN FETCH ar.user WHERE ar.user.id = :userId")
    List<AnimalResult> findAllByUserIdWithUser(@Param("userId") Long userId);

    /**
     * [N+1 발생 주의 — 성능 비교 테스트 전용]
     * Fetch Join 없이 AnimalResult만 조회합니다.
     * 이후 result.getUser() 호출 시 User 수만큼 추가 SELECT가 발생합니다. (N+1 문제)
     */
    @Query("SELECT ar FROM AnimalResult ar WHERE ar.user.id = :userId")
    List<AnimalResult> findAllByUserIdWithoutFetchJoin(@Param("userId") Long userId);
}