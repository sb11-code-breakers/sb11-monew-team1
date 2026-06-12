package com.sprint.mission.monew.domain.useractivity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserAccessDeniedException;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import com.sprint.mission.monew.domain.useractivity.listener.ArticleViewDeletedEvent;
import com.sprint.mission.monew.domain.useractivity.repository.UserActivityMongoRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

  @InjectMocks
  private UserActivityService userActivityService;

  @Mock private UserRepository userRepository;
  @Mock private UserActivityMongoRepository userActivityMongoRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
  }

  @Nested
  @DisplayName("기사 조회 내역 단건 삭제")
  class DeleteArticleView {

    @Test
    @DisplayName("requestUserId가 userId와 다르면 403 예외가 발생한다")
    void requestUserId가_userId와_다르면_403_예외가_발생한다() {
      UUID requestUserId = UUID.randomUUID();

      assertThatThrownBy(() -> userActivityService.deleteArticleView(userId, UUID.randomUUID(), requestUserId))
          .isInstanceOf(UserAccessDeniedException.class);
    }

    @Test
    @DisplayName("존재하지 않는 userId면 예외가 발생한다")
    void 존재하지_않는_userId면_예외가_발생한다() {
      given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

      assertThatThrownBy(() -> userActivityService.deleteArticleView(userId, UUID.randomUUID(), userId))
          .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("성공 시 ArticleViewDeletedEvent를 발행한다")
    void 성공_시_ArticleViewDeletedEvent를_발행한다() {
      UUID articleId = UUID.randomUUID();
      User mockUser = User.create("test@test.com", "테스트유저", "password123!");
      given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(mockUser));

      userActivityService.deleteArticleView(userId, articleId, userId);

      ArgumentCaptor<ArticleViewDeletedEvent> captor = ArgumentCaptor.forClass(ArticleViewDeletedEvent.class);
      then(eventPublisher).should().publishEvent(captor.capture());
      assertThat(captor.getValue().userId()).isEqualTo(userId);
      assertThat(captor.getValue().articleId()).isEqualTo(articleId);
    }
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
    @DisplayName("soft-delete된 userId면 예외가 발생한다")
    void soft_delete된_userId면_예외가_발생한다() {
      // given
      given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userActivityService.getUserActivity(userId, userId))
          .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("활동 내역 조회 시 MongoDB에서 UserActivity를 조회한다")
    void 활동_내역_조회_시_MongoDB에서_UserActivity를_조회한다() {
      // given
      User mockUser = User.create("test@test.com", "테스트유저", "password123!");
      given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(mockUser));
      given(userActivityMongoRepository.findById(userId))
          .willReturn(Optional.of(UserActivity.of(userId, "test@test.com", "테스트유저", Instant.now())));

      // when
      UserActivity result = userActivityService.getUserActivity(userId, userId);

      // then
      verify(userActivityMongoRepository).findById(userId);
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(userId);
      assertThat(result.getEmail()).isEqualTo("test@test.com");
      assertThat(result.getNickname()).isEqualTo("테스트유저");
    }
  }
}
