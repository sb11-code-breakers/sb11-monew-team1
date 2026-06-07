package com.sprint.mission.monew.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.batch.BatchGzipUtils;
import com.sprint.mission.monew.batch.dto.UploadPayload;
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

@ExtendWith(MockitoExtension.class)
public class LogBackupProcessorTest {

  @TempDir
  Path tempDir;

  @InjectMocks
  private LogBackupProcessor processor;

  private LocalDate yesterday;
  private Path file;

  @BeforeEach
  void setUp() {
    yesterday = LocalDate.now().minusDays(1);
    file = tempDir
        .resolve("monew." + yesterday + ".log");
  }

  @Nested
  @DisplayName("백업 로그 파일 변환하기")
  class Processor {

    @Test
    @DisplayName("compressedData 조회 시 내부 배열이 변하지 않고 보호된다")
    void compressedData_getter_오버라이딩_방어적_복사() {
      // given
      byte[] original = {1, 2, 3};
      UploadPayload payload = new UploadPayload(
          Path.of("test.log"),
          "s3-key",
          original
      );

      // when
      byte[] copy = payload.compressedData();

      // 원본, 복사 배열 첫번째 값을 1에서 99로 설정
      original[0] = 99;
      copy[0] = 99;

      // then
      assertThat(payload.compressedData()[0]).isEqualTo((byte) 1);
    }

    @Test
    @DisplayName("백업 로그 파일 변환")
    void 백업_로그_파일_변환() throws Exception {
      // given
      // BeforeEach에서 로그 파일 초기화
      Files.writeString(file, "log content");

      // when
      UploadPayload result = processor.process(file);

      // then

      // UploadPayLoad 검증
      assertThat(result).isNotNull();
      assertThat(result.logFile()).isEqualTo(file);
      assertThat(result.s3Key()).isNotBlank();
      assertThat(result.compressedData()).isNotEmpty();

      // S3 key 검증
      assertThat(result.s3Key()).contains("logs/").contains(String.valueOf(yesterday.getYear()));

      String expectedKey =
          "logs/" + yesterday.format(BatchGzipUtils.PATH_FORMATTER)
              + "/app-" + yesterday.format(BatchGzipUtils.FILE_FORMATTER)
              + ".log.gz";

      assertThat(result.s3Key()).isEqualTo(expectedKey);
      assertThat(result.s3Key()).contains("logs/").contains(String.valueOf(yesterday.getYear()));

      // gzip로 압축됐는지 검증
      assertThat(result.compressedData()).isNotEmpty();
    }
  }
}
