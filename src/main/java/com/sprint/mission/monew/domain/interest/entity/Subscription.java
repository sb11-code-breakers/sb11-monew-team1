package com.sprint.mission.monew.domain.interest.entity;

import com.sprint.mission.monew.common.entity.BaseEntity;
import com.sprint.mission.monew.domain.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "subscriptions",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"interest_id", "user_id"})}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "interest_id", nullable = false)
  private Interest interest;

  public static Subscription create(Interest interest, User user) {
    Subscription subscription = new Subscription();
    subscription.interest = interest;
    subscription.user = user;
    return subscription;
  }
}