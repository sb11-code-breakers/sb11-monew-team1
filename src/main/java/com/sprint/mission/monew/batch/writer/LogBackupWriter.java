package com.sprint.mission.monew.batch.writer;

import com.sprint.mission.monew.batch.exception.LogBackupDeleteFailedException;
import com.sprint.mission.monew.batch.exception.LogBackupFailedException;
import com.sprint.mission.monew.batch.metrics.LogBackupMetrics;
import com.sprint.mission.monew.batch.dto.UploadPayload;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogBackupWriter implements ItemWriter<UploadPayload> {

  private final S3Client s3Client;
  private final LogBackupMetrics metrics;

  @Value("${cloud.aws.s3.bucket}")
  private String bucket;

  @Override
  public void write(Chunk<? extends UploadPayload> chunk) {

    long start = System.nanoTime();

    try {
      for (UploadPayload item : chunk) {

        if (exists(item.s3Key())) {
          log.info("이미 존재 → skip: {}", item.s3Key());
          metrics.countSkipped();
          deleteLocalFile(item);

          continue;
        }

        doUpload(item);
        deleteLocalFile(item);
      }
    } finally {
      metrics.recordDuration(Duration.ofNanos(System.nanoTime() - start));
    }
  }

  private boolean exists(String key) {
    try {
      s3Client.headObject(b -> b.bucket(bucket).key(key));
      return true;
    } catch (NoSuchKeyException e) {
      return false;
    } catch (Exception e) {
      log.error("S3 존재 여부 확인 중 오류 발생: {}", key, e);
      throw LogBackupFailedException.withKey(key, e);
    }
  }

  private void doUpload(UploadPayload item) {
    try {
      s3Client.putObject(
          PutObjectRequest.builder()
              .bucket(bucket)
              .key(item.s3Key())
              .contentType("application/gzip")
              .contentLength((long) item.compressedData().length)
              .build(),
          RequestBody.fromBytes(item.compressedData())
      );

      metrics.countUploaded();
      metrics.recordBytes(item.compressedData().length);
      log.info("업로드 완료: {}", item.s3Key());

    } catch (Exception e) {
      metrics.countFailed();
      throw LogBackupFailedException.withKey(item.s3Key(), e);
    }
  }

  private void deleteLocalFile(UploadPayload item) {
    try {
      Files.delete(item.logFile());
      log.info("로컬 로그 파일 삭제: {}", item.logFile());
    } catch (IOException e) {
      throw LogBackupDeleteFailedException.withPath(item.logFile(), e);
    }
  }

}
