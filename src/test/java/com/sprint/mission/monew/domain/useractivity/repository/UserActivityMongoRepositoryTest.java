package com.sprint.mission.monew.domain.useractivity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.common.config.MongoContainerConfig;
import com.sprint.mission.monew.domain.useractivity.document.RecentArticleView;
import com.sprint.mission.monew.domain.useractivity.document.RecentComment;
import com.sprint.mission.monew.domain.useractivity.document.RecentCommentLike;
import com.sprint.mission.monew.domain.useractivity.document.RecentSubscription;
import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import com.sprint.mission.monew.domain.useractivity.repository.impl.UserActivityCustomMongoRepositoryImpl;
import java.time.Instant;
import java.util.List;
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

  @Autowired
  private UserActivityCustomMongoRepositoryImpl impl;

  private UUID userId;
  private UserActivity activity;
  private Instant now;

  @BeforeEach
  void setUp() {
    repository.deleteAll();
    userId = UUID.randomUUID();
    now = Instant.now();
    activity = UserActivity.of(userId, "test@example.com", "닉네임", now);
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
      UserActivity deleted = UserActivity.of(userId, "test@example.com", null, now);
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
      mongoTemplate.insert(UserActivity.of(userId, "a@b.com", "닉네임", now));
      RecentComment comment = RecentComment.of(
          UUID.randomUUID(), UUID.randomUUID(), "기사 제목", userId, "닉네임", "댓글 내용", 0L, now);

      // when
      impl.pushComment(userId, comment);

      // then
      UserActivity found = mongoTemplate.findById(userId, UserActivity.class);
      assertThat(found).isNotNull();
      assertThat(found.getComments()).hasSize(1);
      assertThat(found.getComments().get(0).getContent()).isEqualTo("댓글 내용");
    }

    @Test
    @DisplayName("댓글이 10개를 초과하면 최신 10개만 유지한다")
    void 댓글이_10개를_초과하면_최신_10개만_유지한다() {
      // given
      UserActivity fullActivity = UserActivity.of(userId, "a@b.com", "닉네임", now);
      for (int i = 1; i <= 10; i++) {
        fullActivity.getComments().add(
            RecentComment.of(UUID.randomUUID(), UUID.randomUUID(), "기존 " + i,
                userId, "닉네임", "기존 " + i, 0L, now));
      }
      mongoTemplate.insert(fullActivity);
      RecentComment newComment = RecentComment.of(
          UUID.randomUUID(), UUID.randomUUID(), "최신 기사", userId, "닉네임", "최신 댓글", 0L, now);

      // when
      impl.pushComment(userId, newComment);

      // then
      UserActivity found = mongoTemplate.findById(userId, UserActivity.class);
      assertThat(found).isNotNull();
      assertThat(found.getComments()).hasSize(10);
    }

    @Test
    @DisplayName("도큐먼트가 없으면 upsert로 신규 생성한다")
    void 도큐먼트가_없으면_upsert로_신규_생성한다() {
      // given
      RecentComment comment = RecentComment.of(
          UUID.randomUUID(), UUID.randomUUID(), "기사", userId, "닉네임", "내용", 0L, now);

      // when
      impl.pushComment(userId, comment);

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
      mongoTemplate.insert(UserActivity.of(userId, "a@b.com", "닉네임", now));
      impl.pushComment(userId, RecentComment.of(
          commentId, UUID.randomUUID(), "기사", userId, "닉네임", "내용", 0L, now));

      // when
      impl.pullComment(userId, commentId);

      // then
      UserActivity found = mongoTemplate.findById(userId, UserActivity.class);
      assertThat(found).isNotNull();
      assertThat(found.getComments()).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 commentId면 배열 변화가 없다")
    void 존재하지_않는_commentId면_배열_변화가_없다() {
      // given
      UUID existingId = UUID.randomUUID();
      mongoTemplate.insert(UserActivity.of(userId, "a@b.com", "닉네임", now));
      impl.pushComment(userId, RecentComment.of(
          existingId, UUID.randomUUID(), "기사", userId, "닉네임", "내용", 0L, now));

      // when
      impl.pullComment(userId, UUID.randomUUID());

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
      mongoTemplate.insert(UserActivity.of(userId, "a@b.com", "구닉네임", now));
      impl.pushComment(userId, RecentComment.of(
          UUID.randomUUID(), UUID.randomUUID(), "기사", userId, "구닉네임", "내용", 0L, now));
      impl.pushComment(userId, RecentComment.of(
          UUID.randomUUID(), UUID.randomUUID(), "기사2", userId, "구닉네임", "내용2", 0L, now));

      // when
      impl.updateNickname(userId, "새닉네임");

      // then
      UserActivity found = mongoTemplate.findById(userId, UserActivity.class);
      assertThat(found).isNotNull();
      assertThat(found.getNickname()).isEqualTo("새닉네임");
      assertThat(found.getComments()).extracting(RecentComment::getUserNickname)
          .containsOnly("새닉네임");
    }
  }

  @Nested
  @DisplayName("anonymize()")
  class Anonymize {

    @Test
    @DisplayName("nickname과 모든 comments의 userNickname을 알 수 없음으로 변경한다")
    void nickname과_모든_comments의_userNickname을_알수없음으로_변경한다() {
      // given
      mongoTemplate.insert(UserActivity.of(userId, "a@b.com", "닉네임", now));
      impl.pushComment(userId, RecentComment.of(
          UUID.randomUUID(), UUID.randomUUID(), "기사", userId, "닉네임", "내용", 0L, now));

      // when
      impl.anonymize(userId);

      // then
      UserActivity found = mongoTemplate.findById(userId, UserActivity.class);
      assertThat(found).isNotNull();
      assertThat(found.getNickname()).isEqualTo("알 수 없음");
      assertThat(found.getComments().get(0).getUserNickname()).isEqualTo("알 수 없음");
    }
  }

  @Nested
  @DisplayName("pullArticleViewsByArticleId()")
  class PullArticleViewsByArticleId {

    @Test
    @DisplayName("모든 유저 도큐먼트에서 해당 articleId의 조회 기록을 제거한다")
    void 모든_유저_도큐먼트에서_해당_articleId의_조회_기록을_제거한다() {
      // given
      UUID articleId = UUID.randomUUID();
      UUID user1 = UUID.randomUUID();
      UUID user2 = UUID.randomUUID();
      mongoTemplate.insert(UserActivity.of(user1, "a@b.com", "유저1", now));
      mongoTemplate.insert(UserActivity.of(user2, "c@d.com", "유저2", now));
      RecentArticleView view1 = RecentArticleView.of(
          UUID.randomUUID(), user1, now, articleId, "출처", "url", "제목", now, "요약", 0L, 0L);
      RecentArticleView view2 = RecentArticleView.of(
          UUID.randomUUID(), user2, now, articleId, "출처", "url", "제목", now, "요약", 0L, 0L);
      impl.pushArticleView(user1, view1);
      impl.pushArticleView(user2, view2);

      // when
      impl.pullArticleViewsByArticleId(articleId);

      // then
      UserActivity found1 = mongoTemplate.findById(user1, UserActivity.class);
      UserActivity found2 = mongoTemplate.findById(user2, UserActivity.class);
      assertThat(found1).isNotNull();
      assertThat(found2).isNotNull();
      assertThat(found1.getArticleViews()).isEmpty();
      assertThat(found2.getArticleViews()).isEmpty();
    }
  }

  @Nested
  @DisplayName("pullCommentsByArticleId()")
  class PullCommentsByArticleId {

    @Test
    @DisplayName("모든 유저 도큐먼트에서 해당 articleId의 댓글들을 제거한다")
    void 모든_유저_도큐먼트에서_해당_articleId의_댓글들을_제거한다() {
      // given
      UUID articleId = UUID.randomUUID();
      UUID user1 = UUID.randomUUID();
      UUID user2 = UUID.randomUUID();
      mongoTemplate.insert(UserActivity.of(user1, "a@b.com", "유저1", now));
      mongoTemplate.insert(UserActivity.of(user2, "c@d.com", "유저2", now));
      impl.pushComment(user1, RecentComment.of(
          UUID.randomUUID(), articleId, "제목1", user1, "유저1", "내용1", 0L, now));
      impl.pushComment(user2, RecentComment.of(
          UUID.randomUUID(), articleId, "제목2", user2, "유저2", "내용2", 0L, now));

      // when
      impl.pullCommentsByArticleId(articleId);

      // then
      UserActivity found1 = mongoTemplate.findById(user1, UserActivity.class);
      UserActivity found2 = mongoTemplate.findById(user2, UserActivity.class);
      assertThat(found1).isNotNull();
      assertThat(found2).isNotNull();
      assertThat(found1.getComments()).isEmpty();
      assertThat(found2.getComments()).isEmpty();
    }
  }

  @Nested
  @DisplayName("pullCommentLikesByArticleId()")
  class PullCommentLikesByArticleId {

    @Test
    @DisplayName("모든 유저 도큐먼트에서 해당 articleId의 댓글 좋아요 기록을 제거한다")
    void 모든_유저_도큐먼트에서_해당_articleId의_댓글_좋아요_기록을_제거한다() {
      // given
      UUID articleId = UUID.randomUUID();
      UUID user1 = UUID.randomUUID();
      UUID user2 = UUID.randomUUID();
      mongoTemplate.insert(UserActivity.of(user1, "a@b.com", "유저1", now));
      mongoTemplate.insert(UserActivity.of(user2, "c@d.com", "유저2", now));
      RecentCommentLike like1 = RecentCommentLike.of(
          UUID.randomUUID(), now, UUID.randomUUID(), articleId, "제목", user1, "유저1", "내용", 0L, now);
      RecentCommentLike like2 = RecentCommentLike.of(
          UUID.randomUUID(), now, UUID.randomUUID(), articleId, "제목", user2, "유저2", "내용", 0L, now);
      impl.pushCommentLike(user1, like1);
      impl.pushCommentLike(user2, like2);

      // when
      impl.pullCommentLikesByArticleId(articleId);

      // then
      UserActivity found1 = mongoTemplate.findById(user1, UserActivity.class);
      UserActivity found2 = mongoTemplate.findById(user2, UserActivity.class);
      assertThat(found1).isNotNull();
      assertThat(found2).isNotNull();
      assertThat(found1.getCommentLikes()).isEmpty();
      assertThat(found2.getCommentLikes()).isEmpty();
    }
  }

  @Nested
  @DisplayName("pullSubscriptionsByInterestId()")
  class PullSubscriptionsByInterestId {

    @Test
    @DisplayName("모든 유저 도큐먼트에서 해당 interestId의 구독 기록을 제거한다")
    void 모든_유저_도큐먼트에서_해당_interestId의_구독_기록을_제거한다() {
      // given
      UUID interestId = UUID.randomUUID();
      UUID user1 = UUID.randomUUID();
      UUID user2 = UUID.randomUUID();
      mongoTemplate.insert(UserActivity.of(user1, "a@b.com", "유저1", now));
      mongoTemplate.insert(UserActivity.of(user2, "c@d.com", "유저2", now));
      RecentSubscription sub1 = RecentSubscription.of(
          UUID.randomUUID(), interestId, "인공지능", List.of("AI"), 1L, now);
      RecentSubscription sub2 = RecentSubscription.of(
          UUID.randomUUID(), interestId, "인공지능", List.of("AI"), 1L, now);
      impl.pushSubscription(user1, sub1);
      impl.pushSubscription(user2, sub2);

      // when
      impl.pullSubscriptionsByInterestId(interestId);

      // then
      UserActivity found1 = mongoTemplate.findById(user1, UserActivity.class);
      UserActivity found2 = mongoTemplate.findById(user2, UserActivity.class);
      assertThat(found1).isNotNull();
      assertThat(found2).isNotNull();
      assertThat(found1.getSubscriptions()).isEmpty();
      assertThat(found2.getSubscriptions()).isEmpty();
    }
  }
}