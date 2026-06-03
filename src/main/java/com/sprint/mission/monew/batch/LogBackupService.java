package com.sprint.mission.monew.batch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPOutputStream;
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
public class LogBackupService {

  private static final DateTimeFormatter PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
  private static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

  private final S3Client s3Client;

  @Value("${cloud.aws.s3.bucket}")
  private String bucket;

  @Value("${monew.log-dir}")
  private String logDir;

  public void upload() {
    LocalDate yesterday = LocalDate.now().minusDays(1);
    Path logFile = Path.of(logDir, "monew." + yesterday + ".log");

    if (!Files.exists(logFile)) {
      log.warn("로그 파일 없음: {}", logFile);
      return;
    }

    String s3Key = "logs/" + yesterday.format(PATH_FORMATTER)
        + "/app-" + yesterday.format(FILE_FORMATTER) + ".log.gz";

    try {
      s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(s3Key).build());
      log.info("이미 업로드됨, 로컬 파일만 삭제: {}", s3Key);
      deleteLocalFile(logFile);
      return;
    } catch (NoSuchKeyException ignored) {
      // 업로드 진행
    } catch (S3Exception e) {
      if (e.statusCode() != 404) {
        throw LogBackupFailedException.withKey(s3Key, e);
      }
    }

    try {
      byte[] compressed = gzip(Files.readAllBytes(logFile));
      s3Client.putObject(
          PutObjectRequest.builder()
              .bucket(bucket)
              .key(s3Key)
              .contentType("application/gzip")
              .contentLength((long) compressed.length)
              .build(),
          RequestBody.fromBytes(compressed));
      log.info("업로드 완료: {}", s3Key);
    } catch (Exception e) {
      throw LogBackupFailedException.withKey(s3Key, e);
    }

    deleteLocalFile(logFile);
  }

  private byte[] gzip(byte[] data) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (GZIPOutputStream gzos = new GZIPOutputStream(bos)) {
      gzos.write(data);
    }
    return bos.toByteArray();
  }

  private void deleteLocalFile(Path logFile) {
    try {
      Files.delete(logFile);
      log.info("로컬 로그 파일 삭제: {}", logFile);
    } catch (IOException e) {
      throw LogBackupDeleteFailedException.withPath(logFile, e);
    }
  }
}