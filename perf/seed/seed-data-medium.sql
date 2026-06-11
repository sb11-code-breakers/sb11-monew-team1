-- 성능 측정용 중간 규모 시드 (유저 1만 · 기사 10만 · 댓글 50만 · 좋아요 100만)
-- 전제: 단건 INSERT 금지, set-based 대량 적재 + 끝에 ANALYZE.
--
-- ⚠️ 운영 DB가 아니라 **성능 측정 전용 DB**(perf/docker-compose.yml의 postgres)에만 실행한다.
--    운영 DB에 가짜 데이터를 넣으면 안 된다.
--
-- 실행:
--   docker compose -f perf/docker-compose.yml up -d postgres
--   psql "postgresql://monew:monew@localhost:5432/monew" -f perf/seed/seed-data-medium.sql
--
-- 재실행 주의: email/source_url이 i 기반 결정값이라 빈 DB에서 1회만 실행한다.
--   다시 채우려면 아래 정리문을 먼저 수동 실행(주석 해제):
--   TRUNCATE comment_likes, comments, article_views, articles, users RESTART IDENTITY CASCADE;

-- gen_random_uuid() 사용 (PostgreSQL 13+ 코어 내장). 구버전이면: CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ── 0. 난수 고정 (재현성) ──────────────────────────────────────
-- 재시드해도 같은 분포가 나오게 random()의 시드를 고정한다 → "인덱스 추가 전후" 같은 측정 비교가
-- 분포 변화로 오염되지 않는다. (gen_random_uuid()는 별도 RNG라 UUID는 매번 달라짐 → ID 풀은 재추출 필요.)
SELECT setseed(0.42);

-- ── 1. 유저 1만 ────────────────────────────────────────────────
-- password: 유효한 BCrypt 형식 해시 1개를 전 유저가 공유한다.
--   대량 시드에서 1만 번 BCrypt 해싱은 느리기만 하고 의미 없어, 고정 해시 재사용이 업계 표준.
--   측정 엔드포인트는 Monew-Request-User-ID 헤더로 인증해 비밀번호를 검증하지 않으므로 값 자체는 무의미(NOT NULL 충족용).
--   로그인 "성공"까지 측정할 일이 생기면 그때만 BCryptPasswordEncoder로 만든 '알려진 평문'의 해시로 교체한다.
\echo '[seed] 1/4 users 1만 적재...'
INSERT INTO users (id, email, nickname, password, email_verified, created_at)
SELECT gen_random_uuid(),
       'user' || i || '@load.test',
       'loaduser' || i,
       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- 고정 placeholder 해시 (검증 안 함)
       true,
       now()
FROM generate_series(1, 10000) AS s(i);

-- ── 2. 기사 10만 ───────────────────────────────────────────────
-- source는 CHECK(NAVER/HANKYUNG/CHOSUN/YONHAP) 통과값으로 분산. source_url은 i로 유니크.
-- publish_date를 분 단위로 흩뿌려 publishDate 정렬·깊은 커서가 의미 있게 한다.
\echo '[seed] 2/4 articles 10만 적재...'
INSERT INTO articles (id, source, source_url, title, summary, publish_date, view_count, comment_count, created_at)
SELECT gen_random_uuid(),
       (ARRAY['NAVER', 'HANKYUNG', 'CHOSUN', 'YONHAP'])[1 + (i % 4)],
       'https://load.test/article/' || i,
       'load article ' || i || ' 뉴스',
       'summary of load article ' || i,
       now() - (i || ' minutes')::interval,
       0, 0,
       now() - (i || ' minutes')::interval
FROM generate_series(1, 100000) AS s(i);

-- ── 3. 댓글 50만 ───────────────────────────────────────────────
-- 기존 유저/기사 id를 배열로 한 번만 뽑아 무작위 인덱싱(상관 서브쿼리 반복 금지 → 빠름).
-- created_at을 90일 범위로 흩뿌려 createdAt 정렬·깊은 커서가 평탄성 검증에 쓰이게 한다.
\echo '[seed] 3/4 comments 50만 적재... (수십 초 소요 가능)'
INSERT INTO comments (id, article_id, user_id, content, like_count, created_at)
SELECT gen_random_uuid(),
       a.ids[1 + floor(random() * array_length(a.ids, 1))::int],
       u.ids[1 + floor(random() * array_length(u.ids, 1))::int],
       'load comment ' || i,
       0,
       now() - (random() * interval '90 days')
FROM generate_series(1, 500000) AS s(i),
     (SELECT array_agg(id) AS ids FROM users)    AS u,
     (SELECT array_agg(id) AS ids FROM articles) AS a;

-- ── 4. 댓글 좋아요 100만 ──────────────────────────────────────
-- 랜덤 (user, comment) 페어 100만개. 표본 100만 / 가능쌍 50억이라 통계적으로 유니크 충돌이
-- ~수십~수백건 발생한다 → UNIQUE(user_id, comment_id)에 ON CONFLICT DO NOTHING으로 흡수.
-- (결과 행 수는 100만에서 충돌 수만큼 약간 모자랄 수 있으나 측정엔 무방.)
\echo '[seed] 4/4 comment_likes 100만 적재... (1~2분 소요 가능)'
INSERT INTO comment_likes (id, user_id, comment_id, created_at)
SELECT gen_random_uuid(),
       u.ids[1 + floor(random() * array_length(u.ids, 1))::int],
       c.ids[1 + floor(random() * array_length(c.ids, 1))::int],
       now() - (random() * interval '90 days')
FROM generate_series(1, 1000000) AS s(i),
     (SELECT array_agg(id) AS ids FROM users)    AS u,
     (SELECT array_agg(id) AS ids FROM comments) AS c
ON CONFLICT (user_id, comment_id) DO NOTHING;

-- ── 5. 비정규화 카운터 정합화 ───────────────────────────────
-- like_count / comment_count를 실제 연결 수와 맞춰 목록 응답이 현실적이게 한다.
\echo '[seed] 카운터 정합화 + ANALYZE...'
UPDATE comments c
SET like_count = sub.cnt
FROM (SELECT comment_id, count(*) AS cnt FROM comment_likes GROUP BY comment_id) sub
WHERE c.id = sub.comment_id;

UPDATE articles a
SET comment_count = sub.cnt
FROM (SELECT article_id, count(*) AS cnt FROM comments WHERE deleted_at IS NULL GROUP BY article_id) sub
WHERE a.id = sub.article_id;
-- view_count는 조회수 등록(W3/C4) 테스트가 채우므로 0으로 둔다(article_views 미시드).

-- ── 6. 통계 갱신 (필수) ───────────────────────────────────────
-- ANALYZE를 안 하면 시드해도 플래너가 풀스캔으로 측정될 수 있다.
ANALYZE users;
ANALYZE articles;
ANALYZE comments;
ANALYZE comment_likes;

\echo '[seed] 완료. 다음: perf/extract-ids.sh 로 ID 풀 추출.'