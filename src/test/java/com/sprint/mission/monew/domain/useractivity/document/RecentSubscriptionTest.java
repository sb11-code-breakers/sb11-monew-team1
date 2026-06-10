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
      String interestName = "IT 기술";
      Instant now = Instant.now();

      // when
      RecentSubscription subscription = RecentSubscription.of(interestId, interestName, now);

      // then
      assertThat(subscription.getInterestId()).isEqualTo(interestId);
      assertThat(subscription.getInterestName()).isEqualTo(interestName);
      assertThat(subscription.getSubscribedAt()).isEqualTo(now);
    }
  }

  @Test
  @DisplayName("RecentSubscription은 변동 필드를 포함하지 않아야 한다 (정확히 3개 필드)")
  void shouldOnlyContainImmutableFields() {
    // 필드 개수가 정확히 3개(interestId, interestName, subscribedAt)인지 확인
    assertThat(RecentSubscription.class.getDeclaredFields()).hasSize(3);
  }
}