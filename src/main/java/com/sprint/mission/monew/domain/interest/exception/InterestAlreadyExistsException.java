package com.sprint.mission.monew.domain.interest.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class InterestAlreadyExistsException extends InterestException {

  private InterestAlreadyExistsException(Map<String, Object> details) {
    super(HttpStatus.CONFLICT, InterestErrorCode.INTEREST_ALREADY_EXISTS, details);
  }

  public static InterestAlreadyExistsException withName(String name) {
    return new InterestAlreadyExistsException(Map.of("name", name));
  }
}
