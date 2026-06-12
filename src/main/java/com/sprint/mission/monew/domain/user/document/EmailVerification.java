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
@Document(collection = "email_verifications")
public class EmailVerification {

  @Id
  private UUID id;
  private String token;
  private UUID userId;

  @Indexed(expireAfter = "0s")
  private Instant expiredAt;

  public static EmailVerification create(UUID userId) {
    EmailVerification ev = new EmailVerification();
    ev.id = UUID.randomUUID();
    ev.token = UUID.randomUUID().toString();
    ev.userId = userId;
    ev.expiredAt = Instant.now().plus(24, ChronoUnit.HOURS);
    return ev;
  }

  public boolean isExpired() {
    return Instant.now().isAfter(expiredAt);
  }
}