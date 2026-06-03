-- 1단계 소규모 테스트 데이터

-- 유저 10명
INSERT INTO users (id, email, nickname, password, created_at, updated_at)
SELECT
    gen_random_uuid(),
    'user' || i || '@test.com',
    'user' || i,
    '$2a$10$abcdefghijklmnopqrstuuVnFq7YFUQIL7ITbegXiVnb1jOXyKOry',
    NOW(),
    NOW()
FROM generate_series(1, 10) AS i;

-- 관심사 5개
INSERT INTO interests (id, name, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'IT', NOW(), NOW()),
    (gen_random_uuid(), '경제', NOW(), NOW()),
    (gen_random_uuid(), '스포츠', NOW(), NOW()),
    (gen_random_uuid(), '정치', NOW(), NOW()),
    (gen_random_uuid(), '문화', NOW(), NOW());

-- 키워드 (관심사당 2개)
INSERT INTO interest_keywords (id, interest_id, keyword)
SELECT gen_random_uuid(), id, '키워드1' FROM interests WHERE
UNION ALL
SELECT gen_random_uuid(), id, '키워드2' FROM interests WHERE ;

-- 기사 20개
INSERT INTO articles (id, source, source_url, title, publish_date, summary, created_at, updated_at)
SELECT
    gen_random_uuid(),
    'NAVER',
    'https://news.naver.com/' || i,
    '테스트 기사 제목 ' || i,
    NOW(),
    '테스트 기사 요약 ' || i,
    NOW(),
    NOW()
FROM generate_series(1, 20) AS i;

-- 구독 (유저당 2개씩, 총 20개)
INSERT INTO subscriptions (id, user_id, interest_id, created_at)
SELECT
    gen_random_uuid(),
    u.id,
    i.id,
    NOW()
FROM users u
         CROSS JOIN interests i
WHERE deleted_at IS NULL
    LIMIT 20;

-- 댓글 (총 50개)
INSERT INTO comments (id, article_id, user_id, content, like_count, created_at, updated_at)
SELECT
    gen_random_uuid(),
    (SELECT id FROM articles ORDER BY RANDOM() LIMIT 1),
  (SELECT id FROM users WHERE deleted_at IS NULL ORDER BY RANDOM() LIMIT 1),
  '테스트 댓글 내용 ' || i,
  0,
  NOW(),
  NOW()
FROM generate_series(1, 50) AS i;

-- 댓글 좋아요 (총 100개)
INSERT INTO comment_likes (id, user_id, comment_id, created_at)
SELECT DISTINCT ON (u.id, c.id)
    gen_random_uuid(),
    u.id,
    c.id,
    NOW()
FROM users u
    CROSS JOIN comments c
WHERE u.deleted_at IS NULL AND c.deleted_at IS NULL
    LIMIT 100;

-- 기사 조회 (총 100개)
INSERT INTO article_views (id, user_id, article_id, created_at)
SELECT DISTINCT ON (u.id, a.id)
    gen_random_uuid(),
    u.id,
    a.id,
    NOW()
FROM users u
    CROSS JOIN articles a
WHERE u.deleted_at IS NULL AND a.deleted_at IS NULL
    LIMIT 100;