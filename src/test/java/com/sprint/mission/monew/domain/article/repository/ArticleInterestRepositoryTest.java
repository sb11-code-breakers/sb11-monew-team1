package com.sprint.mission.monew.domain.article.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.common.config.JpaConfig;
import com.sprint.mission.monew.common.config.QuerydslConfig;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleInterest;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.repository.InterestRepository;
import com.sprint.mission.monew.domain.article.repository.dto.InterestArticleCount;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
@Import({JpaConfig.class, QuerydslConfig.class})
class ArticleInterestRepositoryTest {

  @Autowired ArticleInterestRepository articleInterestRepository;
  @Autowired ArticleRepository articleRepository;
  @Autowired InterestRepository interestRepository;
  @Autowired EntityManager em;

  @BeforeEach
  void setUp() {
    articleInterestRepository.deleteAll();
    articleRepository.deleteAll();
    interestRepository.deleteAll();
    em.flush();
    em.clear();
  }

  @Nested
  @DisplayName("countByInterestSince")
  class CountByInterestSince {

    @Test
    @DisplayName("since 이후 생성된 기사를 관심사별로 집계한다")
    void since_이후_기사를_관심사별로_집계한다() {
      // given
      Instant since = Instant.now().minusSeconds(5);

      Interest interestA = interestRepository.save(Interest.create("인공지능", List.of("AI")));
      Interest interestB = interestRepository.save(Interest.create("경제", List.of("경제")));

      Article a1 = articleRepository.save(
          Article.create(ArticleSource.NAVER, "https://ex.com/1", "AI 기사1", Instant.now(), "요약1"));
      Article a2 = articleRepository.save(
          Article.create(ArticleSource.NAVER, "https://ex.com/2", "AI 기사2", Instant.now(), "요약2"));
      Article a3 = articleRepository.save(
          Article.create(ArticleSource.NAVER, "https://ex.com/3", "경제 기사", Instant.now(), "요약3"));

      articleInterestRepository.save(ArticleInterest.create(a1, interestA));
      articleInterestRepository.save(ArticleInterest.create(a2, interestA));
      articleInterestRepository.save(ArticleInterest.create(a3, interestB));

      em.flush();
      em.clear();

      // when
      List<InterestArticleCount> result = articleInterestRepository.countByInterestSince(since);

      // then
      Map<UUID, Long> countMap = result.stream()
          .collect(Collectors.toMap(
              InterestArticleCount::getInterestId,
              InterestArticleCount::getArticleCount));
      Map<UUID, String> nameMap = result.stream()
          .collect(Collectors.toMap(
              InterestArticleCount::getInterestId,
              InterestArticleCount::getInterestName));
      assertThat(countMap).hasSize(2);
      assertThat(countMap.get(interestA.getId())).isEqualTo(2L);
      assertThat(countMap.get(interestB.getId())).isEqualTo(1L);
      assertThat(nameMap.get(interestA.getId())).isEqualTo("인공지능");
      assertThat(nameMap.get(interestB.getId())).isEqualTo("경제");
    }

    @Test
    @DisplayName("since 이후 기사가 없으면 빈 목록을 반환한다")
    void since_이후_기사가_없으면_빈_목록을_반환한다() {
      // given — since를 미래로 설정해 저장된 기사가 범위 밖이 되도록
      Instant since = Instant.now().plusSeconds(60);

      Interest interest = interestRepository.save(Interest.create("인공지능", List.of("AI")));
      Article a1 = articleRepository.save(
          Article.create(ArticleSource.NAVER, "https://ex.com/1", "AI 기사", Instant.now(), "요약"));
      articleInterestRepository.save(ArticleInterest.create(a1, interest));

      em.flush();
      em.clear();

      // when
      List<InterestArticleCount> result = articleInterestRepository.countByInterestSince(since);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("createdAt이 since와 같은 기사는 집계에서 제외한다")
    void createdAt이_since와_같은_기사는_집계에서_제외한다() {
      // given
      Interest interest = interestRepository.save(Interest.create("인공지능", List.of("AI")));
      Article article = articleRepository.save(
          Article.create(ArticleSource.NAVER, "https://ex.com/boundary", "AI 기사", Instant.now(), "요약"));
      articleInterestRepository.save(ArticleInterest.create(article, interest));
      em.flush();
      em.clear();

      // 저장된 createdAt을 정확히 읽어 since로 사용 (createdAt > since 조건: 같은 시각은 미포함)
      Instant since = articleRepository.findById(article.getId()).orElseThrow().getCreatedAt();

      // when
      List<InterestArticleCount> result = articleInterestRepository.countByInterestSince(since);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("삭제된 기사는 집계에서 제외한다")
    void 삭제된_기사는_집계에서_제외한다() {
      // given
      Instant since = Instant.now().minusSeconds(5);

      Interest interest = interestRepository.save(Interest.create("인공지능", List.of("AI")));
      Article deleted = articleRepository.save(
          Article.create(ArticleSource.NAVER, "https://ex.com/1", "AI 기사", Instant.now(), "요약"));
      deleted.softDelete();
      articleRepository.save(deleted);
      articleInterestRepository.save(ArticleInterest.create(deleted, interest));

      em.flush();
      em.clear();

      // when
      List<InterestArticleCount> result = articleInterestRepository.countByInterestSince(since);

      // then
      assertThat(result).isEmpty();
    }
  }
}
