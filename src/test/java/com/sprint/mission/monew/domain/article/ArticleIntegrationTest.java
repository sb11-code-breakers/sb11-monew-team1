package com.sprint.mission.monew.domain.article;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
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
}
