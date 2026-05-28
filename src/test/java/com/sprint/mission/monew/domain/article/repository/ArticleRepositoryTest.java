package com.sprint.mission.monew.domain.article.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.mission.monew.common.config.QuerydslConfig;
import com.sprint.mission.monew.common.dto.SortDirection;
import com.sprint.mission.monew.domain.article.dto.ArticleOrderBy;
import com.sprint.mission.monew.domain.article.dto.ArticleQueryCondition;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleInterest;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.exception.ArticleInvalidCursorException;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.repository.InterestRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QuerydslConfig.class)
class ArticleRepositoryTest {

  @Autowired ArticleRepository articleRepository;
  @Autowired InterestRepository interestRepository;
  @Autowired EntityManager em;

  @BeforeEach
  void setUp() {
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
        null, null, limit);
  }

  @Nested
  @DisplayName("count")
  class Count {

    @Test
    @DisplayName("기사가 없으면 0을 반환한다")
    void 기사가_없으면_0을_반환한다() {
      // when
      long count = articleRepository.count(defaultCondition(10));

      // then
      assertThat(count).isZero();
    }

    @Test
    @DisplayName("저장된 기사 수만큼 반환한다")
    void 저장된_기사_수만큼_반환한다() {
      // given
      saveArticle(ArticleSource.NAVER, "기사1");
      saveArticle(ArticleSource.HANKYUNG, "기사2");

      // when
      long count = articleRepository.count(defaultCondition(10));

      // then
      assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("소프트딜리트된 기사는 count에서 제외된다")
    void 소프트딜리트된_기사는_count에서_제외된다() {
      // given
      Article article = saveArticle(ArticleSource.NAVER, "삭제될기사");
      article.softDelete();
      articleRepository.save(article);
      saveArticle(ArticleSource.HANKYUNG, "정상기사");

      // when
      long count = articleRepository.count(defaultCondition(10));

      // then
      assertThat(count).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("findAll")
  class FindAll {

    @Test
    @DisplayName("cursor가 없으면 저장된 모든 기사를 반환한다")
    void cursor가_없으면_저장된_모든_기사를_반환한다() {
      // given
      saveArticle(ArticleSource.NAVER, "기사1");
      saveArticle(ArticleSource.HANKYUNG, "기사2");

      // when
      List<Article> result = articleRepository.findAll(defaultCondition(10));

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
      List<Article> result = articleRepository.findAll(defaultCondition(10));

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getTitle()).isEqualTo("정상기사");
    }

    @Test
    @DisplayName("limit+1개를 조회해 hasNext 판별에 사용할 수 있다")
    void limit_1개를_조회해_hasNext_판별에_사용할_수_있다() {
      // given
      saveArticle(ArticleSource.NAVER, "기사1");
      saveArticle(ArticleSource.HANKYUNG, "기사2");
      saveArticle(ArticleSource.CHOSUN, "기사3");

      // when — limit=2이면 내부적으로 limit+1=3개 조회
      List<Article> result = articleRepository.findAll(defaultCondition(2));

      // then
      assertThat(result).hasSize(3); // 서비스에서 limit 초과 여부 판단
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
          null, null, 10);

      // when
      List<Article> result = articleRepository.findAll(condition);

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getTitle()).isEqualTo("인공지능 뉴스");
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
          null, null, 10);

      // when
      List<Article> result = articleRepository.findAll(condition);

      // then
      assertThat(result).hasSize(2);
      assertThat(result).extracting(Article::getSource)
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
          null, null, 10);

      // when
      List<Article> result = articleRepository.findAll(condition);

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getTitle()).isEqualTo("기사2");
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
          null, null, 10);

      // when
      List<Article> result = articleRepository.findAll(condition);

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getTitle()).isEqualTo("기사1");
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
      articleRepository.save(Article.create(ArticleSource.NAVER, "url3", "기사3", t3, null));

      // after=EPOCH → createdAt.lt(EPOCH)=false → 동일 publishDate 타이브레이크 제외
      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, null, null, null, null,
          ArticleOrderBy.PUBLISH_DATE, SortDirection.DESC,
          t3.toString(), Instant.EPOCH, 10);

      // when
      List<Article> result = articleRepository.findAll(condition);

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
      articleRepository.save(Article.create(ArticleSource.NAVER, "url1", "기사1", t1, null));
      articleRepository.save(Article.create(ArticleSource.NAVER, "url2", "기사2", t2, null));
      articleRepository.save(Article.create(ArticleSource.NAVER, "url3", "기사3", t3, null));

      // after=미래 → createdAt.gt(미래)=false → T1 타이브레이크 제외
      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, null, null, null, null,
          ArticleOrderBy.PUBLISH_DATE, SortDirection.ASC,
          t1.toString(), Instant.now().plusSeconds(86400), 10);

      // when
      List<Article> result = articleRepository.findAll(condition);

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
          null, null, 10);

      // when
      List<Article> result = articleRepository.findAll(condition);

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
          null, null, 10);

      // when
      List<Article> result = articleRepository.findAll(condition);

      // then
      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("COMMENT_COUNT DESC cursor가 있으면 cursor 미만 기사만 반환한다")
    void COMMENT_COUNT_DESC_cursor가_있으면_cursor_미만_기사만_반환한다() {
      // given — commentCount=0인 기사 저장
      saveArticle(ArticleSource.NAVER, "기사1");

      // cursor="0", after=EPOCH → commentCount.lt(0)=false, 타이브레이크도 false
      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, null, null, null, null,
          ArticleOrderBy.COMMENT_COUNT, SortDirection.DESC,
          "0", Instant.EPOCH, 10);

      // when
      List<Article> result = articleRepository.findAll(condition);

      // then — commentCount=0은 cursor=0 이하가 아니므로 반환 안 됨
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("VIEW_COUNT DESC cursor가 있으면 cursor 미만 기사만 반환한다")
    void VIEW_COUNT_DESC_cursor가_있으면_cursor_미만_기사만_반환한다() {
      // given — viewCount=0인 기사 저장
      saveArticle(ArticleSource.NAVER, "기사1");

      // cursor="0", after=EPOCH → viewCount.lt(0)=false, 타이브레이크도 false
      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, null, null, null, null,
          ArticleOrderBy.VIEW_COUNT, SortDirection.DESC,
          "0", Instant.EPOCH, 10);

      // when
      List<Article> result = articleRepository.findAll(condition);

      // then — viewCount=0은 cursor=0 이하가 아니므로 반환 안 됨
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("COMMENT_COUNT ASC cursor가 있으면 cursor 초과 기사만 반환한다")
    void COMMENT_COUNT_ASC_cursor가_있으면_cursor_초과_기사만_반환한다() {
      // given
      saveArticle(ArticleSource.NAVER, "기사1");

      // commentCount=0 > 10이면 포함 → 없음
      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, null, null, null, null,
          ArticleOrderBy.COMMENT_COUNT, SortDirection.ASC,
          "10", Instant.now().plusSeconds(86400), 10);

      // when
      List<Article> result = articleRepository.findAll(condition);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("VIEW_COUNT ASC cursor가 있으면 cursor 초과 기사만 반환한다")
    void VIEW_COUNT_ASC_cursor가_있으면_cursor_초과_기사만_반환한다() {
      // given
      saveArticle(ArticleSource.NAVER, "기사1");

      // viewCount=0 > 10이면 포함 → 없음
      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, null, null, null, null,
          ArticleOrderBy.VIEW_COUNT, SortDirection.ASC,
          "10", Instant.now().plusSeconds(86400), 10);

      // when
      List<Article> result = articleRepository.findAll(condition);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("PUBLISH_DATE cursor 값이 파싱 불가능하면 ArticleInvalidCursorException을 던진다")
    void PUBLISH_DATE_cursor_파싱_불가_시_예외_발생() {
      // given
      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, null, null, null, null,
          ArticleOrderBy.PUBLISH_DATE, SortDirection.DESC,
          "not-a-date", Instant.now(), 10);

      // when & then
      assertThatThrownBy(() -> articleRepository.findAll(condition))
          .isInstanceOf(ArticleInvalidCursorException.class);
    }

    @Test
    @DisplayName("COMMENT_COUNT cursor 값이 파싱 불가능하면 ArticleInvalidCursorException을 던진다")
    void COMMENT_COUNT_cursor_파싱_불가_시_예외_발생() {
      // given
      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, null, null, null, null,
          ArticleOrderBy.COMMENT_COUNT, SortDirection.DESC,
          "not-a-number", Instant.now(), 10);

      // when & then
      assertThatThrownBy(() -> articleRepository.findAll(condition))
          .isInstanceOf(ArticleInvalidCursorException.class);
    }

    @Test
    @DisplayName("VIEW_COUNT cursor 값이 파싱 불가능하면 ArticleInvalidCursorException을 던진다")
    void VIEW_COUNT_cursor_파싱_불가_시_예외_발생() {
      // given
      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, null, null, null, null,
          ArticleOrderBy.VIEW_COUNT, SortDirection.DESC,
          "not-a-number", Instant.now(), 10);

      // when & then
      assertThatThrownBy(() -> articleRepository.findAll(condition))
          .isInstanceOf(ArticleInvalidCursorException.class);
    }

    @Test
    @DisplayName("interestId로 필터링하면 해당 관심사 연결 기사만 반환한다")
    void interestId로_필터링하면_연결된_기사만_반환한다() {
      // given
      Interest interest = interestRepository.save(Interest.create("AI", List.of("AI")));
      Article article1 = articleRepository.save(
          Article.create(ArticleSource.NAVER, "url1", "AI 기사", Instant.now(), null));
      articleRepository.save(
          Article.create(ArticleSource.NAVER, "url2", "일반 기사", Instant.now(), null));

      em.persist(ArticleInterest.create(article1, interest));
      em.flush();

      ArticleQueryCondition condition = new ArticleQueryCondition(
          null, interest.getId(), null, null, null,
          ArticleOrderBy.PUBLISH_DATE, SortDirection.DESC,
          null, null, 10);

      // when
      List<Article> result = articleRepository.findAll(condition);

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getTitle()).isEqualTo("AI 기사");
    }
  }
}
