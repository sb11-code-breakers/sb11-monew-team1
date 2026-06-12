package com.sprint.mission.monew.domain.article.service;

import com.sprint.mission.monew.domain.article.event.ArticleNotificationEvent;
import com.sprint.mission.monew.domain.article.repository.ArticleInterestRepository;
import com.sprint.mission.monew.domain.article.repository.dto.InterestArticleCount;
import com.sprint.mission.monew.domain.interest.repository.SubscriptionRepository;
import com.sprint.mission.monew.domain.interest.repository.dto.InterestSubscriber;
import com.sprint.mission.monew.domain.notification.entity.ResourceType;
import org.springframework.context.ApplicationEventPublisher;
import java.time.Instant;
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
public class ArticleNotificationService {

  private final ArticleInterestRepository articleInterestRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public void notifyNewArticles(Instant since) {
    List<InterestArticleCount> counts = articleInterestRepository.countByInterestSince(since);
    if (counts.isEmpty()) {
      log.info("배치 집계 대상 신규 기사 없음");
      return;
    }

    List<UUID> interestIds = counts.stream().map(InterestArticleCount::getInterestId).toList();

    Map<UUID, List<UUID>> subscribersByInterest =
        subscriptionRepository.findSubscribersByInterestIds(interestIds).stream()
            .collect(Collectors.groupingBy(
                InterestSubscriber::getInterestId,
                Collectors.mapping(InterestSubscriber::getUserId, Collectors.toList())));

    for (InterestArticleCount count : counts) {
      List<UUID> subscriberIds =
          subscribersByInterest.getOrDefault(count.getInterestId(), List.of());
      if (subscriberIds.isEmpty()) continue;
      String message = "[" + count.getInterestName() + "]와 관련된 기사가 "
          + count.getArticleCount() + "건 등록되었습니다.";
      eventPublisher.publishEvent(new ArticleNotificationEvent(subscriberIds, message, ResourceType.ARTICLE, count.getInterestId()));
    }

    long totalMatches = counts.stream().mapToLong(InterestArticleCount::getArticleCount).sum();
    log.info("배치 기사 알림 집계 완료: 관심사-기사 매칭={}건, 매칭 관심사={}개", totalMatches, counts.size());
  }
}