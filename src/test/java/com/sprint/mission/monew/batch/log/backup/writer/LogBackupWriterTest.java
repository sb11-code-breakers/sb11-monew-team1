package com.sprint.mission.monew.batch.log.backup.writer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.batch.log.backup.exception.LogBackupFailedException;
import com.sprint.mission.monew.batch.log.backup.metrics.LogBackupMetrics;
import com.sprint.mission.monew.batch.log.backup.dto.UploadPayload;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class LogBackupWriterTest {

  @Mock
  private S3Client s3Client;

  @Mock
  private LogBackupMetrics metrics;

  @InjectMocks
  private LogBackupWriter writer;

  private UploadPayload payload;

  @BeforeEach
  void setUp() {
    payload = new UploadPayload("logs/2026/06/08/app-20260608.log.gz", "data".getBytes());
    ReflectionTestUtils.setField(writer, "bucket", "test-bucket");
  }

  @Nested
  @DisplayName("백업 로그 파일 저장하기")
  class Write {

    @Test
    @DisplayName("S3 존재 여부 확인 실패 시 LogBackupFailedException이 발생한다")
    void S3_존재여부_확인_실패() {
      // given
      Chunk<UploadPayload> chunk = new Chunk<>(List.of(payload));
      given(s3Client.headObject(any(Consumer.class)))
          .willThrow(new RuntimeException("S3 장애"));

      // when & then
      assertThatThrownBy(() -> writer.write(chunk))
          .isInstanceOf(LogBackupFailedException.class);
    }

    @Test
    @DisplayName("S3 업로드 실패 시 LogBackupFailedException이 발생한다")
    void S3_업로드_실패_시_LogBackupFailedException_발생() {
      // given
      given(s3Client.headObject(any(Consumer.class)))
          .willThrow(NoSuchKeyException.builder().build());
      given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
          .willThrow(new RuntimeException("S3 장애"));
      Chunk<UploadPayload> chunk = new Chunk<>(List.of(payload));

      // when & then
      assertThatThrownBy(() -> writer.write(chunk))
          .isInstanceOf(LogBackupFailedException.class);
    }

    @Test
    @DisplayName("이미 S3에 존재하면 업로드를 건너뛴다")
    void 이미_S3에_존재하면_skip() {
      // given
      Chunk<UploadPayload> chunk = new Chunk<>(List.of(payload));
      given(s3Client.headObject(any(Consumer.class)))
          .willReturn(null);

      // when
      writer.write(chunk);

      // then
      verify(metrics).countSkipped();
      verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("S3에 없으면 업로드한다")
    void S3에_없으면_업로드한다() {
      // given
      given(s3Client.headObject(any(Consumer.class)))
          .willThrow(NoSuchKeyException.builder().build());
      Chunk<UploadPayload> chunk = new Chunk<>(List.of(payload));

      // when
      writer.write(chunk);

      // then
      verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("업로드 성공 시 업로드 건수·바이트·소요 시간을 집계한다")
    void 업로드_성공_시_업로드_건수_바이트_소요_시간을_집계한다() {
      // given
      given(s3Client.headObject(any(Consumer.class)))
          .willThrow(NoSuchKeyException.builder().build());
      Chunk<UploadPayload> chunk = new Chunk<>(List.of(payload));

      // when
      writer.write(chunk);

      // then
      verify(metrics).countUploaded();
      verify(metrics).recordBytes(anyLong());
    }
  }
}