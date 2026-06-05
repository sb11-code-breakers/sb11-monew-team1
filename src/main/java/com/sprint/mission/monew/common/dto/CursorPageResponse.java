package com.sprint.mission.monew.common.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CursorPageResponse<T>(
    List<T> content,
    String nextCursor,
    Instant nextAfter,
    UUID nextIdAfter,
    boolean hasNext,
    int size,
    Long totalElements) {

  public static <T> CursorPageResponse<T> of(
      List<T> content,
      String nextCursor,
      Instant nextAfter,
      UUID nextIdAfter,
      boolean hasNext,
      int size,
      Long totalElements) {
    return new CursorPageResponse<>(content, nextCursor, nextAfter, nextIdAfter, hasNext, size, totalElements);
  }
}