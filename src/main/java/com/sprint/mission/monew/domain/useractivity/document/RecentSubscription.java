package com.sprint.mission.monew.domain.useractivity.document;

import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecentSubscription {

  private UUID interestId;
  private String interestName;
  private Instant subscribedAt;
  public static RecentSubscription of(
      UUID interestId, String interestName, Instant subscribedAt) {
    RecentSubscription doc = new RecentSubscription();
    doc.interestId = interestId;
    doc.interestName = interestName;
    doc.subscribedAt = subscribedAt;
    return doc;
  }
}