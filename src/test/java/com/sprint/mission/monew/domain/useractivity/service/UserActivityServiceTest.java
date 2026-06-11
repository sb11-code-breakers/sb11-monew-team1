package com.sprint.mission.monew.domain.useractivity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.domain.article.repository.ArticleViewRepository;
import com.sprint.mission.monew.domain.comment.repository.CommentLikeRepository;
import com.sprint.mission.monew.domain.comment.repository.CommentRepository;
import com.sprint.mission.monew.domain.interest.entity.Subscription;
import com.sprint.mission.monew.domain.interest.repository.SubscriptionRepository;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserAccessDeniedException;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import com.sprint.mission.monew.domain.useractivity.activityresponse.SubscriptionActivityResponse;
import com.sprint.mission.monew.domain.useractivity.activityresponse.UserActivityResponse;
import com.sprint.mission.monew.domain.useractivity.mapper.UserActivityMapper;
import java.time.Instant;
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
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

  @InjectMocks
  private UserActivityService userActivityService;

  @Mock private UserRepository userRepository;
  @Mock private SubscriptionRepository subscriptionRepository;
  @Mock private CommentRepository commentRepository;
  @Mock private CommentLikeRepository commentLikeRepository;
  @Mock private ArticleViewRepository articleViewRepository;
  @Mock private UserActivityMapper userActivityMapper;

  private UUID userId;
  private User user;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    user = User.create("test@test.com", "테스터", "password123");
  }

  @Nested
  @DisplayName("활동 내역 조회")
  class GetUserActivity {

    @Test
    @DisplayName("존재하지 않는 userId면 예외가 발생한다")
    void 존재하지_않는_userId면_예외가_발생한다() {
      // given
      given(userRepository.findByIdAndDeletedAtIsNull(userId))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userActivityService.getUserActivity(userId, userId))
          .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("requestUserId가 userId와 다르면 403 예외가 발생한다")
    void requestUserId가_userId와_다르면_403_예외가_발생한다() {
      // given
      UUID requestUserId = UUID.randomUUID();

      // when & then
      assertThatThrownBy(() -> userActivityService.getUserActivity(userId, requestUserId))
          .isInstanceOf(UserAccessDeniedException.class);
    }

    @Test
    @DisplayName("성공 시 RDB 리포지토리에서 활동 데이터를 조회하여 반환한다")
    void 성공_시_활동_내역을_반환한다() {
      // given
      given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
      given(subscriptionRepository.findAllByUserId(eq(userId), any(Pageable.class))).willReturn(List.of());
      given(commentRepository.findTop10RecentCommentsByUserId(eq(userId), any(Pageable.class))).willReturn(List.of());
      given(commentLikeRepository.findTop10ByUserId(eq(userId), any(Pageable.class))).willReturn(List.of());
      given(articleViewRepository.findTop10ByUserIdAndArticleNotDeleted(eq(userId), any(Pageable.class))).willReturn(List.of());

      // when
      UserActivityResponse result = userActivityService.getUserActivity(userId, userId);

      // then
      assertThat(result).isNotNull();
      verify(subscriptionRepository).findAllByUserId(eq(userId), any(Pageable.class));
    }

    @Test
    @DisplayName("관심사 구독 목록이 매퍼를 통해 변환되어 반환된다")
    void 관심사_구독_목록_변환_검증() {
      // given
      given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));

      UUID interestId = UUID.randomUUID();
      Instant subscribedAt = Instant.now().minusSeconds(3600);

      Subscription mockSubscription = mock(Subscription.class);
      given(subscriptionRepository.findAllByUserId(eq(userId), any(Pageable.class)))
          .willReturn(List.of(mockSubscription));
      given(commentRepository.findTop10RecentCommentsByUserId(eq(userId), any(Pageable.class))).willReturn(List.of());
      given(commentLikeRepository.findTop10ByUserId(eq(userId), any(Pageable.class))).willReturn(List.of());
      given(articleViewRepository.findTop10ByUserIdAndArticleNotDeleted(eq(userId), any(Pageable.class))).willReturn(List.of());

      SubscriptionActivityResponse mockResponse = new SubscriptionActivityResponse(
          UUID.randomUUID(), interestId, "IT 트렌드", List.of(), 1500L, subscribedAt
      );
      given(userActivityMapper.toSubscriptionDto(mockSubscription)).willReturn(mockResponse);

      // when
      UserActivityResponse result = userActivityService.getUserActivity(userId, userId);

      // then
      assertThat(result.subscriptions()).hasSize(1);
      SubscriptionActivityResponse response = result.subscriptions().get(0);
      assertThat(response.interestId()).isEqualTo(interestId);
      assertThat(response.interestName()).isEqualTo("IT 트렌드");
      assertThat(response.interestSubscriberCount()).isEqualTo(1500L);

      verify(subscriptionRepository).findAllByUserId(eq(userId), any(Pageable.class));
    }
  }
}
