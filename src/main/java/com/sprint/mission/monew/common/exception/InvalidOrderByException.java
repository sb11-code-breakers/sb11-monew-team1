package com.sprint.mission.monew.common.exception;

import org.springframework.beans.TypeMismatchException;

public class InvalidOrderByException extends TypeMismatchException {

  private final String detail;

  public InvalidOrderByException(Object value, Class<?> requiredType,
      String message) {
    super(value, requiredType);
    this.detail = message;
  }

  @Override
  public String getMessage() {
    return detail;
  }
}
