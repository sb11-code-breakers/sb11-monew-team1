package com.sprint.mission.monew.domain.useractivity.activityresponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionActivityResponse(
    UUID id,
    UUID interestId,
    String interestName,
    List<String> interestKeywords,
    long interestSubscriberCount,
    Instant createdAt
) {

}