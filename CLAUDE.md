# Payment API

결제/구독(정기결제) 서비스 — Toss페이/카카오페이/네이버페이 연동, 결제 성공 시 OWNER 승급/강등 이벤트 발행.

**문서 관리는 전부 Obsidian에서 한다 (이 파일 제외).** 설계 근거, DB 스키마, PG 연동 가이드, 이슈 목록, "왜 이렇게 했는가" 전부 Obsidian vault의 `4iren/payment/`에 있다. **세션 시작 시 반드시 `00-INDEX.md`부터 읽고, `2-Areas/핸드오프-현재상태.md`와 `2-Areas/아키텍처-Q&A.md`를 확인할 것.** repo(이 파일 제외)엔 문서를 두지 않는다.

## 스택
- Spring Boot 3.5.16, Java 21, Maven
- MySQL 8.0 (전용 스키마 `payment_db`, 다른 서비스 DB와 완전 분리)
- RabbitMQ (Account와의 OWNER 승급/강등 이벤트 연동)
- Redis + Shedlock (자동청구 스케줄러 분산락)

## 아키텍처 원칙 (반드시 지킬 것)
1. **Database-per-Service**: 다른 서비스 DB에 직접 접근 금지. Account/Core와는 RabbitMQ 이벤트로만.
2. **OWNER는 Account 소유 개념**(`UserRole.OWNER`). Core의 `TeamRole.OWNER`는 팀 멤버십 내 별개 역할 — 이름만 같고 무관, 혼동 주의.
3. **하드 삭제 없음**. `billing_keys`/`subscriptions`는 전부 상태 전이(`ACTIVE`/`DELETED`, `PAST_DUE`/`EXPIRED` 등)로만 표현. FK는 `ON DELETE` 절 없음(기본값 `RESTRICT`) — cascade 자체를 안 씀.
4. **`billing_keys.provider_credential`은 반드시 암호화**(`EncryptedStringConverter`). `payment.crypto.password`/`salt`는 `4iren-config-repo`에서 공급, 로컬 application.yaml엔 두지 않음.
5. **결제수단 변경은 in-place UPDATE 금지** — 새 row 등록(ACTIVE) 후 기존 row `DELETED` 처리, `subscriptions.billing_key_id` 재연결.
6. **generic `updated_at` 없음** — 대신 의미 있는 개별 전이 컬럼(`canceled_at`/`expired_at`/`approved_at`/`deleted_at`) 사용.
7. **정기결제는 "등록"과 "청구"가 별개 이벤트**. 등록 확정 전까지 `billing_keys` row 자체를 만들지 않음(Redis TTL로 상관관계만 임시 보관).
8. **한 번이라도 배포된 Flyway 마이그레이션 파일은 절대 수정 금지.** DB 스키마 변경이 필요하면 기존 `V1__...sql`을 고치지 말고 반드시 새 `V2__...sql`, `V3__...sql`처럼 새 버전 파일을 추가할 것. 이미 적용된 파일을 고치면 체크섬이 달라져서 배포 시 `Migration checksum mismatch`로 앱이 아예 안 뜬다(2026-08-27 배포 장애 원인).

## 작업 방식 (Claude 협업 규칙)

1. **핸드오프 문서에 "다음 순서"가 이미 적혀 있어도, 실제 구현/설계 방향은 반드시 먼저 사용자에게 물어보고 진행할 것.** 핸드오프는 "어디까지 됐는지"의 기록이지 "어떻게 짜라"는 확정 지시가 아님 — 순서만 보고 임의로 설계 디테일을 정해서 진행하지 말 것.
2. **외부 API(PG 등) 연동 코드를 짤 때는, 코드를 쓰기 전에 먼저 "우리 서버가 어떤 요청을 보내고, 어떤 응답을 기대하는지"(요청/응답 계약)를 말로 명시하고, 그 계약에 따라 코드가 이렇게 동작한다고 설명한 뒤 작성할 것.** 특히 지금 Toss/카카오/네이버 등 외부 연동이 많아서, 계약을 먼저 정리하지 않으면 존재하지 않는 엔드포인트나 필드를 추측해서 코드에 박아넣는 위험이 큼(실제로 `TossPaymentGateway` 초안이 토스페이먼츠 컨벤션을 추측해서 쓴 게 실제 설계 의도인 토스페이와 어긋났던 사례 있음, 근거: Obsidian ai-log 2026-08-21).
3. **"어떻게 고칠 건지"도 실행 전에 매번 확인받을 것.** 방향(1번)만 한 번 합의하고 끝이 아니라, 여러 파일/여러 레포에 걸친 구체적인 수정 방법(어떤 파일을, 어떤 경로/네이밍으로, 어떤 순서로 바꿀지)을 실제로 고치기 직전에 매번 말로 설명하고 컨펌받은 뒤 진행할 것. 특히 gateway/front-server처럼 이 repo 밖의 다른 레포를 건드릴 때 더 엄격하게 적용.

## 진행 상태
Obsidian `2-Areas/핸드오프-현재상태.md` 참고 — 지금 어디까지 됐고 다음에 뭘 해야 하는지는 여기서 관리.
