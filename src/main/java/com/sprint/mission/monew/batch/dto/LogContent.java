package com.sprint.mission.monew.batch.dto;

import java.time.LocalDate;

public record LogContent(
    LocalDate date,
    byte[] lines,
    int pageNumber
) {

  public LogContent {
    lines = lines == null ? null : lines.clone();
  }

  @Override
  public byte[] lines() {
    return lines == null ? null : lines.clone();
  }

}