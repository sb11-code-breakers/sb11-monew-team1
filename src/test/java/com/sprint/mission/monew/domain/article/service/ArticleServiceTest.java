package com.sprint.mission.monew.domain.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.common.dto.SortDirection;
import com.sprint.mission.monew.domain.article.dto.ArticleResponse;
import com.sprint.mission.monew.domain.article.dto.ArticleOrderBy;
import com.sprint.mission.monew.domain.article.dto.ArticleQueryCondition;
import com.sprint.mission.monew.domain.article.dto.ArticleViewResponse;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.entity.ArticleView;
import com.sprint.mission.monew.domain.article.mapper.ArticleMapper;
import com.sprint.mission.monew.domain.article.mapper.ArticleViewMapper;
import com.sprint.mission.monew.domain.article.exception.ArticleNotFoundException;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.article.repository.ArticleViewRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
  @Mock ArticleViewMapper articleViewMapper;

  UUID requestUserId;
  ArticleQueryCondition defaultCondition;

  @BeforeEach
  void setUp() {
    requestUserId = UUID.randomUUID();
    defaultCondition =
        new ArticleQueryCondition(null, null, null, null, null, ArticleOrderBy.PUBLISH_DATE,
            SortDirection.DESC, null, null, null, 10);
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
      CursorPageResponse<ArticleResponse> expected = CursorPageResponse.of(
          List.of(), null, null, null, false, 0, 0L);
      given(articleRepository.search(any(), eq(requestUserId))).willReturn(expected);

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
      ArticleResponse dto = new ArticleResponse(UUID.randomUUID(), ArticleSource.NAVER,
          "https://example.com", "제목", Instant.now(), "요약", 0, 0, false);
      CursorPageResponse<ArticleResponse> expected = CursorPageResponse.of(
          List.of(dto), null, null, null, false, 1, 1L);
      given(articleRepository.search(any(), eq(requestUserId))).willReturn(expected);

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
              SortDirection.DESC, null, null, null, limit);

      Instant publishDate = Instant.now();
      ArticleResponse dto1 = new ArticleResponse(UUID.randomUUID(), ArticleSource.NAVER,
          "https://1.com", "제목1", publishDate, "요약1", 0, 0, false);
      ArticleResponse dto2 = new ArticleResponse(UUID.randomUUID(), ArticleSource.HANKYUNG,
          "https://2.com", "제목2", publishDate, "요약2", 0, 0, false);

      CursorPageResponse<ArticleResponse> expected = CursorPageResponse.of(
          List.of(dto1, dto2), publishDate.toString(), publishDate, dto2.id(), true, 2, 3L);
      given(articleRepository.search(any(), eq(requestUserId))).willReturn(expected);

      // when
      CursorPageResponse<ArticleResponse> result = articleService.search(condition, requestUserId);

      // then
      assertThat(result.content()).hasSize(2);
      assertThat(result.hasNext()).isTrue();
      assertThat(result.nextCursor()).isEqualTo(publishDate.toString());
    }

    @Test
    @DisplayName("요청자가 조회한 기사는 viewedByMe가 true이다")
    void 요청자가_조회한_기사는_viewedByMe가_true이다() {
      // given
      ArticleResponse dto = new ArticleResponse(UUID.randomUUID(), ArticleSource.NAVER,
          "https://example.com", "제목", Instant.now(), "요약", 0, 0, true);
      CursorPageResponse<ArticleResponse> expected = CursorPageResponse.of(
          List.of(dto), null, null, null, false, 1, 1L);
      given(articleRepository.search(any(), eq(requestUserId))).willReturn(expected);

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
      ArticleQueryCondition condition =
          new ArticleQueryCondition(null, null, null, null, null, ArticleOrderBy.VIEW_COUNT,
              SortDirection.DESC, null, null, null, 1);
      CursorPageResponse<ArticleResponse> expected = CursorPageResponse.of(
          List.of(), "0", null, null, true, 1, 2L);
      given(articleRepository.search(any(), eq(requestUserId))).willReturn(expected);

      // when
      CursorPageResponse<ArticleResponse> result = articleService.search(condition, requestUserId);

      // then
      assertThat(result.hasNext()).isTrue();
      assertThat(result.nextCursor()).isEqualTo("0");
    }

    @Test
    @DisplayName("commentCount 기준 정렬 시 nextCursor가 commentCount 값 문자열이다")
    void commentCount_기준_정렬_시_nextCursor가_commentCount_문자열이다() {
      // given
      ArticleQueryCondition condition =
          new ArticleQueryCondition(null, null, null, null, null, ArticleOrderBy.COMMENT_COUNT,
              SortDirection.DESC, null, null, null, 1);
      CursorPageResponse<ArticleResponse> expected = CursorPageResponse.of(
          List.of(), "0", null, null, true, 1, 2L);
      given(articleRepository.search(any(), eq(requestUserId))).willReturn(expected);

      // when
      CursorPageResponse<ArticleResponse> result = articleService.search(condition, requestUserId);

      // then
      assertThat(result.hasNext()).isTrue();
      assertThat(result.nextCursor()).isEqualTo("0");
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

  @Nested
  @DisplayName("기사 조회수 등록")
  class RegisterView {

    @Test
    @DisplayName("존재하지 않는 기사이면 ArticleNotFoundException을 던진다")
    void 존재하지_않는_기사이면_ArticleNotFoundException을_던진다() {
      // given
      UUID articleId = UUID.randomUUID();
      given(articleRepository.findById(eq(articleId))).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> articleService.registerView(articleId, requestUserId))
          .isInstanceOf(ArticleNotFoundException.class);
    }

    @Test
    @DisplayName("소프트딜리트된 기사이면 ArticleNotFoundException을 던진다")
    void 소프트딜리트된_기사이면_ArticleNotFoundException을_던진다() {
      // given
      Article article = makeArticle(ArticleSource.NAVER);
      article.softDelete();
      given(articleRepository.findById(eq(article.getId()))).willReturn(Optional.of(article));

      // when & then
      assertThatThrownBy(() -> articleService.registerView(article.getId(), requestUserId))
          .isInstanceOf(ArticleNotFoundException.class);
    }

    @Test
    @DisplayName("이미 조회한 기사이면 기존 ArticleView를 반환하고 viewCount를 증가시키지 않는다")
    void 이미_조회한_기사이면_기존_뷰를_반환하고_viewCount를_증가시키지_않는다() {
      // given
      Article article = makeArticle(ArticleSource.NAVER);
      ArticleView existingView = ArticleView.create(requestUserId, article);
      ArticleViewResponse dto = new ArticleViewResponse(
          existingView.getId(), requestUserId, existingView.getCreatedAt(),
          article.getId(), ArticleSource.NAVER, article.getSourceUrl(),
          article.getTitle(), article.getPublishDate(), article.getSummary(),
          0, 0);

      given(articleRepository.findById(eq(article.getId()))).willReturn(Optional.of(article));
      given(articleViewRepository.findByArticleIdAndUserId(eq(article.getId()), eq(requestUserId)))
          .willReturn(Optional.of(existingView));
      given(articleViewMapper.toResponse(eq(existingView), anyInt())).willReturn(dto);

      // when
      ArticleViewResponse result = articleService.registerView(article.getId(), requestUserId);

      // then
      assertThat(result).isEqualTo(dto);
      verify(articleViewRepository, never()).save(any());
      verify(articleRepository, never()).increaseViewCount(any());
    }

    @Test
    @DisplayName("처음 조회하는 기사이면 ArticleView를 저장하고 viewCount를 증가시킨다")
    void 처음_조회하는_기사이면_뷰를_저장하고_viewCount를_증가시킨다() {
      // given
      Article article = makeArticle(ArticleSource.NAVER);
      ArticleView newView = ArticleView.create(requestUserId, article);
      ArticleViewResponse dto = new ArticleViewResponse(
          newView.getId(), requestUserId, newView.getCreatedAt(),
          article.getId(), ArticleSource.NAVER, article.getSourceUrl(),
          article.getTitle(), article.getPublishDate(), article.getSummary(),
          0, 1);

      given(articleRepository.findById(eq(article.getId()))).willReturn(Optional.of(article));
      given(articleViewRepository.findByArticleIdAndUserId(eq(article.getId()), eq(requestUserId)))
          .willReturn(Optional.empty());
      given(articleViewRepository.save(any(ArticleView.class))).willReturn(newView);
      given(articleViewMapper.toResponse(eq(newView), anyInt())).willReturn(dto);

      int viewCountBefore = article.getViewCount();

      // when
      ArticleViewResponse result = articleService.registerView(article.getId(), requestUserId);

      // then
      assertThat(result).isEqualTo(dto);
      verify(articleViewRepository).save(any(ArticleView.class));
      verify(articleRepository).increaseViewCount(eq(article.getId()));
      verify(articleViewMapper).toResponse(eq(newView), eq(viewCountBefore + 1));
    }
  }

  @Nested
  @DisplayName("뉴스 기사 논리 삭제")
  class SoftDelete {

    @Test
    @DisplayName("존재하지 않는 기사이면 ArticleNotFoundException을 던진다")
    void 존재하지_않는_기사이면_ArticleNotFoundException을_던진다() {
      // given
      UUID articleId = UUID.randomUUID();
      given(articleRepository.findById(eq(articleId))).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> articleService.softDelete(articleId))
          .isInstanceOf(ArticleNotFoundException.class);
    }

    @Test
    @DisplayName("이미 논리 삭제된 기사이면 ArticleNotFoundException을 던진다")
    void 이미_논리_삭제된_기사이면_ArticleNotFoundException을_던진다() {
      // given
      Article article = makeArticle(ArticleSource.NAVER);
      article.softDelete();
      given(articleRepository.findById(eq(article.getId()))).willReturn(Optional.of(article));

      // when & then
      assertThatThrownBy(() -> articleService.softDelete(article.getId()))
          .isInstanceOf(ArticleNotFoundException.class);
    }

    @Test
    @DisplayName("존재하는 기사이면 deletedAt을 설정한다")
    void 존재하는_기사이면_deletedAt을_설정한다() {
      // given
      Article article = makeArticle(ArticleSource.NAVER);
      given(articleRepository.findById(eq(article.getId()))).willReturn(Optional.of(article));

      // when
      articleService.softDelete(article.getId());

      // then
      assertThat(article.isDeleted()).isTrue();
    }
  }

  @Nested
  @DisplayName("뉴스 기사 물리 삭제")
  class HardDelete {

    @Test
    @DisplayName("존재하지 않는 기사이면 ArticleNotFoundException을 던진다")
    void 존재하지_않는_기사이면_ArticleNotFoundException을_던진다() {
      // given
      UUID articleId = UUID.randomUUID();
      given(articleRepository.findById(eq(articleId))).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> articleService.hardDelete(articleId))
          .isInstanceOf(ArticleNotFoundException.class);
    }

    @Test
    @DisplayName("논리 삭제된 기사도 물리 삭제할 수 있다")
    void 논리_삭제된_기사도_물리_삭제할_수_있다() {
      // given
      Article article = makeArticle(ArticleSource.NAVER);
      article.softDelete();
      given(articleRepository.findById(eq(article.getId()))).willReturn(Optional.of(article));

      // when
      articleService.hardDelete(article.getId());

      // then
      verify(articleRepository).delete(eq(article));
    }

    @Test
    @DisplayName("존재하는 기사이면 delete를 호출한다")
    void 존재하는_기사이면_delete를_호출한다() {
      // given
      Article article = makeArticle(ArticleSource.NAVER);
      given(articleRepository.findById(eq(article.getId()))).willReturn(Optional.of(article));

      // when
      articleService.hardDelete(article.getId());

      // then
      verify(articleRepository).delete(eq(article));
    }
  }
}
