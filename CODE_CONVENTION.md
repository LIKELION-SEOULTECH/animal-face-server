# AnimalFace 코드 컨벤션

> Spring Boot 3.4.3 / Java 21 / Spring Cloud OpenFeign

---

## 목차

1. [패키지 & 모듈 구조](#1-패키지--모듈-구조)
2. [Controller 컨벤션](#2-controller-컨벤션)
3. [Service 컨벤션](#3-service-컨벤션)
4. [Repository 컨벤션](#4-repository-컨벤션)
5. [Entity 컨벤션](#5-entity-컨벤션)
6. [DTO 컨벤션](#6-dto-컨벤션)
7. [계층 간 코드 흐름 규칙](#7-계층-간-코드-흐름-규칙)
8. [OpenFeign 통신 규칙](#8-openfeign-통신-규칙)
9. [Global 공통 규칙](#9-global-공통-규칙)
10. [명명 규칙 총정리](#10-명명-규칙-총정리)
11. [Lombok 사용 규칙](#11-lombok-사용-규칙)
12. [트랜잭션 규칙](#12-트랜잭션-규칙)
13. [비동기 처리 규칙](#13-비동기-처리-규칙)

---

## 1. 패키지 & 모듈 구조

### 1-1. 최상위 패키지 분류

```
com.likelion.animalface/
├── domain/          # 비즈니스 도메인 레이어
├── global/          # 전역 공통 (설정, 응답, 예외 처리)
└── infra/           # 외부 인프라 연동 (S3, AI 서버 등)
```

**규칙**
- `domain/` : 순수 비즈니스 로직. 외부 기술 의존성을 직접 갖지 않는다.
- `global/` : 모든 도메인이 공유하는 설정·공통 DTO·예외 처리.
- `infra/` : AWS S3, AI 서버처럼 교체 가능한 외부 시스템 구현체.

### 1-2. 도메인 내부 구조

도메인별로 다음 하위 패키지를 갖는다.

```
domain/{도메인명}/
├── controller/
├── service/
├── repository/
├── entity/
└── dto/
    ├── req/     # 요청 DTO
    └── res/     # 응답 DTO
```

**규칙**
- `dto/` 가 단순할 경우(req/res 각 1~2개) 하위 패키지 없이 `dto/` 바로 아래에 둘 수 있다.
  - 예) `AnimalAnalyzeReq.java`, `AnimalResultRes.java` → `dto/` 직속
- `dto/` 파일이 많아질 경우(3개 이상) `req/`, `res/` 로 분리한다.
  - 예) user 도메인: `dto/req/SignupReq.java`, `dto/res/UserIdRes.java`
- 도메인 간 직접 참조는 **엔티티 참조만 허용**. 다른 도메인의 DTO를 import 하지 않는다.

---

## 2. Controller 컨벤션

### 2-1. 클래스 규칙

```java
@RestController
@RequestMapping("/api/{도메인명}")   // 소문자 kebab-case
@RequiredArgsConstructor
public class AnimalController {

    private final AnimalCommandService animalCommandService;
    private final AnimalQueryService animalQueryService;
    // 필요한 경우 infra 계층(S3Provider 등) 직접 주입 가능
}
```

- 클래스명: `{도메인명}Controller`
- URL 매핑: `/api/{도메인명}` 소문자, 복합어는 kebab-case (`/api/animal-result`)
- 생성자 주입 전용 (`@RequiredArgsConstructor`)
- 필드 순서: Command 서비스 → Query 서비스 → 기타 컴포넌트

### 2-2. 메서드 규칙

```java
// GET - 조회
@GetMapping("/presigned-url")
public ApiResponse<PresignedUrlRes> getUploadUrl() { ... }

// POST - 생성 / 실행
@PostMapping("/analyze")
public ApiResponse<String> requestAnalyze(
        @AuthenticationPrincipal User user,
        @RequestBody AnimalAnalyzeReq req
) { ... }

// GET - 목록 조회
@GetMapping("/results")
public ApiResponse<List<AnimalResultRes>> getMyResults(@AuthenticationPrincipal User user) { ... }
```

| HTTP Method | 용도 | 메서드명 접두사 |
|---|---|---|
| GET | 단건 조회 | `get`, `find` |
| GET | 목록 조회 | `getAll`, `getMy`, `list` |
| POST | 생성 / 실행 | `create`, `request`, `signup` |
| PUT / PATCH | 수정 | `update` |
| DELETE | 삭제 | `delete`, `remove` |

**규칙**
- 반환 타입: 항상 `ApiResponse<T>` 로 감싼다.
- 인증된 사용자 정보: `@AuthenticationPrincipal User user` 로 받는다. **클라이언트에서 userId를 받지 않는다.**
- 요청 본문: `@RequestBody {도메인명}{목적}Req req` — 파라미터명은 항상 `req`
- 메서드 내 로직은 서비스 위임 후 `ApiResponse.ok(...)` 반환만 수행한다. (비즈니스 로직 금지)

### 2-3. 주석 규칙

```java
/**
 * 1. S3 업로드 URL 발급 API
 */
@GetMapping("/presigned-url")
public ApiResponse<PresignedUrlRes> getUploadUrl() { ... }
```

- 각 API 메서드 위에 **번호 + 한 줄 설명** Javadoc 주석을 단다.

---

## 3. Service 컨벤션

### 3-1. Command / Query 분리 (CQRS)

쓰기(Command)와 읽기(Query)는 **서비스를 분리**한다.

```
service/
├── {도메인명}CommandService.java   # 생성, 수정, 삭제, 비동기 처리
└── {도메인명}QueryService.java     # 조회 전용 (readOnly)
```

- 단순 도메인(UserService처럼 CRUD가 적은 경우)은 단일 서비스 허용.
  - 단일 서비스명: `{도메인명}Service`

### 3-2. Command Service

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
- 클래스 레벨 `@Transactional` 미사용. 메서드 단위로만 선언.
- 메서드 내 단계는 번호 주석(`// 1.`, `// 2.`)으로 표현.
- 엔티티 생성은 반드시 **팩토리 메서드**(Entity.create(...))를 사용한다. `new` 직접 사용 금지.
- 예외 메시지는 한국어로 작성한다.

### 3-3. Query Service

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
- 조회 후 DTO 변환은 스트림으로 처리하며, `toList()` (Java 16+) 사용.
- 어노테이션 순서: `@RequiredArgsConstructor` → `@Transactional(readOnly = true)` → `@Service`

### 3-4. 단일 Service (UserService 패턴)

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    // 클래스 레벨: readOnly
    // 쓰기 메서드: 개별 @Transactional 추가

    @Transactional
    public void signup(SignupReq req) { ... }

    public UserIdRes getUsername(String phone) { ... }  // readOnly 상속
}
```

---

## 4. Repository 컨벤션

### 4-1. 기본 형태

```java
public interface AnimalResultRepository extends JpaRepository<AnimalResult, Long> {
    // 커스텀 쿼리만 추가
}
```

- `JpaRepository<엔티티, PK타입>` 상속.
- 인터페이스명: `{엔티티명}Repository`

### 4-2. 메서드 명명 규칙

| 목적 | 메서드명 예시 |
|---|---|
| 단건 조회 (Optional) | `findByUsername(String username)` |
| 목록 조회 | `findAllByUserId(Long userId)` |
| 존재 확인 | `existsByUsername(String username)` |
| Fetch Join 포함 | `findAllByUserIdWithUser(Long userId)` |

- Fetch Join 포함 메서드명: `findAll{조건}With{페치조인대상}` 패턴 사용.

### 4-3. 커스텀 JPQL 쿼리

```java
/**
 * [성능 최적화: JPQL Fetch Join]
 * User 엔티티가 LAZY 로딩이므로 목록 조회 시 N+1 문제 발생.
 * JOIN FETCH 로 단일 쿼리에서 User 정보까지 함께 조회.
 */
@Query("SELECT ar FROM AnimalResult ar JOIN FETCH ar.user WHERE ar.user.id = :userId")
List<AnimalResult> findAllByUserIdWithUser(@Param("userId") Long userId);
```

**규칙**
- N+1 발생 가능한 목록 조회: 반드시 `JOIN FETCH` 사용.
- `@Query` 사용 시 바로 위에 최적화 이유를 설명하는 Javadoc 주석 작성.
- `@Param` 은 반드시 명시한다.
- Native Query는 사용하지 않는다. JPQL 또는 Querydsl 사용.

---

## 5. Entity 컨벤션

### 5-1. 클래스 선언

```java
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "animal_results")   // snake_case 복수형
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

    // 팩토리 메서드
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

**Lombok 어노테이션 순서** (위 → 아래)
```
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
```

**규칙**
- `@Setter` 사용 금지. 상태 변경은 도메인 메서드로만.
- 기본 생성자: `AccessLevel.PROTECTED` (JPA 요구사항 + 직접 생성 방지).
- 테이블명: `@Table(name = "...")` snake_case 복수형 명시.
- 연관관계: 기본값 `FetchType.LAZY` 필수. `EAGER` 사용 금지.
- 모든 엔티티는 `BaseTimeEntity` 상속.
- 외래키 컬럼명: `@JoinColumn(name = "{참조엔티티명}_id")`.
- Enum 컬럼: `@Enumerated(EnumType.STRING)` 필수 (ORDINAL 사용 금지).

### 5-2. 도메인 메서드

```java
// 이름: 동사형, 비즈니스 의미를 담는다
public void updatePassword(String encodedPassword) {
    this.password = encodedPassword;
}
```

- 상태 변경 메서드: `update{필드명}`, `change{필드명}` 패턴.
- Setter 역할의 단순 메서드는 작성하지 않는다.

### 5-3. 팩토리 메서드

```java
// 생성 팩토리: create(...)
public static AnimalResult create(User user, String imageKey, AnimalType type, Double score) {
    return AnimalResult.builder()
            .user(user)
            .imageKey(imageKey)
            .animalType(type)
            .score(score)
            .build();
}
```

- 생성 팩토리: `create(...)` — 엔티티 생성 전용.
- 빌더 직접 사용(`AnimalResult.builder()...build()`)은 서비스·테스트 코드에서 금지. 반드시 `create()` 를 통한다.

### 5-4. Enum

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

- Enum명: `{도메인}{역할}` PascalCase.
- 상수: 전부 대문자 SCREAMING_SNAKE_CASE.
- 알 수 없는 값에 대한 `UNKNOWN` 상수를 항상 포함한다.

---

## 6. DTO 컨벤션

### 6-1. 클래스 형태 — Java Record 사용

```java
// 요청 DTO
public record AnimalAnalyzeReq(
        String imageKey
) {}

// 응답 DTO
public record AnimalResultRes(
        Long id,
        String animalName,
        String imageUrl
) {
    public static AnimalResultRes from(AnimalResult animalResult, String imageUrl) {
        return new AnimalResultRes(
                animalResult.getId(),
                animalResult.getAnimalType().name(),
                imageUrl
        );
    }
}
```

**규칙**
- DTO는 **항상 record**로 선언한다. (불변성 보장, 코드 간결)
- Lombok 사용 금지.
- 비즈니스 로직은 담지 않는다.
- `@JsonInclude`, `@JsonProperty` 등 Jackson 어노테이션만 예외적으로 허용.

### 6-2. 클래스 명명 규칙

| 종류 | 패턴 | 예시 |
|---|---|---|
| 요청 DTO | `{도메인명}{목적}Req` | `SignupReq`, `AnimalAnalyzeReq` |
| 응답 DTO | `{도메인명}{목적}Res` | `UserIdRes`, `AnimalResultRes`, `PresignedUrlRes` |
| 외부 API 응답 | `{외부시스템명}{목적}Res` | `AiAnalyzeRes` |

- **`Req`** = Request (요청, 클라이언트 → 서버)
- **`Res`** = Response (응답, 서버 → 클라이언트 또는 서버 → 서버)
- DTO에 `Request`, `Response`, `Dto` 전체 단어 사용 금지. 반드시 `Req`, `Res` 축약형 사용.

### 6-3. 변환 메서드 (팩토리/변환 메서드)

| 메서드명 | 용도 | 선언 위치 |
|---|---|---|
| `from(Entity entity, ...)` | 엔티티 → 응답 DTO 변환 | 응답 DTO 내부 static |
| `to(...)` | 요청 DTO → 엔티티 변환 | 요청 DTO 내부 instance |
| `of(...)` | 여러 값 → DTO 조합 | DTO 내부 static |

```java
// from: 엔티티에서 DTO 생성
public static AnimalResultRes from(AnimalResult result, String imageUrl) { ... }

// to: DTO에서 엔티티 생성 (SignupReq 패턴)
public User to(String encodedPassword) {
    return User.builder()
            .username(username)
            .password(encodedPassword)
            .phone(phone)
            .build();
}

// of: 값을 직접 조합
public static UserPasswordRes of(String tempPassword) {
    return new UserPasswordRes("임시 비밀번호가 발급되었습니다.", tempPassword);
}
```

### 6-4. 패키지 배치 규칙

- dto 내 파일이 **1~4개**: `dto/` 바로 아래 (req/res 구분 없음)
- dto 내 파일이 **5개 이상**: `dto/req/`, `dto/res/` 서브 패키지로 분리

---

## 7. 계층 간 코드 흐름 규칙

### 7-1. 데이터 흐름

```
Client
  │   (JSON Body: AnimalAnalyzeReq)
  ↓
Controller         — @RequestBody AnimalAnalyzeReq req
  │   (DTO 그대로 전달)
  ↓
Service            — analyzeAndSave(userId, req)
  │   (엔티티 변환: AnimalResult.create(...))
  ↓
Repository         — animalResultRepository.save(result)
  │
  ↓
Database
```

**원칙**
1. **Controller → Service**: DTO를 그대로 넘긴다. Controller에서 DTO를 엔티티로 변환하지 않는다.
2. **Service → Repository**: 엔티티를 넘긴다. Repository가 DTO를 받지 않는다.
3. **Service → Controller**: DTO를 반환한다. 엔티티를 Controller까지 올리지 않는다.
4. **엔티티 변환 위치**: Service 내부에서 `DTO.to()` 또는 `Entity.create()` 로 수행.
5. **응답 DTO 변환 위치**: Service 내부에서 `Res.from(entity)` 로 수행.

### 7-2. userId 전달 규칙

```java
// Controller: Security Context에서 추출
@GetMapping("/results")
public ApiResponse<List<AnimalResultRes>> getMyResults(@AuthenticationPrincipal User user) {
    return ApiResponse.ok(animalQueryService.getMyResults(user.getId()));  // Long userId 전달
}

// Service: userId(Long)을 파라미터로 받음
public List<AnimalResultRes> getMyResults(Long userId) { ... }
```

- Controller는 `@AuthenticationPrincipal` 로 User 객체를 받아 `user.getId()` 만 서비스에 전달.
- Service는 User 엔티티 객체가 아닌 `Long userId` 를 받는다.
- 단, 비동기 처리 등 Security Context를 사용할 수 없는 경우 userId를 파라미터로 전달.

### 7-3. 예외 처리 흐름

```java
// Service에서 throw (한국어 메시지)
.orElseThrow(() -> new EntityNotFoundException("유효하지 않은 사용자입니다."));
.orElseThrow(() -> new IllegalArgumentException("이미 존재하는 아이디입니다."));

// 추후 GlobalExceptionHandler(@RestControllerAdvice)에서 ApiResponse.error(...) 로 처리
```

- 예외는 Service에서 throw, Controller는 catch 하지 않는다.
- `EntityNotFoundException`: 엔티티를 찾을 수 없을 때.
- `IllegalArgumentException`: 비즈니스 규칙 위반 (중복, 불일치 등).

---

## 8. OpenFeign 통신 규칙

### 8-1. FeignClient 선언

```java
// 파일 위치: infra/{외부시스템명}/{외부시스템명}Client.java
@FeignClient(name = "ai-server-client", url = "${ai.server.url}")
public interface AiClient {

    /**
     * AI 서버에 이미지 URL을 전송하고 분석 결과를 받는다.
     * I/O 바운드 작업 — Virtual Thread 환경에서 매우 효율적.
     */
    @PostMapping("/analyze")
    AiAnalyzeRes analyzeAnimalFace(@RequestParam("imageUrl") String imageUrl);
}
```

**규칙**
- 파일 위치: `infra/{시스템명}/{시스템명}Client.java`
- 인터페이스명: `{외부시스템명}Client` (예: `AiClient`, `PaymentClient`)
- `name`: `{시스템명}-server-client` 형식 (스프링 빈 이름 충돌 방지)
- `url`: 반드시 `${프로퍼티키}` 외부 설정으로 주입. 하드코딩 금지.
- 메서드명: 실제 동작을 기술하는 동사형 (예: `analyzeAnimalFace`, `sendNotification`)

### 8-2. FeignConfig 설정

```java
// global/config/FeignConfig.java
@EnableFeignClients(basePackages = "com.likelion.animalface")
@Configuration
public class FeignConfig {

    @Bean
    Logger.Level feignLoggerLevel() {
        // 개발: FULL / 운영: BASIC
        return Logger.Level.FULL;
    }

    @Bean
    public Retryer retryer() {
        // 100ms 시작 간격, 최대 1초, 3회 재시도
        return new Retryer.Default(100, TimeUnit.SECONDS.toMillis(1), 3);
    }
}
```

**규칙**
- `@EnableFeignClients(basePackages = "com.likelion.animalface")` 는 Config 클래스에 선언.
- 로거 레벨: 개발 `FULL`, 운영 `BASIC`.
- Retryer: 네트워크 오류 대비 재시도 정책 항상 설정.
- 클래스명: `FeignConfig` (오타 주의: `FeginConfig` ❌)

### 8-3. 외부 API 응답 DTO

```java
// infra/{시스템명}/{시스템명}AnalyzeRes.java  또는  global/infra/{시스템명}/...
public record AiAnalyzeRes(String animalType, Double score) {

    /**
     * AI 서버 문자열 결과를 시스템 Enum으로 안전하게 변환.
     */
    public AnimalType toAnimalType() {
        if (animalType == null || animalType.isBlank()) {
            return AnimalType.UNKNOWN;
        }
        try {
            return AnimalType.valueOf(this.animalType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return AnimalType.UNKNOWN;
        }
    }
}
```

**규칙**
- 외부 API 응답 DTO도 record 사용.
- 외부 시스템의 불안정한 값을 내부 타입으로 변환하는 메서드(`toXxx()`)를 DTO 내부에 작성.
- 변환 시 null·빈값·enum 미존재 케이스를 방어 처리하여 `UNKNOWN` 으로 폴백.
- 예외 발생 시 로그 출력 후 폴백 (서비스 중단 방지).

### 8-4. application.yml 외부 시스템 URL 설정

```yaml
# application.yml
ai:
  server:
    url: http://localhost:8000
```

- 외부 시스템 URL 키: `{시스템명}.server.url` 패턴.
- 환경별 다른 URL은 프로파일로 관리.

---

## 9. Global 공통 규칙

### 9-1. ApiResponse — 통일 응답 형식

```java
// global/dto/ApiResponse.java
public record ApiResponse<T>(
        boolean success,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        T data,
        String message
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, "성공");
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, "요청이 성공적으로 처리되었습니다.");
    }

    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>(true, null, message);
    }

    public static ApiResponse<String> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
```

**Controller에서 사용 규칙**

| 상황 | 사용 메서드 |
|---|---|
| 데이터 반환 (일반) | `ApiResponse.ok(data)` |
| 데이터 반환 (상세 메시지 필요) | `ApiResponse.success(data)` |
| 데이터 없이 메시지만 | `ApiResponse.message("...")` |
| 에러 응답 (ExceptionHandler) | `ApiResponse.error("...")` |

### 9-2. BaseTimeEntity

```java
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)  // SpringDataJPA Auditing
public class BaseTimeEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime modifiedAt;
}
```

- 모든 엔티티는 `BaseTimeEntity` 를 상속한다.
- `@EntityListeners(AuditingEntityListener.class)` — `AutoCloseable.class` 사용 금지.
- Application 클래스 또는 Config에 `@EnableJpaAuditing` 필수.

### 9-3. 설정 파일 구조

```yaml
# application.yml — 공통 설정
spring:
  profiles:
    include: aws
  threads:
    virtual:
      enabled: true   # Java 21 Virtual Threads

# 외부 시스템 URL
ai:
  server:
    url: http://localhost:8000
```

```yaml
# application-aws.yml — AWS 전용 설정 (환경변수 참조)
cloud:
  aws:
    credentials:
      access-key: ${S3_ACCESS_KEY}
      secret-key: ${S3_SECRET_KEY}
    region:
      static: ap-northeast-2
    s3:
      bucket: ${S3_BUCKET_NAME}
```

**규칙**
- 민감 정보(키, 비밀번호): 반드시 `${환경변수명}` 으로 주입. 하드코딩 금지.
- 환경별 설정: `application-{profile}.yml` 파일로 분리.
- 공통 설정만 `application.yml` 에 작성.

---

## 10. 명명 규칙 총정리

### 10-1. 클래스명

| 종류 | 패턴 | 예시 |
|---|---|---|
| Controller | `{도메인}Controller` | `AnimalController` |
| Command Service | `{도메인}CommandService` | `AnimalCommandService` |
| Query Service | `{도메인}QueryService` | `AnimalQueryService` |
| 단일 Service | `{도메인}Service` | `UserService` |
| Repository | `{엔티티}Repository` | `AnimalResultRepository` |
| Entity | `{도메인개념}` (명사형) | `AnimalResult`, `User` |
| Enum | `{도메인}{역할}` | `AnimalType` |
| 요청 DTO | `{도메인}{목적}Req` | `AnimalAnalyzeReq`, `SignupReq` |
| 응답 DTO | `{도메인}{목적}Res` | `AnimalResultRes`, `UserIdRes` |
| 외부 응답 DTO | `{외부시스템}{목적}Res` | `AiAnalyzeRes` |
| Feign Client | `{외부시스템}Client` | `AiClient` |
| Config | `{기능}Config` | `AsyncConfig`, `FeignConfig`, `S3Config` |
| Provider/Helper | `{기능}Provider` | `S3Provider` |

### 10-2. 메서드명

| 계층 | 패턴 | 예시 |
|---|---|---|
| Controller | HTTP 동작 기반 동사 | `getUploadUrl`, `requestAnalyze`, `getMyResults` |
| Command Service | 동작 기반 동사 | `analyzeAndSave`, `signup`, `getPassword` |
| Query Service | `get` / `find` 접두사 | `getMyResults`, `findById` |
| Repository | Spring Data JPA 관례 | `findByUsername`, `findAllByUserIdWithUser` |
| 엔티티 팩토리 | `create(...)` | `AnimalResult.create(...)` |
| DTO 변환 | `from`, `to`, `of` | `AnimalResultRes.from(...)` |
| 도메인 메서드 | `update{필드}` | `updatePassword(...)` |

### 10-3. 변수명

```java
// 요청 파라미터: 항상 req
public ApiResponse<String> requestAnalyze(@RequestBody AnimalAnalyzeReq req)

// 응답 변수: res 또는 의미 있는 이름
UserIdRes res = userService.getUsername(req.phone());

// 엔티티: 도메인명 소문자 camelCase
User user = userRepository.findById(userId)...
AnimalResult result = AnimalResult.create(...)

// 목록: 복수형
List<AnimalResult> results = ...
List<AnimalResultRes> responses = ...
```

### 10-4. 패키지명

- 모두 소문자, 단어 구분 없음 (축약하지 않는다)
- `repositry` ❌ → `repository` ✅ (오타 주의)
- `feign`, `ai`, `s3` 등 기술명은 소문자 그대로.

---

## 11. Lombok 사용 규칙

### 엔티티

```java
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
// @Setter 금지
```

### 서비스 / 컨트롤러 / 컴포넌트

```java
@RequiredArgsConstructor   // 생성자 주입 전용
// @Autowired 사용 금지
```

### 설정 클래스

```java
// Lombok 사용 최소화
// @Value + 생성자 또는 @Bean 메서드 파라미터 주입 사용
```

---

## 12. 트랜잭션 규칙

| 상황 | 설정 |
|---|---|
| Query Service (전체) | 클래스 `@Transactional(readOnly = true)` |
| Command Service 쓰기 메서드 | 메서드 `@Transactional` |
| 단일 Service 기본 | 클래스 `@Transactional(readOnly = true)` |
| 단일 Service 쓰기 메서드 | 메서드 `@Transactional` 추가 |
| @Async 메서드 | 메서드 `@Transactional` (새 트랜잭션) |

- 클래스 레벨에 `@Transactional` (readOnly 없는) 사용 금지.
- Dirty Checking을 활용하는 경우 반드시 `@Transactional` 범위 내에서 수행.

---

## 13. 비동기 처리 규칙

### AsyncConfig

```java
@Bean(name = "asyncExecutor")
public Executor asyncExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();  // Java 21 Virtual Thread
}
```

### 비동기 메서드 선언

```java
@Async("asyncExecutor")     // 빈 이름 명시 필수
@Transactional              // 새 트랜잭션으로 시작
public void analyzeAndSave(Long userId, AnimalAnalyzeReq req) {
    // Security Context가 없으므로 userId를 파라미터로 직접 받는다
    // @AuthenticationPrincipal 사용 불가
}
```

**규칙**
- `@Async` 에 반드시 빈 이름(`"asyncExecutor"`) 명시. 이름 없이 사용 금지.
- 비동기 메서드는 **void 반환**을 원칙으로 한다. (Future 필요 시 별도 논의)
- Security Context는 비동기 스레드에서 공유되지 않으므로, userId 등 인증 정보는 파라미터로 전달.
- 반환값이 필요 없는 "fire and forget" 패턴에 사용한다.
- I/O 바운드 작업(외부 API 호출, 파일 처리)에 한정해 사용한다.

---

## 부록: 자주 하는 실수 체크리스트

| 항목 | 잘못된 예                          | 올바른 예 |
|---|--------------------------------|---|
| DTO 클래스명 | `AnimalRequest`, `UserResponse` | `AnimalAnalyzeReq`, `UserIdRes` |
| 패키지명 오타 | `repositry`                    | `repository` |
| Config 오타 | `FeginConfig`                  | `FeignConfig` |
| 엔티티 직접 생성 | `new AnimalResult(...)`        | `AnimalResult.create(...)` |
| Fetch 전략 | `FetchType.EAGER`              | `FetchType.LAZY` |
| Enum 저장 | `@Enumerated(EnumType.ORDINAL)` | `@Enumerated(EnumType.STRING)` |
| 비동기 빈 이름 | `@Async`                       | `@Async("asyncExecutor")` |
| 트랜잭션 (Command) | 클래스 `@Transactional`           | 메서드별 `@Transactional` |
| URL 하드코딩 | `url = "http://localhost:8000"` | `url = "${ai.server.url}"` |
| 클라이언트 userId | `@RequestParam Long userId`    | `@AuthenticationPrincipal User user` |