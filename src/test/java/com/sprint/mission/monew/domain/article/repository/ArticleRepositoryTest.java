package com.sprint.mission.monew.domain.article.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.sprint.mission.monew.batch.article.backup.dto.ArticleBackupItem;
import com.sprint.mission.monew.common.config.JpaConfig;
import com.sprint.mission.monew.common.config.QuerydslConfig;
import com.sprint.mission.monew.common.dto.SortDirection;
import com.sprint.mission.monew.domain.article.dto.ArticleOrderBy;
import com.sprint.mission.monew.domain.article.dto.ArticleQueryCondition;
import com.sprint.mission.monew.domain.article.dto.ArticleResponse;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleInterest;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.repository.InterestRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, QuerydslConfig.class})
class ArticleRepositoryTest {

  @Autowired ArticleRepository articleRepository;
  @Autowired InterestRepository interestRepository;
  @Autowired EntityManager em;

  UUID requestUserId;

  @BeforeEach
  void setUp() {
    requestUserId = UUID.randomUUID();
    articleRepository.deleteAll();
  }

  private Article saveArticle(ArticleSource source, String title) {
    return articleRepository.save(
        Article.create(source, "https://example.com/" + title, title, Instant.now(), "요약"));
  }

  private ArticleQueryCondition defaultCondition(int limit) {
    return new ArticleQueryCondition(
        null, null, null, null, null,
        ArticleOrderBy.PUBLISH_DATE, SortDirection.DESC,
        null, null, null, limit);
  }

  private static final UUID MIN_UUID = new UUID(0L, 0L);

  @Nested
  @DisplayName("findArticlesForBackup")
  class FindArticlesForBackup {

    private final Instant from = Instant.now().minusSeconds(60);
    private final Instant to = Instant.now().plusSeconds(60);

    @Test
    @DisplayName("소프트딜리트된 기사는 반환하지 않는다")
    void 소프트딜리트된_기사는_반환하지_않는다() {
      // given
      Article article = articleRepository.save(
          Article.create(ArticleSource.NAVER, "https://example.com/deleted", "삭제기사", Instant.now(), null));
      article.softDelete();
      articleRepository.save(article);

      // when
      List<ArticleBackupItem> result = articleRepository.findArticlesForBackup(
          from, to, MIN_UUID, PageRequest.of(0, 10));

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("from~to 범위 밖 기사는 반환하지 않는다")
    void 범위_밖_기사는_반환하지_않는다() {
      // given
      articleRepository.save(
          Article.create(ArticleSource.NAVER, "https://example.com/out", "범위밖기사", Instant.now(), null));

      Instant futureFrom = Instant.now().plusSeconds(3600);
      Instant futureTo = futureFrom.plusSeconds(3600);

      // when
      List<ArticleBackupItem> result = articleRepository.findArticlesForBackup(
          futureFrom, futureTo, MIN_UUID, PageRequest.of(0, 10));

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("from~to 범위 내 기사를 반환한다")
    void 범위_내_기사를_반환한다() {
      // given
      articleRepository.save(
          Article.create(ArticleSource.NAVER, "https://example.com/a1", "기사1", Instant.now(), null));
      articleRepository.save(
          Article.create(ArticleSource.NAVER, "https://example.com/a2", "기사2", Instant.now(), null));

      // when
      List<ArticleBackupItem> result = articleRepository.findArticlesForBackup(
          from, to, MIN_UUID, PageRequest.of(0, 10));

      // then
      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("lastId 커서 이후 기사만 반환한다")
    void lastId_커서_이후_기사만_반환한다() {
      // given
      articleRepository.save(
          Article.create(ArticleSource.NAVER, "https://example.com/b1", "기사A", Instant.now(), null));
      articleRepository.save(
          Article.create(ArticleSource.NAVER, "https://example.com/b2", "기사B", Instant.now(), null));

      // DB 정렬 기준(PostgreSQL UUID 사전순)으로 전체 조회 후 첫 번째 id를 커서로 사용
      List<ArticleBackupItem> all = articleRepository.findArticlesForBackup(
          from, to, MIN_UUID, PageRequest.of(0, 10));
      UUID firstId = all.get(0).id();

      // when
      List<ArticleBackupItem> result = articleRepository.findArticlesForBackup(
          from, to, firstId, PageRequest.of(0, 10));

      // then — DB 기준 첫 번째 이후 기사만 반환
      assertThat(result).hasSize(1);
      assertThat(result.get(0).id()).isEqualTo(all.get(1).id());
    }

    @Test
    @DisplayName("Pageable size 제한이 적용된다")
    void pageable_size_제한이_적용된다() {
      // given
      articleRepository.save(
          Article.create(ArticleSource.NAVER, "https://example.com/c1", "기사1", Instant.now(), null));
      articleRepository.save(
          Article.create(ArticleSource.NAVER, "https://example.com/c2", "기사2", Instant.now(), null));
      articleRepository.save(
          Article.create(ArticleSource.NAVER, "https://example.com/c3", "기사3", Instant.now(), null));

      // when
      List<ArticleBackupItem> result = articleRepository.findArticlesForBackup(
          from, to, MIN_UUID, PageRequest.of(0, 2));

      // then
      assertThat(result).hasSize(2);
    }
  }

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    @DisplayName("cursor가 없으면 저장된 모든 기사를 반환한다")
    void cursor가_없으면_저장된_모든_기사를_반환한다() {
      // given
      saveArticle(ArticleSource.NAVER, "기사1");
      saveArticle(ArticleSource.HANKYUNG, "기사2");

      // when
      List<ArticleResponse> result = articleRepository.search(defaultCondition(10), requestUserId).content();

      // then
      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("소프트딜리트된 기사는 조회 결과에서 제외된다")
    void 소프트딜리트된_기사는_조회_결과에서_제외된다() {
      // given
      Article deleted = saveArticle(ArticleSource.NAVER, "삭제될기사");
      deleted.softDelete();
      articleRepository.save(deleted);
      saveArticle(ArticleSource.HANKYUNG, "정상기사");

      // when
      List<ArticleResponse> result = articleRepository.search(defaultCondition(10), requestUserId).content();

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).title()).isEqualTo("정상기사");
    }

    @Test
    @DisplayName("limit을 초과하면 hasNext가 true이다")
    void limit_초과하면_hasNext가_true이다() {
      // given
      saveArticle(ArticleSource.NAVER, "기사1");
      saveArticle(ArticleSource.HANKYUNG, "기사2");
      saveArticle(ArticleSource.CHOSUN, "기사3");

      // when
      var response = articleRepository.search(defaultCondition(2), requestUserId);

      // then
      assertThat(response.hasNext()).isTrue();
      assertThat(response.content()).hasSize(2);
    }

    @Test
    @DisplayName("keyword로 필터링하면 제목에 keyword가 포함된 기사만 반환한다")
    void keyword로_필터링하면_제목에_keyword가_포함된_기사만_반환한다() {
      // given
      saveArticle(ArticleSource.NAVER, "인공지능 뉴스");
      saveArticle(ArticleSource.HANKYUNG, "경제 동향");

      ArticleQueryCondition condition = new ArticleQueryCondition(
          "인공지능", null, null, null, null,
          ArticleOrderBy.PUBLISH_DATE, SortDirection.DESC,
          null, null, null, 10);

      // when
      List<ArticleResponse> result = articleRepository.search(condition, requestUserId).content();

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).title()).isEqualTo("인공지능 뉴스");
    }

    @Test
    @DisplayName("sourceIn 필터링하면 해당 출처의 기사만 반환한다")
    void sourceIn_필터링하면_해당_출처의_기사만_반환한다() {
      // given
      saveArticle(ArticleSource.NAVER, "네이버 기사");
      saveArticle(ArticleSource.HANKYUNG, "한경 기사");
      saveArticle(ArticleSource.CHOSUN, "조선 기사");

      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, null, List.of(ArticleSource.NAVER, ArticleSource.HANKYUNG), null, null,
          ArticleOrderBy.PUBLISH_DATE, SortDirection.DESC,
          null, null, null, 10);

      // when
      List<ArticleResponse> result = articleRepository.search(condition, requestUserId).content();

      // then
      assertThat(result).hasSize(2);
      assertThat(result).extracting(ArticleResponse::source)
          .containsExactlyInAnyOrder(ArticleSource.NAVER, ArticleSource.HANKYUNG);
    }

    @Test
    @DisplayName("publishDateFrom이 있으면 그 이후 기사만 반환한다")
    void publishDateFrom이_있으면_그_이후_기사만_반환한다() {
      // given
      Instant t1 = Instant.parse("2024-01-01T00:00:00Z");
      Instant t2 = Instant.parse("2024-01-02T00:00:00Z");
      articleRepository.save(Article.create(ArticleSource.NAVER, "url1", "기사1", t1, null));
      articleRepository.save(Article.create(ArticleSource.NAVER, "url2", "기사2", t2, null));

      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, null, null, t2, null,
          ArticleOrderBy.PUBLISH_DATE, SortDirection.DESC,
          null, null, null, 10);

      // when
      List<ArticleResponse> result = articleRepository.search(condition, requestUserId).content();

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).title()).isEqualTo("기사2");
    }

    @Test
    @DisplayName("publishDateTo가 있으면 그 이전 기사만 반환한다")
    void publishDateTo가_있으면_그_이전_기사만_반환한다() {
      // given
      Instant t1 = Instant.parse("2024-01-01T00:00:00Z");
      Instant t2 = Instant.parse("2024-01-02T00:00:00Z");
      articleRepository.save(Article.create(ArticleSource.NAVER, "url1", "기사1", t1, null));
      articleRepository.save(Article.create(ArticleSource.NAVER, "url2", "기사2", t2, null));

      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, null, null, null, t1,
          ArticleOrderBy.PUBLISH_DATE, SortDirection.DESC,
          null, null, null, 10);

      // when
      List<ArticleResponse> result = articleRepository.search(condition, requestUserId).content();

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).title()).isEqualTo("기사1");
    }

    @Test
    @DisplayName("PUBLISH_DATE DESC cursor가 있으면 cursor 이전 기사만 반환한다")
    void publishDate_DESC_cursor가_있으면_cursor_이전_기사만_반환한다() {
      // given
      Instant t1 = Instant.parse("2024-01-01T00:00:00Z");
      Instant t2 = Instant.parse("2024-01-02T00:00:00Z");
      Instant t3 = Instant.parse("2024-01-03T00:00:00Z");
      articleRepository.save(Article.create(ArticleSource.NAVER, "url1", "기사1", t1, null));
      articleRepository.save(Article.create(ArticleSource.NAVER, "url2", "기사2", t2, null));
      Article article3 = articleRepository.save(Article.create(ArticleSource.NAVER, "url3", "기사3", t3, null));

      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, null, null, null, null,
          ArticleOrderBy.PUBLISH_DATE, SortDirection.DESC,
          t3.toString(), article3.getPublishDate(), article3.getId(), 10);

      // when
      List<ArticleResponse> result = articleRepository.search(condition, requestUserId).content();

      // then — T1, T2만 반환 (T3는 cursor와 동일하고 타이브레이크 불충족)
      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("PUBLISH_DATE ASC cursor가 있으면 cursor 이후 기사만 반환한다")
    void publishDate_ASC_cursor가_있으면_cursor_이후_기사만_반환한다() {
      // given
      Instant t1 = Instant.parse("2024-01-01T00:00:00Z");
      Instant t2 = Instant.parse("2024-01-02T00:00:00Z");
      Instant t3 = Instant.parse("2024-01-03T00:00:00Z");
      Article article1 = articleRepository.save(Article.create(ArticleSource.NAVER, "url1", "기사1", t1, null));
      articleRepository.save(Article.create(ArticleSource.NAVER, "url2", "기사2", t2, null));
      articleRepository.save(Article.create(ArticleSource.NAVER, "url3", "기사3", t3, null));

      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, null, null, null, null,
          ArticleOrderBy.PUBLISH_DATE, SortDirection.ASC,
          t1.toString(), article1.getPublishDate(), article1.getId(), 10);

      // when
      List<ArticleResponse> result = articleRepository.search(condition, requestUserId).content();

      // then — T2, T3만 반환 (T1은 cursor와 동일하고 타이브레이크 불충족)
      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("COMMENT_COUNT 정렬로 기사 목록을 반환한다")
    void COMMENT_COUNT_정렬로_기사_목록을_반환한다() {
      // given
      saveArticle(ArticleSource.NAVER, "기사1");
      saveArticle(ArticleSource.HANKYUNG, "기사2");

      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, null, null, null, null,
          ArticleOrderBy.COMMENT_COUNT, SortDirection.DESC,
          null, null, null, 10);

      // when
      List<ArticleResponse> result = articleRepository.search(condition, requestUserId).content();

      // then
      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("VIEW_COUNT ASC 정렬로 기사 목록을 반환한다")
    void VIEW_COUNT_ASC_정렬로_기사_목록을_반환한다() {
      // given
      saveArticle(ArticleSource.NAVER, "기사1");
      saveArticle(ArticleSource.HANKYUNG, "기사2");

      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, null, null, null, null,
          ArticleOrderBy.VIEW_COUNT, SortDirection.ASC,
          null, null, null, 10);

      // when
      List<ArticleResponse> result = articleRepository.search(condition, requestUserId).content();

      // then
      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("COMMENT_COUNT DESC cursor가 있으면 cursor 미만 기사만 반환한다")
    void COMMENT_COUNT_DESC_cursor가_있으면_cursor_미만_기사만_반환한다() {
      // given — commentCount=0인 기사 저장
      Article article = saveArticle(ArticleSource.NAVER, "기사1");

      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, null, null, null, null,
          ArticleOrderBy.COMMENT_COUNT, SortDirection.DESC,
          "0", article.getPublishDate(), article.getId(), 10);

      // when
      List<ArticleResponse> result = articleRepository.search(condition, requestUserId).content();

      // then — commentCount=0은 cursor=0 이하가 아니므로 반환 안 됨
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("VIEW_COUNT DESC cursor가 있으면 cursor 미만 기사만 반환한다")
    void VIEW_COUNT_DESC_cursor가_있으면_cursor_미만_기사만_반환한다() {
      // given — viewCount=0인 기사 저장
      Article article = saveArticle(ArticleSource.NAVER, "기사1");

      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, null, null, null, null,
          ArticleOrderBy.VIEW_COUNT, SortDirection.DESC,
          "0", article.getPublishDate(), article.getId(), 10);

      // when
      List<ArticleResponse> result = articleRepository.search(condition, requestUserId).content();

      // then — viewCount=0은 cursor=0 이하가 아니므로 반환 안 됨
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("COMMENT_COUNT ASC cursor가 있으면 cursor 초과 기사만 반환한다")
    void COMMENT_COUNT_ASC_cursor가_있으면_cursor_초과_기사만_반환한다() {
      // given
      Article article = saveArticle(ArticleSource.NAVER, "기사1");

      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, null, null, null, null,
          ArticleOrderBy.COMMENT_COUNT, SortDirection.ASC,
          "10", article.getPublishDate(), article.getId(), 10);

      // when
      List<ArticleResponse> result = articleRepository.search(condition, requestUserId).content();

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("VIEW_COUNT ASC cursor가 있으면 cursor 초과 기사만 반환한다")
    void VIEW_COUNT_ASC_cursor가_있으면_cursor_초과_기사만_반환한다() {
      // given
      Article article = saveArticle(ArticleSource.NAVER, "기사1");

      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, null, null, null, null,
          ArticleOrderBy.VIEW_COUNT, SortDirection.ASC,
          "10", article.getPublishDate(), article.getId(), 10);

      // when
      List<ArticleResponse> result = articleRepository.search(condition, requestUserId).content();

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("COMMENT_COUNT 정렬로 hasNext가 true이면 nextCursor는 정수 문자열이다")
    void COMMENT_COUNT_hasNext_true이면_nextCursor가_정수다() {
      // given
      saveArticle(ArticleSource.NAVER, "기사1");
      saveArticle(ArticleSource.HANKYUNG, "기사2");

      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, null, null, null, null,
          ArticleOrderBy.COMMENT_COUNT, SortDirection.DESC,
          null, null, null, 1);

      // when
      var response = articleRepository.search(condition, requestUserId);

      // then
      assertThat(response.hasNext()).isTrue();
      assertThat(response.nextCursor()).isNotNull();
      assertThatCode(() -> Integer.parseInt(response.nextCursor())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("VIEW_COUNT 정렬로 hasNext가 true이면 nextCursor는 정수 문자열이다")
    void VIEW_COUNT_hasNext_true이면_nextCursor가_정수다() {
      // given
      saveArticle(ArticleSource.NAVER, "기사1");
      saveArticle(ArticleSource.HANKYUNG, "기사2");

      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, null, null, null, null,
          ArticleOrderBy.VIEW_COUNT, SortDirection.DESC,
          null, null, null, 1);

      // when
      var response = articleRepository.search(condition, requestUserId);

      // then
      assertThat(response.hasNext()).isTrue();
      assertThat(response.nextCursor()).isNotNull();
      assertThatCode(() -> Integer.parseInt(response.nextCursor())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("interestId로 필터링하면 해당 관심사 연결 기사만 반환한다")
    void interestId로_필터링하면_연결된_기사만_반환한다() {
      // given
      Interest interest = interestRepository.save(Interest.create("AI", 2, List.of("AI")));
      Article article1 = articleRepository.save(
          Article.create(ArticleSource.NAVER, "url1", "AI 기사", Instant.now(), null));
      articleRepository.save(
          Article.create(ArticleSource.NAVER, "url2", "일반 기사", Instant.now(), null));

      em.persist(ArticleInterest.create(article1, interest));
      em.flush();

      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, interest.getId(), null, null, null,
          ArticleOrderBy.PUBLISH_DATE, SortDirection.DESC,
          null, null, null, 10);

      // when
      List<ArticleResponse> result = articleRepository.search(condition, requestUserId).content();

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).title()).isEqualTo("AI 기사");
    }
  }

  @Nested
  @DisplayName("findViewCountById")
  class FindViewCountById {

    @Test
    @DisplayName("기사의 초기 조회수는 0이다")
    void 기사의_초기_조회수는_0이다() {
      // given
      Article article = articleRepository.save(
          Article.create(ArticleSource.NAVER, "https://example.com/vc1", "기사", Instant.now(), null));

      // when
      int viewCount = articleRepository.findViewCountById(article.getId());

      // then
      assertThat(viewCount).isZero();
    }

    @Test
    @DisplayName("increaseViewCount 후 조회수가 1 증가한다")
    void increaseViewCount_후_조회수가_1_증가한다() {
      // given
      Article article = articleRepository.save(
          Article.create(ArticleSource.NAVER, "https://example.com/vc2", "기사", Instant.now(), null));
      articleRepository.increaseViewCount(article.getId());

      // when
      int viewCount = articleRepository.findViewCountById(article.getId());

      // then
      assertThat(viewCount).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("findBySourceUrl")
  class FindBySourceUrl {

    @Test
    @DisplayName("존재하는 sourceUrl로 조회하면 기사를 반환한다")
    void 존재하는_sourceUrl로_조회하면_기사를_반환한다() {
      // given
      articleRepository.save(
          Article.create(ArticleSource.NAVER, "https://example.com/news/1", "기사", Instant.now(), null));

      // when
      var result = articleRepository.findBySourceUrl("https://example.com/news/1");

      // then
      assertThat(result).isPresent();
      assertThat(result.get().getTitle()).isEqualTo("기사");
    }

    @Test
    @DisplayName("존재하지 않는 sourceUrl로 조회하면 빈 Optional을 반환한다")
    void 존재하지_않는_sourceUrl로_조회하면_빈_Optional을_반환한다() {
      // when
      var result = articleRepository.findBySourceUrl("https://not-exist.com");

      // then
      assertThat(result).isEmpty();
    }
  }
}
