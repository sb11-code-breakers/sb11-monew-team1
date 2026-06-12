package com.sprint.mission.monew.domain.useractivity.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
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
      UUID subscriptionId = UUID.randomUUID();
      UUID interestId = UUID.randomUUID();
      String interestName = "IT 기술";
      List<String> keywords = List.of("java", "spring");
      long subscriberCount = 10L;
      Instant now = Instant.now();

      // when
      RecentSubscription subscription = RecentSubscription.of(
          subscriptionId, interestId, interestName, keywords, subscriberCount, now);

      // then
      assertThat(subscription.getId()).isEqualTo(subscriptionId);
      assertThat(subscription.getInterestId()).isEqualTo(interestId);
      assertThat(subscription.getInterestName()).isEqualTo(interestName);
      assertThat(subscription.getInterestKeywords()).isEqualTo(keywords);
      assertThat(subscription.getInterestSubscriberCount()).isEqualTo(subscriberCount);
      assertThat(subscription.getCreatedAt()).isEqualTo(now);
    }
  }
}
