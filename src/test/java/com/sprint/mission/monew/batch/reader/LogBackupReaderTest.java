package com.sprint.mission.monew.batch.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class LogBackupReaderTest {

  @TempDir
  Path tempDir;

  @InjectMocks
  private LogBackupReader reader;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(reader, "logDir", tempDir.toString());
  }

  @Nested
  @DisplayName("백업 로그 파일 읽기")
  class Reader {

    @Test
    @DisplayName("전날 로그 파일이 없으면 null을 반환한다")
    void 전날_로그_파일이_없으면_null을_반환한다() throws Exception {
      // given
      // tempDir에 로그 파일 없는 상태

      // when
      Path result = reader.read();

      // then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("로그 파일이 존재하면 Path를 반환한다")
    void 로그_파일이_존재하면_Path를_반환한다() throws Exception {
      // given
      LocalDate yesterday = LocalDate.now().minusDays(1);
      Path logFile = tempDir.resolve("monew." + yesterday + ".log");
      Files.writeString(logFile, "log content");

      // when
      Path result = reader.read();

      // then
      assertThat(result).isNotNull();
      assertThat(result).isEqualTo(logFile);
    }

    @Test
    @DisplayName("read()를 두 번 호출하면 두 번째는 null을 반환한다")
    void read를_두_번_호출하면_두_번째는_null을_반환한다() throws Exception {
      // given
      LocalDate yesterday = LocalDate.now().minusDays(1);
      Path logFile = tempDir.resolve("monew." + yesterday + ".log");
      Files.writeString(logFile, "log content");

      // when
      Path firstResult = reader.read();
      Path secondResult = reader.read();

      // then
      assertThat(firstResult).isNotNull();
      assertThat(secondResult).isNull();
    }
  }

}
