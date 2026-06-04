package com.sprint.mission.monew.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.domain.notification.dto.NotificationQueryCondition;
import com.sprint.mission.monew.domain.notification.dto.NotificationResponse;
import com.sprint.mission.monew.domain.notification.entity.Notification;
import com.sprint.mission.monew.domain.notification.entity.ResourceType;
import com.sprint.mission.monew.domain.notification.exception.NotificationNotFoundException;
import com.sprint.mission.monew.domain.notification.repository.NotificationRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class
NotificationServiceTest {

  @InjectMocks
  NotificationService notificationService;
  @Mock
  NotificationRepository notificationRepository;
  @Mock
  NotificationMetrics notificationMetrics;

  UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
  }

  @Nested
  @DisplayName("알림 단건 확인")
  class Confirm {

    @Test
    @DisplayName("알림이 존재하지 않으면 NotificationNotFoundException이 발생한다")
    void 알림이_존재하지_않으면_NotificationNotFoundException이_발생한다() {
      // given
      UUID notificationId = UUID.randomUUID();
      given(notificationRepository.findByIdAndUserIdAndConfirmedAtIsNull(notificationId, userId))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> notificationService.confirm(notificationId, userId))
          .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    @DisplayName("성공 시 알림의 confirm()이 호출된다")
    void 성공_시_알림의_confirm이_호출된다() {
      // given
      UUID notificationId = UUID.randomUUID();
      Notification notification =
          Notification.create(userId, "알림", ResourceType.INTEREST, UUID.randomUUID());
      given(notificationRepository.findByIdAndUserIdAndConfirmedAtIsNull(notificationId, userId))
          .willReturn(Optional.of(notification));

      // when
      notificationService.confirm(notificationId, userId);

      // then
      assertThat(notification.getConfirmedAt()).isNotNull();
    }
  }

  @Nested
  @DisplayName("알림 전체 확인")
  class ConfirmAll {

    @Test
    @DisplayName("repository.confirmAllByUserId에 위임한다")
    void repository_confirmAllByUserId에_위임한다() {
      // when
      notificationService.confirmAll(userId);

      // then
      then(notificationRepository).should().confirmAllByUserId(any(UUID.class), any());
    }

    @Test
    @DisplayName("알림이 없어도 예외 없이 동작한다")
    void 알림이_없어도_예외_없이_동작한다() {
      // when & then
      org.junit.jupiter.api.Assertions.assertDoesNotThrow(
          () -> notificationService.confirmAll(userId));
    }
  }

  @Nested
  @DisplayName("미확인 알림 목록 조회")
  class FindUnconfirmed {

    @Test
    @DisplayName("repository에 조회를 위임하고 결과를 반환한다")
    void repository에_조회를_위임하고_결과를_반환한다() {
      // given
      NotificationQueryCondition condition = new NotificationQueryCondition(null, null, 10);
      CursorPageResponse<NotificationResponse> expected =
          new CursorPageResponse<>(List.of(), null, null, false, 0, 0L);
      given(notificationRepository.findUnconfirmed(userId, condition)).willReturn(expected);

      // when
      CursorPageResponse<NotificationResponse> result =
          notificationService.findUnconfirmed(userId, condition);

      // then
      assertThat(result).isEqualTo(expected);
      then(notificationRepository).should().findUnconfirmed(userId, condition);
    }
  }

  @Nested
  @DisplayName("만료 알림 일괄 삭제")
  class DeleteExpiredNotifications {

    @Test
    @DisplayName("7일 경과 기준 cutoff로 repository.deleteConfirmedBefore에 위임한다")
    void 만료_기준_cutoff로_repository_deleteConfirmedBefore에_위임한다() {
      // given
      Instant before = Instant.now();

      // when
      notificationService.deleteExpiredNotifications();

      // then
      Instant after = Instant.now();
      then(notificationRepository)
          .should()
          .deleteConfirmedBefore(
              argThat(
                  cutoff ->
                      !cutoff.isBefore(before.minus(7, ChronoUnit.DAYS))
                          && !cutoff.isAfter(after.minus(7, ChronoUnit.DAYS))));
    }

    @Test
    @DisplayName("삭제된 알림 건수를 메트릭으로 집계한다")
    void 삭제된_알림_건수를_메트릭으로_집계한다() {
      // given — repository가 5건 삭제를 반환
      given(notificationRepository.deleteConfirmedBefore(any())).willReturn(5);

      // when
      notificationService.deleteExpiredNotifications();

      // then
      then(notificationMetrics).should().countDeleted(5);
    }
  }

  @Nested
  @DisplayName("댓글 좋아요 알림 생성")
  class CreateCommentLikeNotification {

    @Test
    @DisplayName("댓글 작성자에게 좋아요 알림이 저장된다")
    void 댓글_작성자에게_좋아요_알림이_저장된다() {
      // given
      UUID commentId = UUID.randomUUID();
      UUID commentAuthorId = UUID.randomUUID();
      String likerNickname = "닉네임";
      given(notificationRepository.save(any(Notification.class)))
          .willAnswer(invocation -> invocation.getArgument(0));

      // when
      notificationService.createCommentLikeNotification(commentId, commentAuthorId, likerNickname);

      // then
      then(notificationRepository)
          .should()
          .save(
              argThat(
                  n ->
                      n.getUserId().equals(commentAuthorId)
                          && n.getResourceType() == ResourceType.COMMENT
                          && n.getResourceId().equals(commentId)
                          && n.getContent().contains(likerNickname)));
      then(notificationMetrics).should().countCommentLikeNotification();
    }
  }

  @Nested
  @DisplayName("구독 관심사 기사 등록 알림 일괄 생성")
  class CreateArticleNotifications {

    @Test
    @DisplayName("구독자 수만큼 알림이 saveAll로 저장된다")
    void 구독자_수만큼_알림이_saveAll로_저장된다() {
      // given
      UUID interestId = UUID.randomUUID();
      List<UUID> subscriberIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
      given(notificationRepository.saveAll(any()))
          .willAnswer(invocation -> invocation.getArgument(0));

      // when
      notificationService.createArticleNotifications(interestId, "인공지능", subscriberIds);

      // then
      then(notificationRepository)
          .should()
          .saveAll(
              argThat(notifications -> ((List<?>) notifications).size() == subscriberIds.size()));
      then(notificationMetrics).should().countArticleNotifications(subscriberIds.size());
    }
  }
}
