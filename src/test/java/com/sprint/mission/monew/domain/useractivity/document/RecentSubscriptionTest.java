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
      UUID id = UUID.randomUUID();
      UUID interestId = UUID.randomUUID();
      List<String> keywords = List.of("AI", "클라우드");
      Instant now = Instant.now();

      // when
      RecentSubscription sub = RecentSubscription.of(id, interestId, "IT", keywords, 42L, now);

      // then
      assertThat(sub.getId()).isEqualTo(id);
      assertThat(sub.getInterestId()).isEqualTo(interestId);
      assertThat(sub.getInterestName()).isEqualTo("IT");
      assertThat(sub.getInterestKeywords()).containsExactly("AI", "클라우드");
      assertThat(sub.getInterestSubscriberCount()).isEqualTo(42L);
      assertThat(sub.getCreatedAt()).isEqualTo(now);
    }
  }
}