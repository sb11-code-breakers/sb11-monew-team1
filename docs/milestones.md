# 모뉴(MoNew) 마일스톤 및 이슈 목록

> GitHub Milestone 및 Issue 등록 기준. 이슈 번호는 등록 순서 기준이며 실제 GitHub 번호와 일치해야 함.

---

## 마일스톤 구조

| Phase   | 명칭       | 기간               | Due Date |
|---------|----------|------------------|----------|
| Phase 0 | 사전기간     | ~05/22           | 05/22    |
| Phase 1 | 1차 스프린트  | 05/22~05/29      | 05/29    |
| Phase 2 | 2차 스프린트  | 06/01~06/05 오전   | 06/05    |
| Phase 3 | 중간 발표 준비 | 06/05 오후 ~ 06/07 | 06/07    |
| Phase 4 | 중간 발표    | 06/08            | 06/08    |
| Phase 5 | 3차 스프린트  | 06/09~06/12      | 06/12    |
| Phase 6 | 최종 발표 준비 | 06/13~06/15      | 06/15    |
| Phase 7 | 최종 발표    | 06/16            | 06/16    |

---

## 프리픽스 규칙

| 프리픽스         | 용도                                         |
|--------------|--------------------------------------------|
| `[FEAT]`     | API 구현, 기능 개발, 공통 인프라 코드                   |
| `[FIX]`      | 버그 수정                                      |
| `[DEPLOY]`   | CI/CD 파이프라인, Docker, ECR, ECS, Secrets     |
| `[BATCH]`    | 배치 잡                                       |
| `[TEST]`     | 테스트, 커버리지, 검증                              |
| `[REFACTOR]` | 리팩토링, 교체, 성능 개선                            |
| `[DOCS]`     | README 생성/수정                               |
| `[CHORE]`    | 계획서, 협업 규칙, 프로젝트 관리, 프로젝트 설정, DB 연동, 설정 파일 |
| `[ADR]`      | 아키텍처 결정                                    |
| `[DONE]`     | 완료 마커                                      |

---

## Phase 0 — 사전기간 (~05/22)

> 순서: 계획/문서 → 레포 셋업 → README → 공통 인프라 코드 → ADR

| #   | 제목                                                                 | 담당                |
|-----|--------------------------------------------------------------------|-------------------|
| #1  | `[DOCS] 프로젝트 계획서 작성`                                               | 팀장                |
| #2  | `[CHORE] GitHub Projects 칸반 보드 및 Milestone 설정`                     | 팀장                |
| #3  | `[DOCS] 협업 규칙 문서 작성 (conventions.md, ADR 목록, 마일스톤 목록)`             | 팀장                |
| #4  | `[CHORE] 브랜치 전략 수립 및 보호 규칙 설정`                                     | 팀장                |
| #5  | `[CHORE] .github, .gitignore 초기 설정`                                | 팀장                |
| #6  | `[CHORE] Spring Boot 프로젝트 초기화`                                     | 팀장                |
| #7  | `[DOCS] LLM 최적화 설정`                                                | 팀장                |
| #13 | `[CHORE] .coderabbit.yaml CodeRabbit 컨벤션 설정`                       | 팀장                |
| #14 | `[CHORE] monew-bot 계정 생성 및 Discord 웹훅 / API Secrets 등록`            | 팀장                |
| #16 | `[DOCS] README 커버리지 배지 연동`                                         | 팀장                |
| #17 | `[FEAT] 공통 에러 응답 ErrorResponse 및 GlobalExceptionHandler 구현` ⚠️ 블락커 | 팀장                |
| #18 | `[FEAT] 커스텀 예외 클래스 구현`                                             | 팀장                |
| #19 | `[FEAT] MDC 로깅 인터셉터 구현 (요청 ID + IP)`                               | 팀장                |
| #20 | `[FEAT] 커서 페이지네이션 공통 유틸 CursorPageResponse<T> 구현`                  | 팀장                |
| #21 | `[ADR] ADR-01 테스트 환경 DB 전략 결정`                                     | 전체 — 기한 **05/22** |

---

## Phase 1 — 1차 스프린트 (05/22~05/29)

| #   | 제목                                                                         | 담당                |
|-----|----------------------------------------------------------------------------|-------------------|
| #22 | `[ADR] ADR-02 User 삭제 cascade 처리 범위 결정`                                    | 전체 — 기한 **05/22** |
| #23 | `[ADR] ADR-03 기사 조회수 중복 제거 기준 결정`                                          | 전체 — 기한 **05/22** |
| #24 | `[ADR] ADR-04 뉴스 수집 기존 기사 처리 방식 결정`                                        | 전체 — 기한 **05/22** |
| #25 | `[ADR] ADR-05 알림 생성 트리거 인터페이스 결정`                                          | 전체 — 기한 **05/22** |
| #26 | `[ADR] ADR-06 QueryDSL 커서 페이지네이션 패턴 표준화 결정`                                | 전체 — 기한 **05/22** |
| #27 | `[CHORE] ERD 작성 및 schema.sql 생성 (ADR-02, ADR-03 반영)`                       | 팀장 — 기한 **05/22** |
| #28 | `[FEAT] 회원가입 API 구현 (POST /api/users)`                                     | 최우준               |
| #29 | `[FEAT] 로그인 API 구현 (POST /api/auth/login)`                                 | 최우준               |
| #30 | `[FEAT] 닉네임 수정 API 구현 (PATCH /api/users/{userId})`                         | 최우준               |
| #31 | `[FEAT] 논리 삭제 및 물리 삭제 스케줄러 구현 (DELETE /api/users/{userId})`                | 최우준               |
| #32 | `[FEAT] 물리 삭제 API 구현 (DELETE /api/users/{userId}/hard)`                    | 최우준               |
| #33 | `[FEAT] 관심사 등록 API 구현 (POST /api/interests)`                               | 김호현               |
| #34 | `[FEAT] 관심사 목록 조회 API 구현 (GET /api/interests)`                             | 김호현               |
| #35 | `[FEAT] 관심사 키워드 수정 API 구현 (PATCH /api/interests/{interestId})`             | 김호현               |
| #36 | `[FEAT] 관심사 물리 삭제 API 구현 (DELETE /api/interests/{interestId})`             | 김호현               |
| #37 | `[FEAT] 구독 API 구현 (POST /api/interests/{interestId}/subscriptions)`        | 김호현               |
| #38 | `[FEAT] 구독 취소 API 구현 (DELETE /api/interests/{interestId}/subscriptions)`   | 김호현               |
| #39 | `[FEAT] 뉴스 기사 목록 조회 API 구현 (GET /api/articles)`                            | 안준영               |
| #40 | `[FEAT] 뉴스 기사 단건 조회 API 구현 (GET /api/articles/{articleId})`                | 안준영               |
| #41 | `[FEAT] 출처 목록 조회 API 구현 (GET /api/articles/sources)`                       | 안준영               |
| #42 | `[FEAT] 기사 조회수 등록 API 구현 (POST /api/articles/{articleId}/article-views)`   | 안준영               |
| #43 | `[FEAT] 뉴스 기사 논리 삭제 API 구현 (DELETE /api/articles/{articleId})`             | 안준영               |
| #44 | `[FEAT] 뉴스 기사 물리 삭제 API 구현 (DELETE /api/articles/{articleId}/hard)`        | 안준영               |
| #45 | `[BATCH] 뉴스 수집 배치 구현 (매 시간, Naver API + RSS 4곳)`                           | 안준영               |
| #46 | `[FEAT] 댓글 등록 API 구현 (POST /api/comments)`                                 | 김명근               |
| #47 | `[FEAT] 댓글 목록 조회 API 구현 (GET /api/comments)`                               | 김명근               |
| #48 | `[FEAT] 댓글 수정 API 구현 (PATCH /api/comments/{commentId})`                    | 김명근               |
| #49 | `[FEAT] 댓글 논리 삭제 API 구현 (DELETE /api/comments/{commentId})`                | 김명근               |
| #50 | `[FEAT] 댓글 물리 삭제 API 구현 (DELETE /api/comments/{commentId}/hard)`           | 김명근               |
| #51 | `[FEAT] 댓글 좋아요 API 구현 (POST /api/comments/{commentId}/comment-likes)`      | 김명근               |
| #52 | `[FEAT] 댓글 좋아요 취소 API 구현 (DELETE /api/comments/{commentId}/comment-likes)` | 김명근               |
| #53 | `[FEAT] 미확인 알림 목록 조회 API 구현 (GET /api/notifications)`                      | 엄주혁               |
| #54 | `[FEAT] 알림 단건 확인 API 구현 (PATCH /api/notifications/{notificationId})`       | 엄주혁               |
| #55 | `[FEAT] 알림 전체 확인 API 구현 (PATCH /api/notifications)`                        | 엄주혁               |
| #56 | `[FEAT] 알림 생성 로직 구현 (기사 등록 / 댓글 좋아요 이벤트)`                                  | 엄주혁               |
| #57 | `[BATCH] 알림 자동 삭제 배치 구현 (매일, 확인 후 1주일 경과)`                                 | 엄주혁               |
| #58 | `[FEAT] 활동 내역 조회 API 구현 (GET /api/user-activities/{userId})`               | 노정빈               |
| #59 | `[ADR] ADR-07 구독자 조회 인터페이스 결정`                                             | 전체 — 기한 **05/29** |
| #8  | `[DEPLOY] GitHub Actions CI 워크플로우 구성 (테스트 + 커버리지 80% 게이트)`                 | 팀장                |
| #9  | `[DEPLOY] GitHub Actions CD 워크플로우 구성 (ECS 자동 배포)`                          | 팀장                |
| #10 | `[DEPLOY] Docker 이미지 빌드 및 ECR 푸시 설정`                                       | 팀장                |
| #11 | `[DEPLOY] AWS ECS + ECR 환경 구성`                                             | 팀장                |
| #12 | `[DEPLOY] 환경변수 / Secret 관리 (GitHub Secrets)`                               | 팀장                |
| #15 | `[DEPLOY] PR 자동 리뷰 워크플로우 구성 (pr-review.yml + claude_review.py)`            | 팀장                |

---

## Phase 2 — 2차 스프린트 (06/01~06/05 오전)

| #   | 제목                                                      | 담당                |
|-----|---------------------------------------------------------|-------------------|
| #60 | `[ADR] ADR-08 MongoDB UserActivity 업데이트 연결 방식 결정`       | 전체 — 기한 **06/01** |
| #61 | `[ADR] ADR-09 S3 백업 파일 포맷 및 경로 구조 결정`                   | 전체 — 기한 **06/01** |
| #62 | `[CHORE] MongoDB 연동 설정 (이기종 DB 구성)`                     | 팀장                |
| #63 | `[FEAT] UserActivity 역정규화 모델 설계 및 컬렉션 정의`               | 노정빈               |
| #64 | `[FEAT] 활동 발생 시 MongoDB 문서 사전 업데이트 훅 연결`                | 노정빈               |
| #65 | `[REFACTOR] PostgreSQL JOIN 쿼리 → MongoDB 단순 조회 교체`      | 노정빈               |
| #66 | `[CHORE] AWS S3 연동 설정`                                  | 팀장                |
| #67 | `[BATCH] 날짜 단위 뉴스 기사 S3 백업 배치 구현`                       | 안준영               |
| #68 | `[FEAT] 유실 데이터 복구 API 구현 (GET /api/articles/restore)`   | 안준영               |
| #69 | `[BATCH] 뉴스 수집 / 알림 삭제 / 백업 배치 Spring Batch Job 마이그레이션` | 김명근               |
| #70 | `[FEAT] Spring Actuator 커스텀 메트릭 정의`                     | 엄주혁               |
| #71 | `[BATCH] 날짜별 로그 파일 S3 업로드 배치 구현`                        | 팀장                |

---

## Phase 3 — 중간 발표 준비 (06/05 오후 ~ 06/07)

> 이슈 없음 (마일스톤만)

---

## Phase 4 — 중간 발표 (06/08)

> 이슈 없음 (마일스톤만)

---

## Phase 5 — 3차 스프린트 (06/09~06/12)

| #   | 제목                                   | 담당 |
|-----|--------------------------------------|----|
| #72 | `[TEST] 전체 통합 테스트 및 버그 수정`           | 전체 |
| #73 | `[TEST] 테스트 커버리지 80% 이상 달성 (JaCoCo)` | 전체 |
| #74 | `[TEST] FE 연동 최종 확인`                 | 전체 |
| #75 | `[TEST] API 스펙 일치 여부 검증 (Swagger)`   | 전체 |
| #76 | `[REFACTOR] 성능 이슈 개선`                | 전체 |
| #77 | `[DOCS] README 최종 정리 (커버리지 배지 포함)`   | 팀장 |

---

## Phase 6 — 최종 발표 준비 (06/13~06/15)

> 이슈 없음 (마일스톤만)

---

## Phase 7 — 최종 발표 (06/16)

| #   | 제목             | 담당 |
|-----|----------------|----|
| #78 | `[DONE] 최종 머지` | 팀장 |
