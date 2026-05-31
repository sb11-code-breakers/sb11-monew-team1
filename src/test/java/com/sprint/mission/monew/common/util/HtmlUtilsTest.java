package com.sprint.mission.monew.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class HtmlUtilsTest {

  @Nested
  @DisplayName("strip")
  class Strip {

    @Test
    @DisplayName("null 입력 시 빈 문자열을 반환한다")
    void null_입력_시_빈_문자열을_반환한다() {
      assertThat(HtmlUtils.strip(null)).isEmpty();
    }

    @Test
    @DisplayName("HTML 태그를 제거한다")
    void HTML_태그를_제거한다() {
      assertThat(HtmlUtils.strip("<b>제목</b>")).isEqualTo("제목");
    }

    @Test
    @DisplayName("HTML 엔티티를 디코딩한다")
    void HTML_엔티티를_디코딩한다() {
      assertThat(HtmlUtils.strip("&quot;인용&quot; &amp; &lt;태그&gt;"))
          .isEqualTo("\"인용\" & <태그>");
    }

    @Test
    @DisplayName("태그와 엔티티가 혼합된 문자열을 정리한다")
    void 태그와_엔티티가_혼합된_문자열을_정리한다() {
      assertThat(HtmlUtils.strip("<b>&quot;제목&quot;</b>")).isEqualTo("\"제목\"");
    }

    @Test
    @DisplayName("앞뒤 공백을 제거한다")
    void 앞뒤_공백을_제거한다() {
      assertThat(HtmlUtils.strip("  내용  ")).isEqualTo("내용");
    }

    @Test
    @DisplayName("태그 없는 평문은 그대로 반환한다")
    void 태그_없는_평문은_그대로_반환한다() {
      assertThat(HtmlUtils.strip("평문 기사 제목")).isEqualTo("평문 기사 제목");
    }
  }
}
