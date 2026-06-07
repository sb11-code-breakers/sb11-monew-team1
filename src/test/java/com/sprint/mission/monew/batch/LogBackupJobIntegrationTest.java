package com.sprint.mission.monew.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@SpringBootTest(properties = {"monew.log-dir=${java.io.tmpdir}/monew-test"})
@ActiveProfiles("test")
public class LogBackupJobIntegrationTest {

  @Autowired
  private JobLauncher jobLauncher;

  @Autowired
  private Job logBackupJob;

  @MockitoBean
  private S3Client s3Client;

  @MockitoBean
  private LogBackupMetrics metrics;

  private LocalDate yesterday;
  private Path logFile;

  private Path baseDir;

  @BeforeEach
  void setUp() throws IOException {
    yesterday = LocalDate.now().minusDays(1);
    baseDir = Path.of(System.getProperty("java.io.tmpdir"), "monew-test");

    Files.createDirectories(baseDir);
    logFile = baseDir.resolve("monew." + yesterday + ".log");
  }

  @Nested
  @DisplayName("로그 백업 배치 통합 테스트하기")
  class LogBackupIntegrationTest {

    @Test
    @DisplayName("로그 백업 배치 Job이 정상적으로 실행되어 S3 업로드까지 수행된다")
    void 로그_백업_배치_Job이_정상적으로_실행되어_S3_업로드까지_수행된다() throws Exception {
      // given
      Files.writeString(logFile, "log content");

      given(s3Client.headObject(any(Consumer.class)))
          .willThrow(NoSuchKeyException.builder().build());

      given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
          .willReturn(null);

      JobParameters params = new JobParametersBuilder()
          .addLong("time", Instant.now().toEpochMilli())
          .toJobParameters();

      // when
      JobExecution execution = jobLauncher.run(logBackupJob, params);

      // then
      assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

      verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
      verify(metrics).countUploaded();
      verify(metrics).recordBytes(anyLong());
    }
  }
}
