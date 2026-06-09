package com.sprint.mission.monew.batch.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.batch.service.ArticleBackupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleBackupSchedulerTest {

  @InjectMocks
  ArticleBackupScheduler articleBackupScheduler;
  @Mock
  ArticleBackupService articleBackupService;

  @Nested
  @DisplayName("기사 S3 백업 스케줄러")
  class BackupSchedule {

    @Test
    @DisplayName("backup 호출 시 ArticleBackupService에 위임한다")
    void backup_호출_시_서비스에_위임한다() {
      // when
      articleBackupScheduler.backup();

      // then
      verify(articleBackupService).backup();
    }

    @Test
    @DisplayName("backup 중 예외 발생 시 예외를 외부로 전파하지 않는다")
    void backup_중_예외_발생_시_전파하지_않는다() {
      // given
      doThrow(new RuntimeException("S3 연결 실패")).when(articleBackupService).backup();

      // when & then
      assertThatCode(() -> articleBackupScheduler.backup()).doesNotThrowAnyException();
    }
  }
}
