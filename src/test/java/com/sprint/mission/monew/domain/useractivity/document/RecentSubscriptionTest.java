package com.sprint.mission.monew.domain.useractivity.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RecentSubscriptionTest {

  @Nested
  @DisplayName("RecentSubscription.of()")
  class Of {

    @Test
    @DisplayName("전달한 값으로 RecentSubscription을 생성한다")
    void 전달한_값으로_RecentSubscription을_생성한다() {
      // given
      UUID interestId = UUID.randomUUID();
      String interestName = "스프링 부트";
      Instant now = Instant.now();

      // when
      // 💡 다이어트 파라미터 적용
      RecentSubscription subscription = RecentSubscription.of(interestId, interestName, now);

      // then
      assertThat(subscription.getInterestId()).isEqualTo(interestId);
      assertThat(subscription.getInterestName()).isEqualTo(interestName);

      // ⚠️ 만약 엔티티 변수명이 subscribedAt 이라면 getSubscribedAt()으로 변경하세요.
      assertThat(subscription.getSubscribedAt()).isEqualTo(now);
    }
  }
}