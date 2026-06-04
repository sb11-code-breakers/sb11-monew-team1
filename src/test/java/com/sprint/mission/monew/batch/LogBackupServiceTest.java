package com.sprint.mission.monew.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
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
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class LogBackupServiceTest {

  @InjectMocks LogBackupService logBackupService;
  @Mock S3Client s3Client;
  @Mock LogBackupMetrics logBackupMetrics;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(logBackupService, "logDir", tempDir.toString());
    ReflectionTestUtils.setField(logBackupService, "bucket", "test-bucket");
  }

  @Nested
  @DisplayName("로그 파일 S3 업로드")
  class Upload {

    @Test
    @DisplayName("전날 로그 파일이 없으면 S3 업로드를 호출하지 않는다")
    void 전날_로그_파일이_없으면_S3_업로드를_호출하지_않는다() {
      // given — tempDir에 로그 파일 없음

      // when
      logBackupService.upload();

      // then
      verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("S3에 이미 동일 키가 존재하면 업로드 없이 로컬 파일을 삭제한다")
    void S3에_이미_동일_키가_존재하면_업로드_없이_로컬_파일을_삭제한다() throws IOException {
      // given
      LocalDate yesterday = LocalDate.now().minusDays(1);
      Path logFile = tempDir.resolve("monew." + yesterday + ".log");
      Files.writeString(logFile, "log content");
      given(s3Client.headObject(any(HeadObjectRequest.class)))
          .willReturn(HeadObjectResponse.builder().build());

      // when
      logBackupService.upload();

      // then
      verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
      assertThat(logFile).doesNotExist();
    }

    @Test
    @DisplayName("S3 업로드 중 예외 발생 시 LogBackupFailedException을 던진다")
    void S3_업로드_중_예외_발생_시_LogBackupFailedException을_던진다() throws IOException {
      // given
      LocalDate yesterday = LocalDate.now().minusDays(1);
      Files.writeString(tempDir.resolve("monew." + yesterday + ".log"), "log content");
      given(s3Client.headObject(any(HeadObjectRequest.class)))
          .willThrow(NoSuchKeyException.builder().build());
      given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
          .willThrow(new RuntimeException("S3 연결 오류"));

      // when & then
      assertThatThrownBy(() -> logBackupService.upload())
          .isInstanceOf(LogBackupFailedException.class);
    }

    @Test
    @DisplayName("로컬 파일 삭제 실패 시 LogBackupDeleteFailedException을 던진다")
    void 로컬_파일_삭제_실패_시_LogBackupDeleteFailedException을_던진다() throws IOException {
      // given
      LocalDate yesterday = LocalDate.now().minusDays(1);
      Path logFile = tempDir.resolve("monew." + yesterday + ".log");
      Files.writeString(logFile, "log content");
      given(s3Client.headObject(any(HeadObjectRequest.class)))
          .willThrow(NoSuchKeyException.builder().build());

      try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
        filesMock.when(() -> Files.exists(any())).thenReturn(true);
        filesMock.when(() -> Files.readAllBytes(any())).thenReturn("log content".getBytes());
        filesMock.when(() -> Files.delete(any())).thenThrow(new IOException("삭제 실패"));

        // when & then
        assertThatThrownBy(() -> logBackupService.upload())
            .isInstanceOf(LogBackupDeleteFailedException.class);
      }
    }

    @Test
    @DisplayName("로그 파일이 존재하고 S3에 없으면 업로드 후 로컬 파일을 삭제한다")
    void 로그_파일이_존재하고_S3에_없으면_업로드_후_로컬_파일을_삭제한다() throws IOException {
      // given
      LocalDate yesterday = LocalDate.now().minusDays(1);
      Path logFile = tempDir.resolve("monew." + yesterday + ".log");
      Files.writeString(logFile, "log content");
      given(s3Client.headObject(any(HeadObjectRequest.class)))
          .willThrow(NoSuchKeyException.builder().build());

      // when
      logBackupService.upload();

      // then
      verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
      assertThat(logFile).doesNotExist();
    }

    @Test
    @DisplayName("업로드 성공 시 업로드 건수·바이트·소요 시간을 집계한다")
    void 업로드_성공_시_업로드_건수_바이트_소요_시간을_집계한다() throws IOException {
      // given
      LocalDate yesterday = LocalDate.now().minusDays(1);
      Files.writeString(tempDir.resolve("monew." + yesterday + ".log"), "log content");
      given(s3Client.headObject(any(HeadObjectRequest.class)))
          .willThrow(NoSuchKeyException.builder().build());

      // when
      logBackupService.upload();

      // then
      verify(logBackupMetrics).countUploaded();
      verify(logBackupMetrics).recordBytes(anyLong());
      verify(logBackupMetrics).recordDuration(any(Duration.class));
    }
  }
}