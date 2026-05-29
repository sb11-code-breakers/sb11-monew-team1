package com.sprint.mission.monew.domain.interest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sprint.mission.monew.domain.interest.dto.SubscriptionResponse;
import org.springframework.dao.DataIntegrityViolationException;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.entity.Subscription;
import com.sprint.mission.monew.domain.interest.exception.InterestNotFoundException;
import com.sprint.mission.monew.domain.interest.exception.SubscriptionAlreadyExistsException;
import com.sprint.mission.monew.domain.interest.mapper.SubscriptionMapper;
import com.sprint.mission.monew.domain.interest.repository.InterestRepository;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.interest.repository.SubscriptionRepository;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
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
class SubscriptionServiceTest {

  @InjectMocks
  SubscriptionService subscriptionService;

  @Mock
  InterestRepository interestRepository;

  @Mock
  UserRepository userRepository;

  @Mock
  SubscriptionRepository subscriptionRepository;

  @Mock
  SubscriptionMapper subscriptionMapper;

  UUID interestId;
  UUID userId;

  @BeforeEach
  void setUp() {
    interestId = UUID.randomUUID();
    userId = UUID.randomUUID();
  }

  @Nested
  @DisplayName("관심사 구독")
  class Subscribe {

    @Test
    @DisplayName("존재하지 않는 관심사 구독 시 InterestNotFoundException이 발생한다")
    void 존재하지_않는_관심사_구독_시_InterestNotFoundException이_발생한다() {
      // given
      given(interestRepository.findById(interestId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> subscriptionService.subscribe(interestId, userId))
          .isInstanceOf(InterestNotFoundException.class);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 구독 시 UserNotFoundException이 발생한다")
    void 존재하지_않는_사용자_구독_시_UserNotFoundException이_발생한다() {
      // given
      Interest interest = Interest.create("인공지능", List.of("AI"));
      given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
      given(userRepository.findById(userId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> subscriptionService.subscribe(interestId, userId))
          .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("이미 구독 중인 경우 SubscriptionAlreadyExistsException이 발생한다")
    void 이미_구독_중인_경우_SubscriptionAlreadyExistsException이_발생한다() {
      // given
      Interest interest = Interest.create("인공지능", List.of("AI"));
      User user = User.create("test@test.com", "테스터", "password123!");
      given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
      given(userRepository.findById(userId)).willReturn(Optional.of(user));
      given(subscriptionRepository.existsByInterestIdAndUserId(interestId, userId))
          .willReturn(true);

      // when & then
      assertThatThrownBy(() -> subscriptionService.subscribe(interestId, userId))
          .isInstanceOf(SubscriptionAlreadyExistsException.class);
    }

    @Test
    @DisplayName("저장 시 유니크 충돌이 나면 SubscriptionAlreadyExistsException으로 변환한다")
    void 저장_유니크충돌_시_SubscriptionAlreadyExistsException으로_변환한다() {
      // given
      Interest interest = Interest.create("인공지능", List.of("AI"));
      User user = User.create("test@test.com", "테스터", "password123!");
      given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
      given(userRepository.findById(userId)).willReturn(Optional.of(user));
      given(subscriptionRepository.existsByInterestIdAndUserId(interestId, userId))
          .willReturn(false);
      given(subscriptionRepository.saveAndFlush(any(Subscription.class)))
          .willThrow(new DataIntegrityViolationException("unique constraint"));

      // when & then
      assertThatThrownBy(() -> subscriptionService.subscribe(interestId, userId))
          .isInstanceOf(SubscriptionAlreadyExistsException.class);
    }

    @Test
    @DisplayName("정상 구독 시 SubscriptionResponse를 반환한다")
    void 정상_구독_시_SubscriptionResponse를_반환한다() {
      // given
      Interest interest = Interest.create("인공지능", List.of("AI"));
      User user = User.create("test@test.com", "테스터", "password123!");
      SubscriptionResponse expected = new SubscriptionResponse(
          user.getId(), interest.getId(), "인공지능", List.of("AI"), 1L, null);

      given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
      given(userRepository.findById(userId)).willReturn(Optional.of(user));
      given(subscriptionRepository.existsByInterestIdAndUserId(interestId, userId))
          .willReturn(false);
      given(subscriptionRepository.saveAndFlush(any(Subscription.class)))
          .willAnswer(inv -> inv.getArgument(0));
      given(subscriptionMapper.toResponse(any(Subscription.class))).willReturn(expected);

      // when
      SubscriptionResponse result = subscriptionService.subscribe(interestId, userId);

      // then
      assertThat(result.interestSubscriberCount()).isEqualTo(1L);
      assertThat(result.interestName()).isEqualTo("인공지능");
    }

    @Test
    @DisplayName("정상 구독 시 interest의 subscriberCount가 1 증가한다")
    void 정상_구독_시_interest의_subscriberCount가_1_증가한다() {
      // given
      Interest interest = Interest.create("인공지능", List.of("AI"));
      User user = User.create("test@test.com", "테스터", "password123!");

      given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
      given(userRepository.findById(userId)).willReturn(Optional.of(user));
      given(subscriptionRepository.existsByInterestIdAndUserId(interestId, userId))
          .willReturn(false);
      given(subscriptionRepository.saveAndFlush(any(Subscription.class)))
          .willAnswer(inv -> inv.getArgument(0));
      given(subscriptionMapper.toResponse(any(Subscription.class)))
          .willAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            return new SubscriptionResponse(
                s.getId(), s.getInterest().getId(), s.getInterest().getName(),
                List.of("AI"), s.getInterest().getSubscriberCount(), null);
          });

      // when
      subscriptionService.subscribe(interestId, userId);

      // then
      assertThat(interest.getSubscriberCount()).isEqualTo(1L);
    }
  }
}
