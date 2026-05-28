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

public class CommentTest {

  private Article article;
  private User user;
  private String content;
  private Comment comment;

  @BeforeEach
  void setUp() {
    article = Article.create(ArticleSource.NAVER, "https://test.com", "테스트 기사", Instant.now(), null);
    user = new User();
    content = "댓글 내용";

    comment = Comment.create(article, user, content);
  }

  @Nested
  @DisplayName("댓글 등록하기")
  class Create {

    @Test
    @DisplayName("댓글 등록")
    void 댓글_등록() {
      // given
      // setUp()의 article, user, content 초기화

      // when
      // setUp()의 comment 초기화

      // then
      assertThat(comment.getArticle()).isEqualTo(article);
      assertThat(comment.getUser()).isEqualTo(user);
      assertThat(comment.getContent()).isEqualTo(content);
    }
  }
}
