package com.sprint.mission.monew.batch.dto;

import java.nio.file.Path;

public record UploadPayload(
    Path logFile,
    String s3Key,
    byte[] compressedData
) {

  public UploadPayload {
    compressedData = compressedData == null ? null : compressedData.clone();
  }

  @Override
  public byte[] compressedData() {
    return compressedData == null ? null : compressedData.clone();
  }

}
