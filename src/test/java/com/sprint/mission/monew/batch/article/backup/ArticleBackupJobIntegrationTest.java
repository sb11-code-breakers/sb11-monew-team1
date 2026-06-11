package com.sprint.mission.monew.batch.article.backup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


import com.sprint.mission.monew.batch.article.backup.dto.ArticleBackupItem;
import com.sprint.mission.monew.batch.article.backup.metrics.ArticleBackupMetrics;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@SpringBootTest
@ActiveProfiles("test")
class ArticleBackupJobIntegrationTest {

  @Autowired
  private JobLauncher jobLauncher;

  @Autowired
  private Job articleBackupJob;

  @MockitoBean
  private S3Client s3Client;

  @MockitoBean
  private ArticleBackupMetrics metrics;

  @MockitoBean
  private ArticleRepository articleRepository;

  @Nested
  @DisplayName("기사 백업 배치 통합 테스트하기")
  class ArticleBackupIntegrationTest {

    @Test
    @DisplayName("기사 백업 배치 Job이 정상적으로 실행되어 S3 업로드까지 수행된다")
    void 기사_백업_배치_Job이_정상적으로_실행되어_S3_업로드까지_수행된다() throws Exception {
      // given
      ArticleBackupItem item = new ArticleBackupItem(
          UUID.randomUUID(), ArticleSource.NAVER, "https://news.example.com/1",
          "테스트 기사", Instant.now(), "요약", 0, 0, Instant.now());

      given(articleRepository.findArticlesForBackup(any(), any(), any(UUID.class), any(Pageable.class)))
          .willReturn(List.of(item))
          .willReturn(List.of());
      given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
          .willReturn(null);

      JobParameters params = new JobParametersBuilder()
          .addLong("time", Instant.now().toEpochMilli())
          .toJobParameters();

      // when
      JobExecution execution = jobLauncher.run(articleBackupJob, params);

      // then
      assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
      verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
      verify(metrics).countUploaded();
    }

    @Test
    @DisplayName("대상 기사가 없으면 S3 업로드 없이 COMPLETED가 된다")
    void 대상_기사가_없으면_S3_업로드_없이_COMPLETED가_된다() throws Exception {
      // given
      given(articleRepository.findArticlesForBackup(any(), any(), any(UUID.class), any(Pageable.class)))
          .willReturn(List.of());

      JobParameters params = new JobParametersBuilder()
          .addLong("time", Instant.now().toEpochMilli() + 1)
          .toJobParameters();

      // when
      JobExecution execution = jobLauncher.run(articleBackupJob, params);

      // then
      assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
      verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
  }
}