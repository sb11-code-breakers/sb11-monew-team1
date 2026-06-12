package com.sprint.mission.monew.domain.article.service;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.sprint.mission.monew.domain.article.event.ArticleNotificationEvent;
import com.sprint.mission.monew.domain.article.repository.ArticleInterestRepository;
import com.sprint.mission.monew.domain.notification.entity.ResourceType;
import com.sprint.mission.monew.domain.article.repository.dto.InterestArticleCount;
import com.sprint.mission.monew.domain.interest.repository.SubscriptionRepository;
import com.sprint.mission.monew.domain.interest.repository.dto.InterestSubscriber;
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
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ArticleNotificationServiceTest {

  @InjectMocks
  ArticleNotificationService articleNotificationService;
  @Mock
  ArticleInterestRepository articleInterestRepository;
  @Mock
  SubscriptionRepository subscriptionRepository;
  @Mock
  ApplicationEventPublisher eventPublisher;

  @BeforeEach
  void setUp() {
    // MockitoExtension이 @BeforeEach마다 mock을 재생성하므로 상태 공유 없음
  }

  @Nested
  @DisplayName("notifyNewArticles")
  class NotifyNewArticles {

    @Test
    @DisplayName("신규 기사를 관심사별로 집계해 구독자당 알림 1건을 생성한다")
    void 신규_기사를_관심사별로_집계해_구독자당_알림_1건을_생성한다() {
      // given — 관심사 A에 기사 2건, 관심사 B에 기사 1건
      Instant since = Instant.now();
      UUID interestAId = UUID.randomUUID();
      UUID interestBId = UUID.randomUUID();

      InterestArticleCount countA = interestArticleCount(interestAId, "인공지능", 2L);
      InterestArticleCount countB = interestArticleCount(interestBId, "경제", 1L);
      given(articleInterestRepository.countByInterestSince(since))
          .willReturn(List.of(countA, countB));

      UUID u1 = UUID.randomUUID();
      UUID u2 = UUID.randomUUID();
      UUID u3 = UUID.randomUUID();
      InterestSubscriber s1 = subscriber(interestAId, u1);
      InterestSubscriber s2 = subscriber(interestAId, u2);
      InterestSubscriber s3 = subscriber(interestBId, u3);
      given(subscriptionRepository.findSubscribersByInterestIds(anyList()))
          .willReturn(List.of(s1, s2, s3));

      // when
      articleNotificationService.notifyNewArticles(since);

      // then — A는 2건/구독자 2명, B는 1건/구독자 1명
      then(eventPublisher).should()
          .publishEvent(new ArticleNotificationEvent(
              List.of(u1, u2), "[인공지능]와 관련된 기사가 2건 등록되었습니다.", ResourceType.ARTICLE, interestAId));
      then(eventPublisher).should()
          .publishEvent(new ArticleNotificationEvent(
              List.of(u3), "[경제]와 관련된 기사가 1건 등록되었습니다.", ResourceType.ARTICLE, interestBId));
    }

    @Test
    @DisplayName("신규 기사가 없으면 아무 알림도 생성하지 않는다")
    void 신규_기사가_없으면_아무_알림도_생성하지_않는다() {
      // given
      Instant since = Instant.now();
      given(articleInterestRepository.countByInterestSince(since)).willReturn(List.of());

      // when
      articleNotificationService.notifyNewArticles(since);

      // then
      then(subscriptionRepository).shouldHaveNoInteractions();
      then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("구독자가 없는 관심사는 알림을 생성하지 않는다")
    void 구독자가_없는_관심사는_알림을_생성하지_않는다() {
      // given
      Instant since = Instant.now();
      UUID interestId = UUID.randomUUID();
      InterestArticleCount count = mock(InterestArticleCount.class);
      given(count.getInterestId()).willReturn(interestId);
      given(articleInterestRepository.countByInterestSince(since)).willReturn(List.of(count));
      given(subscriptionRepository.findSubscribersByInterestIds(anyList())).willReturn(List.of());

      // when
      articleNotificationService.notifyNewArticles(since);

      // then
      then(eventPublisher).shouldHaveNoInteractions();
    }
  }

  private InterestArticleCount interestArticleCount(UUID interestId, String name, long count) {
    InterestArticleCount c = mock(InterestArticleCount.class);
    given(c.getInterestId()).willReturn(interestId);
    given(c.getInterestName()).willReturn(name);
    given(c.getArticleCount()).willReturn(count);
    return c;
  }

  private InterestSubscriber subscriber(UUID interestId, UUID userId) {
    InterestSubscriber s = mock(InterestSubscriber.class);
    given(s.getInterestId()).willReturn(interestId);
    given(s.getUserId()).willReturn(userId);
    return s;
  }
}