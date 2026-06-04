-- pgcrypto 확장 활성화 (gen_random_uuid() 사용을 위해)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

BEGIN;

-- 1. 깔끔하게 전체 초기화
TRUNCATE TABLE comment_likes CASCADE;
TRUNCATE TABLE article_views CASCADE;
TRUNCATE TABLE comments CASCADE;
TRUNCATE TABLE subscriptions CASCADE;
TRUNCATE TABLE interest_keywords CASCADE;
TRUNCATE TABLE articles CASCADE;
TRUNCATE TABLE interests CASCADE;
TRUNCATE TABLE users CASCADE;

-- 2. 유저 150명 생성
INSERT INTO users (id, email, nickname, password, created_at, updated_at)
SELECT CAST('bbbbbbbb-0000-0000-0000-' || LPAD(i::text, 12, '0') AS UUID),
       'user' || i || '@test.com',
       'user' || i,
       '$2a$10$abcdefghijklmnopqrstuuVnFq7YFUQIL7ITbegXiVnb1jOXyKOry',
       NOW(),
       NOW()
FROM generate_series(1, 150) AS i;

-- 3. 관심사 15개로 확장
INSERT INTO interests (id, name, created_at, updated_at)
VALUES ('dddddddd-0000-0000-0000-000000000001', 'IT', NOW(), NOW()),
       ('dddddddd-0000-0000-0000-000000000002', '경제', NOW(), NOW()),
       ('dddddddd-0000-0000-0000-000000000003', '스포츠', NOW(), NOW()),
       ('dddddddd-0000-0000-0000-000000000004', '정치', NOW(), NOW()),
       ('dddddddd-0000-0000-0000-000000000005', '문화', NOW(), NOW()),
       ('dddddddd-0000-0000-0000-000000000006', '과학', NOW(), NOW()),
       ('dddddddd-0000-0000-0000-000000000007', '건강', NOW(), NOW()),
       ('dddddddd-0000-0000-0000-000000000008', '교육', NOW(), NOW()),
       ('dddddddd-0000-0000-0000-000000000009', '환경', NOW(), NOW()),
       ('dddddddd-0000-0000-0000-000000000010', '국제', NOW(), NOW()),
       ('dddddddd-0000-0000-0000-000000000011', '게임', NOW(), NOW()),
       ('dddddddd-0000-0000-0000-000000000012', '연예', NOW(), NOW()),
       ('dddddddd-0000-0000-0000-000000000013', '생활', NOW(), NOW()),
       ('dddddddd-0000-0000-0000-000000000014', '요리', NOW(), NOW()),
       ('dddddddd-0000-0000-0000-000000000015', '여행', NOW(), NOW());

-- 4. 키워드 생성 (관심사당 2개씩)
INSERT INTO interest_keywords (id, interest_id, keyword, created_at)
SELECT gen_random_uuid(), id, '키워드1', NOW()
FROM interests
UNION ALL
SELECT gen_random_uuid(), id, '키워드2', NOW()
FROM interests;

-- 5. 기사 100개 생성
INSERT INTO articles (id, source, source_url, title, publish_date, summary, created_at, updated_at)
SELECT CAST('aaaaaaaa-0000-0000-0000-' || LPAD(i::text, 12, '0') AS UUID),
       'NAVER',
       'https://news.naver.com/' || i,
       '테스트 기사 제목 ' || i,
       NOW(),
       '테스트 기사 요약 ' || i,
       NOW(),
       NOW()
FROM generate_series(1, 100) AS i;

-- 6. 구독 정보 매핑 (유저당 10개씩, 총 1,500건)
INSERT INTO subscriptions (id, user_id, interest_id, created_at)
SELECT gen_random_uuid(),
       user_id,
       interest_id,
       NOW()
FROM (SELECT u.id AS      user_id,
             i.id AS      interest_id,
             ROW_NUMBER() OVER (PARTITION BY u.id ORDER BY (id_num + i_num) % 15) AS rn
      FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY id) as id_num FROM users) u
               CROSS JOIN (SELECT id, ROW_NUMBER() OVER (ORDER BY id) as i_num
                           FROM interests) i) sub
WHERE rn <= 10;

-- 7. 댓글 데이터 7,500개 적재
INSERT INTO comments (id, article_id, user_id, content, like_count, created_at, updated_at)
SELECT CAST('cccccccc-0000-0000-0000-' || LPAD(i::text, 12, '0') AS UUID),
       CAST('aaaaaaaa-0000-0000-0000-' || LPAD(((i % 100) + 1)::text, 12, '0') AS UUID),
       CAST('bbbbbbbb-0000-0000-0000-' || LPAD(((i % 150) + 1)::text, 12, '0') AS UUID),
       '테스트 댓글 내용 ' || i,
       0,
       NOW(),
       NOW()
FROM generate_series(1, 7500) AS i;

-- 8. 댓글 좋아요 이력 3,000개 적재
INSERT INTO comment_likes (id, user_id, comment_id, created_at)
SELECT gen_random_uuid(),
       user_id,
       comment_id,
       NOW()
FROM (SELECT u.id AS      user_id,
             c.id AS      comment_id,
             ROW_NUMBER() OVER (PARTITION BY u.id ORDER BY (u.id_num + c.id_num) % 7500) AS rn
      FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY id) as id_num FROM users) u
               CROSS JOIN (SELECT id, ROW_NUMBER() OVER (ORDER BY id) as id_num
                           FROM comments) c) sub
WHERE rn <= 20;

-- 9. 기사 조회 이력 1,500개 적재
INSERT INTO article_views (id, user_id, article_id, created_at)
SELECT gen_random_uuid(),
       user_id,
       article_id,
       NOW()
FROM (SELECT u.id AS      user_id,
             a.id AS      article_id,
             ROW_NUMBER() OVER (PARTITION BY u.id ORDER BY (u.id_num + a.id_num) % 100) AS rn
      FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY id) as id_num FROM users) u
               CROSS JOIN (SELECT id, ROW_NUMBER() OVER (ORDER BY id) as id_num
                           FROM articles) a) sub
WHERE rn <= 10;

COMMIT;