# AnimalFace 기술 스택 (SKILLS)

> 이 파일은 프로젝트에서 사용하는 기술 스택과 라이브러리 버전을 정의한다.
> Claude가 deprecated API를 추천하거나 프로젝트에 없는 라이브러리를 사용하는 실수를 방지한다.

---

## 핵심 스택

| 항목 | 버전 / 상세 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4.3 |
| Build | Gradle (Groovy DSL) |
| ORM | Spring Data JPA (Hibernate 6.x) |
| HTTP Client | Spring Cloud OpenFeign |
| Security | Spring Security |
| Persistence | MySQL (운영), H2 인메모리 (테스트) |
| Object Storage | AWS S3 (`S3Presigner` 사용) |

---

## 주요 라이브러리

| 라이브러리 | 용도 |
|---|---|
| Lombok | 보일러플레이트 코드 제거 |
| Spring Validation (Jakarta) | DTO 입력값 검증 |
| Spring Cloud OpenFeign | 외부 AI 서버 HTTP 통신 |
| AWS SDK v2 (S3Presigner) | S3 Presigned URL 발급 |
| dotenv-java | 로컬 `.env` 파일 환경변수 로딩 |

---

## Java 21 특이사항

- **Virtual Threads** 사용: `application.yml` 에 `spring.threads.virtual.enabled: true` 설정
- `AsyncConfig` 에서 `Executors.newVirtualThreadPerTaskExecutor()` 로 비동기 풀 구성
- Records, Pattern Matching, Text Blocks 등 Java 17~21 문법 전부 사용 가능

---

## 주의사항 (절대 사용 금지)

| 금지 항목 | 이유 | 대체 |
|---|---|---|
| `javax.*` 패키지 | Spring Boot 3.x는 Jakarta EE 10 기반 | `jakarta.*` 사용 |
| `WebSecurityConfigurerAdapter` | Spring Security 5.7+ deprecated | `SecurityFilterChain` Bean 방식 |
| `@Autowired` 필드 주입 | 테스트 어렵고 NPE 위험 | `@RequiredArgsConstructor` 생성자 주입 |
| `FetchType.EAGER` | N+1·성능 문제 | `FetchType.LAZY` + Fetch Join |
| `@Enumerated(EnumType.ORDINAL)` | 순서 변경 시 데이터 오염 | `EnumType.STRING` |
| Native Query | 이식성 저하 | JPQL 또는 Querydsl |
| `@Transactional` 클래스 레벨 (Command) | 쓰기/읽기 분리 불명확 | 메서드 단위 선언 |
| 엔티티 직접 생성 (`new AnimalResult(...)`) | 팩토리 메서드 강제 패턴 위반 | `Entity.create(...)` 팩토리 메서드 사용 |

---

## 설정 파일 구조

```
src/main/resources/
├── application.yml           # 공통 설정 (Virtual Threads, AI URL 등)
└── application-aws.yml       # AWS 자격증명 (환경변수 참조)

src/test/resources/
└── application.yml           # 테스트 전용 (H2, aws 프로파일 비활성화)
```

- 민감 정보(키, 비밀번호)는 반드시 `${환경변수명}` 으로 주입. 하드코딩 금지.
- 외부 시스템 URL 키 패턴: `{시스템명}.server.url`

---

## 테스트 스택

| 항목 | 상세 |
|---|---|
| 프레임워크 | JUnit 5 + AssertJ |
| 목 프레임워크 | Mockito (BDD 스타일: `given/when/then`) |
| 슬라이스 테스트 | `@DataJpaTest`, `@WebMvcTest` |
| 전체 컨텍스트 | `@SpringBootTest` |
| 인메모리 DB | H2 (`MODE=MySQL`) |

> 테스트 상세 규칙은 [test.md](test.md) 참조
