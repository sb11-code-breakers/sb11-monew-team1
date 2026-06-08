package com.sprint.mission.monew.domain.useractivity.document;

import static com.mysema.commons.lang.Assert.assertThat;

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
    @DisplayName("전달한 값으로 UserActivity를 생성한다")
    void 전달한_값으로_UserActivity를_생성한다() {
      // given
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();

      // when
      UserActivity activity = UserActivity.of(id, "test@example.com", "닉네임", now);

      // then
      assertThat(activity.getId()).isEqualTo(id);
      assertThat(activity.getEmail()).isEqualTo("test@example.com");
      assertThat(activity.getNickname()).isEqualTo("닉네임");
      assertThat(activity.getCreatedAt()).isEqualTo(now);
      assertThat(activity.getSubscriptions()).isEmpty();
      assertThat(activity.getComments()).isEmpty();
      assertThat(activity.getCommentLikes()).isEmpty();
      assertThat(activity.getArticleViews()).isEmpty();
    }
  }
}
