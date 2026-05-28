package com.sprint.mission.monew.domain.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.common.dto.SortDirection;
import com.sprint.mission.monew.domain.article.dto.ArticleResponse;
import com.sprint.mission.monew.domain.article.dto.ArticleOrderBy;
import com.sprint.mission.monew.domain.article.dto.ArticleQueryCondition;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.mapper.ArticleMapper;
import com.sprint.mission.monew.domain.article.exception.ArticleNotFoundException;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.article.repository.ArticleViewRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

  @InjectMocks ArticleService articleService;
  @Mock ArticleRepository articleRepository;
  @Mock ArticleViewRepository articleViewRepository;
  @Mock ArticleMapper articleMapper;

  UUID requestUserId;
  ArticleQueryCondition defaultCondition;

  @BeforeEach
  void setUp() {
    requestUserId = UUID.randomUUID();
    defaultCondition =
        new ArticleQueryCondition(null, null, null, null, null, ArticleOrderBy.PUBLISH_DATE,
            SortDirection.DESC, null, null, 10);
  }

  private Article makeArticle(ArticleSource source) {
    return Article.create(source, "https://example.com/" + UUID.randomUUID(), "테스트 기사 제목",
        Instant.now(), "기사 요약");
  }

  @Nested
  @DisplayName("뉴스 기사 목록 조회")
  class Search {

    @Test
    @DisplayName("결과가 없으면 빈 CursorPageResponse를 반환한다")
    void 결과가_없으면_빈_응답을_반환한다() {
      // given
      given(articleRepository.count(any(ArticleQueryCondition.class))).willReturn(0L);
      given(articleRepository.findAll(any(ArticleQueryCondition.class))).willReturn(List.of());

      // when
      CursorPageResponse<ArticleResponse> result = articleService.search(defaultCondition, requestUserId);

      // then
      assertThat(result.content()).isEmpty();
      assertThat(result.hasNext()).isFalse();
      assertThat(result.totalElements()).isZero();
      assertThat(result.nextCursor()).isNull();
      assertThat(result.nextAfter()).isNull();
    }

    @Test
    @DisplayName("limit 이하의 결과가 있으면 hasNext가 false이다")
    void limit_이하의_결과는_hasNext가_false이다() {
      // given
      Article article = makeArticle(ArticleSource.NAVER);
      ArticleResponse dto = new ArticleResponse(article.getId(), ArticleSource.NAVER, article.getSourceUrl(),
          article.getTitle(), article.getPublishDate(), article.getSummary(), 0, 0, false);

      given(articleRepository.count(any(ArticleQueryCondition.class))).willReturn(1L);
      given(articleRepository.findAll(any(ArticleQueryCondition.class))).willReturn(List.of(article));
      given(articleViewRepository.findArticleIdsByArticleIdsAndUserId(any(), eq(requestUserId)))
          .willReturn(Set.of());
      given(articleMapper.toResponse(eq(article), eq(false))).willReturn(dto);

      // when
      CursorPageResponse<ArticleResponse> result = articleService.search(defaultCondition, requestUserId);

      // then
      assertThat(result.content()).hasSize(1);
      assertThat(result.hasNext()).isFalse();
      assertThat(result.nextCursor()).isNull();
    }

    @Test
    @DisplayName("limit+1개가 반환되면 hasNext가 true이고 nextCursor가 설정된다")
    void limit_초과_결과는_hasNext가_true이고_nextCursor가_설정된다() {
      // given
      int limit = 2;
      ArticleQueryCondition condition =
          new ArticleQueryCondition(null, null, null, null, null, ArticleOrderBy.PUBLISH_DATE,
              SortDirection.DESC, null, null, limit);

      Article article1 = makeArticle(ArticleSource.NAVER);
      Article article2 = makeArticle(ArticleSource.HANKYUNG);
      Article article3 = makeArticle(ArticleSource.CHOSUN); // 초과분

      ArticleResponse dto1 = new ArticleResponse(article1.getId(), ArticleSource.NAVER, article1.getSourceUrl(),
          article1.getTitle(), article1.getPublishDate(), article1.getSummary(), 0, 0, false);
      ArticleResponse dto2 = new ArticleResponse(article2.getId(), ArticleSource.HANKYUNG, article2.getSourceUrl(),
          article2.getTitle(), article2.getPublishDate(), article2.getSummary(), 0, 0, false);

      given(articleRepository.count(any(ArticleQueryCondition.class))).willReturn(3L);
      given(articleRepository.findAll(any(ArticleQueryCondition.class)))
          .willReturn(List.of(article1, article2, article3));
      given(articleViewRepository.findArticleIdsByArticleIdsAndUserId(any(), eq(requestUserId)))
          .willReturn(Set.of());
      given(articleMapper.toResponse(eq(article1), eq(false))).willReturn(dto1);
      given(articleMapper.toResponse(eq(article2), eq(false))).willReturn(dto2);
      given(articleRepository.buildCursor(eq(article2), eq(ArticleOrderBy.PUBLISH_DATE)))
          .willReturn(article2.getPublishDate().toString());

      // when
      CursorPageResponse<ArticleResponse> result = articleService.search(condition, requestUserId);

      // then
      assertThat(result.content()).hasSize(2);
      assertThat(result.hasNext()).isTrue();
      // nextCursor = article2.publishDate (마지막 페이지 요소 기준)
      assertThat(result.nextCursor()).isEqualTo(article2.getPublishDate().toString());
      // 단위 테스트에서 @CreatedDate 감사 미동작 → nextAfter는 null
      assertThat(result.nextAfter()).isNull();
    }

    @Test
    @DisplayName("요청자가 조회한 기사는 viewedByMe가 true이다")
    void 요청자가_조회한_기사는_viewedByMe가_true이다() {
      // given
      Article article = makeArticle(ArticleSource.NAVER);
      ArticleResponse dto = new ArticleResponse(article.getId(), ArticleSource.NAVER, article.getSourceUrl(),
          article.getTitle(), article.getPublishDate(), article.getSummary(), 0, 0, true);

      given(articleRepository.count(any(ArticleQueryCondition.class))).willReturn(1L);
      given(articleRepository.findAll(any(ArticleQueryCondition.class))).willReturn(List.of(article));
      given(articleViewRepository.findArticleIdsByArticleIdsAndUserId(any(), eq(requestUserId)))
          .willReturn(Set.of(article.getId()));
      given(articleMapper.toResponse(eq(article), eq(true))).willReturn(dto);

      // when
      CursorPageResponse<ArticleResponse> result = articleService.search(defaultCondition, requestUserId);

      // then
      assertThat(result.content()).hasSize(1);
      assertThat(result.content().get(0).viewedByMe()).isTrue();
    }

    @Test
    @DisplayName("viewCount 기준 정렬 시 nextCursor가 viewCount 값 문자열이다")
    void viewCount_기준_정렬_시_nextCursor가_viewCount_문자열이다() {
      // given
      int limit = 1;
      ArticleQueryCondition condition =
          new ArticleQueryCondition(null, null, null, null, null, ArticleOrderBy.VIEW_COUNT,
              SortDirection.DESC, null, null, limit);

      Article article1 = makeArticle(ArticleSource.NAVER);
      Article article2 = makeArticle(ArticleSource.HANKYUNG);
      ArticleResponse dto1 = new ArticleResponse(article1.getId(), ArticleSource.NAVER,
          article1.getSourceUrl(), article1.getTitle(), article1.getPublishDate(),
          article1.getSummary(), 0, 0, false);

      given(articleRepository.count(any(ArticleQueryCondition.class))).willReturn(2L);
      given(articleRepository.findAll(any(ArticleQueryCondition.class)))
          .willReturn(List.of(article1, article2));
      given(articleViewRepository.findArticleIdsByArticleIdsAndUserId(any(), eq(requestUserId)))
          .willReturn(Set.of());
      given(articleMapper.toResponse(eq(article1), eq(false))).willReturn(dto1);
      given(articleRepository.buildCursor(eq(article1), eq(ArticleOrderBy.VIEW_COUNT)))
          .willReturn("0");

      // when
      CursorPageResponse<ArticleResponse> result = articleService.search(condition, requestUserId);

      // then — article1.viewCount = 0 (default)
      assertThat(result.hasNext()).isTrue();
      assertThat(result.nextCursor()).isEqualTo("0");
    }

    @Test
    @DisplayName("commentCount 기준 정렬 시 nextCursor가 commentCount 값 문자열이다")
    void commentCount_기준_정렬_시_nextCursor가_commentCount_문자열이다() {
      // given
      int limit = 1;
      ArticleQueryCondition condition =
          new ArticleQueryCondition(null, null, null, null, null, ArticleOrderBy.COMMENT_COUNT,
              SortDirection.DESC, null, null, limit);

      Article article1 = makeArticle(ArticleSource.NAVER);
      Article article2 = makeArticle(ArticleSource.HANKYUNG); // 초과분
      ArticleResponse dto1 = new ArticleResponse(article1.getId(), ArticleSource.NAVER, article1.getSourceUrl(),
          article1.getTitle(), article1.getPublishDate(), article1.getSummary(), 5, 0, false);

      given(articleRepository.count(any(ArticleQueryCondition.class))).willReturn(2L);
      given(articleRepository.findAll(any(ArticleQueryCondition.class)))
          .willReturn(List.of(article1, article2));
      given(articleViewRepository.findArticleIdsByArticleIdsAndUserId(any(), eq(requestUserId)))
          .willReturn(Set.of());
      given(articleMapper.toResponse(eq(article1), eq(false))).willReturn(dto1);
      given(articleRepository.buildCursor(eq(article1), eq(ArticleOrderBy.COMMENT_COUNT)))
          .willReturn("0");

      // when
      CursorPageResponse<ArticleResponse> result = articleService.search(condition, requestUserId);

      // then
      assertThat(result.hasNext()).isTrue();
      assertThat(result.nextCursor()).isEqualTo("0"); // article1.commentCount = 0 (default)
    }
  }

  @Nested
  @DisplayName("뉴스 기사 단건 조회")
  class GetArticle {

    @Test
    @DisplayName("존재하지 않는 기사를 조회하면 ArticleNotFoundException을 던진다")
    void 존재하지_않는_기사를_조회하면_ArticleNotFoundException을_던진다() {
      // given
      UUID articleId = UUID.randomUUID();
      given(articleRepository.findById(eq(articleId))).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> articleService.getArticle(articleId, requestUserId))
          .isInstanceOf(ArticleNotFoundException.class);
    }

    @Test
    @DisplayName("소프트딜리트된 기사를 조회하면 ArticleNotFoundException을 던진다")
    void 소프트딜리트된_기사를_조회하면_ArticleNotFoundException을_던진다() {
      // given
      Article article = makeArticle(ArticleSource.NAVER);
      article.softDelete();
      given(articleRepository.findById(eq(article.getId()))).willReturn(Optional.of(article));

      // when & then
      assertThatThrownBy(() -> articleService.getArticle(article.getId(), requestUserId))
          .isInstanceOf(ArticleNotFoundException.class);
    }

    @Test
    @DisplayName("존재하는 기사를 조회하면 ArticleResponse를 반환한다")
    void 존재하는_기사를_조회하면_ArticleResponse를_반환한다() {
      // given
      Article article = makeArticle(ArticleSource.NAVER);
      ArticleResponse dto = new ArticleResponse(article.getId(), ArticleSource.NAVER,
          article.getSourceUrl(), article.getTitle(), article.getPublishDate(),
          article.getSummary(), 0, 0, false);
      given(articleRepository.findById(eq(article.getId()))).willReturn(Optional.of(article));
      given(articleViewRepository.existsByArticleIdAndUserId(eq(article.getId()), eq(requestUserId)))
          .willReturn(false);
      given(articleMapper.toResponse(eq(article), eq(false))).willReturn(dto);

      // when
      ArticleResponse result = articleService.getArticle(article.getId(), requestUserId);

      // then
      assertThat(result).isEqualTo(dto);
    }

    @Test
    @DisplayName("요청자가 조회한 기사는 viewedByMe가 true이다")
    void 요청자가_조회한_기사는_viewedByMe가_true이다() {
      // given
      Article article = makeArticle(ArticleSource.NAVER);
      ArticleResponse dto = new ArticleResponse(article.getId(), ArticleSource.NAVER,
          article.getSourceUrl(), article.getTitle(), article.getPublishDate(),
          article.getSummary(), 0, 0, true);
      given(articleRepository.findById(eq(article.getId()))).willReturn(Optional.of(article));
      given(articleViewRepository.existsByArticleIdAndUserId(eq(article.getId()), eq(requestUserId)))
          .willReturn(true);
      given(articleMapper.toResponse(eq(article), eq(true))).willReturn(dto);

      // when
      ArticleResponse result = articleService.getArticle(article.getId(), requestUserId);

      // then
      assertThat(result.viewedByMe()).isTrue();
    }
  }
}
