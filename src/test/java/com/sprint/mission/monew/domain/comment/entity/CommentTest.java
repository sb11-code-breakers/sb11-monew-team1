package com.sprint.mission.monew.domain.comment.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.user.entity.User;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class CommentTest {

  private Article article;
  private User user;
  private String content;
  private String newContent;
  private Comment comment;

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

    newContent = "수정한 댓글 내용";
  }

  @Nested
  @DisplayName("댓글 등록하기")
  class CreateComment {

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

  @Nested
  @DisplayName("댓글 수정하기")
  class UpdateComment {

    @Test
    @DisplayName("받아온 작성자가 댓글 작성자가 아닐 경우")
    void 댓글_작성자가_아닌지_확인() {
      // given
      // comment는 BeforeEach에서 초기화
      UUID anotherUserId = UUID.randomUUID();

      // when
      boolean result = comment.isOwner(anotherUserId);

      // then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("받아온 작성자는 댓글 작성자가 맞는지 테스트")
    void 댓글_작성자인지_확인() {
      // given
      // user, comment는 BeforeEach에서 초기화
      UUID userId = user.getId();

      // when
      boolean result = comment.isOwner(userId);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("댓글 수정")
    void 댓글_수정() {
      // given
      // setUp()의 comment 초기화

      // when
      comment.updateContent(newContent);

      // then
      assertThat(comment.getContent()).isEqualTo(newContent);
    }
  }

  @Nested
  @DisplayName("댓글 논리 삭제하기")
  class SoftDeleteComment {

    @Test
    @DisplayName("댓글 논리 삭제 재호출 시 deletedAt 그대로 유지")
    void 댓글_논리삭제_재호출_deletedAt_유지() {
      // given
      comment.softDelete();
      Instant firstDeletedAt = comment.getDeletedAt();

      // when
      comment.softDelete();

      // then
      assertThat(comment.isDeleted()).isTrue();
      assertThat(comment.getDeletedAt()).isEqualTo(firstDeletedAt);
    }

    @Test
    @DisplayName("댓글 논리 삭제")
    void 댓글_논리삭제() {
      // given
      // setUp()의 comment 초기화

      // when
      comment.softDelete();

      // then
      assertThat(comment.isDeleted()).isTrue();
      assertThat(comment.getDeletedAt()).isNotNull();
    }
  }
}
