package com.sprint.mission.monew.domain.interest.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InterestKeywordTest {

  @Nested
  @DisplayName("정적 팩토리 메서드")
  class Create {

    @Test
    @DisplayName("interest와 keyword를 전달하면 InterestKeyword가 정상 생성된다")
    void interest와_keyword를_전달하면_InterestKeyword가_정상_생성된다() {
      // given
      Interest interest = Interest.create("인공지능", java.util.List.of("AI"));
      String keyword = "머신러닝";

      // when
      InterestKeyword interestKeyword = InterestKeyword.create(interest, keyword);

      // then
      assertThat(interestKeyword.getId()).isNotNull();
      assertThat(interestKeyword.getInterest()).isEqualTo(interest);
      assertThat(interestKeyword.getKeyword()).isEqualTo(keyword);
    }
  }
}
