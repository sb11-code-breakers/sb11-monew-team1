package com.sprint.mission.monew.batch.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class ArticleBackupMetrics {

  private static final String UPLOADED = "monew.article.backup.uploaded";
  private static final String SKIPPED = "monew.article.backup.skipped";
  private static final String FAILED = "monew.article.backup.failed";
  private static final String BYTES = "monew.article.backup.bytes";
  private static final String DURATION = "monew.article.backup.duration";

  private final Counter uploadedCounter;
  private final Counter skippedCounter;
  private final Counter failedCounter;
  private final Counter bytesCounter;
  private final Timer durationTimer;

  public ArticleBackupMetrics(MeterRegistry registry) {
    this.uploadedCounter = Counter.builder(UPLOADED)
        .description("S3에 업로드된 기사 백업 건수")
        .register(registry);
    this.skippedCounter = Counter.builder(SKIPPED)
        .description("이미 존재해 건너뛴 기사 백업 수")
        .register(registry);
    this.failedCounter = Counter.builder(FAILED)
        .description("실패한 기사 백업 수")
        .register(registry);
    this.bytesCounter = Counter.builder(BYTES)
        .baseUnit("bytes")
        .description("S3에 업로드된 압축 기사 백업 총 바이트")
        .register(registry);
    this.durationTimer = Timer.builder(DURATION)
        .description("기사 백업 1회 소요 시간")
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
