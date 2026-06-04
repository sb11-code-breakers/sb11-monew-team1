package com.sprint.mission.monew.domain.interest.service;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.repository.InterestRepository;
import com.sprint.mission.monew.domain.interest.repository.SubscriptionRepository;
import com.sprint.mission.monew.domain.interest.repository.dto.InterestSubscriber;
import com.sprint.mission.monew.domain.notification.service.NotificationService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterestNotificationService {

  private final ArticleRepository articleRepository;
  private final InterestRepository interestRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final NotificationService notificationService;

  @Transactional
  public void notifyNewArticles(Instant since) {
    List<Article> newArticles = articleRepository.findByCreatedAtAfterAndDeletedAtIsNull(since);
    if (newArticles.isEmpty()) {
      log.info("배치 집계 대상 신규 기사 없음");
      return;
    }

    Map<Interest, Long> countByInterest = new LinkedHashMap<>();
    for (Article article : newArticles) {
      List<Interest> matched =
          interestRepository.findMatchingInterests(article.getTitle(), article.getSummary());
      for (Interest interest : matched) {
        countByInterest.merge(interest, 1L, Long::sum);
      }
    }

    if (countByInterest.isEmpty()) {
      return;
    }

    List<UUID> interestIds = countByInterest.keySet().stream().map(Interest::getId).toList();
    Map<UUID, List<UUID>> subscribersByInterest =
        subscriptionRepository.findSubscribersByInterestIds(interestIds).stream()
            .collect(
                Collectors.groupingBy(
                    InterestSubscriber::getInterestId,
                    Collectors.mapping(InterestSubscriber::getUserId, Collectors.toList())));

    for (Map.Entry<Interest, Long> entry : countByInterest.entrySet()) {
      Interest interest = entry.getKey();
      int count = entry.getValue().intValue();
      List<UUID> subscriberIds =
          subscribersByInterest.getOrDefault(interest.getId(), List.of());
      if (!subscriberIds.isEmpty()) {
        String message = "[" + interest.getName() + "]와 관련된 기사가 " + count + "건 등록되었습니다.";
        notificationService.createArticleNotifications(interest.getId(), message, subscriberIds);
      }
    }

    log.info("배치 기사 알림 집계 완료: 신규 기사={}건, 매칭 관심사={}개", newArticles.size(), countByInterest.size());
  }
}