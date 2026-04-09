# AnimalFace 개발 워크플로우 (WORKFLOW)

> 기능 개발 시 따라야 할 순서와 프로세스를 정의한다.
> "이 기능 만들어줘" 요청 시 아래 순서를 기본으로 진행한다.

---

## 기능 개발 순서

1. **Entity** 정의 (`domain/{도메인}/entity/`)
2. **Repository** 인터페이스 작성 (`domain/{도메인}/repository/`)
3. **Service** 구현 (`domain/{도메인}/service/`)
   - 쓰기·비동기: `{도메인}CommandService`
   - 읽기 전용: `{도메인}QueryService`
   - 단순 CRUD: `{도메인}Service`
4. **DTO** 작성 (`domain/{도메인}/dto/`)
   - 요청: `{도메인}{목적}Req.java` (record)
   - 응답: `{도메인}{목적}Res.java` (record)
5. **Controller** 구현 (`domain/{도메인}/controller/`)
6. **테스트** 작성 (단위 → 슬라이스 순서)
   - Repository → Service → Controller

> 코드 컨벤션은 [code.md](code.md) 참조
> 테스트 작성 규칙은 [test.md](test.md) 참조

---

## 브랜치 전략

```
main              ← 배포 브랜치 (PR 대상)
└── feature/{도메인}/{기능명}   ← 기능 개발 브랜치
```

**브랜치 명명 예시**

```
feature/user/signup
feature/animal/analyze
feature/animal/result-list
fix/animal/n-plus-one
```

---

## 커밋 메시지 (Conventional Commits)

```
{타입}({#이슈번호}): {한국어 요약}

- {파일명}: {변경 내용}
- {파일명}: {변경 내용}
```

| 타입 | 용도 |
|---|---|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `refactor` | 리팩토링 (기능 변경 없음) |
| `test` | 테스트 코드 추가/수정 |
| `docs` | 문서 수정 |
| `chore` | 빌드·설정·의존성 변경 |
| `perf` | 성능 개선 |

**규칙**
- body는 제목과 빈 줄 하나로 구분한다.
- 파일별로 한 줄씩 변경 내용을 간단히 기술한다.
- 변경 파일이 1개인 경우 body 생략 가능.
- **커밋은 최소 기능 단위로 나눈다.** 하나의 커밋에 서로 다른 관심사를 섞지 않는다.
  - 프로덕션 코드와 테스트 코드는 별도 커밋으로 분리한다.
  - 기능 구현(`feat`)과 문서 수정(`docs`)은 별도 커밋으로 분리한다.
  - 리팩토링(`refactor`)과 버그 수정(`fix`)은 별도 커밋으로 분리한다.

**예시**

```
feat(#12): 동물상 분석 비동기 처리 구현

- AnimalCommandService: analyzeAndSave 비동기 메서드 추가
- AsyncConfig: Virtual Thread 기반 asyncExecutor 빈 등록
- AnimalController: 분석 요청 API 엔드포인트 추가
```

```
fix(#8): AnimalResult 목록 조회 N+1 문제 해결

- AnimalResultRepository: findAllByUserIdWithUser에 Fetch Join 적용
```

```
test(#5): UserService 단위 테스트 추가

- UserServiceTest: signup 성공/중복 시나리오 추가
- UserRepositoryTest: findByUsername 쿼리 검증 추가
```

---

## PR 규칙

- PR 제목: 커밋 메시지 형식과 동일
- PR 본문: [PULL_REQUEST_TEMPLATE.md](../../.github/PULL_REQUEST_TEMPLATE.md) 양식 준수
- PR 대상 브랜치: `main`
- 셀프 리뷰 후 팀원 1명 이상 Approve 후 merge
- `main` 브랜치 직접 push 금지

---

## 도메인별 가이드

| 도메인 | 파일 |
|---|---|
| 유저 관리 | [../domains/user.md](../domains/user.md) |
| 동물상 찾기 | [../domains/animal.md](../domains/animal.md) |
