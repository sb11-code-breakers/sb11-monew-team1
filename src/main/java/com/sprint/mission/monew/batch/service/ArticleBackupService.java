package com.sprint.mission.monew.batch.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.monew.batch.dto.ArticleBackupDto;
import com.sprint.mission.monew.batch.exception.ArticleBackupFailedException;
import com.sprint.mission.monew.batch.metrics.ArticleBackupMetrics;
import com.sprint.mission.monew.batch.util.BatchGzipUtils;
import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleBackupService {

  private final ArticleRepository articleRepository;
  private final S3Client s3Client;
  private final ObjectMapper objectMapper;
  private final ArticleBackupMetrics metrics;

  @Value("${cloud.aws.s3.bucket}")
  private String bucket;

  public void backup() {
    long startNanos = System.nanoTime();
    try {
      doBackup();
    } finally {
      metrics.recordDuration(Duration.ofNanos(System.nanoTime() - startNanos));
    }
  }

  private void doBackup() {
    LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
    Instant from = yesterday.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant to = yesterday.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

    List<Article> articles =
        articleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(
            from, to);

    if (articles.isEmpty()) {
      log.info("전날({}) 기사 없음, 백업 skip", yesterday);
      return;
    }

    String s3Key = BatchGzipUtils.articleS3Key(yesterday);

    try {
      s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(s3Key).build());
      log.info("이미 업로드됨, skip: {}", s3Key);
      metrics.countSkipped();
      return;
    } catch (NoSuchKeyException ignored) {
      // 업로드 진행
    } catch (S3Exception e) {
      if (e.statusCode() != 404) {
        log.error("S3 headObject 실패 (key={}): {}", s3Key, e.getMessage());
        metrics.countFailed();
        throw ArticleBackupFailedException.withKey(s3Key, e);
      }
    }

    try {
      List<ArticleBackupDto> dtos = articles.stream().map(ArticleBackupDto::from).toList();
      byte[] compressed = BatchGzipUtils.gzip(serialize(dtos));
      s3Client.putObject(
          PutObjectRequest.builder()
              .bucket(bucket)
              .key(s3Key)
              .contentType("application/gzip")
              .contentLength((long) compressed.length)
              .build(),
          RequestBody.fromBytes(compressed));
      metrics.countUploaded();
      metrics.recordBytes((long) compressed.length);
      log.info("기사 백업 완료: {} ({} 건)", s3Key, articles.size());
    } catch (Exception e) {
      metrics.countFailed();
      throw ArticleBackupFailedException.withKey(s3Key, e);
    }
  }

  private byte[] serialize(List<ArticleBackupDto> dtos) throws JsonProcessingException {
    return objectMapper.writeValueAsBytes(dtos);
  }
}
