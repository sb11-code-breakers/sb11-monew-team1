package com.sprint.mission.monew.common.config;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
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

    // 🟢 팀장님이 지시하신 PostgreSQL 데드락 방지 격리 수준 설정을 주입합니다.
    factory.setIsolationLevelForCreate("ISOLATION_READ_COMMITTED");

    factory.afterPropertiesSet();
    return factory.getObject();
  }
}