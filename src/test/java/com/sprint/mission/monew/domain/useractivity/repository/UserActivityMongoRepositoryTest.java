package com.sprint.mission.monew.domain.useractivity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.common.config.MongoContainerConfig; // 설정 임포트
import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;

@DataMongoTest
@Import(MongoContainerConfig.class) // 도커 컨테이너 설정 주입
class UserActivityMongoRepositoryTest {

  @Autowired
  private UserActivityMongoRepository repository;

  private UUID userId;
  private UserActivity activity;

  @BeforeEach
  void setUp() {
    repository.deleteAll();
    userId = UUID.randomUUID();
    activity = UserActivity.of(userId, "test@example.com", "닉네임", Instant.now());
  }

  @Nested
  @DisplayName("UserActivity 저장 및 단건 조회")
  class SaveAndFind {

    @Test
    @DisplayName("저장한 UserActivity를 id로 조회할 수 있다")
    void 저장한_UserActivity를_id로_조회할_수_있다() {
      // given
      repository.save(activity);

      // when
      Optional<UserActivity> found = repository.findById(userId);

      // then
      assertThat(found).isPresent();
      assertThat(found.get().getEmail()).isEqualTo("test@example.com");
      assertThat(found.get().getNickname()).isEqualTo("닉네임");
    }
  }
}