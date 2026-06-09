package com.sprint.mission.monew.domain.interest.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class SubscriptionNotFoundException extends InterestException {

  private SubscriptionNotFoundException(Map<String, Object> details) {
    super(HttpStatus.NOT_FOUND, InterestErrorCode.SUBSCRIPTION_NOT_FOUND, details);
  }

  public static SubscriptionNotFoundException withIds(UUID interestId, UUID userId) {
    return new SubscriptionNotFoundException(
        Map.of("interestId", interestId, "userId", userId));
  }
}