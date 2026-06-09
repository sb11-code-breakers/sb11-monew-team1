package com.sprint.mission.monew.batch.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.batch.dto.LogContent;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.FilterLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.FilterLogEventsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.FilteredLogEvent;

@ExtendWith(MockitoExtension.class)
class LogBackupReaderTest {

  @Mock
  private CloudWatchLogsClient cloudWatchLogsClient;

  @InjectMocks
  private LogBackupReader reader;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(reader, "logGroup", "test-log-group");
  }

  @Nested
  @DisplayName("CloudWatch 로그 읽기")
  class Read {

    @Test
    @DisplayName("단일 페이지를 읽으면 pageNumber=1인 LogContent를 반환한다")
    void 단일_페이지를_읽으면_pageNumber_1인_LogContent를_반환한다() throws Exception {
      // given
      LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
      FilterLogEventsResponse response = FilterLogEventsResponse.builder()
          .events(List.of(FilteredLogEvent.builder().message("line1").build()))
          .build();
      given(cloudWatchLogsClient.filterLogEvents(any(FilterLogEventsRequest.class)))
          .willReturn(response);

      // when
      LogContent result = reader.read();

      // then
      assertThat(result).isNotNull();
      assertThat(result.pageNumber()).isEqualTo(1);
      assertThat(result.date()).isEqualTo(yesterday);
      assertThat(new String(result.lines(), StandardCharsets.UTF_8)).isEqualTo("line1");
    }

    @Test
    @DisplayName("두 번째 read()는 nextToken을 전달해 pageNumber=2를 반환한다")
    void 두_번째_read는_nextToken으로_pageNumber_2를_반환한다() throws Exception {
      // given
      FilterLogEventsResponse firstPage = FilterLogEventsResponse.builder()
          .events(List.of(FilteredLogEvent.builder().message("line1").build()))
          .nextToken("token123")
          .build();
      FilterLogEventsResponse secondPage = FilterLogEventsResponse.builder()
          .events(List.of(FilteredLogEvent.builder().message("line2").build()))
          .build();
      given(cloudWatchLogsClient.filterLogEvents(any(FilterLogEventsRequest.class)))
          .willReturn(firstPage, secondPage);

      // when
      reader.read();
      LogContent second = reader.read();

      // then
      assertThat(second).isNotNull();
      assertThat(second.pageNumber()).isEqualTo(2);
      assertThat(new String(second.lines(), StandardCharsets.UTF_8)).isEqualTo("line2");

      ArgumentCaptor<FilterLogEventsRequest> reqCaptor =
          ArgumentCaptor.forClass(FilterLogEventsRequest.class);
      verify(cloudWatchLogsClient, times(2)).filterLogEvents(reqCaptor.capture());
      assertThat(reqCaptor.getAllValues().get(0).nextToken()).isNull();
      assertThat(reqCaptor.getAllValues().get(1).nextToken()).isEqualTo("token123");
    }

    @Test
    @DisplayName("마지막 페이지 이후 read()는 null을 반환한다")
    void 마지막_페이지_이후_read는_null을_반환한다() throws Exception {
      // given
      FilterLogEventsResponse onlyPage = FilterLogEventsResponse.builder()
          .events(List.of(FilteredLogEvent.builder().message("line1").build()))
          .build();
      given(cloudWatchLogsClient.filterLogEvents(any(FilterLogEventsRequest.class)))
          .willReturn(onlyPage);

      // when
      reader.read();
      LogContent afterLast = reader.read();

      // then
      assertThat(afterLast).isNull();
    }

    @Test
    @DisplayName("CloudWatch에 어제 로그 이벤트가 없으면 null을 반환한다")
    void CloudWatch에_어제_로그_이벤트가_없으면_null을_반환한다() throws Exception {
      // given
      FilterLogEventsResponse emptyResponse = FilterLogEventsResponse.builder()
          .events(Collections.emptyList())
          .build();
      given(cloudWatchLogsClient.filterLogEvents(any(FilterLogEventsRequest.class)))
          .willReturn(emptyResponse);

      // when
      LogContent result = reader.read();

      // then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("빈 페이지여도 nextToken이 있으면 다음 페이지를 계속 조회한다")
    void 빈_페이지여도_nextToken이_있으면_다음_페이지를_계속_조회한다() throws Exception {
      // given
      FilterLogEventsResponse emptyFirstPage = FilterLogEventsResponse.builder()
          .events(Collections.emptyList())
          .nextToken("token123")
          .build();
      FilterLogEventsResponse secondPage = FilterLogEventsResponse.builder()
          .events(List.of(FilteredLogEvent.builder().message("line2").build()))
          .build();
      given(cloudWatchLogsClient.filterLogEvents(any(FilterLogEventsRequest.class)))
          .willReturn(emptyFirstPage, secondPage);

      // when
      LogContent result = reader.read();

      // then
      assertThat(result).isNotNull();
      assertThat(new String(result.lines(), StandardCharsets.UTF_8)).isEqualTo("line2");
    }
  }
}