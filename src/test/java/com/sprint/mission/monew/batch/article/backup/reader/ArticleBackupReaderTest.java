package com.sprint.mission.monew.batch.article.backup.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.sprint.mission.monew.batch.article.backup.dto.ArticleBackupItem;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArticleBackupReaderTest {

  @InjectMocks
  ArticleBackupReader reader;

  @Mock
  ArticleRepository articleRepository;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(reader, "chunkSize", 100);
  }

  private ArticleBackupItem stubItem() {
    return new ArticleBackupItem(
        UUID.randomUUID(), ArticleSource.NAVER, "https://example.com/" + System.nanoTime(),
        "제목", Instant.now(), "요약", 0, 0, Instant.now());
  }

  @Nested
  @DisplayName("기사 백업 목록 읽기")
  class Read {

    @Test
    @DisplayName("대상 기사가 없으면 null을 반환한다")
    void 대상_기사_없으면_null_반환() {
      // given
      given(articleRepository.findArticlesForBackup(any(), any(), any(), any()))
          .willReturn(List.of());

      // when
      ArticleBackupItem result = reader.read();

      // then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("기사가 있으면 순차적으로 반환하고 끝나면 null을 반환한다")
    void 기사_순차_반환_후_null() {
      // given
      ArticleBackupItem item1 = stubItem();
      ArticleBackupItem item2 = stubItem();

      given(articleRepository.findArticlesForBackup(any(), any(), any(), any()))
          .willReturn(List.of(item1, item2), List.of());

      // when
      ArticleBackupItem r1 = reader.read();
      ArticleBackupItem r2 = reader.read();
      ArticleBackupItem r3 = reader.read();

      // then
      assertThat(r1).isSameAs(item1);
      assertThat(r2).isSameAs(item2);
      assertThat(r3).isNull();
    }
  }
}
