package com.sprint.mission.monew.domain.useractivity.document;

import org.springframework.data.annotation.Id;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(collection = "user_activities")
@CompoundIndex(name = "idx_subscriptions_interestId", def = "{'subscriptions.interestId':1}")
public class UserActivity {

  @Id
  private UUID id;
  private String email;
  private String nickname;
  private Instant createdAt;
  private List<RecentSubscription> subscriptions = new ArrayList<>();
  private List<RecentComment> comments = new ArrayList<>();
  private List<RecentCommentLike> commentLikes = new ArrayList<>();
  private List<RecentArticleView> articleViews = new ArrayList<>();

  public static UserActivity of(UUID id, String email, String nickname, Instant createdAt) {
    UserActivity doc = new UserActivity();
    doc.id = id;
    doc.email = email;
    doc.nickname = nickname;
    doc.createdAt = createdAt;
    return doc;

  }


}
