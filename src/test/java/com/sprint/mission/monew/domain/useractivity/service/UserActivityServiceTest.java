package com.sprint.mission.monew.domain.useractivity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.domain.comment.repository.CommentRepository;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.repository.InterestRepository;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserAccessDeniedException;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import com.sprint.mission.monew.domain.useractivity.activityresponse.SubscriptionActivityResponse;
import com.sprint.mission.monew.domain.useractivity.activityresponse.UserActivityResponse;
import com.sprint.mission.monew.domain.useractivity.document.RecentSubscription;
import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import com.sprint.mission.monew.domain.useractivity.mapper.UserActivityMapper;
import com.sprint.mission.monew.domain.useractivity.repository.UserActivityMongoRepository;
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

@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

  @InjectMocks
  private UserActivityService userActivityService;

  @Mock private UserRepository userRepository;
  @Mock private UserActivityMongoRepository userActivityMongoRepository;

  // 💡 하이브리드 조회를 위한 RDB 리포지토리 및 매퍼
  @Mock private InterestRepository interestRepository;
  @Mock private CommentRepository commentRepository;
  @Mock private ArticleRepository articleRepository;
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
    @DisplayName("성공 시 몽고DB의 불변 데이터와 RDB의 변동 데이터를 병합하여 반환한다")
    void 성공_시_하이브리드_조회로_내역을_반환한다() {
      // given
      given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
      UserActivity mockUserActivity = UserActivity.of(userId, Instant.now());
      given(userActivityMongoRepository.findById(userId)).willReturn(Optional.of(mockUserActivity));

      // when
      UserActivityResponse result = userActivityService.getUserActivity(userId, userId);

      // then
      assertThat(result).isNotNull();
      verify(userActivityMongoRepository).findById(userId);
    }

    @Test
    @DisplayName("관심사 구독 병합: 몽고DB 불변 필드와 RDB 변동 필드가 정확히 병합된다")
    void 관심사_구독_병합_검증() {
      // 1. GIVEN: 유저 통과
      given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));

      // 2. GIVEN: 몽고DB 데이터 준비
      UUID interestId = UUID.randomUUID();
      Instant subscribedAt = Instant.now().minusSeconds(3600);
      UserActivity mockUserActivity = UserActivity.of(userId, Instant.now());

      RecentSubscription recentSubscription = RecentSubscription.of(interestId, "IT 트렌드", subscribedAt);
      mockUserActivity.getSubscriptions().add(recentSubscription);

      given(userActivityMongoRepository.findById(userId)).willReturn(Optional.of(mockUserActivity));

      // 3. GIVEN: RDB 데이터 준비 (현재 구독자 수 1500명)
      Interest mockInterest = Interest.builder()
          .id(interestId)
          .name("IT 트렌드")
          .subscriberCount(1500)
          .build();
      given(interestRepository.findAllById(List.of(interestId))).willReturn(List.of(mockInterest));

      // 4. GIVEN: Mapper 모킹 (응답 DTO 생성)
      // 레코드(Record) 구조에 맞게 가짜 응답을 만들어 매퍼가 반환하도록 설정합니다.
      SubscriptionActivityResponse mockResponse = new SubscriptionActivityResponse(
          interestId, "IT 트렌드", List.of(), 1500
      );
      given(userActivityMapper.toSubscriptionDto(any(RecentSubscription.class), any(Interest.class)))
          .willReturn(mockResponse);

      // 5. WHEN: 서비스 호출
      UserActivityResponse result = userActivityService.getUserActivity(userId, userId);

      // 6. THEN: 데이터 검증
      assertThat(result.subscriptions()).hasSize(1);

      SubscriptionActivityResponse mergedResponse = result.subscriptions().get(0);
      assertThat(mergedResponse.interestId()).isEqualTo(interestId);
      assertThat(mergedResponse.interestName()).isEqualTo("IT 트렌드");
      assertThat(mergedResponse.interestSubscriberCount()).isEqualTo(1500);

      verify(userActivityMongoRepository).findById(userId);
      verify(interestRepository).findAllById(List.of(interestId));
    }
  }
}