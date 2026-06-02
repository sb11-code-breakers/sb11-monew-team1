package com.sprint.mission.monew.domain.useractivity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.domain.article.repository.ArticleViewRepository;
import com.sprint.mission.monew.domain.comment.repository.CommentLikeRepository;
import com.sprint.mission.monew.domain.comment.repository.CommentRepository;
import com.sprint.mission.monew.domain.interest.repository.SubscriptionRepository;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserAccessDeniedException;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import com.sprint.mission.monew.domain.useractivity.activityresponse.UserActivityResponse;
import com.sprint.mission.monew.domain.useractivity.mapper.UserActivityMapper;
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
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

  @InjectMocks
  private UserActivityService userActivityService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private SubscriptionRepository subscriptionRepository;

  @Mock
  private UserActivityMapper userActivityMapper;

  @Mock
  private CommentRepository commentRepository;

  @Mock
  private CommentLikeRepository commentLikeRepository;

  @Mock
  private ArticleViewRepository articleViewRepository;

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
    @DisplayName("soft-delete된 userId면 예외가 발생한다")
    void soft_delete된_userId면_예외가_발생한다() {
      // given
      // soft-delete된 유저는 findByIdAndDeletedAtIsNull에서 걸러져 empty 반환
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
    @DisplayName("성공 시 활동 내역을 반환한다")
    void 성공_시_활동_내역을_반환한다() {
      // given
      given(userRepository.findByIdAndDeletedAtIsNull(userId))
          .willReturn(Optional.of(user));
      given(subscriptionRepository.findAllByUserId(userId, PageRequest.of(0, 10)))
          .willReturn(List.of());
      given(commentRepository.findTop10RecentCommentsByUserId(userId, PageRequest.of(0, 10)))
          .willReturn(List.of());
      given(commentLikeRepository.findTop10ByUserId(userId, PageRequest.of(0, 10)))
          .willReturn(List.of());
      given(articleViewRepository.findTop10ByUserIdAndArticleNotDeleted(userId, PageRequest.of(0, 10)))
          .willReturn(List.of());

      // when
      UserActivityResponse result = userActivityService.getUserActivity(userId, userId);

      // then
      assertThat(result).isNotNull();
      assertThat(result.email()).isEqualTo("test@test.com");
      assertThat(result.nickname()).isEqualTo("테스터");
      assertThat(result.subscriptions()).isNotNull().isEmpty();
      assertThat(result.comments()).isNotNull().isEmpty();
      assertThat(result.commentLikes()).isNotNull().isEmpty();
      assertThat(result.articleViews()).isNotNull().isEmpty();

      verify(subscriptionRepository).findAllByUserId(userId, PageRequest.of(0, 10));
      verify(commentRepository).findTop10RecentCommentsByUserId(userId, PageRequest.of(0, 10));
      verify(commentLikeRepository).findTop10ByUserId(userId, PageRequest.of(0, 10));
      verify(articleViewRepository).findTop10ByUserIdAndArticleNotDeleted(userId, PageRequest.of(0, 10));
    }
  }
}