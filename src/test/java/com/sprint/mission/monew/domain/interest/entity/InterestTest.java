package com.sprint.mission.monew.domain.interest.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InterestTest {

  private String name;
  private List<String> keywords;

  @BeforeEach
  void setUp() {
    name = "인공지능";
    keywords = List.of("AI", "머신러닝", "딥러닝");
  }

  @Nested
  @DisplayName("정적 팩토리 메서드")
  class Create {

    @Test
    @DisplayName("이름과 키워드를 전달하면 관심사가 정상 생성된다")
    void 이름과_키워드를_전달하면_관심사가_정상_생성된다() {
      // when
      Interest interest = Interest.create(name, keywords);

      // then
      assertThat(interest.getId()).isNotNull();
      assertThat(interest.getName()).isEqualTo(name);
      assertThat(interest.getKeywords())
          .extracting(InterestKeyword::getKeyword)
          .containsExactlyElementsOf(keywords);
      assertThat(interest.getSubscriberCount()).isZero();
    }
  }

  @Nested
  @DisplayName("키워드 수정")
  class UpdateKeywords {

    private Interest interest;

    @BeforeEach
    void setUp() {
      interest = Interest.create(name, keywords);
    }

    @Test
    @DisplayName("유효한 키워드 목록으로 교체하면 keywords가 업데이트된다")
    void 유효한_키워드_목록으로_교체하면_keywords가_업데이트된다() {
      // given
      List<String> newKeywords = List.of("자연어처리", "GPT", "트랜스포머");

      // when
      interest.updateKeywords(newKeywords);

      // then
      assertThat(interest.getKeywords())
          .extracting(InterestKeyword::getKeyword)
          .containsExactlyInAnyOrder("자연어처리", "GPT", "트랜스포머");
    }
  }
}
