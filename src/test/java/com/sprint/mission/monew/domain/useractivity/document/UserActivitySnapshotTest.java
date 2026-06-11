package com.sprint.mission.monew.domain.useractivity.document;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.domain.useractivity.activityresponse.UserActivityResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserActivitySnapshotTest {

  @Test
  @DisplayName("스냅샷 생성 시 userId, response, createdAt이 설정된다")
  void 스냅샷_생성_시_userId와_response와_createdAt이_설정된다() {
    // given
    UUID userId = UUID.randomUUID();
    UserActivityResponse response = new UserActivityResponse(
        userId, "test@test.com", "테스터", Instant.now(),
        List.of(), List.of(), List.of(), List.of()
    );

    // when
    UserActivitySnapshot snapshot = UserActivitySnapshot.of(userId, response);

    // then
    assertThat(snapshot.getUserId()).isEqualTo(userId);
    assertThat(snapshot.getResponse()).isEqualTo(response);
    assertThat(snapshot.getCreatedAt()).isNotNull();
  }
}
