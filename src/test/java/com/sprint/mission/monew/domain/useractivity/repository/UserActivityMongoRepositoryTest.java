package com.sprint.mission.monew.domain.useractivity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.common.config.MongoContainerConfig;
import com.sprint.mission.monew.domain.useractivity.document.RecentArticleView;
import com.sprint.mission.monew.domain.useractivity.document.RecentComment;
import com.sprint.mission.monew.domain.useractivity.document.RecentCommentLike;
import com.sprint.mission.monew.domain.useractivity.document.RecentSubscription;
import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import java.time.Instant;
import java.util.List;
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

  private UserActivity newActivity(UUID userId) {
    return UserActivity.of(userId, "test@test.com", "테스트유저", Instant.now());
  }

  private RecentComment newComment(UUID commentId, UUID articleId, String title) {
    return RecentComment.of(commentId, articleId, title, UUID.randomUUID(), "테스트유저", "내용", 0L, Instant.now());
  }

  private RecentSubscription newSubscription(UUID subscriptionId, UUID interestId, String name) {
    return RecentSubscription.of(subscriptionId, interestId, name, List.of("키워드"), 1L, Instant.now());
  }

  private RecentCommentLike newCommentLike(UUID likeId, UUID commentId, UUID articleId, String title) {
    return RecentCommentLike.of(likeId, Instant.now(), commentId, articleId, title,
        UUID.randomUUID(), "댓글작성자", "댓글내용", 1L, Instant.now());
  }

  private RecentArticleView newArticleView(UUID articleViewId, UUID viewedBy, UUID articleId, String title) {
    return RecentArticleView.of(articleViewId, viewedBy, Instant.now(), articleId,
        "NAVER", "https://url", title, Instant.now(), "요약", 0L, 1L);
  }

  @Nested
  @DisplayName("유저 라이프사이클 테스트")
  class LifecycleTest {

    @Test
    @DisplayName("익명화하면 email·nickname이 마스킹되고 모든 활동 배열이 빈 배열로 초기화된다")
    void 익명화하면_개인정보가_마스킹되고_모든_활동_배열이_초기화된다() {
      // given
      UUID userId = UUID.randomUUID();
      repository.createUserActivity(newActivity(userId));
      repository.pushComment(userId, newComment(UUID.randomUUID(), UUID.randomUUID(), "기사"));
      repository.pushSubscription(userId, newSubscription(UUID.randomUUID(), UUID.randomUUID(), "IT"));

      // when
      repository.anonymize(userId);

      // then
      UserActivity found = repository.findById(userId).orElseThrow();
      assertThat(found.getEmail()).isEqualTo("");
      assertThat(found.getNickname()).isEqualTo("알 수 없음");
      assertThat(found.getComments()).isEmpty();
      assertThat(found.getCommentLikes()).isEmpty();
      assertThat(found.getSubscriptions()).isEmpty();
      assertThat(found.getArticleViews()).isEmpty();
    }
  }

  @Nested
  @DisplayName("댓글 활동 테스트")
  class CommentTest {

    @Test
    @DisplayName("댓글을 push 하면 배열 맨 앞에 추가되며 최대 10개만 유지된다")
    void push_및_최대_10개_유지_검증() {
      // given
      UUID userId = UUID.randomUUID();
      repository.createUserActivity(newActivity(userId));

      // when
      for (int i = 1; i <= 11; i++) {
        repository.pushComment(userId, newComment(UUID.randomUUID(), UUID.randomUUID(), "기사" + i));
      }

      // then
      UserActivity found = repository.findById(userId).orElseThrow();
      assertThat(found.getComments()).hasSize(10);
      assertThat(found.getComments().get(0).getArticleTitle()).isEqualTo("기사11");
    }

    @Test
    @DisplayName("조회 시 댓글의 ID 목록과 불변 필드를 정확히 확보한다")
    void findById_ID_및_불변필드_확보_검증() {
      // given
      UUID userId = UUID.randomUUID();
      UUID commentId = UUID.randomUUID();
      repository.createUserActivity(newActivity(userId));
      repository.pushComment(userId, newComment(commentId, UUID.randomUUID(), "불변 기사 제목"));

      // when
      UserActivity found = repository.findById(userId).orElseThrow();

      // then
      assertThat(found.getComments()).hasSize(1);
      assertThat(found.getComments().get(0).getId()).isEqualTo(commentId);
      assertThat(found.getComments().get(0).getArticleTitle()).isEqualTo("불변 기사 제목");
    }

    @Test
    @DisplayName("commentId로 댓글을 단건 pull 할 수 있다")
    void 단건_pull_검증() {
      // given
      UUID userId = UUID.randomUUID();
      UUID commentId = UUID.randomUUID();
      repository.createUserActivity(newActivity(userId));
      repository.pushComment(userId, newComment(commentId, UUID.randomUUID(), "기사"));

      // when
      repository.pullComment(userId, commentId);

      // then
      assertThat(repository.findById(userId).orElseThrow().getComments()).isEmpty();
    }

    @Test
    @DisplayName("commentId로 댓글 내용을 수정할 수 있다")
    void 댓글_내용_수정_검증() {
      // given
      UUID userId = UUID.randomUUID();
      UUID commentId = UUID.randomUUID();
      repository.createUserActivity(newActivity(userId));
      repository.pushComment(userId, newComment(commentId, UUID.randomUUID(), "기사"));

      // when
      repository.updateCommentContent(commentId, "수정된 내용");

      // then
      assertThat(repository.findById(userId).orElseThrow().getComments().get(0).getContent())
          .isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("articleId로 전체 유저의 댓글을 연쇄 삭제(Cascade)한다")
    void 연쇄_삭제_Cascade_검증() {
      // given
      UUID articleId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      repository.createUserActivity(newActivity(userId));
      repository.pushComment(userId, newComment(UUID.randomUUID(), articleId, "제목"));

      // when
      repository.pullCommentsByArticleId(articleId);

      // then
      assertThat(repository.findById(userId).orElseThrow().getComments()).isEmpty();
    }
  }


  @Nested
  @DisplayName("댓글 좋아요 활동 테스트")
  class CommentLikeTest {

    @Test
    @DisplayName("좋아요를 push 하면 배열 맨 앞에 추가되며 최대 10개만 유지된다")
    void push_및_최대_10개_유지_검증() {
      // given
      UUID userId = UUID.randomUUID();
      repository.createUserActivity(newActivity(userId));

      // when
      for (int i = 1; i <= 11; i++) {
        repository.pushCommentLike(userId, newCommentLike(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "제목" + i));
      }

      // then
      UserActivity found = repository.findById(userId).orElseThrow();
      assertThat(found.getCommentLikes()).hasSize(10);
      assertThat(found.getCommentLikes().get(0).getArticleTitle()).isEqualTo("제목11");
    }

    @Test
    @DisplayName("조회 시 좋아요의 ID 목록과 불변 필드를 정확히 확보한다")
    void findById_ID_및_불변필드_확보_검증() {
      // given
      UUID userId = UUID.randomUUID();
      UUID commentId = UUID.randomUUID();
      repository.createUserActivity(newActivity(userId));
      repository.pushCommentLike(userId, newCommentLike(UUID.randomUUID(), commentId, UUID.randomUUID(), "좋아요한 기사"));

      // when
      UserActivity found = repository.findById(userId).orElseThrow();

      // then
      assertThat(found.getCommentLikes()).hasSize(1);
      assertThat(found.getCommentLikes().get(0).getCommentId()).isEqualTo(commentId);
      assertThat(found.getCommentLikes().get(0).getArticleTitle()).isEqualTo("좋아요한 기사");
    }

    @Test
    @DisplayName("commentId로 댓글 좋아요를 단건 pull 할 수 있다")
    void 단건_pull_검증() {
      // given
      UUID userId = UUID.randomUUID();
      UUID commentId = UUID.randomUUID();
      repository.createUserActivity(newActivity(userId));
      repository.pushCommentLike(userId, newCommentLike(UUID.randomUUID(), commentId, UUID.randomUUID(), "기사"));

      // when
      repository.pullCommentLike(userId, commentId);

      // then
      assertThat(repository.findById(userId).orElseThrow().getCommentLikes()).isEmpty();
    }

    @Test
    @DisplayName("articleId로 전체 유저의 댓글 좋아요를 연쇄 삭제(Cascade)한다")
    void 연쇄_삭제_Cascade_검증() {
      // given
      UUID articleId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      repository.createUserActivity(newActivity(userId));
      repository.pushCommentLike(userId, newCommentLike(UUID.randomUUID(), UUID.randomUUID(), articleId, "제목"));

      // when
      repository.pullCommentLikesByArticleId(articleId);

      // then
      assertThat(repository.findById(userId).orElseThrow().getCommentLikes()).isEmpty();
    }
  }


  @Nested
  @DisplayName("구독/관심사 활동 테스트")
  class SubscriptionTest {

    @Test
    @DisplayName("조회 시 관심사의 ID 목록과 불변 필드를 정확히 확보한다")
    void findById_ID_및_불변필드_확보_검증() {
      // given
      UUID userId = UUID.randomUUID();
      UUID interestId = UUID.randomUUID();
      repository.createUserActivity(newActivity(userId));
      repository.pushSubscription(userId, newSubscription(UUID.randomUUID(), interestId, "AI"));

      // when
      UserActivity found = repository.findById(userId).orElseThrow();

      // then
      assertThat(found.getSubscriptions()).hasSize(1);
      assertThat(found.getSubscriptions().get(0).getInterestId()).isEqualTo(interestId);
      assertThat(found.getSubscriptions().get(0).getInterestName()).isEqualTo("AI");
    }

    @Test
    @DisplayName("interestId로 관심사를 단건 pull 할 수 있다")
    void 단건_pull_검증() {
      // given
      UUID userId = UUID.randomUUID();
      UUID interestId = UUID.randomUUID();
      repository.createUserActivity(newActivity(userId));
      repository.pushSubscription(userId, newSubscription(UUID.randomUUID(), interestId, "IT"));

      // when
      repository.pullSubscription(userId, interestId);

      // then
      assertThat(repository.findById(userId).orElseThrow().getSubscriptions()).isEmpty();
    }

    @Test
    @DisplayName("interestId로 전체 유저의 관심사 구독 내역을 연쇄 삭제(Cascade)한다")
    void 연쇄_삭제_Cascade_검증() {
      // given
      UUID interestId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      repository.createUserActivity(newActivity(userId));
      repository.pushSubscription(userId, newSubscription(UUID.randomUUID(), interestId, "IT"));

      // when
      repository.pullSubscriptionsByInterestId(interestId);

      // then
      assertThat(repository.findById(userId).orElseThrow().getSubscriptions()).isEmpty();
    }
  }

  @Nested
  @DisplayName("기사 조회 활동 테스트")
  class ArticleViewTest {

    @Test
    @DisplayName("기사 조회를 push 하면 배열 맨 앞에 추가되며 최대 10개만 유지된다")
    void push_및_최대_10개_유지_검증() {
      // given
      UUID userId = UUID.randomUUID();
      repository.createUserActivity(newActivity(userId));

      // when
      for (int i = 1; i <= 11; i++) {
        repository.pushArticleView(userId, newArticleView(UUID.randomUUID(), userId, UUID.randomUUID(), "제목" + i));
      }

      // then
      UserActivity found = repository.findById(userId).orElseThrow();
      assertThat(found.getArticleViews()).hasSize(10);
      assertThat(found.getArticleViews().get(0).getArticleTitle()).isEqualTo("제목11");
    }

    @Test
    @DisplayName("조회 시 기사 조회의 ID 목록과 불변 필드를 정확히 확보한다")
    void findById_ID_및_불변필드_확보_검증() {
      // given
      UUID userId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();
      repository.createUserActivity(newActivity(userId));
      repository.pushArticleView(userId, newArticleView(UUID.randomUUID(), userId, articleId, "불변 기사 제목"));

      // when
      UserActivity found = repository.findById(userId).orElseThrow();

      // then
      assertThat(found.getArticleViews()).hasSize(1);
      assertThat(found.getArticleViews().get(0).getArticleId()).isEqualTo(articleId);
      assertThat(found.getArticleViews().get(0).getArticleTitle()).isEqualTo("불변 기사 제목");
    }

    @Test
    @DisplayName("articleId로 전체 유저의 기사 조회 내역을 연쇄 삭제(Cascade)한다")
    void 연쇄_삭제_Cascade_검증() {
      // given
      UUID articleId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      repository.createUserActivity(newActivity(userId));
      repository.pushArticleView(userId, newArticleView(UUID.randomUUID(), userId, articleId, "제목"));

      // when
      repository.pullArticleViewsByArticleId(articleId);

      // then
      assertThat(repository.findById(userId).orElseThrow().getArticleViews()).isEmpty();
    }
  }
}
