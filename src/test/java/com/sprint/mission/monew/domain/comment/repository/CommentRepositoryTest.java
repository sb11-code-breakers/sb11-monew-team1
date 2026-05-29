package com.sprint.mission.monew.domain.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.mission.monew.common.config.QuerydslConfig;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.comment.entity.Comment;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QuerydslConfig.class)
public class CommentRepositoryTest {

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private ArticleRepository articleRepository;

  @Autowired
  private UserRepository userRepository;

  private Article article;
  private User user;
  private Comment comment;

  @BeforeEach
  void setUp() {
    article = articleRepository.save(
        Article.create(
            ArticleSource.NAVER,
            "https://example.com/news/1",
            "테스트 기사 제목",
            Instant.parse("2024-01-01T00:00:00Z"),
            "기사 요약 내용"
        ));
    user = userRepository.save(User.create(
        "Test@naver.com", "test", "12345678"
    ));
    comment = Comment.create(article, user, "댓글 내용");
  }

  @Nested
  @DisplayName("save() 테스트")
  class Save {

    @Test
    @DisplayName("댓글 저장 성공")
    void 댓글_저장_성공() {
      // given
      // comment는 BeforeEach에서 초기화

      // when
      Comment savedComment = commentRepository.save(comment);

      // then
      assertThat(savedComment.getId()).isNotNull();
      assertThat(savedComment.getArticle().getId()).isEqualTo(article.getId());
      assertThat(savedComment.getUser().getId()).isEqualTo(user.getId());
      assertThat(savedComment.getContent()).isEqualTo("댓글 내용");
    }
  }

  @Nested
  @DisplayName("findById() 테스트")
  class FindById {

    @Test
    @DisplayName("존재하지 않는 댓글 조회")
    void 존재하지_않는_댓글_조회() {
      // given
      UUID notSavedCommentId = UUID.randomUUID();

      // when
      Optional<Comment> foundComment = commentRepository.findById(notSavedCommentId);

      // then
      assertThat(foundComment).isEmpty();
    }

    @Test
    @DisplayName("댓글 조회 성공")
    void 댓글_조회_성공() {
      // given
      Comment savedComment = commentRepository.save(comment);

      // when
      Comment foundComment = commentRepository.findById(savedComment.getId()).orElseThrow();

      // then
      assertThat(foundComment.getId()).isEqualTo(savedComment.getId());
      assertThat(foundComment.getArticle().getId()).isEqualTo(savedComment.getArticle().getId());
      assertThat(foundComment.getUser().getId()).isEqualTo(savedComment.getUser().getId());
      assertThat(foundComment.getContent()).isEqualTo(savedComment.getContent());
    }

  }

  @Nested
  @DisplayName("delete() 테스트")
  class Delete {

    @Test
    @DisplayName("댓글 삭제 성공")
    void 댓글_삭제_성공() {
      // given
      Comment savedComment = commentRepository.save(comment);
      commentRepository.delete(savedComment);

      // when
      Optional<Comment> result = commentRepository.findById(comment.getId());

      // then
      assertThat(result).isEmpty();
    }
  }
}
