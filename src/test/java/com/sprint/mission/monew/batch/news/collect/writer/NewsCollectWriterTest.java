package com.sprint.mission.monew.batch.news.collect.writer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.batch.news.collect.service.ArticleUpsertService;
import com.sprint.mission.monew.batch.news.collect.metrics.NewsCollectMetrics;
import com.sprint.mission.monew.batch.news.collect.dto.NewsCollectItem;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.interest.service.InterestNotificationService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

@ExtendWith(MockitoExtension.class)
public class NewsCollectWriterTest {

  @Mock
  private ArticleUpsertService articleUpsertService;

  @Mock
  private InterestNotificationService interestNotificationService;

  @Mock
  private NewsCollectMetrics newsCollectMetrics;

  @InjectMocks
  private NewsCollectWriter writer;

  @Nested
  @DisplayName("기사 저장하기")
  class Writer {

    @Test
    @DisplayName("기사 저장 중 예외 발생 시 실패 건수를 기록한다")
    void 기사_저장_중_예외_발생_시_실패_건수_기록() {
      // given
      NewsCollectItem item = new NewsCollectItem(
          ArticleSource.HANKYUNG,
          "https://hankyung.com/1",
          "한경 기사",
          Instant.now(),
          "요약");

      Chunk<NewsCollectItem> chunk = new Chunk<>(List.of(item));

      willThrow(new RuntimeException("DB 장애"))
          .given(articleUpsertService)
          .upsertAll(eq(ArticleSource.HANKYUNG), anyList());

      // when
      writer.write(chunk);

      // then
      verify(newsCollectMetrics).countFailed(ArticleSource.HANKYUNG);
      verify(newsCollectMetrics, never()).countCollected(eq(ArticleSource.HANKYUNG), anyInt());
    }

    @Test
    @DisplayName("알림 전송 실패 시 예외를 전파하지 않는다")
    void 알림_전송_실패_시_예외를_전파하지_않는다() {
      // given
      NewsCollectItem item = new NewsCollectItem(
          ArticleSource.HANKYUNG,
          "https://hankyung.com/1",
          "한경 기사",
          Instant.now(),
          "요약");

      Chunk<NewsCollectItem> chunk = new Chunk<>(List.of(item));

      willThrow(new RuntimeException("notify fail"))
          .given(interestNotificationService)
          .notifyNewArticles(any());

      // when
      writer.write(chunk);

      // then
      verify(articleUpsertService).upsertAll(eq(ArticleSource.HANKYUNG), anyList());
      verify(newsCollectMetrics).countCollected(ArticleSource.HANKYUNG, 1);
    }

    @Test
    @DisplayName("chunk가 비어있으면 upsertAll 호출되지 않는다")
    void chunk가_empty면_upsertAll_호출되지_않는다() {
      // given
      Chunk<NewsCollectItem> chunk = new Chunk<>(List.of());

      // when
      writer.write(chunk);

      // then
      verify(articleUpsertService, never()).upsertAll(any(), anyList());
      verify(newsCollectMetrics, never()).countCollected(any(), anyInt());
    }

    @Test
    @DisplayName("기사 단건 저장")
    void 기사_단건_저장() {
      // given
      Instant publishDate = Instant.now();
      NewsCollectItem item = new NewsCollectItem(
          ArticleSource.HANKYUNG, "https://hankyung.com/1", "한경 기사", publishDate, "요약");

      Chunk<NewsCollectItem> chunk = new Chunk<>(List.of(item));

      // when
      writer.write(chunk);

      // then
      verify(articleUpsertService, times(1)).upsertAll(eq(ArticleSource.HANKYUNG), anyList());
      verify(newsCollectMetrics).countCollected(ArticleSource.HANKYUNG, 1);
    }

    @Test
    @DisplayName("기사 다건 저장")
    void 기사_다건_저장() {
      // given
      // first는 NAVER 기사, second는 HANKYUNG 기사, third는 CHOSUN 기사, fourth는 HANKYUNG 기사
      NewsCollectItem first =
          new NewsCollectItem(
              ArticleSource.NAVER,
              "https://naver.com/1", "네이버 기사 제목1", Instant.now(), "요약1");
      NewsCollectItem second =
          new NewsCollectItem(
              ArticleSource.HANKYUNG,
              "https://hankyung.com/1", "한경 기사 제목1", Instant.now(), "요약1");
      NewsCollectItem third =
          new NewsCollectItem(
              ArticleSource.CHOSUN,
              "https://chosun.com/1", "조선 기사 제목1", Instant.now(), "요약1");
      NewsCollectItem fourth =
          new NewsCollectItem(ArticleSource.HANKYUNG,
              "https://hankyung.com/2", "한경 기사 제목2", Instant.now(), "요약2");

      Chunk<NewsCollectItem> chunk = new Chunk<>(List.of(first, second, third, fourth));

      // when
      writer.write(chunk);

      // then
      // ArticleUpsertService가 3번 호출되어야함(네이버1, 한경2, 조선1)
      verify(articleUpsertService, times(3)).upsertAll(any(), anyList());
      verify(articleUpsertService).upsertAll(eq(ArticleSource.NAVER), anyList());
      verify(articleUpsertService).upsertAll(eq(ArticleSource.HANKYUNG), anyList());
      verify(articleUpsertService).upsertAll(eq(ArticleSource.CHOSUN), anyList());
      verify(newsCollectMetrics).countCollected(ArticleSource.NAVER, 1);
      verify(newsCollectMetrics).countCollected(ArticleSource.HANKYUNG, 2);
      verify(newsCollectMetrics).countCollected(ArticleSource.CHOSUN, 1);
    }
  }
}
