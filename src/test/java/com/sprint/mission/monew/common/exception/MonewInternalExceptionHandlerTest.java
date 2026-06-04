package com.sprint.mission.monew.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sprint.mission.monew.batch.LogBackupFailedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class MonewInternalExceptionHandlerTest {

  MonewInternalExceptionHandler handler = new MonewInternalExceptionHandler();

  Logger logger;
  ListAppender<ILoggingEvent> listAppender;

  @BeforeEach
  void setUp() {
    logger = (Logger) LoggerFactory.getLogger(MonewInternalExceptionHandler.class);
    listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(listAppender);
  }

  @Nested
  @DisplayName("handle")
  class Handle {

    @Test
    @DisplayName("MonewInternalException 발생 시 [ErrorCode] 형식으로 ERROR 레벨 로그를 기록한다")
    void MonewInternalException_발생_시_ErrorCode_형식_ERROR_레벨_로그_기록() {
      // given
      MonewInternalException ex = LogBackupFailedException.withKey(
          "logs/2025-01-01/monew.2025-01-01.log", new RuntimeException("S3 오류"));

      // when
      handler.handle(ex);

      // then
      assertThat(listAppender.list)
          .anyMatch(e -> e.getLevel() == Level.ERROR
              && e.getFormattedMessage().contains("LOG_BACKUP_FAILED")
              && e.getFormattedMessage().contains("로그 파일 S3 업로드 실패"));
    }

    @Test
    @DisplayName("예상치 못한 예외 발생 시 [UNKNOWN] 형식으로 ERROR 레벨 로그를 기록한다")
    void 예상치_못한_예외_발생_시_UNKNOWN_형식_ERROR_레벨_로그_기록() {
      // given
      RuntimeException ex = new RuntimeException("unexpected");

      // when
      handler.handle(ex);

      // then
      assertThat(listAppender.list)
          .anyMatch(e -> e.getLevel() == Level.ERROR
              && e.getFormattedMessage().contains("UNKNOWN")
              && e.getFormattedMessage().contains("unexpected"));
    }
  }
}