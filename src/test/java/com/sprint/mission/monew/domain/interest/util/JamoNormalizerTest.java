package com.sprint.mission.monew.domain.interest.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JamoNormalizerTest {

  @Test
  @DisplayName("null을 입력하면 빈 문자열을 반환한다")
  void null_입력하면_빈_문자열을_반환한다() {
    assertThat(JamoNormalizer.normalize(null)).isEmpty();
  }
}