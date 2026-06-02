package com.sprint.mission.monew.domain.notification.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.event.ArticleCreatedEvent;
import com.sprint.mission.monew.domain.comment.event.CommentLikedEvent;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.repository.InterestRepository;
import com.sprint.mission.monew.domain.interest.repository.SubscriptionRepository;
import com.sprint.mission.monew.domain.interest.repository.dto.InterestSubscriber;
import com.sprint.mission.monew.domain.notification.service.NotificationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

  @InjectMocks NotificationEventListener notificationEventListener;

  @Mock InterestRepository interestRepository;

  @Mock SubscriptionRepository subscriptionRepository;

  @Mock NotificationService notificationService;

  Article article;
  ArticleCreatedEvent event;

  @BeforeEach
  void setUp() {
    article =
        Article.create(
            ArticleSource.NAVER,
            "https://example.com/news",
            "인공지능 관련 뉴스",
            Instant.now(),
            "AI 기술 발전 요약");
    event = new ArticleCreatedEvent(article);
  }

  private static InterestSubscriber subscriber(UUID interestId, UUID userId) {
    return new InterestSubscriber() {
      @Override
      public UUID getInterestId() {
        return interestId;
      }

      @Override
      public UUID getUserId() {
        return userId;
      }
    };
  }

  @Nested
  @DisplayName("CommentLikedEvent 처리")
  class HandleCommentLiked {

    @Test
    @DisplayName("댓글 좋아요 이벤트 수신 시 알림 생성을 위임한다")
    void 댓글_좋아요_이벤트_수신_시_알림_생성을_위임한다() {
      // given
      UUID commentId = UUID.randomUUID();
      UUID commentAuthorId = UUID.randomUUID();
      String likerNickname = "닉네임";
      CommentLikedEvent event = new CommentLikedEvent(commentId, commentAuthorId, likerNickname);

      // when
      notificationEventListener.handleCommentLiked(event);

      // then
      then(notificationService)
          .should()
          .createCommentLikeNotification(commentId, commentAuthorId, likerNickname);
    }
  }

  @Nested
  @DisplayName("ArticleCreatedEvent 처리")
  class HandleArticleCreated {

    @Test
    @DisplayName("매칭 관심사가 없으면 알림 생성을 호출하지 않는다")
    void 매칭_관심사가_없으면_알림_생성을_호출하지_않는다() {
      // given
      given(interestRepository.findMatchingInterests(article.getTitle(), article.getSummary()))
          .willReturn(List.of());

      // when
      notificationEventListener.handleArticleCreated(event);

      // then
      then(notificationService).should(never()).createArticleNotifications(any(), any(), any());
    }

    @Test
    @DisplayName("매칭 관심사마다 구독자를 조회하여 알림 생성을 위임한다")
    void 매칭_관심사마다_구독자를_조회하여_알림_생성을_위임한다() {
      // given
      Interest interest1 = Interest.create("인공지능", List.of("AI", "인공지능"));
      Interest interest2 = Interest.create("테크", List.of("기술", "tech"));
      List<UUID> subscriberIds1 = List.of(UUID.randomUUID(), UUID.randomUUID());
      List<UUID> subscriberIds2 = List.of(UUID.randomUUID());

      List<InterestSubscriber> batchResult =
          List.of(
              subscriber(interest1.getId(), subscriberIds1.get(0)),
              subscriber(interest1.getId(), subscriberIds1.get(1)),
              subscriber(interest2.getId(), subscriberIds2.get(0)));

      given(interestRepository.findMatchingInterests(article.getTitle(), article.getSummary()))
          .willReturn(List.of(interest1, interest2));
      given(
              subscriptionRepository.findSubscribersByInterestIds(
                  List.of(interest1.getId(), interest2.getId())))
          .willReturn(batchResult);

      // when
      notificationEventListener.handleArticleCreated(event);

      // then
      then(notificationService)
          .should()
          .createArticleNotifications(interest1.getId(), interest1.getName(), subscriberIds1);
      then(notificationService)
          .should()
          .createArticleNotifications(interest2.getId(), interest2.getName(), subscriberIds2);
    }
  }
}
