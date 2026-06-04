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
  @DisplayName("알림 생성")
  class Create {

    @Test
    @DisplayName("수신자에게 전달된 메시지 그대로 알림이 저장된다")
    void 수신자에게_전달된_메시지_그대로_알림이_저장된다() {
      // given
      UUID recipientId = UUID.randomUUID();
      String message = "[닉네임]님이 나의 댓글을 좋아합니다.";
      UUID resourceId = UUID.randomUUID();
      given(notificationRepository.save(any(Notification.class)))
          .willAnswer(invocation -> invocation.getArgument(0));

      // when
      notificationService.create(recipientId, message, ResourceType.COMMENT, resourceId);

      // then
      then(notificationRepository)
          .should()
          .save(
              argThat(
                  n ->
                      n.getUserId().equals(recipientId)
                          && n.getResourceType() == ResourceType.COMMENT
                          && n.getResourceId().equals(resourceId)
                          && n.getContent().equals(message)));
      then(notificationMetrics).should().countCommentLikeNotification();
    }

    @Test
    @DisplayName("INTEREST 타입 알림은 기사 알림 메트릭으로 집계된다")
    void INTEREST_타입_알림은_기사_알림_메트릭으로_집계된다() {
      // given
      UUID recipientId = UUID.randomUUID();
      String message = "[인공지능]와 관련된 기사가 1건 등록되었습니다.";
      UUID resourceId = UUID.randomUUID();
      given(notificationRepository.save(any(Notification.class)))
          .willAnswer(invocation -> invocation.getArgument(0));

      // when
      notificationService.create(recipientId, message, ResourceType.INTEREST, resourceId);

      // then — 댓글 메트릭이 아닌 기사 알림 메트릭으로 집계된다
      then(notificationMetrics).should().countArticleNotifications(1);
      then(notificationMetrics).should(org.mockito.Mockito.never()).countCommentLikeNotification();
    }
  }

  @Nested
  @DisplayName("구독 관심사 기사 등록 알림 일괄 생성")
  class CreateArticleNotifications {

    @Test
    @DisplayName("전달받은 메시지로 구독자 수만큼 알림이 saveAll로 저장된다")
    void 전달받은_메시지로_구독자_수만큼_알림이_saveAll로_저장된다() {
      // given
      UUID interestId = UUID.randomUUID();
      String message = "[인공지능]와 관련된 기사가 5건 등록되었습니다.";
      List<UUID> subscriberIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
      given(notificationRepository.saveAll(any()))
          .willAnswer(invocation -> invocation.getArgument(0));

      // when
      notificationService.createArticleNotifications(interestId, message, subscriberIds);

      // then — 구독자 수만큼 저장되고 전달받은 메시지를 그대로 저장한다
      then(notificationRepository)
          .should()
          .saveAll(
              argThat(
                  notifications ->
                      ((List<?>) notifications).size() == subscriberIds.size()
                          && ((List<com.sprint.mission.monew.domain.notification.entity.Notification>) notifications)
                              .stream()
                              .allMatch(
                                  n ->
                                      n.getContent().equals(message)
                                          && n.getResourceType() == ResourceType.INTEREST
                                          && n.getResourceId().equals(interestId))));
      then(notificationMetrics).should().countArticleNotifications(subscriberIds.size());
    }
  }
}
