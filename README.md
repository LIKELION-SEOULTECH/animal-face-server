# 동물상 찾기 프로젝트

서울과학기술대학교 멋쟁이 사자처럼 14기 개발 공통 세션 동물상 찾기 프로젝트

## 프로젝트 개요

사용자의 얼굴 사진을 분석하여 닮은 동물상을 찾아주는 웹 애플리케이션입니다.
Spring Boot 기반의 백엔드 서버로, OpenFeign을 통해 AI 서버와 통신합니다.

## 기술 스택

- **Framework**: Spring Boot 4.0.3
- **Language**: Java 17
- **Build Tool**: Gradle
- **ORM**: Spring Data JPA
- **HTTP Client**: OpenFeign (AI 서버 통신)
- **API Documentation**: SpringDoc OpenAPI 3.0.2

## 주요 기능 - 사용자 얼굴 이미지 업로드
- AI 서버와 Feign Client를 통한 동물상 분석 요청
- 분석 결과 저장 및 조회
- RESTful API 제공

## 개발 환경 설정

### 필수 요구사항

- Java 17 이상
- Gradle 9.3.1 이상

### 프로젝트 실행

```bash
# 프로젝트 클론
git clone [repository-url]
cd animal-face-server

# 빌드 및 실행 (Windows)
gradlew.bat bootRun

# 빌드 및 실행 (Linux)
./gradlew bootRun
```

## AI 서버 연동

### Feign Client 설정

OpenFeign을 사용하여 AI 서버와 통신합니다.

> **참고**: AI 서버 API 명세는 현재 개발 중입니다.

```java
// 예정된 구조 (추후 구현)
@FeignClient(name = "ai-server", url = "${ai.server.url}")
public interface AIServerClient {
    // AI 분석 요청 API
}
```

## 프로젝트 구조

```
animal-face-server/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/likelion/animalface/
│   │   │       ├── AnimalFaceApplication.java
│   │   │       ├── controller/
│   │   │       ├── service/
│   │   │       ├── domain/
│   │   │       ├── repository/
│   │   │       ├── dto/
│   │   │       ├── common/
│   │   │       └── client/  # Feign Client
│   │   └── resources/
│   │       └── application.yml
│   └── test/
└── build.gradle
```

## 개발 진행 상황

- [o] 프로젝트 초기 설정
- [o] Spring Boot 기본 구성
- [ ] Feign Client 설정
- [ ] AI 서버 API 연동
- [ ] 동물상 분석 기능 구현
- [ ] 결과 저장/조회 API 구현
