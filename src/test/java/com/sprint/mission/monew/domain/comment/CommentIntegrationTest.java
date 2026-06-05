package com.sprint.mission.monew.domain.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.comment.entity.Comment;
import com.sprint.mission.monew.domain.comment.repository.CommentRepository;
import com.sprint.mission.monew.domain.comment.service.CommentService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class CommentIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private ArticleRepository articleRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private CommentService commentService;

  private Article article;
  private User user;
  private String content;
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
    content = "댓글 내용";
    comment = commentRepository.save(Comment.create(article, user, content));
  }

  @Nested
  @DisplayName("댓글 등록하기")
  class Create {

    @Test
    @DisplayName("댓글 등록 실패 - 뉴스 기사 ID Null(유효성 검증)")
    void 댓글_등록_실패_뉴스기사ID_null() throws Exception {
      // given
      String requestBody = """
          {
            "userId": "%s",
            "content": "%s"
          }
          """.formatted(user.getId(), content);

      // when & then
      mockMvc.perform(post("/api/comments")
              .header("Monew-Request-User-ID", user.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestBody))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("댓글 등록 실패 - 사용자 ID Null(유효성 검증)")
    void 댓글_등록_실패_사용자ID_null() throws Exception {
      // given
      String requestBody = """
          {
            "articleId": "%s",
            "content": "%s"
          }
          """.formatted(article.getId(), content);

      // when & then
      mockMvc.perform(post("/api/comments")
              .header("Monew-Request-User-ID", user.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestBody))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("댓글 등록 실패 - 댓글 내용 공백(유효성 검증)")
    void 댓글_등록_실패_댓글내용_blank() throws Exception {
      // given
      String requestBody = """
          {
            "articleId": "%s",
            "userId": "%s",
            "content": ""
          }
          """.formatted(article.getId(), user.getId());

      // when & then
      mockMvc.perform(post("/api/comments")
              .header("Monew-Request-User-ID", user.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestBody))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("댓글 등록 성공")
    void 댓글_등록_성공() throws Exception {
      // given
      String requestBody = """
          {
            "articleId": "%s",
            "userId": "%s",
            "content": "%s"
          }
          """.formatted(article.getId(), user.getId(), content);

      // when & then
      mockMvc.perform(post("/api/comments")
              .header("Monew-Request-User-ID", user.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestBody))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.content").value(content));
    }
  }

  @Nested
  @DisplayName("댓글 수정하기")
  class Update {

    @Test
    @DisplayName("댓글 수정 실패 - 댓글이 존재하지 않음")
    void 댓글_수정_실패_댓글_없음() throws Exception {
      // given
      String requestBody = """
          {
            "content": "수정한 댓글 내용"
          }
          """;

      // when & then
      mockMvc.perform(patch("/api/comments/{commentId}", UUID.randomUUID())
              .header("Monew-Request-User-ID", user.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestBody))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("댓글 수정 실패 - 댓글 작성 권한 없음")
    void 댓글_수정_실패_권한_없음() throws Exception {
      // given
      // comment는 BeforeEach에서 초기화
      String requestBody = """
          {
            "content": "수정한 댓글 내용"
          }
          """;

      // when & then
      mockMvc.perform(patch("/api/comments/{commentId}", comment.getId())
              .header("Monew-Request-User-ID", UUID.randomUUID())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestBody))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("댓글 수정 실패 - 수정할 댓글 내용 공백(유효성 검증)")
    void 댓글_수정_실패_수정댓글내용_blank() throws Exception {
      // given
      // comment는 BeforeEach에서 초기화
      String requestBody = """
          {
            "content": ""
          }
          """;

      // when & then
      mockMvc.perform(patch("/api/comments/{commentId}", comment.getId())
              .header("Monew-Request-User-ID", user.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestBody))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("댓글 수정 성공")
    void 댓글_수정_성공() throws Exception {
      // given
      // comment를 BeforeEach에서 초기화
      String requestBody = """
          {
            "content": "수정한 댓글 내용"
          }
          """;

      // when & then
      mockMvc.perform(patch("/api/comments/{commentId}", comment.getId())
              .header("Monew-Request-User-ID", user.getId())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestBody))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").value("수정한 댓글 내용"));
    }
  }

  @Nested
  @DisplayName("댓글 논리 삭제하기")
  class SoftDelete {

    @Test
    @DisplayName("댓글 논리삭제 실패 - 댓글이 존재하지 않음")
    void 댓글_논리삭제_실패_댓글_없음() throws Exception {
      // given
      UUID notExistCommentId = UUID.randomUUID();

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}", notExistCommentId)
              .header("Monew-Request-User-ID", user.getId()))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("댓글 논리삭제 실패 - 삭제 권한 없음")
    void 댓글_논리삭제_실패_권한_없음() throws Exception {
      // given
      UUID unauthorizedUserId = UUID.randomUUID();

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}", comment.getId())
              .header("Monew-Request-User-ID", unauthorizedUserId))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("댓글 논리삭제 성공")
    void 댓글_논리삭제_성공() throws Exception {
      // given
      // comment, user를 BeforeEach에서 초기화

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}", comment.getId())
              .header("Monew-Request-User-ID", user.getId()))
          .andExpect(status().isNoContent());

      // DB 검증
      Comment deletedComment = commentRepository.findById(comment.getId()).orElseThrow();

      assertThat(deletedComment.isDeleted()).isTrue();
      assertThat(deletedComment.getDeletedAt()).isNotNull();
    }
  }

  @Nested
  @DisplayName("댓글 물리 삭제하기")
  class HardDelete {

    @Test
    @DisplayName("댓글 물리삭제 실패 - 댓글이 존재하지 않음")
    void 댓글_물리삭제_실패_댓글_없음() throws Exception {
      // given
      UUID notExistCommentId = UUID.randomUUID();

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}/hard", notExistCommentId)
              .header("Monew-Request-User-ID", user.getId()))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("댓글 물리삭제 성공")
    void 댓글_물리삭제_성공() throws Exception {
      // given
      // comment, user를 BeforeEach에서 초기화

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}/hard", comment.getId())
              .header("Monew-Request-User-ID", user.getId()))
          .andExpect(status().isNoContent());

      // DB 검증
      assertThat(commentRepository.findById(comment.getId())).isEmpty();
    }
  }

  @Nested
  @DisplayName("댓글 목록 조회하기")
  class Find {

    @Test
    @DisplayName("댓글 목록 조회 실패 - orderBy Null")
    void 댓글_목록조회_실패_orderBy_Null() throws Exception {
      // given
      // 유효성 검증 실패 시 데이터 불필요

      // when & then
      mockMvc.perform(get("/api/comments")
              .param("articleId", article.getId().toString())
              .param("direction", "DESC")
              .param("limit", "5")
              .header("Monew-Request-User-ID", user.getId()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("댓글 목록 조회 실패 - direction Null")
    void 댓글_목록조회_실패_direction_Null() throws Exception {
      // given
      // 유효성 검증 실패 시 데이터 불필요

      // when & then
      mockMvc.perform(get("/api/comments")
              .param("articleId", article.getId().toString())
              .param("orderBy", "CREATED_AT")
              .param("limit", "5")
              .header("Monew-Request-User-ID", user.getId()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("댓글 목록 조회 실패 - limit가 Null")
    void 댓글_목록조회_실패_limit_Null() throws Exception {
      // given
      // 유효성 검증 실패 시 데이터 불필요

      // when & then
      mockMvc.perform(get("/api/comments")
              .param("articleId", article.getId().toString())
              .param("orderBy", "CREATED_AT")
              .param("direction", "DESC")
              .header("Monew-Request-User-ID", user.getId()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("댓글 목록 조회 실패 - limit는 최소 1")
    void 댓글_목록조회_실패_limit_Min_One() throws Exception {
      // given
      // 유효성 검증 실패 시 데이터 불필요

      // when & then
      mockMvc.perform(get("/api/comments")
              .param("articleId", article.getId().toString())
              .param("orderBy", "CREATED_AT")
              .param("direction", "DESC")
              .param("limit", "0")
              .header("Monew-Request-User-ID", user.getId()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("댓글 목록 조회 실패 - LIKE_COUNT가 숫자가 아님")
    void 댓글_목록조회_실패_LIKE_COUNT가_숫자아님() throws Exception {
      // given
      // 유효성 검증 실패 시 서비스 호출되지 않음

      // when & then
      mockMvc.perform(
              get("/api/comments")
                  .param("articleId", article.getId().toString())
                  .param("orderBy", "LIKE_COUNT")
                  .param("direction", "DESC")
                  .param("cursor", "notNumber")
                  .param("limit", "0")
                  .header("Monew-Request-User-ID", user.getId().toString()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("댓글 목록 조회 성공 - 등록순")
    void 댓글_목록조회_성공_등록순() throws Exception {
      // given
      // comment도 commentRepository에 save() 되어있음, content.length(), size는 3이 나와야 함
      Comment firstComment = commentRepository.save(Comment.create(article, user, "첫 번째 댓글"));
      Thread.sleep(1000);

      Comment secondComment = commentRepository.save(Comment.create(article, user, "두 번째 댓글"));

      // when & then
      mockMvc.perform(get("/api/comments")
              .param("articleId", article.getId().toString())
              .param("orderBy", "CREATED_AT")
              .param("direction", "DESC")
              .param("limit", "5")
              .header("Monew-Request-User-ID", user.getId()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content.length()").value(3))
          .andExpect(jsonPath("$.size").value(3))
          .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("댓글 목록 조회 성공 - 등록순 cursor 적용")
    void 댓글_목록조회_성공_등록순_cursor() throws Exception {
      // given — comment의 createdAt을 cursor로 사용하면 그 이전 댓글은 없음
      // when & then
      mockMvc.perform(get("/api/comments")
              .param("articleId", article.getId().toString())
              .param("orderBy", "CREATED_AT")
              .param("direction", "DESC")
              .param("cursor", comment.getCreatedAt().toString())
              .param("after", comment.getCreatedAt().toString())
              .param("limit", "5")
              .header("Monew-Request-User-ID", user.getId()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @DisplayName("댓글 목록 조회 성공 - 좋아요순")
    void 댓글_목록조회_성공_좋아요순() throws Exception {
      // given
      Comment firstComment = commentRepository.save(Comment.create(article, user, "첫 번째 댓글"));
      commentRepository.increaseLikeCount(firstComment.getId());
      commentRepository.increaseLikeCount(firstComment.getId());
      Thread.sleep(1000);

      Comment secondComment = commentRepository.save(Comment.create(article, user, "두 번째 댓글"));
      commentRepository.increaseLikeCount(secondComment.getId());

      // when & then
      mockMvc.perform(get("/api/comments")
              .param("articleId", article.getId().toString())
              .param("orderBy", "LIKE_COUNT")
              .param("direction", "DESC")
              .param("cursor", "1")
              .param("after", secondComment.getCreatedAt().toString())
              .param("limit", "5")
              .header("Monew-Request-User-ID", user.getId()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content.length()").value(1))
          .andExpect(jsonPath("$.hasNext").value(false));
    }
  }
}
