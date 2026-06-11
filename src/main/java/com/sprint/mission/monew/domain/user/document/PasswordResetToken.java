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
@Document(collection = "password_reset_tokens")
public class PasswordResetToken {

  private static final long EXPIRY_HOURS = 1;

  @Id
  private UUID id;
  private UUID userId;
  private String code;

  @Indexed(expireAfter = "0s")
  private Instant expiredAt;

  public static PasswordResetToken create(UUID userId) {
    PasswordResetToken token = new PasswordResetToken();
    token.id = UUID.randomUUID();
    token.userId = userId;
    token.code = UUID.randomUUID().toString();
    token.expiredAt = Instant.now().plusSeconds(EXPIRY_HOURS * 3600);
    return token;
  }
}