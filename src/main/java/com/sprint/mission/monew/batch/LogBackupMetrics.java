package com.sprint.mission.monew.batch;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class LogBackupMetrics {

  private static final String UPLOADED = "monew.log.backup.uploaded";
  private static final String SKIPPED = "monew.log.backup.skipped";
  private static final String FAILED = "monew.log.backup.failed";
  private static final String BYTES = "monew.log.backup.bytes";
  private static final String DURATION = "monew.log.backup.duration";

  private final Counter uploadedCounter;
  private final Counter skippedCounter;
  private final Counter failedCounter;
  private final Counter bytesCounter;
  private final Timer durationTimer;

  public LogBackupMetrics(MeterRegistry registry) {
    this.uploadedCounter = Counter.builder(UPLOADED)
        .description("S3에 업로드된 로그 파일 수")
        .register(registry);
    this.skippedCounter = Counter.builder(SKIPPED)
        .description("이미 존재해 건너뛴 로그 백업 수")
        .register(registry);
    this.failedCounter = Counter.builder(FAILED)
        .description("실패한 로그 백업 수")
        .register(registry);
    this.bytesCounter = Counter.builder(BYTES)
        .baseUnit("bytes")
        .description("S3에 업로드된 압축 로그 총 바이트")
        .register(registry);
    this.durationTimer = Timer.builder(DURATION)
        .description("로그 백업 1회 소요 시간")
        .register(registry);
  }

  public void countUploaded() {
    uploadedCounter.increment();
  }

  public void countSkipped() {
    skippedCounter.increment();
  }

  public void countFailed() {
    failedCounter.increment();
  }

  public void recordBytes(long bytes) {
    bytesCounter.increment(bytes);
  }

  public void recordDuration(Duration duration) {
    durationTimer.record(duration);
  }
}