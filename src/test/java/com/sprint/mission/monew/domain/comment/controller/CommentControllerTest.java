package com.sprint.mission.monew.domain.comment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.monew.domain.comment.dto.request.CommentCreateRequest;
import com.sprint.mission.monew.domain.comment.dto.response.CommentResponse;
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
  private CommentResponse response;

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

    response = new CommentResponse(
        commentId,
        articleId,
        userId,
        "댓글 작성자",
        "댓글 내용",
        0,
        false,
        Instant.now()
    );
  }

  @Nested
  @DisplayName("댓글 등록하기")
  class CreateComment {

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
    }

    @Test
    @DisplayName("댓글 등록 성공")
    void 댓글_등록_성공() throws Exception {
      // given
      given(commentService.create(any(CommentCreateRequest.class))).willReturn(response);

      // when & then
      mockMvc.perform(post("/api/comments")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.content").value("댓글 내용"));
    }
  }
}
