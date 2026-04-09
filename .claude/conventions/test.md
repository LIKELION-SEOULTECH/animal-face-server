# AnimalFace 테스트 컨벤션 (TEST_CONVENTION)

> JUnit 5 + AssertJ + Mockito 기반 테스트 작성 규칙.
> 기술 스택 상세는 [skills.md](skills.md) 참조.

---

## 1. 테스트 계층 구조

계층별로 테스트 도구와 목적을 분리한다.

| 계층 | 어노테이션 | 목적 |
|---|---|---|
| Repository | `@DataJpaTest` | JPA 쿼리 메서드 정확성 검증 |
| Service | `@ExtendWith(MockitoExtension.class)` | 비즈니스 로직 단위 검증 |
| Controller | `@WebMvcTest` | HTTP 요청/응답·Security 흐름 검증 |
| 순수 단위 | 어노테이션 없음 | 단일 메서드/변환 로직 검증 |

테스트 파일은 메인 소스와 동일한 패키지 경로에 위치한다.

```
src/test/java/com/likelion/animalface/
├── domain/
│   ├── animal/
│   │   ├── controller/   AnimalControllerTest.java
│   │   ├── service/      AnimalCommandServiceTest.java
│   │   │                 AnimalQueryServiceTest.java
│   │   └── repository/   AnimalResultRepositoryTest.java
│   └── user/
│       ├── controller/   UserControllerTest.java
│       ├── service/      UserServiceTest.java
│       └── repository/   UserRepositoryTest.java
└── global/
    └── infra/ai/         AiAnalyzeResTest.java
```

---

## 2. Repository 테스트 (`@DataJpaTest`)

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)  // H2 인메모리 사용
@Import(JpaAuditingConfig.class)  // BaseTimeEntity Auditing 활성화 필수
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.save(User.builder()
                .username("testuser")
                .phone("01012345678")
                .password("encoded_pw")
                .build());
    }

    @Test
    @DisplayName("findByUsername: 존재하는 username → 사용자 반환")
    void findByUsername_success() {
        Optional<User> result = userRepository.findByUsername("testuser");
        assertThat(result).isPresent();
    }
}
```

**규칙**
- `@AutoConfigureTestDatabase(replace = ANY)`: H2 인메모리 DB로 교체. MySQL 의존성 제거.
- `@Import(JpaAuditingConfig.class)`: `@DataJpaTest` 슬라이스는 `@EnableJpaAuditing` 을 자동 로드하지 않으므로 반드시 명시 import.
- `@BeforeEach` 로 테스트 데이터를 직접 저장한다. 픽스처 파일(SQL) 사용 지양.
- N+1 비교 테스트처럼 SQL 콘솔 출력이 목적인 경우 `em.flush()` + `em.clear()` 로 1차 캐시를 초기화한 뒤 조회한다.

---

## 3. Service 테스트 (`@ExtendWith(MockitoExtension.class)`)

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("signup 성공: 중복 없으면 암호화 후 저장")
    void signup_success() {
        // given
        SignupReq req = new SignupReq("newuser", "rawPassword1!", "01011111111");
        given(userRepository.findByUsername("newuser")).willReturn(Optional.empty());
        given(passwordEncoder.encode("rawPassword1!")).willReturn("encoded_pw");

        // when
        userService.signup(req);

        // then
        verify(passwordEncoder).encode("rawPassword1!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("signup 실패: 중복 username → IllegalArgumentException")
    void signup_duplicateUsername() {
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> userService.signup(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 존재하는 아이디입니다.");

        verify(userRepository, never()).save(any());  // 저장 호출 없음 검증
    }
}
```

**규칙**
- Spring 컨텍스트를 로드하지 않는다. 의존성은 전부 `@Mock` 으로 대체.
- `@InjectMocks` 대상 클래스의 `@Async` 는 무시되고 동기로 실행된다. 비즈니스 로직 검증에 활용.
- BDD 스타일 (`given / when / then`) 주석으로 테스트 흐름을 구분한다.
- 저장·호출 검증에는 `verify(...)`, 미호출 검증에는 `verify(..., never())` 를 사용한다.
- `ArgumentCaptor` 로 저장된 엔티티의 내부 값을 직접 검증한다.

```java
// ArgumentCaptor 활용 예: 저장된 AnimalResult 내용 검증
ArgumentCaptor<AnimalResult> captor = ArgumentCaptor.forClass(AnimalResult.class);
verify(animalResultRepository).save(captor.capture());

assertThat(captor.getValue().getAnimalType()).isEqualTo(AnimalType.DOG);
assertThat(captor.getValue().getScore()).isEqualTo(0.92);
```

---

## 4. Controller 테스트 (`@WebMvcTest`)

```java
@WebMvcTest(AnimalController.class)
@Import(SecurityConfig.class)  // 실제 Security 정책(CSRF 비활성화, permitAll 경로) 반영
class AnimalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnimalCommandService animalCommandService;

    @MockBean
    private AnimalQueryService animalQueryService;

    @MockBean
    private S3Provider s3Provider;
}
```

**규칙**
- `@WebMvcTest(XxxController.class)`: 해당 컨트롤러만 로드. Service·Repository는 `@MockBean`.
- `@Import(SecurityConfig.class)` 반드시 추가. 미추가 시 POST 요청이 CSRF로 403 반환.
- 인증이 필요한 엔드포인트는 두 가지 방법으로 처리:

```java
// 방법 1 — @WithMockUser (@AuthenticationPrincipal 없는 경우)
@Test
@WithMockUser
void findId_success() { ... }

// 방법 2 — UsernamePasswordAuthenticationToken (User 엔티티를 principal로 직접 주입)
//          @AuthenticationPrincipal User user 파라미터가 있는 엔드포인트에 사용
private UsernamePasswordAuthenticationToken userAuth() {
    User mockUser = User.builder()
            .username("testuser").phone("01012345678").password("pw").build();
    return new UsernamePasswordAuthenticationToken(mockUser, null, Collections.emptyList());
}

mockMvc.perform(get("/api/animal/results")
        .with(authentication(userAuth())))
        .andExpect(status().isOk());
```

- 미인증 응답 코드: `SecurityConfig` 에 `formLogin()` 미설정 시 `403 Forbidden` 반환 (302 리다이렉트 아님).
- Spring Boot 3.x(Spring 6)에서 `@ExceptionHandler` 없이 컨트롤러 예외 발생 시 `MockMvc.perform()` 이 `ServletException` 으로 전파됨 → `assertThatThrownBy` 로 검증.

```java
// 전역 ExceptionHandler 없는 환경에서 예외 검증
assertThatThrownBy(() ->
        mockMvc.perform(post("/api/user/signup").contentType(APPLICATION_JSON).content(...)))
        .isInstanceOf(jakarta.servlet.ServletException.class)
        .hasCauseInstanceOf(IllegalArgumentException.class)
        .hasRootCauseMessage("이미 존재하는 아이디입니다.");
```

---

## 5. 순수 단위 테스트

Spring 컨텍스트 없이 단일 메서드의 입출력만 검증한다.

```java
class AiAnalyzeResTest {

    @ParameterizedTest
    @ValueSource(strings = {"dog", "Dog", "dOg", "DOG"})
    @DisplayName("toAnimalType: 대소문자 무관 → AnimalType.DOG")
    void toAnimalType_caseInsensitive(String input) {
        assertThat(new AiAnalyzeRes(input, 0.9).toAnimalType()).isEqualTo(AnimalType.DOG);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("toAnimalType: null 또는 빈 문자열 → AnimalType.UNKNOWN")
    void toAnimalType_nullOrEmpty(String input) {
        assertThat(new AiAnalyzeRes(input, 0.0).toAnimalType()).isEqualTo(AnimalType.UNKNOWN);
    }
}
```

**규칙**
- `@ParameterizedTest` + `@ValueSource` / `@NullAndEmptySource` 로 엣지 케이스를 한 번에 커버한다.
- 변환 메서드(`toAnimalType`, `from`, `of`)는 순수 단위 테스트로 작성한다.

---

## 6. 테스트용 application.yml

`src/test/resources/application.yml` 은 메인 설정을 완전히 덮어쓴다.

```yaml
spring:
  profiles:
    include:            # aws 프로파일 비활성화 (S3·dotenv 의존성 제거)
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        highlight_sql: true
        use_sql_comments: true

logging:
  level:
    org.hibernate.orm.jdbc.bind: trace   # 바인딩 파라미터 출력

ai:
  server:
    url: http://localhost:8000           # Feign 빈 등록 실패 방지
```

**규칙**
- `spring.profiles.include:` 를 비워 aws 프로파일을 비활성화한다. S3·dotenv 의존성이 테스트에 개입하지 않는다.
- H2 `MODE=MySQL`: MySQL 방언을 에뮬레이션하여 쿼리 호환성을 높인다.
- `ddl-auto: create-drop`: 테스트 시작 시 스키마 생성, 종료 시 제거.
- `ai.server.url` 은 반드시 포함. 미포함 시 `@FeignClient` 빈 등록 단계에서 `MalformedURLException` 발생.

---

## 7. 전체 컨텍스트 로드 테스트 (`@SpringBootTest`)

```java
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
```

**규칙**
- AWS 자격증명 프로퍼티를 더미 값으로 인라인 주입하여 `S3Config` 빈 생성 오류를 방지한다.
- `S3Presigner` 는 빌드 시점에 실제 AWS 연결을 맺지 않으므로 더미 자격증명으로도 빈 생성이 가능하다.
- 실제 외부 API 연동이 필요한 테스트는 별도 통합 테스트로 분리한다.

---

## 8. 테스트 메서드 명명 규칙

```
{테스트대상}_{시나리오}()
```

| 패턴 | 예시 |
|---|---|
| 성공 케이스 | `signup_success`, `getMyResults_returnsMappedDtos` |
| 실패 케이스 | `signup_duplicateUsername`, `analyzeAndSave_userNotFound` |
| 조건 기술 | `toAnimalType_caseInsensitive`, `getMyResults_empty` |

- `@DisplayName` 은 한국어로 테스트 의도를 서술한다. 형식: `"[대상] [조건]: [기대 결과]"`
- 테스트 메서드명은 영문 camelCase, `@DisplayName` 은 한국어 문장형으로 작성한다.

---

## 9. 자주 하는 테스트 실수 체크리스트

| 항목 | 잘못된 예 | 올바른 예 |
|---|---|---|
| `@DataJpaTest` Auditing | `@EnableJpaAuditing` 누락 → `createdAt` null | `@Import(JpaAuditingConfig.class)` 추가 |
| `@WebMvcTest` CSRF | `@Import(SecurityConfig.class)` 누락 → POST 403 | `@Import(SecurityConfig.class)` 추가 |
| 미인증 상태 코드 | `formLogin()` 없을 때 401 기대 | 403 (`Http403ForbiddenEntryPoint`) |
| test yaml 누락 | `ai.server.url` 없음 → Feign MalformedURLException | test yaml에 `ai.server.url` 추가 |
| 예외 전파 (Spring 6) | `andExpect(status().is5xxServerError())` | `assertThatThrownBy` + `hasCauseInstanceOf` |
| 인증 주입 방식 혼용 | `@WithMockUser` → `User` 타입 캐스트 실패 | `authentication(new UsernamePasswordAuthenticationToken(user, ...))` |
