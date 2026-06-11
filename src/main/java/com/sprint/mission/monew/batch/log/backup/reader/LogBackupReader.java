package com.sprint.mission.monew.batch.log.backup.reader;

import com.sprint.mission.monew.batch.log.backup.dto.LogContent;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.FilterLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.FilterLogEventsResponse;

@Slf4j
@StepScope
@Component
@RequiredArgsConstructor
public class LogBackupReader implements ItemReader<LogContent> {

  private final CloudWatchLogsClient cloudWatchLogsClient;

  @Value("${monew.log-group}")
  private String logGroup;

  @Value("${monew.log-stream-prefix}")
  private String logStreamPrefix;

  private LocalDate date;
  private long startTime;
  private long endTime;
  private String nextToken;
  private int pageNumber = 0;
  private boolean initialized = false;
  private boolean done = false;

  @Override
  public LogContent read() {
    if (done) {
      return null;
    }

    if (!initialized) {
      date = LocalDate.now(ZoneOffset.UTC).minusDays(1);
      startTime = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
      endTime = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() - 1;
      initialized = true;
    }

    while (!done) {
      FilterLogEventsRequest.Builder requestBuilder = FilterLogEventsRequest.builder()
          .logGroupName(logGroup)
          .logStreamNamePrefix(logStreamPrefix)
          .startTime(startTime)
          .endTime(endTime);
      if (nextToken != null) {
        requestBuilder.nextToken(nextToken);
      }

      FilterLogEventsResponse response = cloudWatchLogsClient.filterLogEvents(requestBuilder.build());
      List<String> lines = response.events().stream()
          .map(e -> e.message())
          .toList();

      nextToken = response.nextToken();
      if (nextToken == null) {
        done = true;
      }

      if (!lines.isEmpty()) {
        pageNumber++;
        byte[] content = String.join("\n", lines).getBytes(StandardCharsets.UTF_8);
        return new LogContent(date, content, pageNumber);
      }
    }

    if (pageNumber == 0) {
      log.warn("CloudWatch에서 어제({}) 로그를 찾을 수 없음: {}", date, logGroup);
    }
    return null;
  }
}