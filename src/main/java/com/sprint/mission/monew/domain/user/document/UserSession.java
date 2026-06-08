package com.sprint.mission.monew.domain.user.document;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(collection = "user_sessions")
public class UserSession {

  @Id
  private UUID id;

  private UUID userId;

  private String ip;

  private String deviceFingerprint;

  private Instant lastAccessedAt;

  @Indexed(expireAfter = "0s")
  private Instant expiresAt;

  public static UserSession create(UUID userId, String ip, String deviceFingerprint,
      int timeoutMinutes) {
    UserSession session = new UserSession();
    session.id = UUID.randomUUID();
    session.userId = userId;
    session.ip = ip;
    session.deviceFingerprint = deviceFingerprint;
    session.lastAccessedAt = Instant.now();
    session.expiresAt = session.lastAccessedAt.plus(timeoutMinutes, ChronoUnit.MINUTES);
    return session;
  }

  public void refreshExpiry(int timeoutMinutes) {
    this.lastAccessedAt = Instant.now();
    this.expiresAt = this.lastAccessedAt.plus(timeoutMinutes, ChronoUnit.MINUTES);
  }
}