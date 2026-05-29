package com.sprint.mission.monew.domain.interest.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.domain.user.entity.User;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SubscriptionTest {

  @Nested
  @DisplayName("정적 팩토리 메서드")
  class Create {

    @Test
    @DisplayName("정상 생성 시 Interest와 User가 올바르게 설정된다")
    void 정상_생성_시_Interest와_User가_올바르게_설정된다() {
      // given
      Interest interest = Interest.create("인공지능", List.of("AI"));
      User user = User.create("test@test.com", "테스터", "password123!");

      // when
      Subscription subscription = Subscription.create(interest, user);

      // then
      assertThat(subscription.getInterest()).isEqualTo(interest);
      assertThat(subscription.getUser()).isEqualTo(user);
      assertThat(subscription.getId()).isNotNull();
      assertThat(subscription.getCreatedAt()).isNull(); // Auditing 미동작 (Spring 없음)
    }
  }
}
