package com.sprint.mission.monew.batch.common.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
public class SkipLoggingListenerTest {

  @InjectMocks
  private SkipLoggingListener listener;

  private ListAppender<ILoggingEvent> appender;
  private Logger logger;

  @BeforeEach
  void setUp() {
    logger = (Logger) LoggerFactory.getLogger(SkipLoggingListener.class);

    appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(appender);
    appender.stop();
  }

  @Test
  @DisplayName("Read 단계에서 Skip 발생 시 로그가 출력된다")
  void read_단계_스킵_시_로그출력() {
    // given
    RuntimeException exception = new RuntimeException("Read 실패");

    // when
    listener.onSkipInRead(exception);

    // then
    assertFalse(appender.list.isEmpty());
    ILoggingEvent logEvent = appender.list.get(0);
    assertThat(logEvent.getLevel()).isEqualTo(Level.ERROR);
    assertThat(logEvent.getFormattedMessage()).contains("배치 Reader skip");
  }

  @Test
  @DisplayName("Process 단계에서 Skip 발생 시 로그가 출력된다")
  void process_단계_스킵_시_로그출력() {
    // given
    RuntimeException exception = new RuntimeException("Process 실패");

    // when
    listener.onSkipInProcess("item", exception);

    // then
    assertFalse(appender.list.isEmpty());
    ILoggingEvent logEvent = appender.list.get(0);
    assertThat(logEvent.getLevel()).isEqualTo(Level.ERROR);
    assertThat(logEvent.getFormattedMessage()).contains("배치 Processor skip");
  }

  @Test
  @DisplayName("Write 단계에서 Skip 발생 시 로그가 출력된다")
  void write_단계_스킵_시_로그출력() {
    // given
    RuntimeException exception = new RuntimeException("Write 실패");

    // when
    listener.onSkipInWrite("item", exception);

    // then
    assertFalse(appender.list.isEmpty());
    ILoggingEvent logEvent = appender.list.get(0);
    assertThat(logEvent.getLevel()).isEqualTo(Level.ERROR);
    assertThat(logEvent.getFormattedMessage()).contains("배치 Writer skip");
  }
}
