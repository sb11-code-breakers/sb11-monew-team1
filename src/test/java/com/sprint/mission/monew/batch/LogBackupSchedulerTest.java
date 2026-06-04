package com.sprint.mission.monew.batch;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogBackupSchedulerTest {

  @InjectMocks
  LogBackupScheduler logBackupScheduler;
  @Mock
  LogBackupService logBackupService;

  @Nested
  @DisplayName("로그 파일 S3 업로드 스케줄러")
  class UploadLogs {

    @Test
    @DisplayName("uploadLogs 호출 시 LogBackupService에 위임한다")
    void uploadLogs_호출_시_서비스에_위임한다() {
      // when
      logBackupScheduler.uploadLogs();

      // then
      verify(logBackupService).upload();
    }
  }
}
