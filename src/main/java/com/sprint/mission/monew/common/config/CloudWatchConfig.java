package com.sprint.mission.monew.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;

@Configuration
public class CloudWatchConfig {

  private final String region;

  public CloudWatchConfig(@Value("${cloud.aws.region.static}") String region) {
    this.region = region;
  }

  @Bean
  public CloudWatchLogsClient cloudWatchLogsClient() {
    return CloudWatchLogsClient.builder()
        .region(Region.of(region))
        .build();
  }

  @Bean
  @ConditionalOnProperty(name = "management.cloudwatch.metrics.export.enabled", havingValue = "true")
  public CloudWatchAsyncClient cloudWatchAsyncClient() {
    return CloudWatchAsyncClient.builder()
        .region(Region.of(region))
        .build();
  }
}