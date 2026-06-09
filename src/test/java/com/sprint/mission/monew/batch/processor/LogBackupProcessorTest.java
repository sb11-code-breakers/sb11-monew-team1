package com.sprint.mission.monew.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.batch.dto.LogContent;
import com.sprint.mission.monew.batch.dto.UploadPayload;
import com.sprint.mission.monew.batch.util.BatchGzipUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogBackupProcessorTest {

  @InjectMocks
  private LogBackupProcessor processor;

  private LocalDate yesterday;

  @BeforeEach
  void setUp() {
    yesterday = LocalDate.now().minusDays(1);
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
    @DisplayName("compressedData가 null이면 null을 반환한다")
    void compressedData가_null이면_null을_반환한다() {
      // given
      UploadPayload payload = new UploadPayload("s3-key", null);

      // when & then
      assertThat(payload.compressedData()).isNull();
    }

    @Test
    @DisplayName("LogContent를 UploadPayload로 변환한다")
    void LogContent를_UploadPayload로_변환한다() throws Exception {
      // given
      byte[] lines = "log content".getBytes();
      LogContent item = new LogContent(yesterday, lines, 1);

      // when
      UploadPayload result = processor.process(item);

      // then
      assertThat(result).isNotNull();
      assertThat(result.s3Key()).isEqualTo(
          "logs/" + yesterday.format(BatchGzipUtils.PATH_FORMATTER)
              + "/app-" + yesterday.format(BatchGzipUtils.FILE_FORMATTER) + "-001.log.gz"
      );
      assertThat(result.compressedData()).isNotEmpty();
    }

    @Test
    @DisplayName("압축된 데이터를 원본으로 복원할 수 있다")
    void 압축된_데이터를_원본으로_복원할_수_있다() throws Exception {
      // given
      byte[] lines = "log content".getBytes(StandardCharsets.UTF_8);
      LogContent item = new LogContent(yesterday, lines, 1);

      // when
      UploadPayload result = processor.process(item);

      // then
      byte[] decompressed = decompress(result.compressedData());
      assertThat(new String(decompressed, StandardCharsets.UTF_8)).isEqualTo("log content");
    }
  }

  @Nested
  @DisplayName("LogContent 방어적 복사")
  class LogContentDefensiveCopy {

    @Test
    @DisplayName("lines() 조회 시 내부 배열이 변하지 않고 보호된다")
    void lines_getter_방어적_복사() {
      // given
      byte[] original = {1, 2, 3};
      LogContent content = new LogContent(yesterday, original, 1);

      // when
      byte[] copy = content.lines();
      original[0] = 99;
      copy[0] = 99;

      // then
      assertThat(content.lines()[0]).isEqualTo((byte) 1);
    }

    @Test
    @DisplayName("lines가 null이면 null을 반환한다")
    void lines가_null이면_null을_반환한다() {
      // given
      LogContent content = new LogContent(yesterday, null, 1);

      // when & then
      assertThat(content.lines()).isNull();
    }
  }

  private byte[] decompress(byte[] compressed) throws IOException {
    try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      gis.transferTo(out);
      return out.toByteArray();
    }
  }
}