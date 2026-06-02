package com.sprint.mission.monew.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
public class SesConfig {

  @Value("${cloud.aws.region.static}")
  private String region;

  @Bean
  public SesClient sesClient() {
    return SesClient.builder()
        .region(Region.of(region))
        .build();
  }
}