package com.sprint.mission.monew.batch.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BatchExceptionHandler {

  public void handle(Throwable e) {
    if (e instanceof BatchException be) {
      log.error("[{}] {}", be.getErrorCode().name(), be.getMessage(), be);
    } else {
      log.error("[UNKNOWN] {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
    }
  }
}