package com.sprint.mission.monew.domain.useractivity.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sprint.mission.monew.domain.useractivity.document.RecentArticleView;
import com.sprint.mission.monew.domain.useractivity.document.RecentComment;
import com.sprint.mission.monew.domain.useractivity.document.RecentCommentLike;
import com.sprint.mission.monew.domain.useractivity.document.RecentSubscription;
import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import com.sprint.mission.monew.domain.useractivity.repository.UserActivityMongoRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserActivityEventListenerTest {

  @Mock
  UserActivityMongoRepository userActivityMongoRepository;

  @InjectMocks
  UserActivityEventListener listener;

  @Captor ArgumentCaptor<UserActivity> activityCaptor;
  @Captor ArgumentCaptor<RecentSubscription> subscriptionCaptor;
  @Captor ArgumentCaptor<RecentComment> commentCaptor;
  @Captor ArgumentCaptor<RecentCommentLike> commentLikeCaptor;
  @Captor ArgumentCaptor<RecentArticleView> articleViewCaptor;

  @Nested
  @DisplayName("UserCreatedEvent")
  class UserCreated {
    @Test
    @DisplayName("UserCreatedEvent를 받으면 email, nickname 포함 UserActivity 도큐먼트를 생성한다")
    void UserCreatedEvent를_받으면_email과_nickname_포함_UserActivity를_생성한다() {
      // given
      UUID userId = UUID.randomUUID();
      Instant now = Instant.now();

      // when
      listener.handle(new UserCreatedEvent(userId, "test@test.com", "테스트유저", now));

      // then
      verify(userActivityMongoRepository).createUserActivity(activityCaptor.capture());
      UserActivity captured = activityCaptor.getValue();
      assertThat(captured.getId()).isEqualTo(userId);
      assertThat(captured.getEmail()).isEqualTo("test@test.com");
      assertThat(captured.getNickname()).isEqualTo("테스트유저");
    }
  }

  @Nested
  @DisplayName("UserDeletedEvent")
  class UserDeleted {
    @Test
    @DisplayName("UserDeletedEvent를 받으면 anonymize와 anonymizeCommentLikesByCommentUserId를 모두 호출한다")
    void UserDeletedEvent를_받으면_anonymize와_anonymizeCommentLikes를_호출한다() {
      // given
      UUID userId = UUID.randomUUID();

      // when
      listener.handle(new UserDeletedEvent(userId));

      // then
      verify(userActivityMongoRepository).anonymize(userId);
      verify(userActivityMongoRepository).anonymizeCommentLikesByCommentUserId(userId);
    }
  }

  @Nested
  @DisplayName("UserNicknameUpdatedEvent")
  class UserNicknameUpdated {
    @Test
    @DisplayName("UserNicknameUpdatedEvent를 받으면 MongoDB nickname을 업데이트한다")
    void UserNicknameUpdatedEvent를_받으면_MongoDB_nickname을_업데이트한다() {
      // given
      UUID userId = UUID.randomUUID();

      // when
      listener.handle(new UserNicknameUpdatedEvent(userId, "새닉네임"));

      // then
      verify(userActivityMongoRepository).updateNickname(userId, "새닉네임");
    }
  }

  @Nested
  @DisplayName("SubscriptionCreatedEvent")
  class SubscriptionCreated {
    @Test
    @DisplayName("SubscriptionCreatedEvent를 받으면 {interestId, interestName, keywords, subscriberCount, subscribedAt} 추출 및 push")
    void SubscriptionCreatedEvent를_받으면_interestId와_keywords를_push한다() {
      // given
      UUID userId = UUID.randomUUID();
      UUID subscriptionId = UUID.randomUUID();
      UUID interestId = UUID.randomUUID();
      Instant subscribedAt = Instant.now();
      String interestName = "IT 기술";
      List<String> keywords = List.of("java", "spring");
      long subscriberCount = 5L;

      SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
          userId, subscriptionId, interestId, interestName, keywords, subscriberCount, subscribedAt);

      // when
      listener.handle(event);

      // then
      verify(userActivityMongoRepository).pushSubscription(eq(userId), subscriptionCaptor.capture());
      RecentSubscription captured = subscriptionCaptor.getValue();
      assertThat(captured.getInterestId()).isEqualTo(interestId);
      assertThat(captured.getInterestName()).isEqualTo(interestName);
      assertThat(captured.getInterestKeywords()).isEqualTo(keywords);
      assertThat(captured.getInterestSubscriberCount()).isEqualTo(subscriberCount);
      assertThat(captured.getCreatedAt()).isNotNull();
    }
  }

  @Nested
  @DisplayName("SubscriptionCancelledEvent")
  class SubscriptionCancelled {
    @Test
    @DisplayName("SubscriptionCancelledEvent를 받으면 pullSubscription을 호출한다")
    void SubscriptionCancelledEvent를_받으면_pullSubscription을_호출한다() {
      // given
      UUID userId = UUID.randomUUID();
      UUID targetId = UUID.randomUUID();

      // when
      listener.handle(new SubscriptionCancelledEvent(userId, targetId));

      // then
      verify(userActivityMongoRepository).pullSubscription(userId, targetId);
    }
  }

  @Nested
  @DisplayName("CommentCreatedEvent")
  class CommentCreated {
    @Test
    @DisplayName("CommentCreatedEvent를 받으면 {commentId, articleId, articleTitle, userId, userNickname, content, likeCount, createdAt} 추출 및 push")
    void CommentCreatedEvent를_받으면_모든_필드를_push한다() {
      // given
      UUID userId = UUID.randomUUID();
      UUID commentId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();
      String articleTitle = "TDD 정석 가이드";
      String content = "댓글 내용입니다";
      String userNickname = "작성자";
      Instant createdAt = Instant.now();

      CommentCreatedEvent event = new CommentCreatedEvent(userId, commentId, articleId, articleTitle, content, userNickname, 0L, createdAt);

      // when
      listener.handle(event);

      // then
      verify(userActivityMongoRepository).pushComment(eq(userId), commentCaptor.capture());
      RecentComment captured = commentCaptor.getValue();
      assertThat(captured.getId()).isEqualTo(commentId);
      assertThat(captured.getArticleId()).isEqualTo(articleId);
      assertThat(captured.getArticleTitle()).isEqualTo(articleTitle);
      assertThat(captured.getUserId()).isEqualTo(userId);
      assertThat(captured.getUserNickname()).isEqualTo(userNickname);
      assertThat(captured.getContent()).isEqualTo(content);
      assertThat(captured.getLikeCount()).isZero();
      assertThat(captured.getCreatedAt()).isEqualTo(createdAt);
    }
  }

  @Nested
  @DisplayName("CommentUpdatedEvent")
  class CommentUpdated {
    @Test
    @DisplayName("CommentUpdatedEvent를 받으면 MongoDB comments 배열의 content를 업데이트한다")
    void CommentUpdatedEvent를_받으면_content를_업데이트한다() {
      // given
      UUID commentId = UUID.randomUUID();
      String newContent = "수정된 댓글 내용";

      // when
      listener.handle(new CommentUpdatedEvent(commentId, newContent));

      // then
      verify(userActivityMongoRepository).updateCommentContent(commentId, newContent);
    }
  }

  @Nested
  @DisplayName("CommentDeletedEvent")
  class CommentDeleted {
    @Test
    @DisplayName("CommentDeletedEvent를 받으면 작성자의 comments 배열에서 해당 댓글을 pull한다")
    void CommentDeletedEvent를_받으면_작성자의_comments에서_pull한다() {
      // given
      UUID authorId = UUID.randomUUID();
      UUID commentId = UUID.randomUUID();

      // when
      listener.handle(new CommentDeletedEvent(authorId, commentId));

      // then
      verify(userActivityMongoRepository).pullComment(authorId, commentId);
    }
  }

  @Nested
  @DisplayName("CommentLikedEvent")
  class CommentLiked {
    @Test
    @DisplayName("CommentLikedEvent를 받으면 모든 필드 추출 및 push")
    void CommentLikedEvent를_받으면_모든_필드를_push한다() {
      // given
      UUID userId = UUID.randomUUID();
      UUID commentId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();
      UUID commentUserId = UUID.randomUUID();
      String articleTitle = "클린 아키텍처";
      String commentUserNickname = "댓글작성자";
      String commentContent = "댓글내용";
      Instant commentCreatedAt = Instant.now().minusSeconds(3600);
      Instant likedAt = Instant.now();
      long commentLikeCount = 3L;

      CommentLikedEvent event = new CommentLikedEvent(
          userId, UUID.randomUUID(), likedAt,
          commentId, articleId, articleTitle,
          commentUserId, commentUserNickname, commentContent,
          commentLikeCount, commentCreatedAt
      );

      // when
      listener.handle(event);

      // then
      verify(userActivityMongoRepository).pushCommentLike(eq(userId), commentLikeCaptor.capture());
      RecentCommentLike captured = commentLikeCaptor.getValue();
      assertThat(captured.getCommentId()).isEqualTo(commentId);
      assertThat(captured.getArticleId()).isEqualTo(articleId);
      assertThat(captured.getArticleTitle()).isEqualTo(articleTitle);
      assertThat(captured.getCommentUserId()).isEqualTo(commentUserId);
      assertThat(captured.getCommentUserNickname()).isEqualTo(commentUserNickname);
      assertThat(captured.getCommentContent()).isEqualTo(commentContent);
      assertThat(captured.getCommentLikeCount()).isEqualTo(commentLikeCount);
      assertThat(captured.getCommentCreatedAt()).isEqualTo(commentCreatedAt);
      assertThat(captured.getCreatedAt()).isEqualTo(likedAt);
    }
  }

  @Nested
  @DisplayName("CommentLikeRemovedEvent")
  class CommentLikeRemoved {
    @Test
    @DisplayName("CommentLikeRemovedEvent를 받으면 pullCommentLike를 호출한다")
    void CommentLikeRemovedEvent를_받으면_pullCommentLike를_호출한다() {
      // given
      UUID userId = UUID.randomUUID();
      UUID commentId = UUID.randomUUID();

      // when
      listener.handle(new CommentLikeRemovedEvent(userId, commentId));

      // then
      verify(userActivityMongoRepository).pullCommentLike(userId, commentId);
    }
  }

  @Nested
  @DisplayName("ArticleViewedEvent")
  class ArticleViewed {
    @Test
    @DisplayName("ArticleViewedEvent를 받으면 모든 필드 추출 및 push")
    void ArticleViewedEvent를_받으면_모든_필드를_push한다() {
      // given
      UUID userId = UUID.randomUUID();
      UUID articleViewId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();
      Instant viewTime = Instant.now();

      ArticleViewedEvent event = new ArticleViewedEvent(
          userId, articleViewId, viewTime, articleId,
          "NAVER", "https://url", "오늘의 뉴스", Instant.now(), "기사 요약",
          10L, 100L
      );

      // when
      listener.handle(event);

      // then
      verify(userActivityMongoRepository).pushArticleView(eq(userId), articleViewCaptor.capture());
      RecentArticleView captured = articleViewCaptor.getValue();
      assertThat(captured.getArticleId()).isEqualTo(articleId);
      assertThat(captured.getArticleTitle()).isEqualTo("오늘의 뉴스");
      assertThat(captured.getSource()).isEqualTo("NAVER");
      assertThat(captured.getViewedBy()).isEqualTo(userId);
      assertThat(captured.getId()).isEqualTo(articleViewId);
      assertThat(captured.getArticleCommentCount()).isEqualTo(10L);
      assertThat(captured.getArticleViewCount()).isEqualTo(100L);
      assertThat(captured.getCreatedAt()).isEqualTo(viewTime);
    }
  }

  @Nested
  @DisplayName("ArticleDeletedEvent")
  class ArticleDeleted {
    @Test
    @DisplayName("ArticleDeletedEvent를 받으면 연쇄 삭제(Cascade) pull 3개 메서드를 호출한다")
    void ArticleDeletedEvent를_받으면_cascade_pull_3개를_호출한다() {
      // given
      UUID articleId = UUID.randomUUID();

      // when
      listener.handle(new ArticleDeletedEvent(articleId));

      // then
      verify(userActivityMongoRepository).pullArticleViewsByArticleId(articleId);
      verify(userActivityMongoRepository).pullCommentsByArticleId(articleId);
      verify(userActivityMongoRepository).pullCommentLikesByArticleId(articleId);
    }
  }

  @Nested
  @DisplayName("InterestDeletedEvent")
  class InterestDeleted {
    @Test
    @DisplayName("InterestDeletedEvent를 받으면 pullSubscriptionsByInterestId를 호출한다")
    void InterestDeletedEvent를_받으면_pullSubscriptionsByInterestId를_호출한다() {
      // given
      UUID interestId = UUID.randomUUID();

      // when
      listener.handle(new InterestDeletedEvent(interestId));

      // then
      verify(userActivityMongoRepository).pullSubscriptionsByInterestId(interestId);
    }
  }
}
