package com.sprint.mission.monew.domain.interest.entity;

import com.sprint.mission.monew.common.entity.BaseEntity;
import jakarta.persistence.Column;
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
    name = "interest_keywords",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"interest_id", "keyword"})}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterestKeyword extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "interest_id", nullable = false)
  private Interest interest;

  @Column(nullable = false, length = 50)
  private String keyword;

  public static InterestKeyword create(Interest interest, String keyword) {
    InterestKeyword interestKeyword = new InterestKeyword();
    interestKeyword.interest = interest;
    interestKeyword.keyword = keyword;
    return interestKeyword;
  }
}
