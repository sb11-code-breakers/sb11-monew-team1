package com.sprint.mission.monew.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mockito.ArgumentCaptor;

import com.sprint.mission.monew.batch.metrics.LogBackupMetrics;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.FilterLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.FilterLogEventsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.FilteredLogEvent;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@SpringBootTest(properties = {"monew.log-group=test-log-group"})
@ActiveProfiles("test")
class LogBackupJobIntegrationTest {

  @Autowired
  private JobLauncher jobLauncher;

  @Autowired
  private Job logBackupJob;

  @MockitoBean
  private S3Client s3Client;

  @MockitoBean
  private LogBackupMetrics metrics;

  @MockitoBean
  private CloudWatchLogsClient cloudWatchLogsClient;

  @Nested
  @DisplayName("로그 백업 배치 통합 테스트하기")
  class LogBackupIntegrationTest {

    @Test
    @DisplayName("로그 백업 배치 Job이 정상적으로 실행되어 S3 업로드까지 수행된다")
    void 로그_백업_배치_Job이_정상적으로_실행되어_S3_업로드까지_수행된다() throws Exception {
      // given
      FilterLogEventsResponse response = FilterLogEventsResponse.builder()
          .events(List.of(FilteredLogEvent.builder().message("log line").build()))
          .build();
      given(cloudWatchLogsClient.filterLogEvents(any(FilterLogEventsRequest.class)))
          .willReturn(response);
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

    @Test
    @DisplayName("두 페이지에 걸친 로그가 각각 별도 파일로 S3에 업로드된다")
    void 두_페이지에_걸친_로그가_각각_별도_파일로_S3에_업로드된다() throws Exception {
      // given
      FilterLogEventsResponse firstPage = FilterLogEventsResponse.builder()
          .events(List.of(FilteredLogEvent.builder().message("page1-log").build()))
          .nextToken("token")
          .build();
      FilterLogEventsResponse secondPage = FilterLogEventsResponse.builder()
          .events(List.of(FilteredLogEvent.builder().message("page2-log").build()))
          .build();
      given(cloudWatchLogsClient.filterLogEvents(any(FilterLogEventsRequest.class)))
          .willReturn(firstPage, secondPage);
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

      ArgumentCaptor<PutObjectRequest> putReqCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
      verify(s3Client, times(2)).putObject(putReqCaptor.capture(), any(RequestBody.class));
      List<String> uploadedKeys = putReqCaptor.getAllValues().stream()
          .map(PutObjectRequest::key)
          .toList();
      assertThat(uploadedKeys).doesNotHaveDuplicates();
      assertThat(uploadedKeys).anyMatch(k -> k.endsWith("-001.log.gz"));
      assertThat(uploadedKeys).anyMatch(k -> k.endsWith("-002.log.gz"));
      verify(metrics, times(2)).countUploaded();
    }
  }
}