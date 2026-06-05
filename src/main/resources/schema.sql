-- =====================
-- 1. users
-- =====================
CREATE TABLE IF NOT EXISTS users
(
    id             UUID                     NOT NULL,
    email          VARCHAR(255)             NOT NULL,
    nickname       VARCHAR(255)             NOT NULL,
    password       VARCHAR(255)             NOT NULL,
    email_verified BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE,
    deleted_at     TIMESTAMP WITH TIME ZONE,
                                 PRIMARY KEY (id),
    UNIQUE (email)
    );

CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users (deleted_at)
    WHERE deleted_at IS NULL;


-- =====================
-- 2. interests
-- =====================
CREATE TABLE IF NOT EXISTS interests
(
    id               UUID                     NOT NULL,
    name             VARCHAR(50)              NOT NULL,
    subscriber_count BIGINT                   NOT NULL DEFAULT 0,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id),
    UNIQUE (name)
);

CREATE INDEX IF NOT EXISTS idx_interests_subscriber_count ON interests (subscriber_count);


-- =====================
-- 3. interest_keywords  (@ElementCollection — PK 없음)
-- =====================
CREATE TABLE IF NOT EXISTS interest_keywords
(
    id          UUID                     NOT NULL,
    interest_id UUID                     NOT NULL,
    keyword     VARCHAR(50)              NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (interest_id, keyword),
    FOREIGN KEY (interest_id) REFERENCES interests (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_interest_keywords_interest_id ON interest_keywords (interest_id);


-- =====================
-- 4. subscriptions
-- =====================
-- ADR-02: 사용자 물리 삭제 시 user_id SET NULL (구독 레코드 보존)
CREATE TABLE IF NOT EXISTS subscriptions
(
    id          UUID                     NOT NULL,
    user_id     UUID,
    interest_id UUID                     NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (user_id, interest_id),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    FOREIGN KEY (interest_id) REFERENCES interests (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscriptions (user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_interest_id ON subscriptions (interest_id);


-- =====================
-- 5. articles
-- =====================
CREATE TABLE IF NOT EXISTS articles
(
    id            UUID                     NOT NULL,
    source        VARCHAR(20)              NOT NULL CHECK (source IN ('NAVER', 'HANKYUNG', 'CHOSUN', 'YONHAP')),
    source_url    VARCHAR(2048)            NOT NULL,
    title         VARCHAR(500)             NOT NULL,
    publish_date  TIMESTAMP WITH TIME ZONE NOT NULL,
    summary       TEXT,
    comment_count INT                      NOT NULL DEFAULT 0,
    view_count    INT                      NOT NULL DEFAULT 0,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE,
    deleted_at    TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id),
    UNIQUE (source_url)
);

CREATE INDEX IF NOT EXISTS idx_articles_publish_date ON articles (publish_date);
CREATE INDEX IF NOT EXISTS idx_articles_comment_count ON articles (comment_count);
CREATE INDEX IF NOT EXISTS idx_articles_view_count ON articles (view_count);
CREATE INDEX IF NOT EXISTS idx_articles_deleted_at ON articles (deleted_at)
    WHERE deleted_at IS NULL;


-- =====================
-- 6. article_interests  (join entity — @ManyToMany 대체)
-- =====================
CREATE TABLE IF NOT EXISTS article_interests
(
    id          UUID                     NOT NULL,
    article_id  UUID                     NOT NULL,
    interest_id UUID                     NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (article_id, interest_id),
    FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE,
    FOREIGN KEY (interest_id) REFERENCES interests (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_article_interests_article_id ON article_interests (article_id);
CREATE INDEX IF NOT EXISTS idx_article_interests_interest_id ON article_interests (interest_id);


-- =====================
-- 7. article_views
-- =====================
-- ADR-02: 사용자 물리 삭제 시 user_id SET NULL (조회 이력 보존)
CREATE TABLE IF NOT EXISTS article_views
(
    id         UUID                     NOT NULL,
    user_id    UUID,
    article_id UUID                     NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (user_id, article_id),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_article_views_user_id ON article_views (user_id);
CREATE INDEX IF NOT EXISTS idx_article_views_article_id ON article_views (article_id);


-- =====================
-- 8. comments
-- =====================
-- ADR-02: 기사 삭제 시 CASCADE, 사용자 물리 삭제 시 user_id SET NULL (댓글 보존)
-- content 최대 길이: 요구사항 확정 시 조정
CREATE TABLE IF NOT EXISTS comments
(
    id         UUID                     NOT NULL,
    article_id UUID                     NOT NULL,
    user_id    UUID,
    content    VARCHAR(500)             NOT NULL,
    like_count BIGINT                      NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id),
    FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_comments_article_id ON comments (article_id);
CREATE INDEX IF NOT EXISTS idx_comments_user_id ON comments (user_id);
CREATE INDEX IF NOT EXISTS idx_comments_like_count ON comments (like_count);
CREATE INDEX IF NOT EXISTS idx_comments_deleted_at ON comments (deleted_at)
    WHERE deleted_at IS NULL;


-- =====================
-- 9. comment_likes
-- =====================
-- ADR-02: 댓글 삭제 시 CASCADE, 사용자 물리 삭제 시 user_id SET NULL
CREATE TABLE IF NOT EXISTS comment_likes
(
    id         UUID                     NOT NULL,
    user_id    UUID,
    comment_id UUID                     NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (user_id, comment_id),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    FOREIGN KEY (comment_id) REFERENCES comments (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_comment_likes_user_id ON comment_likes (user_id);
CREATE INDEX IF NOT EXISTS idx_comment_likes_comment_id ON comment_likes (comment_id);


-- =====================
-- 10. notifications
-- =====================
-- confirmed_at IS NULL → 미확인, IS NOT NULL → 확인됨 (확인 후 7일 경과 시 배치 삭제)
-- ADR-02: 사용자 물리 삭제 시 user_id SET NULL
CREATE TABLE IF NOT EXISTS notifications
(
    id            UUID                     NOT NULL,
    user_id       UUID,
    content       VARCHAR(255)             NOT NULL,
    resource_type VARCHAR(20)              NOT NULL CHECK (resource_type IN ('INTEREST', 'COMMENT')),
    resource_id   UUID                     NOT NULL,
    confirmed_at  TIMESTAMP WITH TIME ZONE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications (user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_unconfirmed ON notifications (user_id, created_at)
    WHERE confirmed_at IS NULL;

-- =====================
-- 11. email_verifications
-- =====================
CREATE TABLE IF NOT EXISTS email_verifications
(
    id         UUID                     NOT NULL,
    token      VARCHAR(255)             NOT NULL,
    user_id    UUID                     NOT NULL,
    expired_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                             PRIMARY KEY (id),
    UNIQUE (token),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_email_verifications_user_id ON email_verifications (user_id);

-- =====================
-- 12. password_reset_tokens
-- =====================
CREATE TABLE IF NOT EXISTS password_reset_tokens
(
    id         UUID                     NOT NULL,
    user_id    UUID                     NOT NULL,
    code       VARCHAR(255)             NOT NULL,
    expired_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (code),
    UNIQUE (user_id),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user_id ON password_reset_tokens (user_id);