-- 1단계 소규모 테스트 데이터

-- 유저 10명
-- 유저 10명 (고정 UUID)
CREATE EXTENSION IF NOT EXISTS pgcrypto;
BEGIN;
INSERT INTO users (id, email, nickname, password, created_at, updated_at)
VALUES ('aaaaaaaa-0000-0000-0000-000000000001', 'user1@test.com', 'user1',
        '$2a$10$abcdefghijklmnopqrstuuVnFq7YFUQIL7ITbegXiVnb1jOXyKOry', NOW(), NOW()),
       ('aaaaaaaa-0000-0000-0000-000000000002', 'user2@test.com', 'user2',
        '$2a$10$abcdefghijklmnopqrstuuVnFq7YFUQIL7ITbegXiVnb1jOXyKOry', NOW(), NOW()),
       ('aaaaaaaa-0000-0000-0000-000000000003', 'user3@test.com', 'user3',
        '$2a$10$abcdefghijklmnopqrstuuVnFq7YFUQIL7ITbegXiVnb1jOXyKOry', NOW(), NOW()),
       ('aaaaaaaa-0000-0000-0000-000000000004', 'user4@test.com', 'user4',
        '$2a$10$abcdefghijklmnopqrstuuVnFq7YFUQIL7ITbegXiVnb1jOXyKOry', NOW(), NOW()),
       ('aaaaaaaa-0000-0000-0000-000000000005', 'user5@test.com', 'user5',
        '$2a$10$abcdefghijklmnopqrstuuVnFq7YFUQIL7ITbegXiVnb1jOXyKOry', NOW(), NOW()),
       ('aaaaaaaa-0000-0000-0000-000000000006', 'user6@test.com', 'user6',
        '$2a$10$abcdefghijklmnopqrstuuVnFq7YFUQIL7ITbegXiVnb1jOXyKOry', NOW(), NOW()),
       ('aaaaaaaa-0000-0000-0000-000000000007', 'user7@test.com', 'user7',
        '$2a$10$abcdefghijklmnopqrstuuVnFq7YFUQIL7ITbegXiVnb1jOXyKOry', NOW(), NOW()),
       ('aaaaaaaa-0000-0000-0000-000000000008', 'user8@test.com', 'user8',
        '$2a$10$abcdefghijklmnopqrstuuVnFq7YFUQIL7ITbegXiVnb1jOXyKOry', NOW(), NOW()),
       ('aaaaaaaa-0000-0000-0000-000000000009', 'user9@test.com', 'user9',
        '$2a$10$abcdefghijklmnopqrstuuVnFq7YFUQIL7ITbegXiVnb1jOXyKOry', NOW(), NOW()),
       ('aaaaaaaa-0000-0000-0000-000000000010', 'user10@test.com', 'user10',
        '$2a$10$abcdefghijklmnopqrstuuVnFq7YFUQIL7ITbegXiVnb1jOXyKOry', NOW(), NOW());

-- 관심사 5개
INSERT INTO interests (id, name, created_at, updated_at)
VALUES (gen_random_uuid(), 'IT', NOW(), NOW()),
       (gen_random_uuid(), '경제', NOW(), NOW()),
       (gen_random_uuid(), '스포츠', NOW(), NOW()),
       (gen_random_uuid(), '정치', NOW(), NOW()),
       (gen_random_uuid(), '문화', NOW(), NOW());

-- 키워드 (관심사당 2개)
-- 키워드 (관심사당 2개)
INSERT INTO interest_keywords (id, interest_id, keyword, created_at)
SELECT gen_random_uuid(), id, '키워드1', NOW()
FROM interests
UNION ALL
SELECT gen_random_uuid(), id, '키워드2', NOW()
FROM interests;

-- 기사 20개
INSERT INTO articles (id, source, source_url, title, publish_date, summary, created_at, updated_at)
SELECT gen_random_uuid(),
       'NAVER',
       'https://news.naver.com/' || i,
       '테스트 기사 제목 ' || i,
       NOW(),
       '테스트 기사 요약 ' || i,
       NOW(),
       NOW()
FROM generate_series(1, 20) AS i;

-- 구독 (유저당 2개씩, 총 20개)
-- 구독 (유저당 2개씩, 총 20개)
INSERT INTO subscriptions (id, user_id, interest_id, created_at)
SELECT gen_random_uuid(),
       user_id,
       interest_id,
       NOW()
FROM (SELECT u.id AS      user_id,
             i.id AS      interest_id,
             ROW_NUMBER() OVER (PARTITION BY u.id ORDER BY i.id) AS rn
      FROM users u
               CROSS JOIN interests i
      WHERE u.deleted_at IS NULL) sub
WHERE rn <= 2;

-- 댓글 (총 50개)
-- 댓글 (총 50개)
INSERT INTO comments (id, article_id, user_id, content, like_count, created_at, updated_at)
SELECT gen_random_uuid(),
       (SELECT id
        FROM articles
        ORDER BY RANDOM()
           LIMIT 1 ),
  (
    SELECT id
    FROM users
    WHERE deleted_at IS NULL
    ORDER BY RANDOM()
    LIMIT 1
  ),
  '테스트 댓글 내용 ' || i,
  0,
  NOW(),
  NOW()
FROM generate_series(1, 50) AS i;

-- 댓글 좋아요 (총 100개)
INSERT INTO comment_likes (id, user_id, comment_id, created_at)
SELECT DISTINCT
ON (u.id, c.id)
    gen_random_uuid(),
    u.id,
    c.id,
    NOW()
FROM users u
    CROSS JOIN comments c
WHERE u.deleted_at IS NULL
  AND c.deleted_at IS NULL
    LIMIT 100;

-- 기사 조회 (총 100개)
INSERT INTO article_views (id, user_id, article_id, created_at)
SELECT DISTINCT
ON (u.id, a.id)
    gen_random_uuid(),
    u.id,
    a.id,
    NOW()
FROM users u
    CROSS JOIN articles a
WHERE u.deleted_at IS NULL
  AND a.deleted_at IS NULL
    LIMIT 100;
COMMIT;