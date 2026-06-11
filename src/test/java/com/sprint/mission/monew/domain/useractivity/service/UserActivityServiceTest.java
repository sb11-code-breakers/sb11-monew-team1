package com.sprint.mission.monew.domain.useractivity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.domain.article.repository.ArticleViewRepository;
import com.sprint.mission.monew.domain.comment.repository.CommentLikeRepository;
import com.sprint.mission.monew.domain.comment.repository.CommentRepository;
import com.sprint.mission.monew.domain.interest.repository.InterestRepository;
import com.sprint.mission.monew.domain.interest.repository.SubscriptionRepository;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserAccessDeniedException;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import com.sprint.mission.monew.domain.useractivity.activityresponse.UserActivityResponse;
import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import com.sprint.mission.monew.domain.useractivity.mapper.UserActivityMapper;
import com.sprint.mission.monew.domain.useractivity.repository.UserActivityMongoRepository;
import java.time.Instant;
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

  // 현재 서비스 의존성 (생성자 주입 유지를 위해 필요)
  @Mock private UserRepository userRepository;
  @Mock private SubscriptionRepository subscriptionRepository;
  @Mock private CommentRepository commentRepository;
  @Mock private CommentLikeRepository commentLikeRepository;
  @Mock private ArticleViewRepository articleViewRepository;
  @Mock private UserActivityMapper userActivityMapper;

  // 신규: 하이브리드 파이프라인에서 필요한 의존성 (현재 서비스에 없어서 주입되지 않음 → RED)
  @Mock private UserActivityMongoRepository userActivityMongoRepository;
  @Mock private InterestRepository interestRepository;

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
    @DisplayName("requestUserId가 userId와 다르면 403 예외가 발생한다")
    void requestUserId가_userId와_다르면_403_예외가_발생한다() {
      // given
      UUID requestUserId = UUID.randomUUID();

      // when & then
      assertThatThrownBy(() -> userActivityService.getUserActivity(userId, requestUserId))
          .isInstanceOf(UserAccessDeniedException.class);
    }

    @Test
    @DisplayName("존재하지 않는 userId면 예외가 발생한다")
    void 존재하지_않는_userId면_예외가_발생한다() {
      // given
      given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userActivityService.getUserActivity(userId, userId))
          .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("활동 내역 조회 시 MongoDB에서 불변 메타데이터를 먼저 조회한다")
    void 활동_내역_조회_시_MongoDB에서_불변_메타데이터를_먼저_조회한다() {
      // given
      given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
      given(userActivityMongoRepository.findById(userId))
          .willReturn(Optional.of(UserActivity.of(userId, Instant.now())));

      // when
      UserActivityResponse result = userActivityService.getUserActivity(userId, userId);

      // then
      verify(userActivityMongoRepository).findById(userId);
      assertThat(result).isNotNull();
      assertThat(result.id()).isNotNull();
    }
  }
}
