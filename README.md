# 4iren-payment-api

결제/구독(정기결제) 서비스. Toss페이/카카오페이/네이버페이 연동, 결제 성공 시 Account 서비스로 OWNER 승급/강등 이벤트를 발행한다.

## 기술 스택

- Spring Boot 3.5.16, Java 21, Maven
- MySQL 8.0 + Flyway
- RabbitMQ (Account 서비스와의 이벤트 연동)
- Redis + Shedlock (자동청구 스케줄러 분산락)
- springdoc-openapi (Swagger UI)

## API

| 도메인 | Base Path |
|---|---|
| 구독 조회/해지 | `/api/payment/subscriptions` |
| 결제 이력 조회 | `/api/payment/payments` |
| 요금제 가격 조회 (공개) | `/api/payment/plans` |
| 토스페이 빌링키 등록/변경/콜백 | `/api/payment/billing-keys/toss` |
| 카카오페이 빌링키 등록/변경/콜백 | `/api/payment/billing-keys/kakao` |

대부분의 엔드포인트는 `X-USER-ID`, `X-USER-ROLE` 헤더로 인증/인가한다(게이트웨이가 실어줌).

서버 실행 후 아래에서 전체 API 스펙을 확인할 수 있다.

- Swagger UI: `/swagger-ui.html`
- OpenAPI 스펙: `/v3/api-docs`

## 로컬 실행

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

`spring.config.import`로 Spring Cloud Config Server(`localhost:8888`)에서 설정을 받아온다. Config Server가 떠 있지 않으면 MySQL 접속 정보를 못 받아 내장 H2로 자동 폴백되는데, 이 경우 Flyway 마이그레이션이 MySQL 전용 문법이라 실패한다 — Config Server(+ 실제 MySQL/Redis/RabbitMQ)를 먼저 띄울 것.

## 빌드/테스트

```bash
./mvnw verify
```

JaCoCo 라인 커버리지 60% 미만이면 `verify` 단계에서 빌드가 실패한다.

## 문서

설계 근거, DB 스키마, PG 연동 가이드, 이슈 목록 등 상세 문서는 팀 Obsidian vault에서 관리한다. 이 저장소에는 협업 규칙과 아키텍처 원칙을 담은 [CLAUDE.md](CLAUDE.md)만 둔다.
