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
  private MongoTemplate mongoTemplate;
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

    @Test
    @DisplayName("댓글이 10개를 초과하면 최신 10개만 유지한다")
    void 댓글이_10개를_초과하면_최신_10개만_유지한다() {
      // given
      UserActivity fullActivity = UserActivity.of(userId, "a@b.com", "닉네임", Instant.now());
      for (int i = 1; i <= 10; i++) {
        fullActivity.getComments().add(
            RecentComment.of(
                UUID.randomUUID(), UUID.randomUUID(), "기존 제목 " + i,
                userId, "닉네임", "기존 내용 " + i, 0L, Instant.now()
            )
        );
      }
      mongoTemplate.insert(fullActivity);

      RecentComment newComment = RecentComment.of(
          UUID.randomUUID(), UUID.randomUUID(), "가장 최신 기사",
          userId, "닉네임", "가장 최신 댓글 내용", 0L, Instant.now()
      );

      // when
      userActivityMongoRepositoryImpl.pushComment(userId, newComment);

      // then
      UserActivity found = mongoTemplate.findById(userId, UserActivity.class);
      assertThat(found).isNotNull();
      assertThat(found.getComments()).hasSize(10);
      assertThat(found.getComments().get(0).getId()).isEqualTo(newComment.getId());
    }

    @Test
    @DisplayName("도큐먼트가 없으면 upsert로 신규 생성한다")
    void 도큐먼트가_없으면_upsert로_신규_생성한다() {
      // given
      RecentComment comment = RecentComment.of(
          UUID.randomUUID(), UUID.randomUUID(), "기사",
          userId, "닉네임", "내용", 0L, Instant.now()
      );

      // when
      userActivityMongoRepositoryImpl.pushComment(userId, comment);

      // then
      UserActivity found = mongoTemplate.findById(userId, UserActivity.class);
      assertThat(found).isNotNull();
      assertThat(found.getComments()).hasSize(1);
    }
  }

  @Nested
  @DisplayName("pullComment()")
  class PullComment {

    @Test
    @DisplayName("해당 commentId의 댓글을 제거한다")
    void 해당_commentId의_댓글을_제거한다() {
      // given
      UUID commentId = UUID.randomUUID();
      mongoTemplate.insert(UserActivity.of(userId, "a@b.com", "닉네임", Instant.now()));

      userActivityMongoRepositoryImpl.pushComment(userId, RecentComment.of(
          commentId, UUID.randomUUID(), "기사", userId, "닉네임", "내용", 0L, Instant.now()
      ));

      // when
      userActivityMongoRepositoryImpl.pullComment(userId, commentId);

      // then
      UserActivity found = mongoTemplate.findById(userId, UserActivity.class);
      assertThat(found).isNotNull();
      assertThat(found.getComments()).isEmpty();
    }

    //
    @Test
    @DisplayName("존재하지 않는 commentId면 배열 변화가 없다")
    void 존재하지_않는_commentId면_배열_변화가_없다() {
      // given
      UUID existingId = UUID.randomUUID();
      mongoTemplate.insert(UserActivity.of(userId, "a@b.com", "닉네임", Instant.now()));
      userActivityMongoRepositoryImpl.pushComment(userId, RecentComment.of(
          existingId, UUID.randomUUID(), "기사", userId, "닉네임", "내용", 0L, Instant.now()
      ));

      // when
      userActivityMongoRepositoryImpl.pullComment(userId, UUID.randomUUID());

      // then
      UserActivity found = mongoTemplate.findById(userId, UserActivity.class);
      assertThat(found).isNotNull();
      assertThat(found.getComments()).hasSize(1);
    }
  }
  @Nested
  @DisplayName("updateNickname()")
  class UpdateNickname {

    @Test
    @DisplayName("nickname과 모든 comments의 userNickname을 동시에 변경한다")
    void nickname과_모든_comments의_userNickname을_동시에_변경한다() {
      // given
      mongoTemplate.insert(UserActivity.of(userId, "a@b.com", "구닉네임", Instant.now()));
      userActivityMongoRepositoryImpl.pushComment(userId, RecentComment.of(UUID.randomUUID(), UUID.randomUUID(),
          "기사", userId, "구닉네임", "내용", 0L, Instant.now()));
      userActivityMongoRepositoryImpl.pushComment(userId, RecentComment.of(UUID.randomUUID(), UUID.randomUUID(),
          "기사2", userId, "구닉네임", "내용2", 0L, Instant.now()));

      // when
      userActivityMongoRepositoryImpl.updateNickname(userId, "새닉네임");

      // then
      UserActivity found = mongoTemplate.findById(userId, UserActivity.class);
      assertThat(found.getNickname()).isEqualTo("새닉네임");
      assertThat(found.getComments())
          .extracting(RecentComment::getUserNickname)
          .containsOnly("새닉네임");
    }
  }
}