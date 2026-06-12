package com.sprint.mission.monew.domain.user.document;

import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(collection = "user_unlock_tokens")
public class UserUnlockToken {

  private static final long EXPIRY_HOURS = 24;

  @Id
  private UUID id;
  private UUID userId;
  private String token;

  @Indexed(expireAfter = "0s")
  private Instant expiredAt;

  public static UserUnlockToken create(UUID userId) {
    UserUnlockToken unlockToken = new UserUnlockToken();
    unlockToken.id = UUID.randomUUID();
    unlockToken.userId = userId;
    unlockToken.token = UUID.randomUUID().toString();
    unlockToken.expiredAt = Instant.now().plusSeconds(EXPIRY_HOURS * 3600);
    return unlockToken;
  }
}