# PIMTO 이동식 자판기 서버

이동식 자판기 관리 및 사용자 서비스를 제공하는 Spring Boot 기반 웹 서버입니다.

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.2.5 |
| View | Thymeleaf |
| Security | Spring Security 6 (BCrypt) |
| DB | MariaDB (운영) / H2 (개발) |
| ORM | Spring Data JPA |
| Real-time | WebSocket |
| Build | Gradle |
| Etc | Lombok |

---

## 주요 기능

- 상품 목록 조회 및 구매
- 회원가입 / 로그인 / 아이디·비밀번호 찾기
- 포인트 적립 및 사용
- 마이페이지 (주문내역, 포인트 현황)
- 1:1 문의 게시판 (비밀글, 관리자 답글)
- 실시간 채팅 (WebSocket)
- 관리자 페이지 (상품 관리, 회원 관리, 매출 통계, 로봇 관제)

---

## 실행 방법

### 1. DB 설정

MariaDB에 `vending_machine` 데이터베이스를 생성합니다.

```sql
CREATE DATABASE vending_machine CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

`src/main/resources/application.properties`에서 DB 접속 정보를 확인·수정합니다.

```properties
spring.datasource.url=jdbc:mariadb://localhost:3306/vending_machine?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=mysql1234
```

### 2. 빌드 및 실행

```bash
./gradlew bootRun
```

접속 주소: `http://localhost:8080`

### 3. 기본 관리자 계정

| 아이디 | 비밀번호 |
|--------|----------|
| admin | admin |

---

## 패키지 구조

```
src/main/java/com/example/vendingmachine/
├── config/          # SecurityConfig, WebSocketConfig, DataInitializer
├── controller/      # 요청 처리 (View / API 분리)
├── domain/          # JPA Entity
├── repository/      # Spring Data JPA Repository
└── service/         # 비즈니스 로직
```

---

## 브랜치 전략

```
개인 브랜치 (kwantae / yooyeon)
        ↓  PR
      test  ←  통합 테스트
        ↓  PR
     master  ←  배포
```

- 기능 개발은 개인 브랜치에서 진행합니다.
- `test` 브랜치에서 통합 후 동작을 확인합니다.
- 확인된 코드만 `master`로 병합합니다.
- 직접 `master` push는 금지합니다.

---

## 커밋 컨벤션

```
<type>(<scope>): <subject>
```

| type | 설명 |
|------|------|
| feat | 새 기능 추가 |
| fix | 버그 수정 |
| refactor | 리팩토링 (기능 변경 없음) |
| style | UI / CSS 변경 |
| chore | 빌드·설정 변경 |
| docs | 문서 수정 |

**예시**

```
feat(member): 마이페이지 포인트 기능 추가
fix(inquiry): 비밀글 접근 권한 오류 수정
style(admin): 관리자 페이지 모바일 레이아웃 수정
```

---

## 개발 규칙

1. **브랜치 보호** — `master`에 직접 커밋하지 않습니다.
2. **PR 필수** — `test → master` 병합은 반드시 PR을 통해 진행합니다.
3. **application.properties 보호** — DB 비밀번호 등 민감 정보는 커밋 전 확인합니다.
4. **DDL 자동 생성** — `spring.jpa.hibernate.ddl-auto=update` 설정이므로 Entity 변경 시 팀원에게 공유합니다.
5. **네이밍** — 클래스는 PascalCase, 변수·메서드는 camelCase, URL은 kebab-case를 사용합니다.

---

## 팀원

| 역할 | 이름 |
|------|------|
| 문의·채팅·관리자 기능 | kwantae |
| 마이페이지·포인트·회원 기능 | yooyeon |
