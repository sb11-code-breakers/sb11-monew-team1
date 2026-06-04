package com.sprint.mission.monew.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MonewInternalExceptionHandler {

  public void handle(Throwable e) {
    if (e instanceof MonewInternalException mie) {
      log.error("[{}] {}", mie.getErrorCode().name(), mie.getMessage(), mie);
    } else {
      log.error("[UNKNOWN] {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
    }
  }
}