package com.sprint.mission.monew.domain.useractivity.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserActivityDocumentTest {

  @Nested
  @DisplayName("UserActivity.of()")
  class Of {

    @Test
    @DisplayName("전달한 값으로 메인 UserActivity 도큐먼트를 생성한다")
    void 전달한_값으로_UserActivity를_생성한다() {
      // given
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();

      // when
      // 💡 다이어트 파라미터 적용 (이메일, 닉네임 제거하고 불변 데이터와 ID만 유지)
      UserActivity activity = UserActivity.of(id, now);

      // then
      assertThat(activity.getId()).isEqualTo(id);
      assertThat(activity.getCreatedAt()).isEqualTo(now);

      // 내부 활동 배열들이 null이 아닌 빈 배열로 잘 초기화되었는지 검증
      assertThat(activity.getSubscriptions()).isEmpty();
      assertThat(activity.getComments()).isEmpty();
      assertThat(activity.getCommentLikes()).isEmpty();
      assertThat(activity.getArticleViews()).isEmpty();
    }
  }
}