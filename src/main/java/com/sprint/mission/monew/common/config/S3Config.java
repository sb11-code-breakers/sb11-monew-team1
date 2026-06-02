package com.sprint.mission.monew.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

  private final String region;

  public S3Config(@Value("${cloud.aws.region.static}") String region) {
    this.region = region;
  }

  @Bean
  public S3Client s3Client() {
    return S3Client.builder()
        .region(Region.of(region))
        .build();
  }
}
