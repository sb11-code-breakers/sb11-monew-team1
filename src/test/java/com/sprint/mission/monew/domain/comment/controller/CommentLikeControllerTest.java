package com.sprint.mission.monew.domain.comment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.comment.dto.CommentLikeResponse;
import com.sprint.mission.monew.domain.comment.entity.Comment;
import com.sprint.mission.monew.domain.comment.exception.CommentLikeAlreadyExistsException;
import com.sprint.mission.monew.domain.comment.exception.CommentLikeNotFoundException;
import com.sprint.mission.monew.domain.comment.exception.CommentNotFoundException;
import com.sprint.mission.monew.domain.comment.service.CommentLikeService;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommentLikeController.class)
public class CommentLikeControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private CommentLikeService commentLikeService;

  private Article article;
  private User user;
  private Comment comment;
  private UUID articleId;
  private UUID userId;
  private UUID commentId;
  private UUID commentLikeId;
  private CommentLikeResponse response;

  @BeforeEach
  void setUp() {
    article = Article.create(
        ArticleSource.NAVER,
        "https://example.com/news/1",
        "테스트 기사 제목",
        Instant.parse("2024-01-01T00:00:00Z"),
        "기사 요약 내용"
    );
    user = User.create("Test@naver.com", "test", "12345678");
    comment = Comment.create(article, user, "댓글 내용");

    articleId = article.getId();
    userId = user.getId();
    commentId = comment.getId();
    commentLikeId = UUID.randomUUID();

    response = new CommentLikeResponse(
        commentLikeId,
        userId,
        Instant.now(),
        commentId,
        articleId,
        comment.getUser().getId(),
        comment.getUser().getNickname(),
        comment.getContent(),
        comment.getLikeCount(),
        comment.getCreatedAt()
    );
  }

  @Nested
  @DisplayName("댓글 좋아요 등록하기")
  class Controller_Create_CommentLike {

    @Test
    @DisplayName("댓글 좋아요 등록 실패 - 사용자가 존재하지 않음(404 에러)")
    void 댓글_좋아요_등록_실패_사용자_없음() throws Exception {
      // given
      given(commentLikeService.create(any(), any())).willThrow(
          UserNotFoundException.withId(userId));

      // when & then
      mockMvc.perform(post("/api/comments/{commentId}/comment-likes", commentId)
              .header("Monew-Request-User-ID", userId))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("댓글 좋아요 등록 실패 - 댓글이 존재하지 않음(404 에러)")
    void 댓글_좋아요_등록_실패_댓글_없음() throws Exception {
      // given
      given(commentLikeService.create(any(), any())).willThrow(
          CommentNotFoundException.withId(commentId));

      // when & then
      mockMvc.perform(post("/api/comments/{commentId}/comment-likes", commentId)
              .header("Monew-Request-User-ID", userId))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("댓글 좋아요 등록 실패 - 이미 좋아요가 등록되어 있음(409 에러)")
    void 댓글_좋아요_등록_실패_좋아요_중복등록() throws Exception {
      // given
      given(commentLikeService.create(any(), any())).willThrow(
          CommentLikeAlreadyExistsException.withId(userId, commentId));

      // when & then
      mockMvc.perform(post("/api/comments/{commentId}/comment-likes", commentId)
              .header("Monew-Request-User-ID", userId))
          .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("댓글 좋아요 등록 성공")
    void 댓글_좋아요_등록_성공() throws Exception {
      // given
      // userId, commentId는 BeforeEach에서 초기화
      given(commentLikeService.create(commentId, userId)).willReturn(response);

      // when & then
      mockMvc.perform(post("/api/comments/{commentId}/comment-likes", commentId)
              .header("Monew-Request-User-ID", userId))
          .andExpect(status().isCreated());
    }
  }

  @Nested
  @DisplayName("댓글 좋아요 취소하기")
  class Controller_Cancel_CommentLike {

    @Test
    @DisplayName("댓글 좋아요 취소 실패 - 좋아요가 존재하지 않음(404 에러)")
    void 댓글_좋아요_취소_실패_좋아요_없음() throws Exception {
      // given
      doThrow(CommentLikeNotFoundException.withId(userId, commentId)).when(commentLikeService)
          .cancel(commentId, userId);

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}/comment-likes", commentId)
              .header("Monew-Request-User-ID", userId))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("댓글 좋아요 취소 성공")
    void 댓글_좋아요_취소_성공() throws Exception {
      // given
      doNothing().when(commentLikeService).cancel(commentId, userId);

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}/comment-likes", commentId)
              .header("Monew-Request-User-ID", userId))
          .andExpect(status().isNoContent());
    }
  }
}
