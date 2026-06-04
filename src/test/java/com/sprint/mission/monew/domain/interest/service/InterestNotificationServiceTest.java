package com.sprint.mission.monew.domain.interest.service;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.repository.InterestRepository;
import com.sprint.mission.monew.domain.interest.repository.SubscriptionRepository;
import com.sprint.mission.monew.domain.interest.repository.dto.InterestSubscriber;
import com.sprint.mission.monew.domain.notification.service.NotificationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterestNotificationServiceTest {

  @InjectMocks
  InterestNotificationService interestNotificationService;
  @Mock
  ArticleRepository articleRepository;
  @Mock
  InterestRepository interestRepository;
  @Mock
  SubscriptionRepository subscriptionRepository;
  @Mock
  NotificationService notificationService;

  @Test
  @DisplayName("신규 기사를 관심사별로 집계해 구독자당 알림 1건을 생성한다")
  void 신규_기사를_관심사별로_집계해_구독자당_알림_1건을_생성한다() {
    // given — 신규 기사 3건: 2건은 관심사 A, 1건은 관심사 B에 매칭
    Instant since = Instant.now();
    Article a1 = article("AI 기사1", "요약1");
    Article a2 = article("AI 기사2", "요약2");
    Article a3 = article("경제 기사", "요약3");
    given(articleRepository.findByCreatedAtAfterAndDeletedAtIsNull(since))
        .willReturn(List.of(a1, a2, a3));

    UUID interestAId = UUID.randomUUID();
    UUID interestBId = UUID.randomUUID();
    Interest interestA = interest(interestAId, "인공지능");
    Interest interestB = interest(interestBId, "경제");
    given(interestRepository.findMatchingInterests("AI 기사1", "요약1")).willReturn(List.of(interestA));
    given(interestRepository.findMatchingInterests("AI 기사2", "요약2")).willReturn(List.of(interestA));
    given(interestRepository.findMatchingInterests("경제 기사", "요약3")).willReturn(List.of(interestB));

    UUID u1 = UUID.randomUUID();
    UUID u2 = UUID.randomUUID();
    UUID u3 = UUID.randomUUID();
    InterestSubscriber s1 = subscriber(interestAId, u1);
    InterestSubscriber s2 = subscriber(interestAId, u2);
    InterestSubscriber s3 = subscriber(interestBId, u3);
    given(subscriptionRepository.findSubscribersByInterestIds(anyList()))
        .willReturn(List.of(s1, s2, s3));

    // when
    interestNotificationService.notifyNewArticles(since);

    // then — A는 2건/구독자 2명, B는 1건/구독자 1명으로 각각 1번씩 생성 요청
    then(notificationService).should()
        .createArticleNotifications(
            interestAId, "[인공지능]와 관련된 기사가 2건 등록되었습니다.", List.of(u1, u2));
    then(notificationService).should()
        .createArticleNotifications(
            interestBId, "[경제]와 관련된 기사가 1건 등록되었습니다.", List.of(u3));
  }

  @Test
  @DisplayName("신규 기사가 없으면 아무 알림도 생성하지 않는다")
  void 신규_기사가_없으면_아무_알림도_생성하지_않는다() {
    // given
    Instant since = Instant.now();
    given(articleRepository.findByCreatedAtAfterAndDeletedAtIsNull(since)).willReturn(List.of());

    // when
    interestNotificationService.notifyNewArticles(since);

    // then
    then(notificationService).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("매칭됐지만 구독자가 없는 관심사는 알림을 생성하지 않는다")
  void 구독자가_없는_관심사는_알림을_생성하지_않는다() {
    // given
    Instant since = Instant.now();
    Article a1 = article("AI 기사", "요약");
    given(articleRepository.findByCreatedAtAfterAndDeletedAtIsNull(since)).willReturn(List.of(a1));

    UUID interestAId = UUID.randomUUID();
    Interest interestA = mock(Interest.class);
    given(interestA.getId()).willReturn(interestAId);
    given(interestRepository.findMatchingInterests("AI 기사", "요약")).willReturn(List.of(interestA));
    given(subscriptionRepository.findSubscribersByInterestIds(anyList())).willReturn(List.of());

    // when
    interestNotificationService.notifyNewArticles(since);

    // then
    then(notificationService).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("매칭되는 관심사가 없으면 구독자 조회 없이 알림을 생성하지 않는다")
  void 매칭되는_관심사가_없으면_알림을_생성하지_않는다() {
    // given
    Instant since = Instant.now();
    Article a1 = article("랜덤 기사", "내용");
    given(articleRepository.findByCreatedAtAfterAndDeletedAtIsNull(since)).willReturn(List.of(a1));
    given(interestRepository.findMatchingInterests("랜덤 기사", "내용")).willReturn(List.of());

    // when
    interestNotificationService.notifyNewArticles(since);

    // then
    then(subscriptionRepository).shouldHaveNoInteractions();
    then(notificationService).shouldHaveNoInteractions();
  }

  private Article article(String title, String summary) {
    Article article = mock(Article.class);
    given(article.getTitle()).willReturn(title);
    given(article.getSummary()).willReturn(summary);
    return article;
  }

  private Interest interest(UUID id, String name) {
    Interest interest = mock(Interest.class);
    given(interest.getId()).willReturn(id);
    given(interest.getName()).willReturn(name);
    return interest;
  }

  private InterestSubscriber subscriber(UUID interestId, UUID userId) {
    InterestSubscriber subscriber = mock(InterestSubscriber.class);
    given(subscriber.getInterestId()).willReturn(interestId);
    given(subscriber.getUserId()).willReturn(userId);
    return subscriber;
  }
}