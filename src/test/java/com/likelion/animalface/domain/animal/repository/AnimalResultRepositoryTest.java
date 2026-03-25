package com.likelion.animalface.domain.animal.repository;

import com.likelion.animalface.domain.animal.entity.AnimalResult;
import com.likelion.animalface.domain.animal.entity.AnimalType;
import com.likelion.animalface.domain.user.entity.User;
import com.likelion.animalface.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.likelion.animalface.global.config.JpaAuditingConfig;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.List;

/**
 * AnimalResult Repository N+1 vs Fetch Join 쿼리 비교 테스트
 *
 * <p>목적: LAZY 로딩 환경에서 Fetch Join 유무에 따른 실제 SQL 쿼리 발생 수 차이를 눈으로 확인한다.
 *
 * <p>@DataJpaTest 는 JPA 슬라이스만 로드하므로 @EnableJpaAuditing 이 담긴
 * JpaAuditingConfig 를 @Import 로 명시적으로 주입해야 BaseTimeEntity Auditing이 동작한다.
 *
 * <p>실행 후 콘솔에서 확인할 것:
 * <ul>
 *   <li>fetchJoinQuery 테스트 → SELECT 쿼리 1개 (AnimalResult + User JOIN)</li>
 *   <li>lazyLoadingQuery 테스트 → SELECT 쿼리 1 + N개 (AnimalResult 1번 + User N번)</li>
 * </ul>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(JpaAuditingConfig.class)
class AnimalResultRepositoryTest {

    @Autowired
    private AnimalResultRepository animalResultRepository;

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager em;

    private Long savedUserId;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.builder()
                .username("testuser")
                .phone("01012345678")
                .password("encoded_pw")
                .build());

        savedUserId = user.getId();

        // 동물상 결과 3건 저장 → getUser() 호출 시 N+1이 3번 발생하는 상황 연출
        animalResultRepository.save(AnimalResult.create(user, "animal/key-dog", AnimalType.DOG, 0.92));
        animalResultRepository.save(AnimalResult.create(user, "animal/key-cat", AnimalType.CAT, 0.85));
        animalResultRepository.save(AnimalResult.create(user, "animal/key-fox", AnimalType.FOX, 0.71));

        em.flush();  // INSERT 쿼리를 DB로 즉시 반영
        em.clear();  // 1차 캐시(영속성 컨텍스트) 초기화 → 이후 조회는 반드시 DB로 쿼리
    }

    // ──────────────────────────────────────────────
    // 테스트 1. Fetch Join → 단일 쿼리 (최적화)
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("[최적화] Fetch Join: AnimalResult + User를 단일 SELECT로 조회한다")
    void fetchJoinQuery_shouldIssue_singleQuery() {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  [Fetch Join] 쿼리 시작 — SELECT 1회 예상");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        // JOIN FETCH로 AnimalResult와 User를 한 번에 조회
        List<AnimalResult> results = animalResultRepository.findAllByUserIdWithUser(savedUserId);

        // getUser()는 이미 영속성 컨텍스트에 로드된 객체 → 추가 쿼리 없음
        results.forEach(r ->
                System.out.printf("  >> 동물상: %-5s | 유저: %s%n",
                        r.getAnimalType(), r.getUser().getUsername())
        );

        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  [Fetch Join] 완료 — 총 SELECT 1개 (위 쿼리 로그 확인)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
    }

    // ──────────────────────────────────────────────
    // 테스트 2. LAZY 로딩 → N+1 쿼리 발생
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("[N+1 발생] 지연 로딩: getUser() 호출마다 추가 SELECT가 발생한다")
    void lazyLoadingQuery_shouldIssue_nPlusOneQuery() {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  [N+1 발생] 쿼리 시작 — SELECT 1 + N회 예상");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        // AnimalResult만 조회 (User는 LAZY 프록시 상태)
        List<AnimalResult> results = animalResultRepository.findAllByUserIdWithoutFetchJoin(savedUserId);

        // getUser().getUsername() 호출 시마다 User SELECT 발생 → 결과 3건이면 추가 쿼리 3번
        results.forEach(r ->
                System.out.printf("  >> 동물상: %-5s | 유저: %s  ← 이 줄마다 User SELECT 1회 발생%n",
                        r.getAnimalType(), r.getUser().getUsername())
        );

        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.printf("  [N+1 발생] 완료 — 총 SELECT %d개 (1 + %d)%n",
                1 + results.size(), results.size());
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
    }
}
