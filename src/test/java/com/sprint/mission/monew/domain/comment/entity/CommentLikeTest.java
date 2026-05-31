package com.sprint.mission.monew.domain.comment.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.user.entity.User;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class CommentLikeTest {

  private Article article;
  private User user;
  private String content;
  private Comment comment;
  private CommentLike commentLike;

  @BeforeEach
  void setUp() {
    article = Article.create(
        ArticleSource.NAVER,
        "https://example.com/news/1",
        "테스트 기사 제목",
        Instant.parse("2024-01-01T00:00:00Z"),
        "기사 요약 내용"
    );

    user = User.create(
        "Test@naver.com", "test", "12345678"
    );
    content = "댓글 내용";

    comment = Comment.create(article, user, content);

    commentLike = CommentLike.create(user, comment);
  }

  @Nested
  @DisplayName("댓글 좋아요 등록하기")
  class CreateCommentLike {

    @Test
    @DisplayName("댓글 좋아요 등록")
    void 댓글_좋아요_등록() {
      // given
      // comment(article, user, content)는 BeforeEach에서 초기화

      // when
      // commentLike는 BeforeEach에서 초기화

      // then
      assertThat(commentLike.getUser()).isEqualTo(user);
      assertThat(commentLike.getComment()).isEqualTo(comment);
    }

  }
}
