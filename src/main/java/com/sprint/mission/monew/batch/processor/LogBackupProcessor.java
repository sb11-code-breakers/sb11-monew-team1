package com.sprint.mission.monew.batch.processor;

import com.sprint.mission.monew.batch.BatchGzipUtils;
import com.sprint.mission.monew.batch.dto.UploadPayload;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogBackupProcessor implements ItemProcessor<Path, UploadPayload> {


  @Override
  public UploadPayload process(Path file) throws Exception {
    LocalDate date = extractDate(file);

    String s3Key = "logs/" + date.format(BatchGzipUtils.PATH_FORMATTER)
        + "/app-" + date.format(BatchGzipUtils.FILE_FORMATTER) + ".log.gz";

    byte[] compressed = BatchGzipUtils.gzip(Files.readAllBytes(file));

    return new UploadPayload(file, s3Key, compressed);
  }

  private LocalDate extractDate(Path file) {
    String name = file.getFileName().toString();
    String dateStr = name.replace("monew.", "").replace(".log", "");
    return LocalDate.parse(dateStr);
  }

}
