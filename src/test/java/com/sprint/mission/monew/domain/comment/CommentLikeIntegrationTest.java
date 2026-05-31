package com.sprint.mission.monew.domain.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.comment.entity.Comment;
import com.sprint.mission.monew.domain.comment.entity.CommentLike;
import com.sprint.mission.monew.domain.comment.repository.CommentLikeRepository;
import com.sprint.mission.monew.domain.comment.repository.CommentRepository;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class CommentLikeIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ArticleRepository articleRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private CommentLikeRepository commentLikeRepository;

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
    user = userRepository.save(
        User.create(
            "Test@naver.com", "test", "12345678"
        ));
    comment = commentRepository.save(
        Comment.create(article, user, "댓글 내용")
    );
  }

  @Nested
  @DisplayName("댓글 좋아요 등록하기")
  class Create {

    @Test
    @DisplayName("댓글 좋아요 등록 실패 - 사용자가 존재하지 않음")
    void 댓글_좋아요_등록_실패_사용자_없음() throws Exception {
      // given
      UUID notExistUserId = UUID.randomUUID();

      // when & then
      mockMvc.perform(post("/api/comments/{commentId}/comment-likes", comment.getId())
              .header("Monew-Request-User-ID", notExistUserId))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("댓글 좋아요 등록 실패 - 댓글이 존재하지 않음")
    void 댓글_좋아요_등록_실패_댓글_없음() throws Exception {
      // given
      UUID notExistCommentId = UUID.randomUUID();

      // when & then
      mockMvc.perform(post("/api/comments/{commentId}/comment-likes", notExistCommentId)
              .header("Monew-Request-User-ID", user.getId()))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("댓글 좋아요 등록 실패 - 이미 좋아요가 등록되어 있음")
    void 댓글_좋아요_등록_실패_좋아요_중복등록() throws Exception {
      // given
      // 미리 좋아요 생성
      commentLikeRepository.save(CommentLike.create(user, comment));

      // when & then
      mockMvc.perform(post("/api/comments/{commentId}/comment-likes", comment.getId())
              .header("Monew-Request-User-ID", user.getId()))
          .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("댓글 좋아요 등록 성공")
    void 댓글_좋아요_등록_성공() throws Exception {
      // given
      // comment(article, user)는 BeforeEach에서 초기화

      // when & then
      mockMvc.perform(post("/api/comments/{commentId}/comment-likes", comment.getId())
              .header("Monew-Request-User-ID", user.getId()))
          .andExpect(status().isCreated());

      // DB 검증
      boolean exists = commentLikeRepository.existsByUserIdAndCommentId(user.getId(),
          comment.getId());

      assertThat(exists).isTrue();

      // LikeCount 증가 검증
      Comment foundComment = commentRepository.findById(comment.getId()).orElseThrow();
      assertThat(foundComment.getLikeCount()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("댓글 좋아요 취소하기")
  class Cancel {

    @Test
    @DisplayName("댓글 좋아요 취소 실패 - 좋아요가 존재하지 않음")
    void 댓글_좋아요_취소_실패_좋아요_없음() throws Exception {
      // given
      UUID notExistCommentId = UUID.randomUUID();
      UUID notExistUserId = UUID.randomUUID();

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}/comment-likes", notExistCommentId)
              .header("Monew-Request-User-ID", notExistUserId))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("댓글 좋아요 취소 성공")
    void 댓글_좋아요_취소_성공() throws Exception {
      // given
      // comment(article, user)는 BeforeEach에서 초기화
      commentLikeRepository.save(CommentLike.create(user, comment));
      commentRepository.increaseLikeCount(comment.getId());

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}/comment-likes", comment.getId())
              .header("Monew-Request-User-ID", user.getId()))
          .andExpect(status().isNoContent());

      // DB 검증
      boolean exists = commentLikeRepository.existsByUserIdAndCommentId(user.getId(),
          comment.getId());
      assertThat(exists).isFalse();

      // LikeCount 감소 검증
      Comment foundComment = commentRepository.findById(comment.getId()).orElseThrow();
      assertThat(foundComment.getLikeCount()).isEqualTo(0);
    }
  }

}
