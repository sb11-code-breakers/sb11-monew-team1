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
@Table(name = "password_reset_tokens")
public class PasswordResetToken extends BaseEntity {

  private static final long EXPIRY_HOURS = 1;

  @Column(nullable = false)
  private UUID userId;

  @Column(nullable = false, unique = true)
  private String code;

  @Column(nullable = false)
  private Instant expiredAt;

  public static PasswordResetToken create(UUID userId) {
    PasswordResetToken token = new PasswordResetToken();
    token.userId = userId;
    token.code = UUID.randomUUID().toString();
    token.expiredAt = Instant.now().plusSeconds(EXPIRY_HOURS * 3600);
    return token;
  }
}