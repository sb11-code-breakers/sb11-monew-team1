package com.sprint.mission.monew.domain.article;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.entity.ArticleView;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.article.repository.ArticleViewRepository;
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
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ArticleIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired ArticleRepository articleRepository;
  @Autowired ArticleViewRepository articleViewRepository;

  private static final String URL = "/api/articles";
  private static final String USER_ID_HEADER = "Monew-Request-User-ID";

  @BeforeEach
  void setUp() {
    articleRepository.deleteAll();
  }

  @Nested
  @DisplayName("GET /api/articles — 뉴스 기사 목록 조회")
  class Search {

    @Test
    @DisplayName("기사가 없으면 빈 목록을 반환한다")
    void 기사가_없으면_빈_목록을_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(
              get(URL)
                  .header(USER_ID_HEADER, UUID.randomUUID())
                  .param("orderBy", "publishDate")
                  .param("direction", "DESC")
                  .param("limit", "10"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content").isEmpty())
          .andExpect(jsonPath("$.hasNext").value(false))
          .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("저장된 기사가 응답 content에 포함된다")
    void 저장된_기사가_응답_content에_포함된다() throws Exception {
      // given
      articleRepository.save(Article.create(
          ArticleSource.NAVER,
          "https://example.com/news/1",
          "테스트 기사",
          Instant.now(),
          "기사 요약"));

      // when & then
      mockMvc
          .perform(
              get(URL)
                  .header(USER_ID_HEADER, UUID.randomUUID())
                  .param("orderBy", "publishDate")
                  .param("direction", "DESC")
                  .param("limit", "10"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content[0].title").value("테스트 기사"))
          .andExpect(jsonPath("$.content[0].source").value("NAVER"))
          .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("소프트딜리트된 기사는 응답에 포함되지 않는다")
    void 소프트딜리트된_기사는_응답에_포함되지_않는다() throws Exception {
      // given
      Article article = articleRepository.save(Article.create(
          ArticleSource.NAVER,
          "https://example.com/news/deleted",
          "삭제된 기사",
          Instant.now(),
          "요약"));
      article.softDelete();
      articleRepository.save(article);

      // when & then
      mockMvc
          .perform(
              get(URL)
                  .header(USER_ID_HEADER, UUID.randomUUID())
                  .param("orderBy", "publishDate")
                  .param("direction", "DESC")
                  .param("limit", "10"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isEmpty())
          .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("limit보다 기사가 많으면 hasNext가 true이다")
    void limit보다_기사가_많으면_hasNext가_true이다() throws Exception {
      // given
      for (int i = 0; i < 3; i++) {
        articleRepository.save(Article.create(
            ArticleSource.NAVER,
            "https://example.com/news/" + i,
            "기사 " + i,
            Instant.now(),
            "요약"));
      }

      // when & then
      mockMvc
          .perform(
              get(URL)
                  .header(USER_ID_HEADER, UUID.randomUUID())
                  .param("orderBy", "publishDate")
                  .param("direction", "DESC")
                  .param("limit", "2"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.hasNext").value(true))
          .andExpect(jsonPath("$.content.length()").value(2));
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
    @DisplayName("존재하지 않는 articleId이면 404를 반환한다")
    void 존재하지_않는_articleId이면_404를_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(get(URL + "/{articleId}", UUID.randomUUID())
              .header(USER_ID_HEADER, UUID.randomUUID()))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("소프트딜리트된 기사를 조회하면 404를 반환한다")
    void 소프트딜리트된_기사를_조회하면_404를_반환한다() throws Exception {
      // given
      Article article = articleRepository.save(Article.create(
          ArticleSource.NAVER, "https://example.com/news/deleted", "삭제된 기사",
          Instant.now(), "요약"));
      article.softDelete();
      articleRepository.save(article);

      // when & then
      mockMvc
          .perform(get(URL + "/{articleId}", article.getId())
              .header(USER_ID_HEADER, UUID.randomUUID()))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("존재하는 기사를 조회하면 200과 기사 정보를 반환한다")
    void 존재하는_기사를_조회하면_200과_기사_정보를_반환한다() throws Exception {
      // given
      Article article = articleRepository.save(Article.create(
          ArticleSource.NAVER, "https://example.com/news/1", "테스트 기사", Instant.now(), "요약"));

      // when & then
      mockMvc
          .perform(get(URL + "/{articleId}", article.getId())
              .header(USER_ID_HEADER, UUID.randomUUID()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(article.getId().toString()))
          .andExpect(jsonPath("$.title").value("테스트 기사"))
          .andExpect(jsonPath("$.source").value("NAVER"))
          .andExpect(jsonPath("$.viewedByMe").value(false));
    }

    @Test
    @DisplayName("이미 조회한 기사는 viewedByMe가 true이다")
    void 이미_조회한_기사는_viewedByMe가_true이다() throws Exception {
      // given
      Article article = articleRepository.save(Article.create(
          ArticleSource.NAVER, "https://example.com/news/viewed", "조회된 기사", Instant.now(), "요약"));
      UUID userId = UUID.randomUUID();
      articleViewRepository.save(ArticleView.create(userId, article));

      // when & then
      mockMvc
          .perform(get(URL + "/{articleId}", article.getId())
              .header(USER_ID_HEADER, userId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.viewedByMe").value(true));
    }
  }
}
