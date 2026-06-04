-- 1. 기존 데이터가 있다면 꼬이지 않게 깔끔하게 초기화 (Soft delete 고려하지 않고 강제 초기화)
TRUNCATE TABLE comment_likes CASCADE;
TRUNCATE TABLE article_views CASCADE;
TRUNCATE TABLE comments CASCADE;
TRUNCATE TABLE subscriptions CASCADE;
TRUNCATE TABLE interest_keywords CASCADE;
TRUNCATE TABLE articles CASCADE;
TRUNCATE TABLE interests CASCADE;
TRUNCATE TABLE users CASCADE;

-- 2. 유저 50명 (고정 bbbbbbbb UUID)
INSERT INTO users (id, email, nickname, password, created_at, updated_at)
SELECT
    CAST('bbbbbbbb-0000-0000-0000-' || LPAD(i::text, 12, '0') AS UUID),
    'user' || i || '@test.com',
    'user' || i,
    '$2a$10$abcdefghijklmnopqrstuuVnFq7YFUQIL7ITbegXiVnb1jOXyKOry',
    NOW(),
    NOW()
FROM generate_series(1, 50) AS i;

-- 3. 관심사 10개 (고정 dddddddd UUID)
INSERT INTO interests (id, name, created_at, updated_at)
VALUES
    ('dddddddd-0000-0000-0000-000000000001', 'IT', NOW(), NOW()),
    ('dddddddd-0000-0000-0000-000000000002', '경제', NOW(), NOW()),
    ('dddddddd-0000-0000-0000-000000000003', '스포츠', NOW(), NOW()),
    ('dddddddd-0000-0000-0000-000000000004', '정치', NOW(), NOW()),
    ('dddddddd-0000-0000-0000-000000000005', '문화', NOW(), NOW()),
    ('dddddddd-0000-0000-0000-000000000006', '과학', NOW(), NOW()),
    ('dddddddd-0000-0000-0000-000000000007', '건강', NOW(), NOW()),
    ('dddddddd-0000-0000-0000-000000000008', '교육', NOW(), NOW()),
    ('dddddddd-0000-0000-0000-000000000009', '환경', NOW(), NOW()),
    ('dddddddd-0000-0000-0000-000000000010', '국제', NOW(), NOW());

-- 4. 키워드 (관심사당 2개)
INSERT INTO interest_keywords (id, interest_id, keyword, created_at)
SELECT gen_random_uuid(), id, '키워드1', NOW() FROM interests
UNION ALL
SELECT gen_random_uuid(), id, '키워드2', NOW() FROM interests;

-- 5. 기사 100개 (고정 aaaaaaaa UUID)
INSERT INTO articles (id, source, source_url, title, publish_date, summary, created_at, updated_at)
SELECT
    CAST('aaaaaaaa-0000-0000-0000-' || LPAD(i::text, 12, '0') AS UUID),
    'NAVER',
    'https://news.naver.com/' || i,
    '테스트 기사 제목 ' || i,
    NOW(),
    '테스트 기사 요약 ' || i,
    NOW(),
    NOW()
FROM generate_series(1, 100) AS i;

-- 6. 구독 (유저당 다채롭게 5개씩, 총 250개 매핑)
INSERT INTO subscriptions (id, user_id, interest_id, created_at)
SELECT
    gen_random_uuid(),
    user_id,
    interest_id,
    NOW()
FROM (
         SELECT
             u.id AS user_id,
             i.id AS interest_id,
             ROW_NUMBER() OVER (PARTITION BY u.id ORDER BY (id_num + i_num) % 10) AS rn
         FROM (
                  SELECT id, ROW_NUMBER() OVER (ORDER BY id) as id_num FROM users
              ) u
                  CROSS JOIN (
             SELECT id, ROW_NUMBER() OVER (ORDER BY id) as i_num FROM interests
         ) i
     ) sub
WHERE rn <= 5;

-- 7. 댓글 (총 500개, 기사당 5개씩 규칙적으로 골고루 분산, 고정 cccccccc UUID)
INSERT INTO comments (id, article_id, user_id, content, like_count, created_at, updated_at)
SELECT
    CAST('cccccccc-0000-0000-0000-' || LPAD(i::text, 12, '0') AS UUID),
    CAST('aaaaaaaa-0000-0000-0000-' || LPAD(((i % 100) + 1)::text, 12, '0') AS UUID),
    CAST('bbbbbbbb-0000-0000-0000-' || LPAD(((i % 50) + 1)::text, 12, '0') AS UUID),
    '테스트 댓글 내용 ' || i,
    0,
    NOW(),
    NOW()
FROM generate_series(1, 2500) AS i;  -- 500 → 2500

-- 8. 댓글 좋아요 (총 1000개 골고루 분산)
INSERT INTO comment_likes (id, user_id, comment_id, created_at)
SELECT DISTINCT ON (u.id, c.id)
    gen_random_uuid(), u.id, c.id, NOW()
FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY id) as rn FROM users) u
    CROSS JOIN (SELECT id, ROW_NUMBER() OVER (ORDER BY id) as rn FROM comments) c
WHERE (u.rn + c.rn) % 5 = 0
    LIMIT 1000;

-- 9. 기사 조회 (총 1000개 골고루 분산)
INSERT INTO article_views (id, user_id, article_id, created_at)
SELECT DISTINCT ON (u.id, a.id)
    gen_random_uuid(), u.id, a.id, NOW()
FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY id) as rn FROM users) u
    CROSS JOIN (SELECT id, ROW_NUMBER() OVER (ORDER BY id) as rn FROM articles) a
WHERE (u.rn + a.rn) % 5 = 0
    LIMIT 1000;