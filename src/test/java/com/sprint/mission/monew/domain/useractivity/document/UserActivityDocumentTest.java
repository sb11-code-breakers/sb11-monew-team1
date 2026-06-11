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
      UserActivity activity = UserActivity.of(id, "test@test.com", "테스트유저", now);

      // then
      assertThat(activity.getId()).isEqualTo(id);
      assertThat(activity.getEmail()).isEqualTo("test@test.com");
      assertThat(activity.getNickname()).isEqualTo("테스트유저");
      assertThat(activity.getCreatedAt()).isEqualTo(now);

      assertThat(activity.getSubscriptions()).isEmpty();
      assertThat(activity.getComments()).isEmpty();
      assertThat(activity.getCommentLikes()).isEmpty();
      assertThat(activity.getArticleViews()).isEmpty();
    }
  }
}
