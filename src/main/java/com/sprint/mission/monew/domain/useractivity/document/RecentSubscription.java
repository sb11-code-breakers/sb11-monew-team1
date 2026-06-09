package com.sprint.mission.monew.domain.useractivity.document;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecentSubscription {

  private UUID id;
  private UUID interestId;
  private String interestName;
  private List<String> interestKeywords;
  private long interestSubscriberCount;
  private Instant createdAt;

  public static RecentSubscription of(
      UUID id, UUID interestId, String interestName,
      List<String> interestKeywords, long interestSubscriberCount, Instant createdAt) {
    RecentSubscription doc = new RecentSubscription();
    doc.id = id;
    doc.interestId = interestId;
    doc.interestName = interestName;
    doc.interestKeywords = interestKeywords != null ? new ArrayList<>(interestKeywords) : new ArrayList<>();
    doc.interestSubscriberCount = interestSubscriberCount;
    doc.createdAt = createdAt;
    return doc;
  }
}