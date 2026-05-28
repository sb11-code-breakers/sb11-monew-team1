package com.sprint.mission.monew.domain.article.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.domain.article.dto.ArticleResponse;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.exception.ArticleNotFoundException;
import com.sprint.mission.monew.domain.article.service.ArticleService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ArticleController.class)
class ArticleControllerTest {

  @Autowired MockMvc mockMvc;
  @MockitoBean ArticleService articleService;

  private static final String URL = "/api/articles";
  private static final String USER_ID_HEADER = "Monew-Request-User-ID";

  @Nested
  @DisplayName("GET /api/articles — 뉴스 기사 목록 조회")
  class Search {

    @Test
    @DisplayName("Monew-Request-User-ID 헤더가 없으면 400을 반환한다")
    void Monew_Request_User_ID_헤더가_없으면_400을_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(
              get(URL)
                  .param("orderBy", "publishDate")
                  .param("direction", "DESC")
                  .param("limit", "10"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("orderBy가 없으면 400을 반환한다")
    void orderBy가_없으면_400을_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(
              get(URL)
                  .header(USER_ID_HEADER, UUID.randomUUID())
                  .param("direction", "DESC")
                  .param("limit", "10"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("direction이 없으면 400을 반환한다")
    void direction이_없으면_400을_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(
              get(URL)
                  .header(USER_ID_HEADER, UUID.randomUUID())
                  .param("orderBy", "publishDate")
                  .param("limit", "10"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("limit이 0이면 400을 반환한다")
    void limit이_0이면_400을_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(
              get(URL)
                  .header(USER_ID_HEADER, UUID.randomUUID())
                  .param("orderBy", "publishDate")
                  .param("direction", "DESC")
                  .param("limit", "0"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("지원하지 않는 orderBy 값이면 400을 반환한다")
    void 지원하지_않는_orderBy_값이면_400을_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(
              get(URL)
                  .header(USER_ID_HEADER, UUID.randomUUID())
                  .param("orderBy", "invalid")
                  .param("direction", "DESC")
                  .param("limit", "10"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("정상 요청이면 200과 CursorPageResponse를 반환한다")
    void 정상_요청이면_200과_CursorPageResponse를_반환한다() throws Exception {
      // given
      CursorPageResponse<ArticleResponse> response =
          CursorPageResponse.of(List.of(), null, null, false, 0, 0L);
      given(articleService.search(any(), any(UUID.class))).willReturn(response);

      // when & then
      mockMvc
          .perform(
              get(URL)
                  .header(USER_ID_HEADER, UUID.randomUUID())
                  .param("orderBy", "publishDate")
                  .param("direction", "DESC")
                  .param("limit", "10"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.hasNext").value(false))
          .andExpect(jsonPath("$.totalElements").value(0));
    }
  }

  @Nested
  @DisplayName("GET /api/articles/{articleId} — 뉴스 기사 단건 조회")
  class GetArticle {

    @Test
    @DisplayName("Monew-Request-User-ID 헤더가 없으면 400을 반환한다")
    void Monew_Request_User_ID_헤더가_없으면_400을_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(get(URL + "/{articleId}", UUID.randomUUID()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유효하지 않은 형식의 articleId이면 400을 반환한다")
    void 유효하지_않은_형식의_articleId이면_400을_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(get(URL + "/{articleId}", "not-a-uuid").header(USER_ID_HEADER, UUID.randomUUID()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 articleId이면 404를 반환한다")
    void 존재하지_않는_articleId이면_404를_반환한다() throws Exception {
      // given
      UUID articleId = UUID.randomUUID();
      given(articleService.getArticle(eq(articleId), any(UUID.class)))
          .willThrow(ArticleNotFoundException.withId(articleId));

      // when & then
      mockMvc
          .perform(get(URL + "/{articleId}", articleId).header(USER_ID_HEADER, UUID.randomUUID()))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("정상 요청이면 200과 ArticleResponse를 반환한다")
    void 정상_요청이면_200과_ArticleResponse를_반환한다() throws Exception {
      // given
      UUID articleId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      ArticleResponse response = new ArticleResponse(articleId, ArticleSource.NAVER,
          "https://example.com", "제목", Instant.now(), "요약", 0, 0, false);
      given(articleService.getArticle(eq(articleId), eq(userId))).willReturn(response);

      // when & then
      mockMvc
          .perform(get(URL + "/{articleId}", articleId).header(USER_ID_HEADER, userId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(articleId.toString()))
          .andExpect(jsonPath("$.title").value("제목"))
          .andExpect(jsonPath("$.source").value("NAVER"))
          .andExpect(jsonPath("$.viewedByMe").value(false));
    }
  }
}
