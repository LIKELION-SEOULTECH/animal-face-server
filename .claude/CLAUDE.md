# AnimalFace — Claude 컨텍스트 진입점

> 일반적으로 코드 컨벤션은 팀 내부에서만 공유하지만, 이 프로젝트는 토이 프로젝트인 만큼
> 다른 사람들이 Claude Code와 협업하는 방식을 참고할 수 있도록 `.claude/` 폴더를 공개합니다.

> Spring Boot 3.4.3 / Java 21 / Spring Cloud OpenFeign

## 문서 구조

```
.claude/
├── CLAUDE.md                  ← 지금 여기
├── conventions/
│   ├── code.md                ← 공통 코드 스타일 & 설계 원칙
│   ├── skills.md              ← 기술 스택 & 의존성 & 사용 금지 API
│   ├── test.md                ← 테스트 계층별 작성 규칙
│   └── workflow.md            ← 기능 개발 순서, 브랜치, 커밋 메시지
└── domains/
    ├── user.md                ← 유저 관리 도메인 전용 컨벤션
    └── animal.md              ← 동물상 찾기 도메인 전용 컨벤션
```

## 빠른 참조

| 상황 | 읽을 파일 |
|---|---|
| 코드 작성 시작 전 | [conventions/code.md](conventions/code.md) |
| 기술 스택 / 라이브러리 확인 | [conventions/skills.md](conventions/skills.md) |
| 기능 개발 순서 / 브랜치 전략 | [conventions/workflow.md](conventions/workflow.md) |
| 테스트 코드 작성 | [conventions/test.md](conventions/test.md) |
| User 도메인 작업 | [domains/user.md](domains/user.md) |
| Animal 도메인 작업 | [domains/animal.md](domains/animal.md) |

## 핵심 원칙 (항상 적용)

- DTO는 **record** 사용. Lombok 금지.
- 엔티티 생성은 **`Entity.create(...)` 팩토리 메서드** 사용. `new` 직접 사용 금지.
- `@AuthenticationPrincipal` 로 인증 사용자 추출. 클라이언트에서 `userId` 받지 않는다.
- `FetchType.EAGER` 금지. `FetchType.LAZY` + Fetch Join.
- `javax.*` 금지. `jakarta.*` 사용.
- 예외 메시지는 한국어.