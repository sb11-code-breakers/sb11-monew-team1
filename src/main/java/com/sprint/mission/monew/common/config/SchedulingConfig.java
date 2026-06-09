package com.sprint.mission.monew.common.config;

import com.sprint.mission.monew.batch.exception.BatchExceptionHandler;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestClient;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulingConfig {

  private final BatchExceptionHandler exceptionHandler;

  @Bean
  public ThreadPoolTaskScheduler taskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(2);
    scheduler.setThreadNamePrefix("scheduler-");
    scheduler.setErrorHandler(exceptionHandler::handle);
    scheduler.initialize();
    return scheduler;
  }

  @Bean
  public RestClient restClient() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(5));
    factory.setReadTimeout(Duration.ofSeconds(10));
    return RestClient.builder()
        .requestFactory(factory)
        .build();
  }
}
