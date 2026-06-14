package com.sprint.mission.monew.domain.interest.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.domain.interest.config.SynonymProperties;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SynonymUtilsTest {

  SynonymUtils synonymUtils;

  @BeforeEach
  void setUp() {
    synonymUtils = new SynonymUtils(new SynonymProperties(
        List.of(Set.of("뉴스", "소식")),
        List.of(Set.of("최신", "최근", "요즘"))
    ));
  }

  @Nested
  @DisplayName("jaccardSimilarity")
  class JaccardSimilarity {

    @Test
    @DisplayName("두 토큰 리스트가 모두 비어 있으면 1.0을 반환한다")
    void 두_리스트_모두_비어있으면_1_0을_반환한다() {
      // union == 0 → return 1.0 (line 53 true branch)
      assertThat(synonymUtils.jaccardSimilarity(List.of(), List.of())).isEqualTo(1.0);
    }

    @Test
    @DisplayName("prefix만 있는 두 토큰이 같은 그룹이면 1.0을 반환한다")
    void prefix만_있는_두_토큰이_같은_그룹이면_1_0을_반환한다() {
      // 정규화 후 suffix 없는 "최신AI", "최근AI" → matchPrefix 직접 진입(line 102) → P0_ai 동일
      assertThat(synonymUtils.jaccardSimilarity(
          List.of(JamoNormalizer.normalize("최신AI")),
          List.of(JamoNormalizer.normalize("최근AI"))
      )).isGreaterThanOrEqualTo(1.0);
    }
  }

  @Nested
  @DisplayName("expandSearchTokens")
  class ExpandSearchTokens {

    @Test
    @DisplayName("prefix 단어로 시작하는 토큰이 있으면 제거된 형태도 결과에 포함된다")
    void prefix_단어로_시작하면_제거된_형태도_포함된다() {
      // "최신AI" → stripRawSuffix 실패 → stripRawPrefix("최신AI") 진입(line 67) → "AI" 추가
      List<String> result = synonymUtils.expandSearchTokens(List.of("최신AI"));
      assertThat(result).contains("AI");
    }
  }
}