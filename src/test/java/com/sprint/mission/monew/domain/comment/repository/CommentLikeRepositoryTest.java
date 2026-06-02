package com.sprint.mission.monew.domain.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.common.config.JpaConfig;
import com.sprint.mission.monew.common.config.QuerydslConfig;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.comment.entity.Comment;
import com.sprint.mission.monew.domain.comment.entity.CommentLike;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
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
@Import({JpaConfig.class, QuerydslConfig.class})
public class CommentLikeRepositoryTest {

  @Autowired
  private CommentLikeRepository commentLikeRepository;

  @Autowired
  private ArticleRepository articleRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private CommentRepository commentRepository;


  private Article article;
  private User user;
  private Comment comment;

  @BeforeEach
  void setUp() {
    commentLikeRepository.deleteAll();
    commentRepository.deleteAll();
    userRepository.deleteAll();
    articleRepository.deleteAll();

    article = articleRepository.save(Article.create(
        ArticleSource.NAVER,
        "https://example.com/news/1",
        "테스트 기사 제목",
        Instant.parse("2024-01-01T00:00:00Z"),
        "기사 요약 내용"
    ));
    user = userRepository.save(User.create("test@naver.com", "test", "12345678"));
    comment = commentRepository.save(Comment.create(article, user, "댓글 내용"));
  }

  @Nested
  @DisplayName("save() 테스트")
  class Save {

    @Test
    @DisplayName("save() 테스트")
    void 댓글좋아요_저장_성공() {
      // given
      CommentLike like = CommentLike.create(user, comment);

      // when
      CommentLike saved = commentLikeRepository.save(like);

      // then
      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getUser().getId()).isEqualTo(user.getId());
      assertThat(saved.getComment().getId()).isEqualTo(comment.getId());
    }
  }

  @Nested
  @DisplayName("사용자ID와 댓글ID가 존재하는 행이 있는지 조회")
  class ExistsByUserIdAndCommentId {

    @Test
    @DisplayName("사용자ID와 댓글ID가 존재하는 행 조회 실패 - 존재하지 않음")
    void 사용자ID와_댓글ID_존재_조회_실패_행_없음() {
      // given
      // user, comment를 BeforeEach에서 초기화

      // when
      boolean result = commentLikeRepository.existsByUserIdAndCommentId(user.getId(),
          comment.getId());

      // then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("사용자ID와 댓글ID가 존재하는 행 조회 성공")
    void 사용자ID와_댓글ID_존재_조회_성공() {
      // given
      // user, comment를 BeforeEach에서 초기화
      commentLikeRepository.save(CommentLike.create(user, comment));

      // when
      boolean result = commentLikeRepository.existsByUserIdAndCommentId(user.getId(),
          comment.getId());

      // then
      assertThat(result).isTrue();
    }
  }

  @Nested
  @DisplayName("사용자ID와 댓글ID가 일치하는 행 삭제")
  class DeleteByUserIdAndCommentId {

    @Test
    @DisplayName("사용자ID와 댓글ID가 일치하는 행 삭제 성공")
    void 사용자ID와_댓글ID_행_삭제_성공() {
      // given
      commentLikeRepository.save(CommentLike.create(user, comment));

      // when
      int deletedCount = commentLikeRepository.deleteByUserIdAndCommentId(user.getId(),
          comment.getId());

      // then
      assertThat(deletedCount).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("유저가 좋아요한 댓글 목록 ID 조회하기")
  class findLikedCommentIds {

    @Test
    @DisplayName("유저가 좋아요한 댓글 목록 ID 조회 성공")
    void 유저가_좋아요한_댓글ID_목록조회_성공() {
      // given
      User otherUser = userRepository.save(User.create("test2@naver.com", "test2", "12345678"));
      Comment firstComment = commentRepository.save(Comment.create(article, user, "첫 번째 댓글"));
      Comment secondComment = commentRepository.save(Comment.create(article, user, "두 번째 댓글"));
      Comment thirdComment = commentRepository.save(Comment.create(article, user, "세 번째 댓글"));

      commentLikeRepository.save(CommentLike.create(user, firstComment));
      commentLikeRepository.save(CommentLike.create(user, secondComment));

      commentLikeRepository.save(CommentLike.create(otherUser, thirdComment));

      // when
      Set<UUID> result = commentLikeRepository.findLikedCommentIds(user.getId(),
          List.of(firstComment.getId(), secondComment.getId(), thirdComment.getId()));

      // then
      assertThat(result).hasSize(2)
          .containsExactlyInAnyOrder(firstComment.getId(), secondComment.getId());

    }
  }

}
