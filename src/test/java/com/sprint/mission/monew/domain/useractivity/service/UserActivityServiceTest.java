package com.sprint.mission.monew.domain.useractivity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.domain.user.exception.UserAccessDeniedException;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.useractivity.activityresponse.UserActivityResponse;
import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
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

  @Mock private UserActivityMongoRepository userActivityMongoRepository;

  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
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
      given(userActivityMongoRepository.findById(userId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userActivityService.getUserActivity(userId, userId))
          .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("활동 내역 조회 시 MongoDB에서 UserActivity를 조회한다")
    void 활동_내역_조회_시_MongoDB에서_UserActivity를_조회한다() {
      // given
      given(userActivityMongoRepository.findById(userId))
          .willReturn(Optional.of(UserActivity.of(userId, "test@test.com", "테스트유저", Instant.now())));

      // when
      UserActivityResponse result = userActivityService.getUserActivity(userId, userId);

      // then
      verify(userActivityMongoRepository).findById(userId);
      assertThat(result).isNotNull();
      assertThat(result.id()).isEqualTo(userId);
      assertThat(result.email()).isEqualTo("test@test.com");
      assertThat(result.nickname()).isEqualTo("테스트유저");
    }
  }
}
