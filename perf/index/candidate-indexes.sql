-- 인덱스 A/B용 후보 인덱스.
--
-- ⚠️ 이건 "측정용으로 로컬에 적용해 효과를 확인"하는 후보다. 운영 DDL의 SoT는 src/main/resources/schema.sql.
--    효과가 확인되면 정식 마이그레이션 + 리뷰로 schema.sql에 올린다(여기서 끝이 아님).
--
-- 사용 흐름:
--   1) perf/index/explain-queries.sql 로 "현재"(인덱스 추가 전) 플랜 측정 → Sort/Seq Scan 기록
--   2) 이 파일 적용 → ANALYZE
--   3) explain-queries.sql 다시 측정 → Sort 사라지고 Index Scan 되는지, buffers 평탄해지는지 비교
--
-- 적용:
--   psql "postgresql://monew:monew@localhost:5432/monew" -f perf/index/candidate-indexes.sql

-- ── I1/I2: 키셋 정렬을 받칠 복합 인덱스 (부분 인덱스로 소프트딜리트 제외까지 흡수) ──
-- 댓글: article_id + createdAt 정렬 + id tie-break
CREATE INDEX IF NOT EXISTS idx_comments_article_created_id
  ON comments (article_id, created_at DESC, id DESC)
  WHERE deleted_at IS NULL;

-- 댓글: 좋아요순 정렬
CREATE INDEX IF NOT EXISTS idx_comments_article_like_created_id
  ON comments (article_id, like_count DESC, created_at DESC, id DESC)
  WHERE deleted_at IS NULL;

-- 기사: 발행일순 정렬 + 키셋 tie-break
CREATE INDEX IF NOT EXISTS idx_articles_publish_created_id
  ON articles (publish_date DESC, created_at DESC, id DESC)
  WHERE deleted_at IS NULL;

-- ── I3: 키워드 검색(선행 와일드카드) — trigram GIN ──
-- 앱 쿼리가 lower(col) LIKE lower(?)(QueryDSL containsIgnoreCase)이므로,
-- 평문 gin(col)이 아니라 표현식 인덱스 gin(lower(col))이어야 실제 쿼리가 인덱스를 탄다.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS idx_articles_title_lower_trgm
  ON articles USING gin (lower(title) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_articles_summary_lower_trgm
  ON articles USING gin (lower(summary) gin_trgm_ops);

-- 적용 후 통계 갱신 (안 하면 새 인덱스를 플래너가 안 고를 수 있음)
ANALYZE comments;
ANALYZE articles;

-- ── 롤백 (A/B 비교 후 원상복구하려면 주석 해제) ──
-- DROP INDEX IF EXISTS idx_comments_article_created_id;
-- DROP INDEX IF EXISTS idx_comments_article_like_created_id;
-- DROP INDEX IF EXISTS idx_articles_publish_created_id;
-- DROP INDEX IF EXISTS idx_articles_title_lower_trgm;
-- DROP INDEX IF EXISTS idx_articles_summary_lower_trgm;
-- 참고: 복합 인덱스가 커버하면 단일 idx_comments_like_count 는 I5에서 미사용으로 잡힐 수 있다(제거 후보).