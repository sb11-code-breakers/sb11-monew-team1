package com.sprint.mission.monew.domain.interest.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class InterestNotFoundException extends InterestException {

  private InterestNotFoundException(Map<String, Object> details) {
    super(HttpStatus.NOT_FOUND, InterestErrorCode.INTEREST_NOT_FOUND, details);
  }

  public static InterestNotFoundException withId(UUID interestId) {
    return new InterestNotFoundException(Map.of("interestId", interestId));
  }
}