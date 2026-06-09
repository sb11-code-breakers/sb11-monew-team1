package com.sprint.mission.monew.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.batch.util.BatchGzipUtils;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BatchGzipUtilsTest {

  @Nested
  @DisplayName("articleS3Key")
  class ArticleS3Key {

    @Test
    @DisplayName("날짜로 기사 백업 S3 키를 생성한다")
    void 날짜로_기사_백업_S3_키를_생성한다() {
      // given
      LocalDate date = LocalDate.of(2026, 6, 8);

      // when
      String key = BatchGzipUtils.articleS3Key(date);

      // then
      assertThat(key).isEqualTo("articles/2026/06/08/articles-20260608.json.gz");
    }
  }

  @Nested
  @DisplayName("logS3Key")
  class LogS3Key {

    @Test
    @DisplayName("날짜로 로그 백업 S3 키를 생성한다")
    void 날짜로_로그_백업_S3_키를_생성한다() {
      // given
      LocalDate date = LocalDate.of(2026, 6, 8);

      // when
      String key = BatchGzipUtils.logS3Key(date);

      // then
      assertThat(key).isEqualTo("logs/2026/06/08/app-20260608.log.gz");
    }
  }
}
