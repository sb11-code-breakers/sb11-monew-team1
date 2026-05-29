package com.sprint.mission.monew.domain.notification.entity;

import com.sprint.mission.monew.common.entity.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseUpdatableEntity {

  @Column(name = "user_id", columnDefinition = "uuid")
  private UUID userId;

  @Column(nullable = false)
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(name = "resource_type", nullable = false)
  private ResourceType resourceType;

  @Column(name = "resource_id", nullable = false, columnDefinition = "uuid")
  private UUID resourceId;

  @Column(name = "confirmed_at")
  private Instant confirmedAt;

  private Notification(
      UUID userId,
      String content,
      ResourceType resourceType,
      UUID resourceId
  ) {
    this.userId = userId;
    this.content = content;
    this.resourceType = resourceType;
    this.resourceId = resourceId;
  }

  public static Notification create(
      UUID userId,
      String content,
      ResourceType resourceType,
      UUID resourceId
  ) {
    return new Notification(
        userId,
        content,
        resourceType,
        resourceId
    );
  }

  public boolean isConfirmed() {
    return confirmedAt != null;
  }

  public void confirm() {
    if (!isConfirmed()) {
      confirmedAt = Instant.now();
    }
  }
}
