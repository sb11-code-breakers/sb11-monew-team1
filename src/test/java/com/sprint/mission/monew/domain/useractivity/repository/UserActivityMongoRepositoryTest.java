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
      repository.save(activity);
      Optional<UserActivity> found = repository.findById(userId);
      assertThat(found).isPresent();
    }
  }

  @Nested
  @DisplayName("nickname 존재 여부로 활성 사용자 조회")
  class FindByIdAndNicknameIsNotNull {
    @Test
    @DisplayName("nickname이 있으면 UserActivity를 반환한다")
    void nickname이_있으면_UserActivity를_반환한다() {
      repository.save(activity);
      Optional<UserActivity> found = repository.findByIdAndNicknameIsNotNull(userId);
      assertThat(found).isPresent();
    }

    @Test
    @DisplayName("nickname이 null이면 empty를 반환한다")
    void nickname이_null이면_empty를_반환한다() {
      UserActivity deleted = UserActivity.of(userId, "test@example.com", null, Instant.now());
      repository.save(deleted);
      Optional<UserActivity> found = repository.findByIdAndNicknameIsNotNull(userId);
      assertThat(found).isEmpty();
    }
  }

  @Nested
  @DisplayName("pushComment()")
  class PushComment {
    @Test
    @DisplayName("댓글을 배열 첫 번째에 삽입한다")
    void 댓글을_배열_첫_번째에_삽입한다() {
      mongoTemplate.insert(UserActivity.of(userId, "a@b.com", "닉네임", Instant.now()));
      RecentComment comment = RecentComment.of(UUID.randomUUID(), UUID.randomUUID(), "기사 제목", userId, "닉네임", "댓글 내용", 0L, Instant.now());
      userActivityMongoRepositoryImpl.pushComment(userId, comment);
      UserActivity found = mongoTemplate.findById(userId, UserActivity.class);
      assertThat(found).isNotNull();
    }

    @Test
    @DisplayName("댓글이 10개를 초과하면 최신 10개만 유지한다")
    void 댓글이_10개를_초과하면_최신_10개만_유지한다() {
      UserActivity fullActivity = UserActivity.of(userId, "a@b.com", "닉네임", Instant.now());
      for (int i = 1; i <= 10; i++) {
        fullActivity.getComments().add(RecentComment.of(UUID.randomUUID(), UUID.randomUUID(), "기존 " + i, userId, "닉네임", "기존 " + i, 0L, Instant.now()));
      }
      mongoTemplate.insert(fullActivity);
      RecentComment newComment = RecentComment.of(UUID.randomUUID(), UUID.randomUUID(), "최신 기사", userId, "닉네임", "최신 댓글", 0L, Instant.now());
      userActivityMongoRepositoryImpl.pushComment(userId, newComment);
      UserActivity found = mongoTemplate.findById(userId, UserActivity.class);
      assertThat(found.getComments()).hasSize(10);
    }

    @Test
    @DisplayName("도큐먼트가 없으면 upsert로 신규 생성한다")
    void 도큐먼트가_없으면_upsert로_신규_생성한다() {
      RecentComment comment = RecentComment.of(UUID.randomUUID(), UUID.randomUUID(), "기사", userId, "닉네임", "내용", 0L, Instant.now());
      userActivityMongoRepositoryImpl.pushComment(userId, comment);
      UserActivity found = mongoTemplate.findById(userId, UserActivity.class);
      assertThat(found).isNotNull();
    }
  }

  @Nested
  @DisplayName("pullComment()")
  class PullComment {
    @Test
    @DisplayName("해당 commentId의 댓글을 제거한다")
    void 해당_commentId의_댓글을_제거한다() {
      UUID commentId = UUID.randomUUID();
      mongoTemplate.insert(UserActivity.of(userId, "a@b.com", "닉네임", Instant.now()));
      userActivityMongoRepositoryImpl.pushComment(userId, RecentComment.of(commentId, UUID.randomUUID(), "기사", userId, "닉네임", "내용", 0L, Instant.now()));
      userActivityMongoRepositoryImpl.pullComment(userId, commentId);
      UserActivity found = mongoTemplate.findById(userId, UserActivity.class);
      assertThat(found.getComments()).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 commentId면 배열 변화가 없다")
    void 존재하지_않는_commentId면_배열_변화가_없다() {
      UUID existingId = UUID.randomUUID();
      mongoTemplate.insert(UserActivity.of(userId, "a@b.com", "닉네임", Instant.now()));
      userActivityMongoRepositoryImpl.pushComment(userId, RecentComment.of(existingId, UUID.randomUUID(), "기사", userId, "닉네임", "내용", 0L, Instant.now()));
      userActivityMongoRepositoryImpl.pullComment(userId, UUID.randomUUID());
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
      mongoTemplate.insert(UserActivity.of(userId, "a@b.com", "구닉네임", Instant.now()));
      userActivityMongoRepositoryImpl.pushComment(userId, RecentComment.of(UUID.randomUUID(), UUID.randomUUID(), "기사", userId, "구닉네임", "내용", 0L, Instant.now()));
      userActivityMongoRepositoryImpl.pushComment(userId, RecentComment.of(UUID.randomUUID(), UUID.randomUUID(), "기사2", userId, "구닉네임", "내용2", 0L, Instant.now()));
      userActivityMongoRepositoryImpl.updateNickname(userId, "새닉네임");
      UserActivity found = mongoTemplate.findById(userId, UserActivity.class);
      assertThat(found.getNickname()).isEqualTo("새닉네임");
      assertThat(found.getComments()).extracting(RecentComment::getUserNickname).containsOnly("새닉네임");
    }
  }

  @Nested
  @DisplayName("anonymize()")
  class Anonymize {
    @Test
    @DisplayName("nickname과 모든 comments의 userNickname을 알 수 없음으로 변경한다")
    void nickname과_모든_comments의_userNickname을_알수없음으로_변경한다() {
      mongoTemplate.insert(UserActivity.of(userId, "a@b.com", "닉네임", Instant.now()));
      userActivityMongoRepositoryImpl.pushComment(userId, RecentComment.of(UUID.randomUUID(), UUID.randomUUID(), "기사", userId, "닉네임", "내용", 0L, Instant.now()));
      userActivityMongoRepositoryImpl.anonymize(userId);
      UserActivity found = mongoTemplate.findById(userId, UserActivity.class);
      assertThat(found.getNickname()).isEqualTo("알 수 없음");
      assertThat(found.getComments().get(0).getUserNickname()).isEqualTo("알 수 없음");
    }
  }

  // 🔴 [커밋 29] 모든 유저 대상 기사 조회 기록 제거 Red 테스트 추가 단계
  @Nested
  @DisplayName("pullArticleViewsByArticleId()")
  class PullArticleViewsByArticleId {
    @Test
    @DisplayName("모든 유저 도큐먼트에서 해당 articleId의 조회 기록을 제거한다")
    void 모든_유저_도큐먼트에서_해당_articleId의_조회_기록을_제거한다() {
      UUID articleId = UUID.randomUUID();
      UUID user1 = UUID.randomUUID();
      UUID user2 = UUID.randomUUID();
      mongoTemplate.insert(UserActivity.of(user1, "a@b.com", "유저1", Instant.now()));
      mongoTemplate.insert(UserActivity.of(user2, "c@d.com", "유저2", Instant.now()));

      // 🔴 현재 pushArticleView 메서드가 구현체에 없으므로 컴파일 에러 발생 대상
      userActivityMongoRepositoryImpl.pushArticleView(user1, articleId);
      userActivityMongoRepositoryImpl.pushArticleView(user2, articleId);

      // 🔴 현재 pullArticleViewsByArticleId 메서드가 구현체에 없으므로 컴파일 에러 발생 대상
      userActivityMongoRepositoryImpl.pullArticleViewsByArticleId(articleId);

      assertThat(mongoTemplate.findById(user1, UserActivity.class).getArticleViews()).isEmpty();
      assertThat(mongoTemplate.findById(user2, UserActivity.class).getArticleViews()).isEmpty();
    }
  }
}