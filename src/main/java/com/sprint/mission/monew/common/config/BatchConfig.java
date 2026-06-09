package com.sprint.mission.monew.common.config;

import javax.sql.DataSource;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchDataSourceScriptDatabaseInitializer;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Isolation;

// Spring Batch JobRepository 기본값은 ISOLATION_SERIALIZABLE이지만,
// log-upload와 news-collect가 매시 정각에 동시 실행되면서 PostgreSQL SSI 충돌 발생.
// Spring Batch는 version 컬럼 낙관적 락을 사용하므로 READ_COMMITTED/REPEATABLE_READ도
// 환경·동시성 요구사항에 따라 사용 가능하다 (Spring Batch 공식 문서 참고).
// DefaultBatchConfiguration 상속 시 BatchAutoConfiguration이 백오프되어
// BatchDataSourceScriptDatabaseInitializer도 꺼지므로 여기서 직접 등록한다.
@Configuration
@EnableConfigurationProperties(BatchProperties.class)
public class BatchConfig extends DefaultBatchConfiguration {

  @Override
  protected Isolation getIsolationLevelForCreate() {
    return Isolation.READ_COMMITTED;
  }

  @Bean
  public BatchDataSourceScriptDatabaseInitializer batchDataSourceInitializer(
      DataSource dataSource, BatchProperties properties) {
    return new BatchDataSourceScriptDatabaseInitializer(dataSource, properties.getJdbc());
  }
}