package com.sprint.mission.monew.domain.interest.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import com.sprint.mission.monew.common.exception.MonewException;
import java.util.Map;
import java.util.UUID;

public class SubscriptionNotFoundException extends MonewException {

  private SubscriptionNotFoundException(Map<String, Object> details) {
    super(ErrorCode.SUBSCRIPTION_NOT_FOUND, details);
  }

  public static SubscriptionNotFoundException withIds(UUID interestId, UUID userId) {
    return new SubscriptionNotFoundException(
        Map.of("interestId", interestId, "userId", userId));
  }
}
