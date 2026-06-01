package com.sprint.mission.monew.domain.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.mission.monew.common.config.JpaConfig;
import com.sprint.mission.monew.common.config.QuerydslConfig;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.comment.entity.Comment;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, QuerydslConfig.class})
public class CommentRepositoryTest {

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private ArticleRepository articleRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TestEntityManager testEntityManager;

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

  @Nested
  @DisplayName("findTop10RecentCommentsByUserId() 테스트")
  class FindTop10RecentCommentsByUserId {

    @Test
    @DisplayName("존재하지 않는 userId로 조회하면 빈 리스트를 반환한다")
    void 존재하지_않는_userId로_조회하면_빈_리스트를_반환한다() {
      // given
      UUID nonExistentUserId = UUID.randomUUID();

      // when
      List<Comment> result = commentRepository
          .findTop10RecentCommentsByUserId(nonExistentUserId, PageRequest.of(0, 10));

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("댓글이 없는 userId로 조회하면 빈 리스트를 반환한다")
    void 댓글이_없는_userId로_조회하면_빈_리스트를_반환한다() {
      // given
      // 댓글 없이 user만 있음

      // when
      List<Comment> result = commentRepository
          .findTop10RecentCommentsByUserId(user.getId(),PageRequest.of(0, 10));

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("삭제된 댓글은 조회되지 않는다")
    void 삭제된_댓글은_조회되지_않는다() {
      // given
      Comment savedComment = commentRepository.save(comment);
      savedComment.softDelete();
      commentRepository.save(savedComment);

      // when
      List<Comment> result = commentRepository
          .findTop10RecentCommentsByUserId(user.getId(),PageRequest.of(0, 10));

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("삭제된 기사의 댓글은 조회되지 않는다")
    void 삭제된_기사의_댓글은_조회되지_않는다() {
      // given
      commentRepository.save(comment);
      article.softDelete();
      articleRepository.save(article);

      // when
      List<Comment> result = commentRepository
          .findTop10RecentCommentsByUserId(user.getId(),PageRequest.of(0, 10));

      // then
      assertThat(result).isEmpty();
    }
    @Test
    @DisplayName("댓글이 있으면 최근 10건을 반환한다")
    void 댓글이_있으면_최근_10건을_반환한다() {
      // given
      for (int i = 0; i < 15; i++) {
        commentRepository.save(Comment.create(article, user, "댓글 " + i));
      }

      // when
      List<Comment> result = commentRepository
          .findTop10RecentCommentsByUserId(user.getId(),PageRequest.of(0, 10));

      // then
      assertThat(result).hasSize(10);
    }
  }



  @Nested
  @DisplayName("increaseLikeCount() 테스트")
  class IncreaseLikeCount {

    @Test
    @DisplayName("댓글 좋아요 +1 증가 성공")
    void 댓글_좋아요_1_증가_성공() {
      // given
      Comment savedComment = commentRepository.save(comment);
      Comment before = commentRepository.findById(savedComment.getId()).orElseThrow();
      assertThat(before.getLikeCount()).isEqualTo(0);

      // when
      commentRepository.increaseLikeCount(comment.getId());

      testEntityManager.flush();
      testEntityManager.clear();

      // then
      Comment after = commentRepository.findById(savedComment.getId()).orElseThrow();
      assertThat(after.getLikeCount()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("decreaseLikeCount() 테스트")
  class DecreaseLikeCount {

    @Test
    @DisplayName("댓글 좋아요 취소 성공")
    void 댓글_좋아요_취소_성공() {
      // given
      Comment savedComment = commentRepository.save(comment);

      commentRepository.findById(savedComment.getId()).orElseThrow();
      commentRepository.increaseLikeCount(savedComment.getId());
      testEntityManager.flush();
      testEntityManager.clear();
      Comment before = commentRepository.findById(savedComment.getId()).orElseThrow();
      assertThat(before.getLikeCount()).isEqualTo(1);

      // when
      commentRepository.decreaseLikeCount(savedComment.getId());

      testEntityManager.flush();
      testEntityManager.clear();

      // then
      Comment after = commentRepository.findById(savedComment.getId()).orElseThrow();
      assertThat(after.getLikeCount()).isEqualTo(0);
    }
  }
}
