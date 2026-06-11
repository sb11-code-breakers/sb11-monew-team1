package com.sprint.mission.monew.batch.news.collect;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class NewsCollectJobIntegrationTest {

  @Autowired
  private JobLauncher jobLauncher;

  @Autowired
  private Job newsCollectJob;

  @Autowired
  private ArticleRepository articleRepository;

  @BeforeEach
  void setUp() {
    articleRepository.deleteAll();

    Article article1 = Article.create(ArticleSource.NAVER,
        "https://naver.com/1" + UUID.randomUUID(), "네이버 기사 제목1",
        Instant.now(), "요약1");
    Article article2 = Article.create(ArticleSource.CHOSUN,
        "https://chosun.com/1" + UUID.randomUUID(), "한경 기사 제목1",
        Instant.now(), "요약1");

    articleRepository.save(article1);
    articleRepository.save(article2);
  }

  @Nested
  @DisplayName("뉴스 수집 배치 통합 테스트하기")
  class NewsCollectIntegrationTest {

    @Test
    @DisplayName("뉴스 수집 배치 통합테스트")
    void 뉴스_수집_배치_통합테스트_성공() throws Exception {
      // given
      long beforeCount = articleRepository.count();

      JobParameters params = new JobParametersBuilder()
          .addLong("time", Instant.now().toEpochMilli())
          .toJobParameters();

      // when
      JobExecution execution = jobLauncher.run(newsCollectJob, params);

      // then
      assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

      long afterCount = articleRepository.count();
      assertThat(afterCount).isGreaterThanOrEqualTo(beforeCount);
    }
  }
}