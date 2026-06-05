package com.sprint.mission.monew.domain.user.entity;

import com.sprint.mission.monew.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "user_unlock_tokens")
public class UserUnlockToken extends BaseEntity {

  private static final long EXPIRY_HOURS = 24;

  @Column(nullable = false)
  private UUID userId;

  @Column(nullable = false, unique = true)
  private String token;

  @Column(nullable = false)
  private Instant expiredAt;

  public static UserUnlockToken create(UUID userId) {
    UserUnlockToken unlockToken = new UserUnlockToken();
    unlockToken.userId = userId;
    unlockToken.token = UUID.randomUUID().toString();
    unlockToken.expiredAt = Instant.now().plusSeconds(EXPIRY_HOURS * 3600);
    return unlockToken;
  }
}