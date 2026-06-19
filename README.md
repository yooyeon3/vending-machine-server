<div align="center">

# 🤖 PIMTO
**자율주행 이동식 자판기 웹 관제 시스템 (Smart Mobile Vending Machine & Monitoring System)**

[![Java](https://img.shields.io/badge/Java-17-007396?style=for-the-badge&logo=java&logoColor=white)](#)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](#)
[![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)](#)
[![ROS](https://img.shields.io/badge/ROS-Noetic-22314E?style=for-the-badge&logo=ros&logoColor=white)](#)
[![WebSocket](https://img.shields.io/badge/WebSocket-Live-000000?style=for-the-badge)](#)
[![Gemini API](https://img.shields.io/badge/Gemini_AI-Chatbot-4285F4?style=for-the-badge&logo=google&logoColor=white)](#)

*고객이 자판기를 찾는 것이 아니라, 자판기가 고객을 찾아가는 스마트 O2O 솔루션*

</div>

<br>

## 📝 Project Overview
**PIMTO**는 사용자에게 찾아가는 자율주행 로봇 기반의 이동식 자판기 웹 서비스 및 관제 시스템입니다.
사용자는 웹을 통해 간편하게 상품을 구매하거나 AI 챗봇 및 실시간 채팅으로 문의할 수 있으며, 관리자는 원격에서 실시간으로 로봇의 위치를 관제하고 재고 및 매출 통계를 관리할 수 있는 **통합 솔루션**입니다.

<br>

## ✨ Key Features

### 👥 1. 보안 및 권한 관리 (Spring Security)
* `USER`와 `ADMIN` 권한을 완벽히 분리하여 안전한 페이지 접근 제어.
* 세션 및 쿠키 기반의 안전한 로그인 유지 및 아이디/비밀번호 찾기 지원.

### 🥤 2. 스마트 자판기 주문 시스템
* 메인 대시보드에서 직관적인 상품 이미지 및 실시간 재고 상태 확인.
* 구매 발생 시 즉각적인 DB 반영(재고 감소) 및 안전한 트랜잭션 처리.
* 사용자 마이페이지 내 **과거 구매 이력(Order History)** 조회 기능 제공.

### 🤖 3. AI 챗봇 & 실시간 채팅 시스템 (WebSocket)
* **Google Gemini API 연동:** 사용자의 단순 질문이나 이용 방법에 대해 AI가 즉각 답변하는 스마트 챗봇.
* **WebSocket 실시간 통신:** AI가 해결하지 못한 이슈는 관리자와의 1:1 실시간 채팅으로 전환하여 즉각적인 고객 서비스(CS) 응대.

### 📊 4. 엔터프라이즈급 관리자 대시보드 (Admin)
* **재고 관리:** 클릭 한 번으로 상품 추가/제거 및 직관적인 (+)/(-) 재고 컨트롤.
* **데이터 시각화:** `PurchaseHistory` 데이터를 기반으로 한 **시간대별 판매량 선 그래프(Chart.js)** 제공.
* **📡 실시간 로봇 관제 (ROS 연동):** `rosbridge_websocket`을 통해 실제 로봇의 SLAM 맵을 브라우저 캔버스로 실시간 스트리밍 및 긴급 호출(원위치 복귀) 기능 탑재.

<br>

## 📂 Architecture & Directory Structure
철저한 **MVC 패턴**과 도메인 중심(Domain-driven) 계층형 아키텍처를 적용하여 유지보수성과 확장성을 극대화했습니다.

```text
📦 PIMTO-Project
 ┣ 📂 src/main/java/com/example/vendingmachine
 │  ┣ ⚙️ config       # Spring Security, WebSocket, Data Init 등 전역 환경 설정
 │  ┣ 🎮 controller   # 클라이언트 웹 요청(HTTP/WS) 처리 및 뷰 반환 (MVC - Controller)
 │  ┣ 🏛️ domain       # 데이터베이스 테이블과 매핑되는 JPA 엔티티 (Member, Product 등)
 │  ┣ 🗄️ repository   # 데이터베이스와 직접 통신하여 CRUD를 수행하는 JPA 인터페이스
 │  ┗ 🛠️ service      # 핵심 비즈니스 로직 처리 및 트랜잭션 관리
 │
 ┗ 📂 src/main/resources
    ┣ 🖼️ static       # CSS, JS, 상품/로봇 이미지 로고 등 정적 리소스
    ┗ 🖥️ templates    # Thymeleaf 기반 동적 HTML 뷰 (MVC - View)
       ┗ 🧩 fragments # 재사용 가능한 UI 컴포넌트 모듈화 (네비게이션, 채팅 위젯 등)
```

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
