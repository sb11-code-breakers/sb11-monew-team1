package com.sprint.mission.monew.batch.article.backup.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.monew.batch.article.backup.dto.ArticleBackupItem;
import com.sprint.mission.monew.batch.article.backup.exception.ArticleBackupFailedException;
import com.sprint.mission.monew.batch.article.backup.metrics.ArticleBackupMetrics;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class ArticleBackupWriterTest {

  @Mock
  private S3Client s3Client;

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private ArticleBackupMetrics metrics;

  @InjectMocks
  private ArticleBackupWriter writer;

  private ArticleBackupItem item;

  @BeforeEach
  void setUp() throws JsonProcessingException {
    item = new ArticleBackupItem(
        UUID.randomUUID(), ArticleSource.NAVER, "https://news.example.com/1",
        "테스트 기사", Instant.now(), "요약", 0, 0, Instant.now());
    ReflectionTestUtils.setField(writer, "bucket", "test-bucket");
    given(objectMapper.writeValueAsBytes(any())).willReturn("[]".getBytes());
  }

  @Nested
  @DisplayName("기사 청크 S3 업로드")
  class Write {

    @Test
    @DisplayName("청크 기사 목록을 JSON 직렬화 후 Gzip 압축 후 S3 업로드한다")
    void 청크_기사_목록을_JSON_직렬화_후_Gzip_압축_후_S3_업로드한다() throws Exception {
      // given
      Chunk<ArticleBackupItem> chunk = new Chunk<>(List.of(item));

      // when
      writer.write(chunk);

      // then
      verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("S3 키에 청크 인덱스가 포함된다")
    void S3_키에_청크_인덱스가_포함된다() throws Exception {
      // given
      Chunk<ArticleBackupItem> chunk = new Chunk<>(List.of(item));

      // when
      writer.write(chunk);

      // then
      ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
      verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
      assertThat(captor.getValue().key()).contains("-001.json.gz");
    }

    @Test
    @DisplayName("업로드 실패 시 ArticleBackupFailedException이 발생한다")
    void 업로드_실패_시_ArticleBackupFailedException_발생() throws Exception {
      // given
      Chunk<ArticleBackupItem> chunk = new Chunk<>(List.of(item));
      given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
          .willThrow(new RuntimeException("S3 오류"));

      // when & then
      assertThatThrownBy(() -> writer.write(chunk))
          .isInstanceOf(ArticleBackupFailedException.class);
      verify(metrics).countFailed();
    }
  }
}