package com.sprint.mission.monew.common.config;

import javax.sql.DataSource;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchDataSourceScriptDatabaseInitializer;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Isolation;

/**
 * [Spring Batch 5 설정]
 * PostgreSQL의 SERIALIZABLE 격리 수준에서 발생하는 동시 실행 트랜잭션 충돌 방지 설정.
 * log-upload와 news-collect가 매시 정각에 동시 실행되면서 PostgreSQL SSI 충돌이 발생하므로,
 * JobRepository 생성 격리 수준을 READ_COMMITTED로 조정합니다.
 *
 * DefaultBatchConfiguration 상속 시 BatchAutoConfiguration이 백오프되어
 * 메타데이터 테이블 생성을 위한 데이터베이스 이니셜라이저를 직접 등록합니다.
 */
@Configuration
@EnableConfigurationProperties(BatchProperties.class)
public class BatchConfig extends DefaultBatchConfiguration {

  @Override
  protected Isolation getIsolationLevelForCreate() {
    // JobRepository 생성 시 격리 수준을 READ_COMMITTED로 조정
    return Isolation.READ_COMMITTED;
  }

  @Bean
  public BatchDataSourceScriptDatabaseInitializer batchDataSourceInitializer(
      DataSource dataSource, BatchProperties properties) {
    return new BatchDataSourceScriptDatabaseInitializer(dataSource, properties.getJdbc());
  }
}