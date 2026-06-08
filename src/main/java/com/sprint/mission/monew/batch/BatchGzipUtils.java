package com.sprint.mission.monew.batch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPOutputStream;

public final class BatchGzipUtils {

  public static final DateTimeFormatter PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
  public static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

  private BatchGzipUtils() {}

  public static String articleS3Key(LocalDate date) {
    return "articles/" + date.format(PATH_FORMATTER)
        + "/articles-" + date.format(FILE_FORMATTER) + ".json.gz";
  }

  public static String logS3Key(LocalDate date) {
    return "logs/" + date.format(PATH_FORMATTER)
        + "/app-" + date.format(FILE_FORMATTER) + ".log.gz";
  }

  public static byte[] gzip(byte[] data) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (GZIPOutputStream gzos = new GZIPOutputStream(bos)) {
      gzos.write(data);
    }
    return bos.toByteArray();
  }
}
