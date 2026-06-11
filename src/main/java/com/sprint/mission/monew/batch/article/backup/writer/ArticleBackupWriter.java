package com.sprint.mission.monew.batch.article.backup.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.monew.batch.article.backup.dto.ArticleBackupItem;
import com.sprint.mission.monew.batch.article.backup.exception.ArticleBackupFailedException;
import com.sprint.mission.monew.batch.article.backup.metrics.ArticleBackupMetrics;
import com.sprint.mission.monew.batch.common.utils.BatchGzipUtils;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class ArticleBackupWriter implements ItemWriter<ArticleBackupItem> {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final S3Client s3Client;
  private final ObjectMapper objectMapper;
  private final ArticleBackupMetrics metrics;

  @Value("${cloud.aws.s3.bucket}")
  private String bucket;

  private final LocalDate targetDate = LocalDate.now(KST).minusDays(1);
  private final AtomicInteger chunkCounter = new AtomicInteger(0);

  @Override
  public void write(Chunk<? extends ArticleBackupItem> chunk) throws Exception {
    int index = chunkCounter.incrementAndGet();
    String s3Key = BatchGzipUtils.articleS3Key(targetDate, index);

    try {
      byte[] compressed = BatchGzipUtils.gzip(objectMapper.writeValueAsBytes(chunk.getItems()));
      s3Client.putObject(
          PutObjectRequest.builder()
              .bucket(bucket)
              .key(s3Key)
              .contentType("application/gzip")
              .contentLength((long) compressed.length)
              .build(),
          RequestBody.fromBytes(compressed)
      );
      metrics.countUploaded();
      metrics.recordBytes(compressed.length);
      log.info("기사 청크 백업 완료: {} ({} 건)", s3Key, chunk.size());
    } catch (Exception e) {
      metrics.countFailed();
      throw ArticleBackupFailedException.withKey(s3Key, e);
    }
  }
}