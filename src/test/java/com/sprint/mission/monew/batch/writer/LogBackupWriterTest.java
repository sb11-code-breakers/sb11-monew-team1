package com.sprint.mission.monew.batch.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.batch.exception.LogBackupDeleteFailedException;
import com.sprint.mission.monew.batch.exception.LogBackupFailedException;
import com.sprint.mission.monew.batch.metrics.LogBackupMetrics;
import com.sprint.mission.monew.batch.dto.UploadPayload;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
public class LogBackupWriterTest {

  @TempDir
  private Path tempDir;

  @Mock
  private S3Client s3Client;

  @Mock
  private LogBackupMetrics metrics;

  @InjectMocks
  private LogBackupWriter writer;

  private Path logFile;
  LocalDate yesterday;
  private UploadPayload payload;

  @BeforeEach
  void setUp() {
    yesterday = LocalDate.now().minusDays(1);
    logFile = tempDir.resolve("monew." + yesterday + ".log");
    payload = new UploadPayload(logFile, "key", "data".getBytes());
    ReflectionTestUtils.setField(writer, "bucket", "test-bucket");
  }

  @Nested
  @DisplayName("백업 로그 파일 저장하기")
  class Writer {

    @Test
    @DisplayName("이미 S3에 존재하면 업로드를 건너뛰고 로컬 파일을 삭제한다")
    void 이미_S3에_존재하면_skip() throws IOException {
      // given
      Files.writeString(logFile, "log content");
      Chunk<UploadPayload> chunk = new Chunk<>(List.of(payload));

      // headObject 성공 = 이미 존재
      given(s3Client.headObject(any(Consumer.class)))
          .willReturn(null);

      // when
      writer.write(chunk);

      // then
      verify(metrics).countSkipped();
      verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));

      // 로컬 파일 삭제됐는지 검증
      assertThat(logFile).doesNotExist();
    }

    @Test
    @DisplayName("S3 존재 여부 확인 실패 시 LogBackupFailedException 발생으로 Job이 실패한다")
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
    @DisplayName("S3 업로드 실패 시 LogBackupFailedException 발생으로 Job이 실패한다")
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
    @DisplayName("로컬 파일 삭제 실패 시 LogBackupDeleteFailedException 발생으로 Job이 실패한다")
    void 로컬_파일_삭제_실패_시_Job_실패() throws Exception {
      // given
      Files.writeString(logFile, "log content");

      given(s3Client.headObject(any(Consumer.class)))
          .willThrow(NoSuchKeyException.builder().build());

      given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
          .willReturn(null);

      Chunk<UploadPayload> chunk = new Chunk<>(List.of(payload));

      try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
        filesMock.when(() -> Files.exists(any())).thenReturn(true);
        filesMock.when(() -> Files.readAllBytes(any())).thenReturn("log content".getBytes());
        filesMock.when(() -> Files.delete(any())).thenThrow(new IOException("삭제 실패"));

        // when & then
        assertThatThrownBy(() -> writer.write(chunk))
            .isInstanceOf(LogBackupDeleteFailedException.class);
      }
    }

    @Test
    @DisplayName("S3에 파일 업로드")
    void S3에_파일_업로드() throws IOException {
      // given
      Files.writeString(logFile, "log content");

      given(s3Client.headObject(any(Consumer.class)))
          .willThrow(NoSuchKeyException.builder().build());

      Chunk<UploadPayload> chunk = new Chunk<>(List.of(payload));

      // when
      writer.write(chunk);

      // then
      verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("로그 파일이 존재하고 S3에 없으면 업로드 후 로컬 파일을 삭제한다")
    void 로그_파일이_존재하고_S3에_없으면_업로드_후_로컬_파일을_삭제한다() throws IOException {
      // given
      Files.writeString(logFile, "log content");
      given(s3Client.headObject(any(Consumer.class)))
          .willThrow(NoSuchKeyException.builder().build());
      Chunk<UploadPayload> chunk = new Chunk<>(List.of(payload));

      // when
      writer.write(chunk);

      // then
      verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
      assertThat(logFile).doesNotExist();
    }

    @Test
    @DisplayName("업로드 성공 시 업로드 건수·바이트·소요 시간을 집계한다")
    void 업로드_성공_시_업로드_건수_바이트_소요_시간을_집계한다() throws IOException {
      // given
      Files.writeString(logFile, "log content");
      given(s3Client.headObject(any(Consumer.class)))
          .willThrow(NoSuchKeyException.builder().build());

      Chunk<UploadPayload> chunk = new Chunk<>(List.of(payload));

      // when
      writer.write(chunk);

      // then
      verify(metrics).countUploaded();
      verify(metrics).recordBytes(anyLong());
      verify(metrics).recordDuration(any(Duration.class));
    }
  }
}
