package com.sprint.mission.monew.batch.dto;

public record UploadPayload(
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