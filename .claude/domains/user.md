# User 도메인 컨벤션 (USER)

> 유저 관리 도메인(`domain/user/`)에 특화된 규칙을 정의한다.
> 공통 코딩 스타일은 [../conventions/code.md](../conventions/code.md) 를 따른다.

---

## 도메인 개요

| 항목 | 내용 |
|---|---|
| 패키지 | `domain/user/` |
| 주요 기능 | 회원가입, 아이디 찾기, 임시 비밀번호 발급 |
| 서비스 패턴 | 단일 `UserService` (CRUD 간단, CQRS 분리 불필요) |
| 엔티티 | `User` |

---

## 서비스 패턴 — 단일 UserService

User 도메인은 쓰기/읽기 작업이 적어 CQRS 분리 없이 단일 서비스를 사용한다.

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)   // 클래스 기본: readOnly
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional                // 쓰기 메서드만 개별 선언
    public void signup(SignupReq req) {
        // 1. 중복 검증
        if (userRepository.findByUsername(req.username()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }
        // 2. 비밀번호 암호화 후 저장
        String encodedPassword = passwordEncoder.encode(req.password());
        userRepository.save(req.to(encodedPassword));
    }

    public UserIdRes getUsername(String phone) { ... }  // readOnly 상속
}
```

**규칙**
- 클래스 레벨 `@Transactional(readOnly = true)` 기본 설정.
- 쓰기 메서드(`signup`, `resetPassword` 등)에만 `@Transactional` 추가.
- Spring Security의 `UserDetailsService` 구현이 필요한 경우 `UserService` 또는 별도 `UserDetailsServiceImpl` 에 위치.

---

## Entity — User

```java
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "users")
@Entity
public class User extends BaseTimeEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String phone;

    // 도메인 메서드
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
```

**규칙**
- 테이블명: `users` (복수형, SQL 예약어 `user` 충돌 방지)
- `UserDetails` 구현 시 `getAuthorities()` 등 필수 메서드는 최소한으로 구현.
- 비밀번호 변경은 `updatePassword(encodedPassword)` 도메인 메서드를 통한다.

---

## DTO

| 클래스 | 위치 | 설명 |
|---|---|---|
| `SignupReq` | `dto/req/SignupReq.java` | 회원가입 요청 |
| `UserIdRes` | `dto/res/UserIdRes.java` | 아이디(username) 응답 |
| `UserPasswordRes` | `dto/res/UserPasswordRes.java` | 임시 비밀번호 응답 |

```java
// SignupReq — to() 메서드로 엔티티 변환
public record SignupReq(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String phone
) {
    public User to(String encodedPassword) {
        return User.builder()
                .username(username)
                .password(encodedPassword)
                .phone(phone)
                .build();
    }
}

// UserIdRes — of() 팩토리 패턴
public record UserIdRes(String username) {
    public static UserIdRes of(User user) {
        return new UserIdRes(user.getUsername());
    }
}

// UserPasswordRes — 메시지 + 임시 비밀번호
public record UserPasswordRes(String message, String tempPassword) {
    public static UserPasswordRes of(String tempPassword) {
        return new UserPasswordRes("임시 비밀번호가 발급되었습니다.", tempPassword);
    }
}
```

---

## Repository

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByPhone(String phone);
    boolean existsByUsername(String username);
}
```

---

## Controller — /api/user

```java
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 1. 회원가입 API
     */
    @PostMapping("/signup")
    public ApiResponse<Void> signup(@RequestBody SignupReq req) {
        userService.signup(req);
        return ApiResponse.message("회원가입이 완료되었습니다.");
    }

    /**
     * 2. 아이디 찾기 API
     */
    @GetMapping("/find-id")
    public ApiResponse<UserIdRes> findId(@RequestParam String phone) {
        return ApiResponse.ok(userService.getUsername(phone));
    }
}
```

**규칙**
- `signup` 처럼 반환 데이터가 없는 경우 `ApiResponse<Void>` + `ApiResponse.message(...)` 사용.
- 인증이 불필요한 엔드포인트(`/signup`, `/find-id`)는 `SecurityConfig` 의 `permitAll()` 경로에 추가.
- 인증된 사용자 정보가 필요한 경우 `@AuthenticationPrincipal User user` 로 받는다.

---

## 인증 처리

- Spring Security `UserDetailsService` 구현 → `loadUserByUsername(username)` 에서 `UserRepository.findByUsername()` 호출.
- JWT 인증 시 Security Context에 `UsernamePasswordAuthenticationToken(user, null, authorities)` 저장.
- 컨트롤러에서 인증 사용자 추출: `@AuthenticationPrincipal User user` (클라이언트에서 userId를 받지 않는다).

---

## 테스트 예시 요약

> 전체 테스트 규칙은 [../conventions/test.md](../conventions/test.md) 참조

| 테스트 클래스 | 계층 | 주요 검증 |
|---|---|---|
| `UserRepositoryTest` | `@DataJpaTest` | `findByUsername`, `findByPhone` 쿼리 정확성 |
| `UserServiceTest` | `@ExtendWith(MockitoExtension.class)` | 중복 가입 예외, 비밀번호 암호화 검증 |
| `UserControllerTest` | `@WebMvcTest` | 회원가입 201, 중복 아이디 예외 전파, 미인증 접근 |

**UserServiceTest 핵심 시나리오**

```
signup_success                  — 중복 없을 때 암호화 후 저장 검증
signup_duplicateUsername        — IllegalArgumentException 발생, save 미호출 검증
getUsername_success             — phone으로 username 반환 검증
getUsername_notFound            — EntityNotFoundException 발생 검증
```
