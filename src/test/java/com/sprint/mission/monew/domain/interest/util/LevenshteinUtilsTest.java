package com.sprint.mission.monew.domain.interest.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LevenshteinUtilsTest {

  @Nested
  @DisplayName("similarity")
  class Similarity {

    @Test
    @DisplayName("첫 번째 인자가 null이면 IllegalArgumentException을 던진다")
    void 첫번째_인자가_null이면_예외를_던진다() {
      assertThatThrownBy(() -> LevenshteinUtils.similarity(null, "abc"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("두 번째 인자가 null이면 IllegalArgumentException을 던진다")
    void 두번째_인자가_null이면_예외를_던진다() {
      assertThatThrownBy(() -> LevenshteinUtils.similarity("abc", null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("두 문자열이 모두 빈 문자열이면 1.0을 반환한다")
    void 두_문자열이_모두_빈_문자열이면_1_0을_반환한다() {
      assertThat(LevenshteinUtils.similarity("", "")).isEqualTo(1.0);
    }
  }

  @Nested
  @DisplayName("distance")
  class Distance {

    @Test
    @DisplayName("첫 번째 인자가 null이면 IllegalArgumentException을 던진다")
    void 첫번째_인자가_null이면_예외를_던진다() {
      assertThatThrownBy(() -> LevenshteinUtils.distance(null, "abc"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("두 번째 인자가 null이면 IllegalArgumentException을 던진다")
    void 두번째_인자가_null이면_예외를_던진다() {
      assertThatThrownBy(() -> LevenshteinUtils.distance("abc", null))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }
}