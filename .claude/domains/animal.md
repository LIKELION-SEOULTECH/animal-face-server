# Animal 도메인 컨벤션 (ANIMAL)

> 동물상 찾기 도메인(`domain/animal/`)에 특화된 규칙을 정의한다.
> 공통 코딩 스타일은 [../conventions/code.md](../conventions/code.md) 를 따른다.

---

## 도메인 개요

| 항목 | 내용 |
|---|---|
| 패키지 | `domain/animal/` |
| 주요 기능 | S3 Presigned URL 발급, AI 서버 분석 요청(비동기), 결과 조회 |
| 서비스 패턴 | CQRS — `AnimalCommandService` + `AnimalQueryService` |
| 엔티티 | `AnimalResult` |
| 외부 연동 | AWS S3 (`S3Provider`), AI 서버 (`AiClient` — OpenFeign) |

---

## 서비스 패턴 — CQRS

쓰기·비동기 처리는 `AnimalCommandService`, 조회는 `AnimalQueryService` 로 분리한다.

```
service/
├── AnimalCommandService.java   # 분석 요청(비동기), 저장
└── AnimalQueryService.java     # 결과 목록 조회 (readOnly)
```

### AnimalCommandService

```java
@Service
@RequiredArgsConstructor
public class AnimalCommandService {

    private final AiClient aiClient;
    private final S3Provider s3Provider;
    private final AnimalResultRepository animalResultRepository;
    private final UserRepository userRepository;

    @Async("asyncExecutor")
    @Transactional
    public void analyzeAndSave(Long userId, AnimalAnalyzeReq req) {

        // 1. [보안/무결성] DB에서 사용자 재검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("유효하지 않은 사용자입니다."));

        // 2. 외부 API 호출
        String viewUrl = s3Provider.getPresignedUrlForView(req.imageKey());
        AiAnalyzeRes aiRes = aiClient.analyzeAnimalFace(viewUrl);

        // 3. 팩토리 메서드로 엔티티 생성
        AnimalResult result = AnimalResult.create(user, req.imageKey(), aiRes.toAnimalType(), aiRes.score());

        // 4. 저장
        animalResultRepository.save(result);
    }
}
```

**규칙**
- `@Async("asyncExecutor")` — 빈 이름 필수 명시. `@Async` 단독 사용 금지.
- 비동기 메서드는 `void` 반환 원칙 (fire and forget).
- Security Context는 비동기 스레드에서 공유되지 않으므로 `userId` 를 파라미터로 직접 전달.
- 단계별로 번호 주석(`// 1.`, `// 2.`) 작성.
- 엔티티 생성은 반드시 `AnimalResult.create(...)` 팩토리 메서드 사용. `new` 직접 사용 금지.

### AnimalQueryService

```java
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class AnimalQueryService {

    private final AnimalResultRepository animalResultRepository;
    private final S3Provider s3Provider;

    public List<AnimalResultRes> getMyResults(Long userId) {
        List<AnimalResult> results = animalResultRepository.findAllByUserIdWithUser(userId);

        return results.stream()
                .map(result -> {
                    String viewUrl = s3Provider.getPresignedUrlForView(result.getImageKey());
                    return AnimalResultRes.from(result, viewUrl);
                })
                .toList();
    }
}
```

**규칙**
- 클래스 레벨 `@Transactional(readOnly = true)` 필수.
- 어노테이션 순서: `@RequiredArgsConstructor` → `@Transactional(readOnly = true)` → `@Service`

---

## Entity — AnimalResult

```java
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "animal_results")
@Entity
public class AnimalResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnimalType animalType;

    @Column(nullable = false)
    private String imageKey;

    private Double score;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public static AnimalResult create(User user, String imageKey, AnimalType type, Double score) {
        return AnimalResult.builder()
                .user(user)
                .imageKey(imageKey)
                .animalType(type)
                .score(score)
                .build();
    }
}
```

---

## Enum — AnimalType

```java
@Getter
@RequiredArgsConstructor
public enum AnimalType {
    DOG("강아지상"),
    CAT("고양이상"),
    FOX("여우상"),
    UNKNOWN("알수없음");

    private final String name;
}
```

**규칙**
- AI 서버에서 인식 불가 결과 반환 시 `UNKNOWN` 폴백.
- 새 동물 유형 추가 시 Enum 상수 + `name` 필드값 함께 추가.

---

## DTO

| 클래스 | 위치 | 설명 |
|---|---|---|
| `AnimalAnalyzeReq` | `dto/AnimalAnalyzeReq.java` | 분석 요청 (imageKey) |
| `AnimalResultRes` | `dto/AnimalResultRes.java` | 결과 응답 (id, animalName, imageUrl) |
| `PresignedUrlRes` | `dto/PresignedUrlRes.java` | S3 Presigned URL 응답 |

```java
// 요청 DTO
public record AnimalAnalyzeReq(String imageKey) {}

// 응답 DTO
public record AnimalResultRes(Long id, String animalName, String imageUrl) {
    public static AnimalResultRes from(AnimalResult result, String imageUrl) {
        return new AnimalResultRes(
                result.getId(),
                result.getAnimalType().name(),
                imageUrl
        );
    }
}
```

---

## Repository

```java
public interface AnimalResultRepository extends JpaRepository<AnimalResult, Long> {

    /**
     * [성능 최적화: JPQL Fetch Join]
     * User 엔티티가 LAZY 로딩이므로 목록 조회 시 N+1 문제 발생.
     * JOIN FETCH 로 단일 쿼리에서 User 정보까지 함께 조회.
     */
    @Query("SELECT ar FROM AnimalResult ar JOIN FETCH ar.user WHERE ar.user.id = :userId")
    List<AnimalResult> findAllByUserIdWithUser(@Param("userId") Long userId);
}
```

**규칙**
- 목록 조회 메서드는 반드시 `JOIN FETCH` 사용하여 N+1 방지.
- `@Query` 사용 시 바로 위에 최적화 이유를 Javadoc으로 설명.
- Fetch Join 포함 메서드명: `findAll{조건}With{페치조인대상}` 패턴.

---

## Controller — /api/animal

```java
@RestController
@RequestMapping("/api/animal")
@RequiredArgsConstructor
public class AnimalController {

    private final AnimalCommandService animalCommandService;
    private final AnimalQueryService animalQueryService;
    private final S3Provider s3Provider;

    /**
     * 1. S3 업로드 URL 발급 API
     */
    @GetMapping("/presigned-url")
    public ApiResponse<PresignedUrlRes> getUploadUrl() { ... }

    /**
     * 2. 동물상 분석 요청 API (비동기)
     */
    @PostMapping("/analyze")
    public ApiResponse<String> requestAnalyze(
            @AuthenticationPrincipal User user,
            @RequestBody AnimalAnalyzeReq req
    ) {
        animalCommandService.analyzeAndSave(user.getId(), req);
        return ApiResponse.message("분석 요청이 접수되었습니다.");
    }

    /**
     * 3. 내 분석 결과 목록 조회 API
     */
    @GetMapping("/results")
    public ApiResponse<List<AnimalResultRes>> getMyResults(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(animalQueryService.getMyResults(user.getId()));
    }
}
```

**규칙**
- 비동기 분석 응답: 데이터 없이 `ApiResponse.message("분석 요청이 접수되었습니다.")` 반환.
- `userId` 는 Security Context(`@AuthenticationPrincipal`)에서 추출. 클라이언트에서 받지 않는다.
- `analyzeAndSave` 는 비동기이므로 Controller는 즉시 응답 반환.

---

## 외부 연동 — OpenFeign (AiClient)

파일 위치: `infra/ai/AiClient.java`

```java
@FeignClient(name = "ai-server-client", url = "${ai.server.url}")
public interface AiClient {

    /**
     * AI 서버에 이미지 URL을 전송하고 분석 결과를 받는다.
     */
    @PostMapping("/analyze")
    AiAnalyzeRes analyzeAnimalFace(@RequestParam("imageUrl") String imageUrl);
}
```

```java
// infra/ai/AiAnalyzeRes.java
public record AiAnalyzeRes(String animalType, Double score) {

    public AnimalType toAnimalType() {
        if (animalType == null || animalType.isBlank()) return AnimalType.UNKNOWN;
        try {
            return AnimalType.valueOf(this.animalType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return AnimalType.UNKNOWN;
        }
    }
}
```

**규칙**
- `url`: 반드시 `${ai.server.url}` 외부 설정. 하드코딩 금지.
- `AiAnalyzeRes.toAnimalType()` 에서 null·빈값·미존재 enum을 `UNKNOWN` 으로 방어 처리.
- FeignConfig 상세는 [../conventions/code.md](../conventions/code.md) 섹션 8 참조.

---

## 외부 연동 — S3Provider

파일 위치: `infra/s3/S3Provider.java`

| 메서드 | 설명 |
|---|---|
| `getPresignedUrlForUpload(imageKey)` | 클라이언트 업로드용 PUT Presigned URL 발급 |
| `getPresignedUrlForView(imageKey)` | 이미지 조회용 GET Presigned URL 발급 |

---

## 테스트 예시 요약

> 전체 테스트 규칙은 [../conventions/test.md](../conventions/test.md) 참조

| 테스트 클래스 | 계층 | 주요 검증 |
|---|---|---|
| `AnimalResultRepositoryTest` | `@DataJpaTest` | `findAllByUserIdWithUser` Fetch Join 쿼리, N+1 비교 |
| `AnimalCommandServiceTest` | `@ExtendWith(MockitoExtension.class)` | analyzeAndSave 성공, 존재하지 않는 사용자 예외 |
| `AnimalQueryServiceTest` | `@ExtendWith(MockitoExtension.class)` | 결과 목록 DTO 변환, S3 URL 매핑 검증 |
| `AnimalControllerTest` | `@WebMvcTest` | presigned-url 200, analyze 202, results 인증 필요 |
| `AiAnalyzeResTest` | 순수 단위 | `toAnimalType` 대소문자 무관, null/빈값 UNKNOWN 폴백 |

**AnimalCommandServiceTest 핵심 시나리오**

```
analyzeAndSave_success                 — AI 호출 후 AnimalResult 저장 검증 (ArgumentCaptor)
analyzeAndSave_userNotFound            — EntityNotFoundException 발생 검증
analyzeAndSave_unknownAnimalType       — AiAnalyzeRes 미존재 enum → UNKNOWN 저장 검증
```
