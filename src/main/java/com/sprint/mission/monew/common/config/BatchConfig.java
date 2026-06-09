package com.sprint.mission.monew.common.config;

import javax.sql.DataSource;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.boot.autoconfigure.batch.BatchDataSourceScriptDatabaseInitializer;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableConfigurationProperties(BatchProperties.class) // 배치 속성 활성화
public class BatchConfig {

  /**
   * PostgreSQL의 SERIALIZABLE 격리 수준에서 발생하는 동시 실행 트랜잭션 충돌 방지
   * JobRepository를 직접 등록하며 격리 수준을 ISOLATION_READ_COMMITTED로 조정합니다.
   */
  @Bean
  public JobRepository jobRepository(DataSource dataSource, PlatformTransactionManager transactionManager) throws Exception {
    JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
    factory.setDataSource(dataSource);
    factory.setTransactionManager(transactionManager);

    factory.setIsolationLevelForCreate("ISOLATION_READ_COMMITTED");

    factory.afterPropertiesSet();
    return factory.getObject();
  }

  /**
   * JobRepository 수동 등록 시 배치가 자동 비활성화되어
   * 스크립트가 실행되지 않는 현상을 방지하기 위해 이니셜라이저를 명시적으로 등록합니다.
   */
  @Bean
  public BatchDataSourceScriptDatabaseInitializer batchDataSourceInitializer(
      DataSource dataSource, BatchProperties properties) {
    return new BatchDataSourceScriptDatabaseInitializer(dataSource, properties.getJdbc());
  }
}