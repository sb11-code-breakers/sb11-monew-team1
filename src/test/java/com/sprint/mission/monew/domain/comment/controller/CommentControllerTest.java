package com.sprint.mission.monew.domain.comment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.monew.domain.comment.dto.request.CommentCreateRequest;
import com.sprint.mission.monew.domain.comment.dto.response.CommentResponse;
import com.sprint.mission.monew.domain.comment.exception.CommentAccessDeniedException;
import com.sprint.mission.monew.domain.comment.exception.CommentNotFoundException;
import com.sprint.mission.monew.domain.comment.service.CommentService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommentController.class)
public class CommentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private CommentService commentService;

  private UUID articleId;
  private UUID userId;
  private UUID commentId;
  private CommentCreateRequest request;
  private CommentResponse createResponse;
  private CommentResponse updateResponse;

  @BeforeEach
  void setUp() {
    articleId = UUID.randomUUID();
    userId = UUID.randomUUID();
    commentId = UUID.randomUUID();

    request = new CommentCreateRequest(
        articleId,
        userId,
        "댓글 내용"
    );

    createResponse = new CommentResponse(
        commentId,
        articleId,
        userId,
        "댓글 작성자",
        "댓글 내용",
        0,
        false,
        Instant.now()
    );

    updateResponse = new CommentResponse(
        commentId,
        articleId,
        userId,
        "댓글 작성자",
        "수정한 댓글 내용",
        0,
        false,
        Instant.now()
    );
  }

  @Nested
  @DisplayName("댓글 등록하기")
  class Controller_Create_Comment {

    @Test
    @DisplayName("댓글 등록 실패 - 뉴스 기사 ID Null(유효성 검증, 400 에러)")
    void 댓글_등록_실패_뉴스기사ID_null() throws Exception {
      // given
      String invalidRawJson = """
          {
              "userId": "12345678-1234-1234-1234-123456789012",
              "content": "댓글 내용"
          }
          """;

      // when & then
      mockMvc.perform(post("/api/comments")
              .contentType(MediaType.APPLICATION_JSON)
              .content(invalidRawJson))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(commentService); // 유효성 검증 실패 시 CommentService가 미호출 되어야함
    }

    @Test
    @DisplayName("댓글 등록 실패 - 사용자 ID Null(유효성 검증, 400 에러)")
    void 댓글_등록_실패_사용자ID_null() throws Exception {
      // given
      String invalidRawJson = """
          {
              "articleId": "12345678-1234-1234-1234-123456789012",
              "content": "댓글 내용"
          }
          """;

      // when & then
      mockMvc.perform(post("/api/comments")
              .contentType(MediaType.APPLICATION_JSON)
              .content(invalidRawJson))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(commentService); // 유효성 검증 실패 시 CommentService가 미호출 되어야함
    }

    @Test
    @DisplayName("댓글 등록 실패 - 댓글 내용 공백(유효성 검증, 400 에러)")
    void 댓글_등록_실패_댓글내용_blank() throws Exception {
      // given
      String invalidRawJson = """
          {
              "articleId": "12345678-1234-1234-1234-123456789012",
              "userId": "12345678-1234-1234-1234-123456789012",
              "content": ""
          }
          """;

      // when & then
      mockMvc.perform(post("/api/comments")
              .contentType(MediaType.APPLICATION_JSON)
              .content(invalidRawJson))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(commentService); // 유효성 검증 실패 시 CommentService가 미호출 되어야함
    }

    @Test
    @DisplayName("댓글 등록 성공")
    void 댓글_등록_성공() throws Exception {
      // given
      given(commentService.create(any(CommentCreateRequest.class))).willReturn(createResponse);

      // when & then
      mockMvc.perform(post("/api/comments")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.content").value("댓글 내용"));
    }
  }

  @Nested
  @DisplayName("댓글 수정하기")
  class Controller_Update_Comment {

    @Test
    @DisplayName("댓글 수정 실패 - 댓글이 존재하지 않음(404 에러)")
    void 댓글_수정_실패_댓글_없음() throws Exception {
      // given
      given(commentService.update(any(), any(), any())).willThrow(
          CommentNotFoundException.withId(commentId));

      // when & then
      String rawJson = """
          {
          "content": "수정한 댓글 내용"
          }
          """;

      mockMvc.perform(patch("/api/comments/{commentId}", commentId)
              .header("Monew-Request-User-ID", userId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(rawJson))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("댓글 수정 실패 - 댓글 작성 권한 없음(403 에러)")
    void 댓글_수정_실패_권한_없음() throws Exception {
      // given
      given(commentService.update(any(), any(), any())).willThrow(
          CommentAccessDeniedException.withId(commentId));

      // when & then
      String rawJson = """
          {
          "content": "수정한 댓글 내용"
          }
          """;

      mockMvc.perform(patch("/api/comments/{commentId}", commentId)
              .header("Monew-Request-User-ID", userId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(rawJson))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("댓글 수정 실패 - 댓글 내용 공백(유효성 검증, 400 에러)")
    void 댓글_수정_실패_댓글내용_blank() throws Exception {
      // given
      String invalidRawJson = """
          {
              "content": ""
          }
          """;

      // when & then
      mockMvc.perform(patch("/api/comments/{commentId}", commentId)
              .header("Monew-Request-User-ID", userId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(invalidRawJson))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(commentService); // 유효성 검증 실패 시 CommentService가 미호출 되어야함
    }

    @Test
    @DisplayName("댓글 수정 성공")
    void 댓글_수정_성공() throws Exception {
      // given
      // response는 BeforeEach에서 초기화
      given(commentService.update(any(), any(), any())).willReturn(updateResponse);

      // when & then
      String rawJson = """
          {
          "content": "수정한 댓글 내용"
          }
          """;

      mockMvc.perform(patch("/api/comments/{commentId}", commentId)
              .header("Monew-Request-User-ID", userId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(rawJson))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").value("수정한 댓글 내용"));
    }
  }

  @Nested
  @DisplayName("댓글 논리 삭제하기")
  class Controller_SoftDelete_Comment {

    @Test
    @DisplayName("댓글 논리삭제 실패 - 댓글이 존재하지 않음(404 에러)")
    void 댓글_논리삭제_실패_댓글_없음() throws Exception {
      // given
      doThrow(CommentNotFoundException.withId(commentId)).when(commentService)
          .softDelete(commentId, userId);

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}", commentId)
              .header("Monew-Request-User-ID", userId))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("댓글 논리삭제 실패 - 권한 없음(403 에러)")
    void 댓글_논리삭제_실패_권한_없음() throws Exception {
      // given
      doThrow(CommentAccessDeniedException.withId(commentId)).when(commentService)
          .softDelete(commentId, userId);

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}", commentId)
              .header("Monew-Request-User-ID", userId))
          .andExpect(status().isForbidden());
    }


    @Test
    @DisplayName("댓글 논리 삭제 성공")
    void 댓글_논리삭제_성공() throws Exception {
      // given
      doNothing().when(commentService).softDelete(commentId, userId);

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}", commentId)
              .header("Monew-Request-User-ID", userId))
          .andExpect(status().isNoContent());
    }
  }
}
