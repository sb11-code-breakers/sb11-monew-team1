package com.sprint.mission.monew.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

  private static final long EXPIRY_HOURS = 1;

  @Id
  @Column(columnDefinition = "uuid", nullable = false, updatable = false)
  private UUID id = UUID.randomUUID();

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