package com.sprint.mission.monew.domain.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.batch.dto.NotificationCleanupItem;
import com.sprint.mission.monew.common.config.JpaConfig;
import com.sprint.mission.monew.common.config.QuerydslConfig;
import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.domain.notification.dto.NotificationQueryCondition;
import com.sprint.mission.monew.domain.notification.dto.NotificationResponse;
import com.sprint.mission.monew.domain.notification.entity.Notification;
import com.sprint.mission.monew.domain.notification.entity.ResourceType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, QuerydslConfig.class})
class NotificationRepositoryTest {

  @Autowired
  NotificationRepository notificationRepository;

  private UUID userId;

  @BeforeEach
  void setUp() {
    notificationRepository.deleteAll();
    userId = UUID.randomUUID();
  }

  @Nested
  @DisplayName("findUnconfirmed")
  class FindUnconfirmed {

    @Test
    @DisplayName("커서 없이 조회하면 해당 사용자의 미확인 알림만 반환한다")
    void 커서_없이_조회하면_해당_사용자의_미확인_알림만_반환한다() {
      // given
      notificationRepository.save(
          Notification.create(userId, "알림1", ResourceType.INTEREST, UUID.randomUUID()));
      notificationRepository.save(
          Notification.create(userId, "알림2", ResourceType.INTEREST, UUID.randomUUID()));
      NotificationQueryCondition condition = new NotificationQueryCondition(null, null, null, 10);

      // when
      CursorPageResponse<NotificationResponse> result =
          notificationRepository.findUnconfirmed(userId, condition);

      // then
      assertThat(result.content()).hasSize(2);
      assertThat(result.content()).allMatch(r -> r.userId().equals(userId));
    }

    @Test
    @DisplayName("확인된 알림은 조회 결과에 포함되지 않는다")
    void 확인된_알림은_조회_결과에_포함되지_않는다() {
      // given
      Notification unconfirmed = notificationRepository.save(
          Notification.create(userId, "미확인", ResourceType.INTEREST, UUID.randomUUID()));
      Notification confirmed = notificationRepository.save(
          Notification.create(userId, "확인됨", ResourceType.INTEREST, UUID.randomUUID()));
      confirmed.confirm();
      notificationRepository.save(confirmed);
      NotificationQueryCondition condition = new NotificationQueryCondition(null, null, null, 10);

      // when
      CursorPageResponse<NotificationResponse> result =
          notificationRepository.findUnconfirmed(userId, condition);

      // then
      assertThat(result.content()).hasSize(1);
      assertThat(result.content().get(0).id()).isEqualTo(unconfirmed.getId());
    }

    @Test
    @DisplayName("다른 사용자의 알림은 조회 결과에 포함되지 않는다")
    void 다른_사용자의_알림은_조회_결과에_포함되지_않는다() {
      // given
      notificationRepository.save(
          Notification.create(userId, "내 알림", ResourceType.INTEREST, UUID.randomUUID()));
      notificationRepository.save(
          Notification.create(UUID.randomUUID(), "타인 알림", ResourceType.INTEREST,
              UUID.randomUUID()));
      NotificationQueryCondition condition = new NotificationQueryCondition(null, null, null, 10);

      // when
      CursorPageResponse<NotificationResponse> result =
          notificationRepository.findUnconfirmed(userId, condition);

      // then
      assertThat(result.content()).hasSize(1);
      assertThat(result.content().get(0).userId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("데이터가 limit보다 많으면 hasNext=true를 반환한다")
    void 데이터가_limit보다_많으면_hasNext_true를_반환한다() {
      // given
      for (int i = 0; i < 3; i++) {
        notificationRepository.save(
            Notification.create(userId, "알림" + i, ResourceType.INTEREST, UUID.randomUUID()));
      }
      NotificationQueryCondition condition = new NotificationQueryCondition(null, null, null, 2);

      // when
      CursorPageResponse<NotificationResponse> result =
          notificationRepository.findUnconfirmed(userId, condition);

      // then
      assertThat(result.hasNext()).isTrue();
      assertThat(result.content()).hasSize(2);
    }
  }

  @Nested
  @DisplayName("findByIdAndUserIdAndConfirmedAtIsNull")
  class FindByIdAndUserIdAndConfirmedAtIsNull {

    @Test
    @DisplayName("존재하지 않는 알림 ID면 빈 Optional을 반환한다")
    void 존재하지_않는_알림_ID면_빈_Optional을_반환한다() {
      // given
      UUID notExistId = UUID.randomUUID();

      // when
      Optional<Notification> result = notificationRepository.findByIdAndUserIdAndConfirmedAtIsNull(
          notExistId, userId);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("다른 사용자의 알림이면 빈 Optional을 반환한다")
    void 다른_사용자의_알림이면_빈_Optional을_반환한다() {
      // given
      UUID otherUserId = UUID.randomUUID();
      Notification notification = notificationRepository.save(
          Notification.create(otherUserId, "타인 알림", ResourceType.INTEREST, UUID.randomUUID()));

      // when
      Optional<Notification> result = notificationRepository.findByIdAndUserIdAndConfirmedAtIsNull(
          notification.getId(), userId);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("이미 확인된 알림이면 빈 Optional을 반환한다")
    void 이미_확인된_알림이면_빈_Optional을_반환한다() {
      // given
      Notification notification = notificationRepository.save(
          Notification.create(userId, "확인된 알림", ResourceType.INTEREST, UUID.randomUUID()));
      notification.confirm();
      notificationRepository.save(notification);

      // when
      Optional<Notification> result = notificationRepository.findByIdAndUserIdAndConfirmedAtIsNull(
          notification.getId(), userId);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("id와 userId가 모두 일치하고 미확인 상태면 알림을 반환한다")
    void id와_userId가_모두_일치하고_미확인_상태면_알림을_반환한다() {
      // given
      Notification notification = notificationRepository.save(
          Notification.create(userId, "내 알림", ResourceType.INTEREST, UUID.randomUUID()));

      // when
      Optional<Notification> result = notificationRepository.findByIdAndUserIdAndConfirmedAtIsNull(
          notification.getId(), userId);

      // then
      assertThat(result).isPresent();
      assertThat(result.get().getId()).isEqualTo(notification.getId());
      assertThat(result.get().getUserId()).isEqualTo(userId);
    }
  }

  @Nested
  @DisplayName("confirmAllByUserId")
  class ConfirmAllByUserId {

    @Test
    @DisplayName("미확인 알림 전체 확인 시 모두 confirmedAt이 설정된다")
    void 미확인_알림_전체_확인_시_모두_confirmedAt이_설정된다() {
      // given
      notificationRepository.save(
          Notification.create(userId, "알림1", ResourceType.INTEREST, UUID.randomUUID()));
      notificationRepository.save(
          Notification.create(userId, "알림2", ResourceType.INTEREST, UUID.randomUUID()));

      // when
      notificationRepository.confirmAllByUserId(userId, Instant.now());

      // then
      List<Notification> all = notificationRepository.findAll();
      assertThat(all).allMatch(Notification::isConfirmed);
    }

    @Test
    @DisplayName("미확인 알림이 0건이어도 예외 없이 동작한다")
    void 미확인_알림이_0건이어도_예외_없이_동작한다() {
      // given — 알림 없음

      // when & then
      org.junit.jupiter.api.Assertions.assertDoesNotThrow(
          () -> notificationRepository.confirmAllByUserId(userId, Instant.now()));
    }

    @Test
    @DisplayName("이미 확인된 알림은 confirmAll 호출 후에도 confirmedAt이 변경되지 않는다")
    void 이미_확인된_알림은_confirmAll_호출_후에도_confirmedAt이_변경되지_않는다() {
      // given
      Notification confirmed = notificationRepository.save(
          Notification.create(userId, "확인됨", ResourceType.INTEREST, UUID.randomUUID()));
      confirmed.confirm();
      notificationRepository.save(confirmed);

      // when
      Instant confirmAllTime = Instant.now();
      notificationRepository.confirmAllByUserId(userId, confirmAllTime);

      // then — confirmAll 이전에 확인된 알림이므로 confirmedAt이 confirmAllTime보다 이전이어야 함
      Notification reloaded = notificationRepository.findById(confirmed.getId()).orElseThrow();
      assertThat(reloaded.isConfirmed()).isTrue();
      assertThat(reloaded.getConfirmedAt()).isBefore(confirmAllTime);
    }
  }

  @Nested
  @DisplayName("deleteConfirmedBefore")
  class DeleteConfirmedBefore {

    @Test
    @DisplayName("확인 후 7일 경과한 알림은 물리 삭제된다")
    void 확인_후_7일_경과한_알림은_물리_삭제된다() {
      // given
      notificationRepository.save(
          Notification.create(userId, "오래된 알림", ResourceType.INTEREST, UUID.randomUUID()));
      notificationRepository.confirmAllByUserId(userId, Instant.now().minus(8, ChronoUnit.DAYS));

      // when
      int deleted = notificationRepository.deleteConfirmedBefore(Instant.now().minus(7, ChronoUnit.DAYS));

      // then
      assertThat(deleted).isEqualTo(1);
      assertThat(notificationRepository.findAll()).isEmpty();
    }
  }

  @Nested
  @DisplayName("confirmedAt + id 기준 cursor 조회가 정렬된 순서로 반환하기")
  class FindNotificationsForCleanup {

    @Test
    @DisplayName("confirmedAt + id 기준 cursor 조회가 정렬된 순서로 반환된다")
    void findNotificationsForCleanup_ordering_test() {
      Instant cutoff = Instant.now();

      Notification notification1 =
          Notification.create(userId, "삭제될 알림", ResourceType.INTEREST, UUID.randomUUID());
      Notification notification2 =
          Notification.create(userId, "남아있을 알림", ResourceType.INTEREST, UUID.randomUUID());

      ReflectionTestUtils.setField(notification1, "confirmedAt", cutoff.minusSeconds(100));
      ReflectionTestUtils.setField(notification2, "confirmedAt", cutoff.plusSeconds(100));

      notificationRepository.save(notification1);
      notificationRepository.save(notification2);

      List<NotificationCleanupItem> result =
          notificationRepository.findNotificationsForCleanup(
              cutoff,
              Instant.EPOCH,
              UUID.randomUUID(),
              PageRequest.of(0, 10)
          );

      assertThat(result)
          .extracting(NotificationCleanupItem::id)
          .containsExactly(notification1.getId());
    }
  }
}