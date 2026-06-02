package com.sprint.mission.monew.domain.notification.listener;

import com.sprint.mission.monew.domain.article.event.ArticleCreatedEvent;
import com.sprint.mission.monew.domain.comment.event.CommentLikedEvent;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.repository.InterestRepository;
import com.sprint.mission.monew.domain.interest.repository.SubscriptionRepository;
import com.sprint.mission.monew.domain.interest.repository.dto.InterestSubscriber;
import com.sprint.mission.monew.domain.notification.service.NotificationService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private final InterestRepository interestRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final NotificationService notificationService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCommentLiked(CommentLikedEvent event) {
    notificationService.createCommentLikeNotification(
        event.commentId(), event.commentAuthorId(), event.likerNickname());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleArticleCreated(ArticleCreatedEvent event) {
    String title = event.article().getTitle();
    String summary = event.article().getSummary();

    List<Interest> interests = interestRepository.findMatchingInterests(title, summary);
    if (interests.isEmpty()) {
      return;
    }

    List<UUID> interestIds = interests.stream().map(Interest::getId).toList();
    Map<UUID, List<UUID>> subscribersByInterest =
        subscriptionRepository.findSubscribersByInterestIds(interestIds).stream()
            .collect(
                Collectors.groupingBy(
                    InterestSubscriber::getInterestId,
                    Collectors.mapping(InterestSubscriber::getUserId, Collectors.toList())));

    for (Interest interest : interests) {
      List<UUID> subscriberIds =
          subscribersByInterest.getOrDefault(interest.getId(), List.of());
      notificationService.createArticleNotifications(
          interest.getId(), interest.getName(), subscriberIds);
    }
    log.info("기사 등록 이벤트 처리 완료: 매칭 관심사={}개, 기사 제목={}", interests.size(), title);
  }
}
