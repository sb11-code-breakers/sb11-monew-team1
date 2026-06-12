-- 인덱스 효율 측정 — EXPLAIN (ANALYZE, BUFFERS).
-- 도구는 k6가 아니라 PostgreSQL 실행계획. 첫 페이지 vs 깊은 페이지 플랜·buffers 비교가 핵심.
--
-- 실행:
--   psql "postgresql://monew:monew@localhost:5432/monew" -f perf/index/explain-queries.sql
--   (시드 + ANALYZE 완료 상태에서. 측정 전 통계 최신화는 아래 0단계가 수행)
--
-- 인기 기사·깊은 커서는 \gset 으로 자동 선정·추출 → 수정 없이 그대로 실행된다.
-- ⚠️ medium 시드는 댓글을 기사에 균등 분포한다 → I1 worst-case(인기 기사 수십만 댓글)가 약하다.
--    진짜 worst-case가 필요하면 아래 "(선택) 인기 기사 만들기"를 1회 실행 후 다시 돌린다.

\timing on

-- (선택) I1 worst-case: 한 기사에 댓글 20만 몰아주기. 필요할 때만 주석 해제.
-- WITH hot AS (SELECT id FROM articles WHERE deleted_at IS NULL ORDER BY comment_count DESC LIMIT 1),
--      u AS (SELECT array_agg(id) AS ids FROM users)
-- INSERT INTO comments (id, article_id, user_id, content, like_count, created_at)
-- SELECT gen_random_uuid(), (SELECT id FROM hot),
--        u.ids[1 + floor(random() * array_length(u.ids, 1))::int],
--        'hot comment ' || i, 0, now() - (random() * interval '90 days')
-- FROM generate_series(1, 200000) AS s(i), u;

-- ── 0. 통계 최신화 (안 하면 인덱스 있어도 Seq Scan으로 측정됨) ──
ANALYZE users; ANALYZE articles; ANALYZE comments; ANALYZE notifications;

-- ── 측정 대상 자동 선정 ──────────────────────────────────────
-- 가장 댓글 많은 기사 → :hotarticleid
SELECT id AS hotarticleid FROM articles WHERE deleted_at IS NULL
ORDER BY comment_count DESC LIMIT 1 \gset
\echo '>>> 대상 인기 기사 hotarticleid =' :'hotarticleid'

-- 깊은 페이지 OFFSET을 그 기사 댓글 수의 절반으로(항상 행 존재) → :cdeepoffset
SELECT greatest(0, (count(*) / 2))::int AS cdeepoffset
FROM comments WHERE article_id = :'hotarticleid' AND deleted_at IS NULL \gset
-- 깊은 커서 3총사 추출(키셋 측정용) → :ccursorcreated :ccursorid
SELECT created_at AS ccursorcreated, id AS ccursorid
FROM comments WHERE article_id = :'hotarticleid' AND deleted_at IS NULL
ORDER BY created_at DESC, id DESC OFFSET :cdeepoffset LIMIT 1 \gset
\echo '>>> 댓글 깊은 커서 offset =' :cdeepoffset

-- 기사 목록(publish_date)용 깊은 커서 → :adeepoffset :apublish :acreated :aid
SELECT greatest(0, (count(*) / 2))::int AS adeepoffset FROM articles WHERE deleted_at IS NULL \gset
SELECT publish_date AS apublish, created_at AS acreated, id AS aid
FROM articles WHERE deleted_at IS NULL
ORDER BY publish_date DESC, created_at DESC, id DESC OFFSET :adeepoffset LIMIT 1 \gset

-- 알림 기준선용 유저(미확인 알림 많은 유저, 없으면 임의 유저) → :nuserid
SELECT coalesce(
  (SELECT user_id FROM notifications WHERE confirmed_at IS NULL
   GROUP BY user_id ORDER BY count(*) DESC LIMIT 1),
  (SELECT id FROM users WHERE deleted_at IS NULL LIMIT 1)
) AS nuserid \gset

-- ════════════════════════════════════════════════════════════
-- I1 · 댓글 목록 (article_id + createdAt 정렬) — 첫 vs 깊은 페이지
-- ════════════════════════════════════════════════════════════
\echo '=== I1-A 댓글 첫 페이지 ==='
EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
SELECT c.id, c.created_at, c.like_count
FROM comments c
WHERE c.article_id = :'hotarticleid' AND c.deleted_at IS NULL
ORDER BY c.created_at DESC, c.id DESC
LIMIT 51;

\echo '=== I1-B 댓글 깊은 페이지(키셋) — Sort 노드·buffers 증가 여부가 핵심 ==='
EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
SELECT c.id, c.created_at, c.like_count
FROM comments c
WHERE c.article_id = :'hotarticleid' AND c.deleted_at IS NULL
  AND (c.created_at < :'ccursorcreated'
       OR (c.created_at = :'ccursorcreated' AND c.id < :'ccursorid'))
ORDER BY c.created_at DESC, c.id DESC
LIMIT 51;

-- ════════════════════════════════════════════════════════════
-- I2 · 기사 목록 (publish_date 정렬) — 첫 vs 깊은 페이지
-- ════════════════════════════════════════════════════════════
\echo '=== I2-A 기사 첫 페이지 ==='
EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
SELECT a.id, a.publish_date, a.created_at
FROM articles a
WHERE a.deleted_at IS NULL
ORDER BY a.publish_date DESC, a.created_at DESC, a.id DESC
LIMIT 51;

\echo '=== I2-B 기사 깊은 페이지(키셋) ==='
EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
SELECT a.id, a.publish_date, a.created_at
FROM articles a
WHERE a.deleted_at IS NULL
  AND (a.publish_date < :'apublish'
       OR (a.publish_date = :'apublish'
           AND (a.created_at < :'acreated'
                OR (a.created_at = :'acreated' AND a.id < :'aid'))))
ORDER BY a.publish_date DESC, a.created_at DESC, a.id DESC
LIMIT 51;

-- ════════════════════════════════════════════════════════════
-- I3 · 키워드 검색 (선행 와일드카드 ILIKE → B-tree 무력, Seq Scan 기대)
-- ════════════════════════════════════════════════════════════
\echo '=== I3 키워드 검색 — Seq Scan + Filter 기대(→ trigram 대상) ==='
EXPLAIN (ANALYZE, BUFFERS)
-- 정렬은 실제 호출 계약(read.js R1' = orderBy=commentCount)과 동일하게 맞춘다.
-- 술어도 실제 앱 쿼리(QueryDSL containsIgnoreCase = lower(col) LIKE lower(?))와 동일하게 맞춘다.
--   ILIKE로 적으면 gin(col gin_trgm_ops)는 타지만, 앱의 lower()+LIKE는 그 인덱스를 못 탄다
--   → 측정 결론이 어긋남. 그래서 candidate-indexes.sql도 gin(lower(col))로 둔다.
SELECT a.id FROM articles a
WHERE a.deleted_at IS NULL
  AND (lower(a.title) LIKE lower('%뉴스%') OR lower(a.summary) LIKE lower('%뉴스%'))
ORDER BY a.comment_count DESC, a.id DESC
LIMIT 51;

-- ════════════════════════════════════════════════════════════
-- I4 · 알림 목록 — 기준선(이미 정합한 모범 인덱스)
-- ════════════════════════════════════════════════════════════
\echo '=== I4 알림(기준선) — idx_notifications_unconfirmed Index Scan, Sort 없음 기대 ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT n.id, n.created_at FROM notifications n
WHERE n.user_id = :'nuserid' AND n.confirmed_at IS NULL
ORDER BY n.created_at DESC, n.id DESC
LIMIT 51;

-- ════════════════════════════════════════════════════════════
-- I5 · 인덱스 사용 통계 (미사용 색출)
-- ════════════════════════════════════════════════════════════
\echo '=== I5 인덱스 사용 통계 (idx_scan=0 = 조회 미사용 → 제거 후보) ==='
SELECT relname AS table_name, indexrelname AS index_name,
       idx_scan, idx_tup_read,
       pg_size_pretty(pg_relation_size(indexrelid)) AS size
FROM pg_stat_user_indexes
JOIN pg_index USING (indexrelid)
WHERE schemaname = 'public'
ORDER BY idx_scan ASC, pg_relation_size(indexrelid) DESC;