package com.sprint.mission.monew.batch.article.backup.scheduler;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

import com.sprint.mission.monew.batch.article.backup.service.ArticleBackupService;
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
    @DisplayName("executeBackup 호출 시 ArticleBackupService에 위임한다")
    void executeBackup_호출_시_서비스에_위임한다() throws Exception {
      // when
      articleBackupScheduler.executeBackup();

      // then
      then(articleBackupService).should().executeBackup();
    }

    @Test
    @DisplayName("executeBackup 중 예외 발생 시 외부로 전파한다")
    void executeBackup_중_예외_발생_시_외부로_전파한다() throws Exception {
      // given
      doThrow(new RuntimeException("배치 실패")).when(articleBackupService).executeBackup();

      // when & then
      assertThatThrownBy(() -> articleBackupScheduler.executeBackup())
          .isInstanceOf(RuntimeException.class);
    }
  }
}