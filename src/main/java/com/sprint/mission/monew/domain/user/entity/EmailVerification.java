package com.sprint.mission.monew.domain.user.entity;

import com.sprint.mission.monew.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "email_verifications")
@Entity
public class EmailVerification extends BaseEntity {

  @Column(nullable = false, unique = true)
  private String token;

  @Column(nullable = false)
  private UUID userId;

  @Column(nullable = false)
  private Instant expiredAt;

  public static EmailVerification create(UUID userId) {
    EmailVerification ev = new EmailVerification();
    ev.token = UUID.randomUUID().toString();
    ev.userId = userId;
    ev.expiredAt = Instant.now().plus(24, ChronoUnit.HOURS);
    return ev;
  }

  public boolean isExpired() {
    return Instant.now().isAfter(expiredAt);
  }
}