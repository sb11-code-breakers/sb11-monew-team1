package com.sprint.mission.monew.batch.log.backup.processor;

import com.sprint.mission.monew.batch.log.backup.dto.LogContent;
import com.sprint.mission.monew.batch.log.backup.dto.UploadPayload;
import com.sprint.mission.monew.batch.common.utils.BatchGzipUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogBackupProcessor implements ItemProcessor<LogContent, UploadPayload> {

  @Override
  public UploadPayload process(LogContent item) throws Exception {
    String s3Key = "logs/" + item.date().format(BatchGzipUtils.PATH_FORMATTER)
        + "/app-" + item.date().format(BatchGzipUtils.FILE_FORMATTER)
        + String.format("-%03d", item.pageNumber()) + ".log.gz";

    byte[] compressed = BatchGzipUtils.gzip(item.lines());

    return new UploadPayload(s3Key, compressed);
  }
}