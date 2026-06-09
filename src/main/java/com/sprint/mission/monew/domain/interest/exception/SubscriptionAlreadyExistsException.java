package com.sprint.mission.monew.domain.interest.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class SubscriptionAlreadyExistsException extends InterestException {

  private SubscriptionAlreadyExistsException(Map<String, Object> details) {
    super(HttpStatus.CONFLICT, InterestErrorCode.SUBSCRIPTION_ALREADY_EXISTS, details);
  }

  public static SubscriptionAlreadyExistsException withIds(UUID interestId, UUID userId) {
    return new SubscriptionAlreadyExistsException(
        Map.of("interestId", interestId, "userId", userId));
  }
}