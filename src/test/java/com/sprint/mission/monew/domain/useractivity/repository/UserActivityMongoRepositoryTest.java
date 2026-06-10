package com.sprint.mission.monew.domain.useractivity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.domain.useractivity.document.RecentArticleView;
import com.sprint.mission.monew.domain.useractivity.document.RecentComment;
import com.sprint.mission.monew.domain.useractivity.document.RecentCommentLike;
import com.sprint.mission.monew.domain.useractivity.document.RecentSubscription;
import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import com.sprint.mission.monew.common.config.MongoContainerConfig;
import com.sprint.mission.monew.domain.useractivity.repository.impl.UserActivityCustomMongoRepositoryImpl;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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

  @AfterEach
  void tearDown() {
    mongoTemplate.dropCollection(UserActivity.class);
  }

  // ==========================================
  // 1. 유저 라이프사이클 관리 테스트
  // ==========================================
  @Nested
  @DisplayName("유저 라이프사이클 테스트")
  class LifecycleTest {

    @Test
    @DisplayName("익명화하면 모든 활동 배열이 빈 배열로 초기화된다")
    void 익명화하면_모든_활동_배열이_초기화된다() {
      UUID userId = UUID.randomUUID();
      repository.createUserActivity(UserActivity.of(userId, Instant.now()));
      repository.pushComment(userId, RecentComment.of(UUID.randomUUID(), UUID.randomUUID(), "기사", Instant.now()));
      repository.pushSubscription(userId, RecentSubscription.of(UUID.randomUUID(), "IT", Instant.now()));

      repository.anonymize(userId);

      UserActivity found = repository.findById(userId).orElseThrow();
      assertThat(found.getComments()).isEmpty();
      assertThat(found.getCommentLikes()).isEmpty();
      assertThat(found.getSubscriptions()).isEmpty();
      assertThat(found.getArticleViews()).isEmpty();
    }
  }

  // ==========================================
  // 2. 댓글(Comment) 활동 테스트
  // ==========================================
  @Nested
  @DisplayName("댓글 활동 테스트")
  class CommentTest {

    @Test
    @DisplayName("댓글을 push 하면 배열 맨 앞에 추가되며 최대 10개만 유지된다")
    void push_및_최대_10개_유지_검증() {
      UUID userId = UUID.randomUUID();
      repository.createUserActivity(UserActivity.of(userId, Instant.now()));

      for (int i = 1; i <= 11; i++) {
        repository.pushComment(userId, RecentComment.of(UUID.randomUUID(), UUID.randomUUID(), "기사" + i, Instant.now()));
      }

      UserActivity found = repository.findById(userId).orElseThrow();
      assertThat(found.getComments()).hasSize(10);
      assertThat(found.getComments().get(0).getArticleTitle()).isEqualTo("기사11");
    }

    @Test
    @DisplayName("조회 시 댓글의 ID 목록과 불변 필드를 정확히 확보한다")
    void findById_ID_및_불변필드_확보_검증() {
      UUID userId = UUID.randomUUID();
      UUID commentId = UUID.randomUUID();
      repository.createUserActivity(UserActivity.of(userId, Instant.now()));
      repository.pushComment(userId, RecentComment.of(commentId, UUID.randomUUID(), "불변 기사 제목", Instant.now()));

      UserActivity found = repository.findById(userId).orElseThrow();

      assertThat(found.getComments()).hasSize(1);
      assertThat(found.getComments().get(0).getCommentId()).isEqualTo(commentId);
      assertThat(found.getComments().get(0).getArticleTitle()).isEqualTo("불변 기사 제목");
    }

    @Test
    @DisplayName("commentId로 댓글을 단건 pull 할 수 있다")
    void 단건_pull_검증() {
      UUID userId = UUID.randomUUID();
      UUID commentId = UUID.randomUUID();
      repository.createUserActivity(UserActivity.of(userId, Instant.now()));
      repository.pushComment(userId, RecentComment.of(commentId, UUID.randomUUID(), "기사", Instant.now()));

      repository.pullComment(userId, commentId);

      assertThat(repository.findById(userId).orElseThrow().getComments()).isEmpty();
    }

    @Test
    @DisplayName("articleId로 전체 유저의 댓글을 연쇄 삭제(Cascade)한다")
    void 연쇄_삭제_Cascade_검증() {
      UUID articleId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      repository.createUserActivity(UserActivity.of(userId, Instant.now()));
      repository.pushComment(userId, RecentComment.of(UUID.randomUUID(), articleId, "제목", Instant.now()));

      repository.pullCommentsByArticleId(articleId);

      assertThat(repository.findById(userId).orElseThrow().getComments()).isEmpty();
    }
  }

  // ==========================================
  // 3. 댓글 좋아요(CommentLike) 활동 테스트
  // ==========================================
  @Nested
  @DisplayName("댓글 좋아요 활동 테스트")
  class CommentLikeTest {

    @Test
    @DisplayName("좋아요를 push 하면 배열 맨 앞에 추가되며 최대 10개만 유지된다")
    void push_및_최대_10개_유지_검증() {
      UUID userId = UUID.randomUUID();
      repository.createUserActivity(UserActivity.of(userId, Instant.now()));

      for (int i = 1; i <= 11; i++) {
        repository.pushCommentLike(userId, RecentCommentLike.of(UUID.randomUUID(), UUID.randomUUID(), "제목" + i, Instant.now(), Instant.now()));
      }

      UserActivity found = repository.findById(userId).orElseThrow();
      assertThat(found.getCommentLikes()).hasSize(10);
      assertThat(found.getCommentLikes().get(0).getArticleTitle()).isEqualTo("제목11");
    }

    @Test
    @DisplayName("조회 시 좋아요의 ID 목록과 불변 필드를 정확히 확보한다")
    void findById_ID_및_불변필드_확보_검증() {
      UUID userId = UUID.randomUUID();
      UUID commentId = UUID.randomUUID();
      repository.createUserActivity(UserActivity.of(userId, Instant.now()));
      repository.pushCommentLike(userId, RecentCommentLike.of(commentId, UUID.randomUUID(), "좋아요한 기사", Instant.now(), Instant.now()));

      UserActivity found = repository.findById(userId).orElseThrow();

      assertThat(found.getCommentLikes()).hasSize(1);
      assertThat(found.getCommentLikes().get(0).getCommentId()).isEqualTo(commentId);
      assertThat(found.getCommentLikes().get(0).getArticleTitle()).isEqualTo("좋아요한 기사");
    }

    @Test
    @DisplayName("articleId로 전체 유저의 댓글 좋아요를 연쇄 삭제(Cascade)한다")
    void 연쇄_삭제_Cascade_검증() {
      UUID articleId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      repository.createUserActivity(UserActivity.of(userId, Instant.now()));
      repository.pushCommentLike(userId, RecentCommentLike.of(UUID.randomUUID(), articleId, "제목", Instant.now(), Instant.now()));

      repository.pullCommentLikesByArticleId(articleId);

      assertThat(repository.findById(userId).orElseThrow().getCommentLikes()).isEmpty();
    }
  }

  // ==========================================
  // 4. 구독/관심사(Subscription) 활동 테스트
  // ==========================================
  @Nested
  @DisplayName("구독/관심사 활동 테스트")
  class SubscriptionTest {

    @Test
    @DisplayName("조회 시 관심사의 ID 목록과 불변 필드를 정확히 확보한다")
    void findById_ID_및_불변필드_확보_검증() {
      UUID userId = UUID.randomUUID();
      UUID interestId = UUID.randomUUID();
      repository.createUserActivity(UserActivity.of(userId, Instant.now()));
      repository.pushSubscription(userId, RecentSubscription.of(interestId, "AI", Instant.now()));

      UserActivity found = repository.findById(userId).orElseThrow();

      assertThat(found.getSubscriptions()).hasSize(1);
      assertThat(found.getSubscriptions().get(0).getInterestId()).isEqualTo(interestId);
      assertThat(found.getSubscriptions().get(0).getInterestName()).isEqualTo("AI");
    }

    @Test
    @DisplayName("interestId로 관심사를 단건 pull 할 수 있다")
    void 단건_pull_검증() {
      UUID userId = UUID.randomUUID();
      UUID interestId = UUID.randomUUID();
      repository.createUserActivity(UserActivity.of(userId, Instant.now()));
      repository.pushSubscription(userId, RecentSubscription.of(interestId, "IT", Instant.now()));

      repository.pullSubscription(userId, interestId);

      assertThat(repository.findById(userId).orElseThrow().getSubscriptions()).isEmpty();
    }

    @Test
    @DisplayName("interestId로 전체 유저의 관심사 구독 내역을 연쇄 삭제(Cascade)한다")
    void 연쇄_삭제_Cascade_검증() {
      UUID interestId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      repository.createUserActivity(UserActivity.of(userId, Instant.now()));
      repository.pushSubscription(userId, RecentSubscription.of(interestId, "IT", Instant.now()));

      repository.pullSubscriptionsByInterestId(interestId);

      assertThat(repository.findById(userId).orElseThrow().getSubscriptions()).isEmpty();
    }
  }

  // ==========================================
  // 5. 기사 조회(ArticleView) 활동 테스트
  // ==========================================
  @Nested
  @DisplayName("기사 조회 활동 테스트")
  class ArticleViewTest {

    @Test
    @DisplayName("기사 조회를 push 하면 배열 맨 앞에 추가되며 최대 10개만 유지된다")
    void push_및_최대_10개_유지_검증() {
      UUID userId = UUID.randomUUID();
      repository.createUserActivity(UserActivity.of(userId, Instant.now()));

      for (int i = 1; i <= 11; i++) {
        repository.pushArticleView(userId, RecentArticleView.of(UUID.randomUUID(), "NAVER", "https://url", "제목" + i, Instant.now(), "요약", Instant.now()));
      }

      UserActivity found = repository.findById(userId).orElseThrow();
      assertThat(found.getArticleViews()).hasSize(10);
      assertThat(found.getArticleViews().get(0).getArticleTitle()).isEqualTo("제목11");
    }

    @Test
    @DisplayName("조회 시 기사 조회의 ID 목록과 불변 필드를 정확히 확보한다")
    void findById_ID_및_불변필드_확보_검증() {
      UUID userId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();
      repository.createUserActivity(UserActivity.of(userId, Instant.now()));
      repository.pushArticleView(userId, RecentArticleView.of(articleId, "NAVER", "https://url", "불변 기사 제목", Instant.now(), "요약", Instant.now()));

      UserActivity found = repository.findById(userId).orElseThrow();

      assertThat(found.getArticleViews()).hasSize(1);
      assertThat(found.getArticleViews().get(0).getArticleId()).isEqualTo(articleId);
      assertThat(found.getArticleViews().get(0).getArticleTitle()).isEqualTo("불변 기사 제목");
    }

    @Test
    @DisplayName("articleId로 전체 유저의 기사 조회 내역을 연쇄 삭제(Cascade)한다")
    void 연쇄_삭제_Cascade_검증() {
      UUID articleId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      repository.createUserActivity(UserActivity.of(userId, Instant.now()));
      repository.pushArticleView(userId, RecentArticleView.of(articleId, "NAVER", "https://url", "제목", Instant.now(), "요약", Instant.now()));

      repository.pullArticleViewsByArticleId(articleId);

      assertThat(repository.findById(userId).orElseThrow().getArticleViews()).isEmpty();
    }
  }
}