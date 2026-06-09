package com.sprint.mission.monew.domain.useractivity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.common.config.MongoContainerConfig;
import com.sprint.mission.monew.domain.useractivity.document.RecentComment;
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
import org.springframework.data.mongodb.core.MongoTemplate;

@DataMongoTest
@Import(MongoContainerConfig.class)
class UserActivityMongoRepositoryTest {

  @Autowired
  private UserActivityMongoRepository repository;

  @Autowired
  private MongoTemplate mongoTemplate; // 🟢 @Autowired 추가로 스프링 빈 주입
  private UserActivityMongoRepositoryImpl userActivityMongoRepositoryImpl;

  private UUID userId;
  private UserActivity activity;

  @BeforeEach
  void setUp() {
    repository.deleteAll();
    userActivityMongoRepositoryImpl = new UserActivityMongoRepositoryImpl(mongoTemplate);
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

  @Nested
  @DisplayName("nickname 존재 여부로 활성 사용자 조회")
  class FindByIdAndNicknameIsNotNull {

    @Test
    @DisplayName("nickname이 있으면 UserActivity를 반환한다")
    void nickname이_있으면_UserActivity를_반환한다() {
      // given
      repository.save(activity);

      // when
      Optional<UserActivity> found = repository.findByIdAndNicknameIsNotNull(userId);

      // then
      assertThat(found).isPresent();
      assertThat(found.get().getId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("nickname이 null이면 empty를 반환한다")
    void nickname이_null이면_empty를_반환한다() {
      // given
      UserActivity deleted = UserActivity.of(userId, "test@example.com", null, Instant.now());
      repository.save(deleted);

      // when
      Optional<UserActivity> found = repository.findByIdAndNicknameIsNotNull(userId);

      // then
      assertThat(found).isEmpty();
    }
  }

  @Nested
  @DisplayName("pushComment()")
  class PushComment {

    @Test
    @DisplayName("댓글을 배열 첫 번째에 삽입한다")
    void 댓글을_배열_첫_번째에_삽입한다() {
      // given
      mongoTemplate.insert(UserActivity.of(userId, "a@b.com", "닉네임", Instant.now()));

      RecentComment comment = RecentComment.of(
          UUID.randomUUID(), UUID.randomUUID(), "기사 제목",
          userId, "닉네임", "댓글 내용", 0L, Instant.now()
      );

      // when
      userActivityMongoRepositoryImpl.pushComment(userId, comment);

      // then
      UserActivity found = mongoTemplate.findById(userId, UserActivity.class);
      assertThat(found).isNotNull();
      assertThat(found.getComments()).hasSize(1);
      assertThat(found.getComments().get(0).getId()).isEqualTo(comment.getId());
    }
  }
}