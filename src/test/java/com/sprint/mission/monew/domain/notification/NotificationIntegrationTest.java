package com.sprint.mission.monew.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.monew.common.config.MongoContainerConfig;
import com.sprint.mission.monew.domain.notification.entity.Notification;
import com.sprint.mission.monew.domain.notification.entity.ResourceType;
import com.sprint.mission.monew.domain.notification.repository.NotificationRepository;
import com.sprint.mission.monew.domain.user.document.UserSession;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import com.sprint.mission.monew.domain.user.repository.UserSessionRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(MongoContainerConfig.class)
public class NotificationIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private NotificationRepository notificationRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserSessionRepository userSessionRepository;

  private User user;
  private UUID sessionToken;

  @BeforeEach
  void setUp() {
    user = userRepository.save(User.create("notify@test.com", "알림테스트유저", "password123!"));

    UserSession session = UserSession.create(user.getId(), "127.0.0.1", "1acaf8f7bdf7054e8279b8a17955fc66", 30);
    userSessionRepository.save(session);
    sessionToken = session.getId();
  }

  @Nested
  @DisplayName("GET /api/notifications — 미확인 알림 목록 조회")
  class FindUnconfirmed {

    @Test
    @DisplayName("미확인 알림 목록과 응답 필드를 올바르게 반환한다")
    void 미확인_알림_목록과_응답_필드를_올바르게_반환한다() throws Exception {
      // given
      UUID resourceId = UUID.randomUUID();
      notificationRepository.save(
          Notification.create(user.getId(), "알림 내용", ResourceType.ARTICLE, resourceId));

      // when & then
      mockMvc.perform(get("/api/notifications")
              .header("Monew-Request-User-ID", sessionToken)
              .param("limit", "10"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content.length()").value(1))
          .andExpect(jsonPath("$.totalElements").value(1))
          .andExpect(jsonPath("$.hasNext").value(false))
          .andExpect(jsonPath("$.content[0].id").exists())
          .andExpect(jsonPath("$.content[0].createdAt").exists())
          .andExpect(jsonPath("$.content[0].confirmed").value(false))
          .andExpect(jsonPath("$.content[0].userId").value(user.getId().toString()))
          .andExpect(jsonPath("$.content[0].content").value("알림 내용"))
          .andExpect(jsonPath("$.content[0].resourceType").value("ARTICLE"))
          .andExpect(jsonPath("$.content[0].resourceId").value(resourceId.toString()));
    }

    @Test
    @DisplayName("확인된 알림은 목록에 포함되지 않는다")
    void 확인된_알림은_목록에_포함되지_않는다() throws Exception {
      // given
      notificationRepository.save(
          Notification.create(user.getId(), "미확인", ResourceType.ARTICLE, UUID.randomUUID()));
      Notification confirmed = notificationRepository.save(
          Notification.create(user.getId(), "확인됨", ResourceType.ARTICLE, UUID.randomUUID()));
      confirmed.confirm();
      notificationRepository.save(confirmed);

      // when & then
      mockMvc.perform(get("/api/notifications")
              .header("Monew-Request-User-ID", sessionToken)
              .param("limit", "10"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content.length()").value(1))
          .andExpect(jsonPath("$.totalElements").value(1))
          .andExpect(jsonPath("$.content[0].confirmed").value(false));
    }

    @Test
    @DisplayName("limit보다 알림이 많으면 hasNext=true와 nextCursor를 반환한다")
    void limit보다_알림이_많으면_hasNext와_nextCursor를_반환한다() throws Exception {
      // given
      for (int i = 0; i < 3; i++) {
        notificationRepository.save(
            Notification.create(user.getId(), "알림" + i, ResourceType.ARTICLE, UUID.randomUUID()));
      }

      // when & then
      mockMvc.perform(get("/api/notifications")
              .header("Monew-Request-User-ID", sessionToken)
              .param("limit", "2"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content.length()").value(2))
          .andExpect(jsonPath("$.hasNext").value(true))
          .andExpect(jsonPath("$.nextCursor").exists())
          .andExpect(jsonPath("$.nextAfter").exists());
    }

    @Test
    @DisplayName("cursor만 있고 after가 없으면 400을 반환한다")
    void cursor만_있고_after가_없으면_400을_반환한다() throws Exception {
      mockMvc.perform(get("/api/notifications")
              .header("Monew-Request-User-ID", sessionToken)
              .param("limit", "10")
              .param("cursor", "1970-01-01T00:00:00Z"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("after만 있고 cursor가 없으면 400을 반환한다")
    void after만_있고_cursor가_없으면_400을_반환한다() throws Exception {
      mockMvc.perform(get("/api/notifications")
              .header("Monew-Request-User-ID", sessionToken)
              .param("limit", "10")
              .param("after", "1970-01-01T00:00:00Z"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("cursor가 있으면 cursor 이후 알림만 반환한다")
    void cursor가_있으면_cursor_이후_알림만_반환한다() throws Exception {
      // given
      notificationRepository.save(
          Notification.create(user.getId(), "알림", ResourceType.ARTICLE, UUID.randomUUID()));

      // when & then — 최신순(DESC)이므로 cursor=먼 미래 → createdAt < cursor인 알림 1건 반환
      mockMvc.perform(get("/api/notifications")
              .header("Monew-Request-User-ID", sessionToken)
              .param("limit", "10")
              .param("cursor", "2999-01-01T00:00:00Z")
              .param("after", "2999-01-01T00:00:00Z")
              .param("idAfter", UUID.randomUUID().toString()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("Monew-Request-User-ID 헤더가 없으면 401을 반환한다")
    void 헤더가_없으면_401을_반환한다() throws Exception {
      mockMvc.perform(get("/api/notifications"))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PATCH /api/notifications/{notificationId} — 알림 단건 확인")
  class Confirm {

    @Test
    @DisplayName("성공 시 204를 반환하고 DB에 confirmedAt이 설정된다")
    void 성공_시_204를_반환하고_DB에_confirmedAt이_설정된다() throws Exception {
      // given
      Notification notification = notificationRepository.save(
          Notification.create(user.getId(), "알림", ResourceType.ARTICLE, UUID.randomUUID()));

      // when
      mockMvc.perform(patch("/api/notifications/{notificationId}", notification.getId())
              .header("Monew-Request-User-ID", sessionToken))
          .andExpect(status().isNoContent());

      // then — DB 상태 검증
      Notification result = notificationRepository.findById(notification.getId()).orElseThrow();
      assertThat(result.isConfirmed()).isTrue();
      assertThat(result.getConfirmedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 확인된 알림을 재확인하면 404를 반환한다")
    void 이미_확인된_알림을_재확인하면_404를_반환한다() throws Exception {
      // given
      Notification notification = notificationRepository.save(
          Notification.create(user.getId(), "알림", ResourceType.ARTICLE, UUID.randomUUID()));
      notification.confirm();
      notificationRepository.save(notification);

      // when & then
      mockMvc.perform(patch("/api/notifications/{notificationId}", notification.getId())
              .header("Monew-Request-User-ID", sessionToken))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("존재하지 않는 알림 확인 시 404와 에러 응답을 반환한다")
    void 존재하지_않는_알림_확인_시_404와_에러_응답을_반환한다() throws Exception {
      // when & then
      mockMvc.perform(patch("/api/notifications/{notificationId}", UUID.randomUUID())
              .header("Monew-Request-User-ID", sessionToken))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("타인의 알림 확인 시 404를 반환한다")
    void 타인의_알림_확인_시_404를_반환한다() throws Exception {
      // given
      User other = userRepository.save(User.create("other@test.com", "타인", "password123!"));
      Notification notification = notificationRepository.save(
          Notification.create(other.getId(), "타인 알림", ResourceType.ARTICLE, UUID.randomUUID()));

      // when & then
      mockMvc.perform(patch("/api/notifications/{notificationId}", notification.getId())
              .header("Monew-Request-User-ID", sessionToken))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Monew-Request-User-ID 헤더가 없으면 401을 반환한다")
    void 헤더가_없으면_401을_반환한다() throws Exception {
      mockMvc.perform(patch("/api/notifications/{notificationId}", UUID.randomUUID()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PATCH /api/notifications — 알림 전체 확인")
  class ConfirmAll {

    @Test
    @DisplayName("성공 시 204를 반환하고 DB의 모든 미확인 알림에 confirmedAt이 설정된다")
    void 성공_시_204를_반환하고_DB의_모든_미확인_알림에_confirmedAt이_설정된다() throws Exception {
      // given
      notificationRepository.save(
          Notification.create(user.getId(), "알림1", ResourceType.ARTICLE, UUID.randomUUID()));
      notificationRepository.save(
          Notification.create(user.getId(), "알림2", ResourceType.COMMENT, UUID.randomUUID()));

      // when
      mockMvc.perform(patch("/api/notifications")
              .header("Monew-Request-User-ID", sessionToken))
          .andExpect(status().isNoContent());

      // then — DB 상태 검증: 해당 유저의 모든 알림이 확인 처리됐는지 직접 조회
      boolean anyUnconfirmed = notificationRepository.findAll().stream()
          .filter(n -> n.getUserId().equals(user.getId()))
          .anyMatch(n -> !n.isConfirmed());
      assertThat(anyUnconfirmed).isFalse();
    }

    @Test
    @DisplayName("이미 확인된 알림은 전체 확인 후에도 confirmedAt이 변경되지 않는다")
    void 이미_확인된_알림은_전체_확인_후에도_confirmedAt이_변경되지_않는다() throws Exception {
      // given
      Notification confirmed = notificationRepository.save(
          Notification.create(user.getId(), "확인됨", ResourceType.ARTICLE, UUID.randomUUID()));
      confirmed.confirm();
      notificationRepository.save(confirmed);

      // when
      Instant confirmAllTime = Instant.now();
      mockMvc.perform(patch("/api/notifications")
              .header("Monew-Request-User-ID", sessionToken))
          .andExpect(status().isNoContent());

      // then — confirmAll 이전에 확인된 알림이므로 confirmedAt이 confirmAllTime보다 이전이어야 함
      Notification reloaded = notificationRepository.findById(confirmed.getId()).orElseThrow();
      assertThat(reloaded.isConfirmed()).isTrue();
      assertThat(reloaded.getConfirmedAt()).isBefore(confirmAllTime);
    }

    @Test
    @DisplayName("전체 확인 후 목록 조회 시 미확인 알림이 0건이다")
    void 전체_확인_후_목록_조회_시_미확인_알림이_0건이다() throws Exception {
      // given
      notificationRepository.save(
          Notification.create(user.getId(), "알림1", ResourceType.ARTICLE, UUID.randomUUID()));
      notificationRepository.save(
          Notification.create(user.getId(), "알림2", ResourceType.ARTICLE, UUID.randomUUID()));

      // when — 전체 확인
      mockMvc.perform(patch("/api/notifications")
              .header("Monew-Request-User-ID", sessionToken))
          .andExpect(status().isNoContent());

      // then — 목록 조회 시 0건
      mockMvc.perform(get("/api/notifications")
              .header("Monew-Request-User-ID", sessionToken)
              .param("limit", "10"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content.length()").value(0))
          .andExpect(jsonPath("$.totalElements").value(0))
          .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("Monew-Request-User-ID 헤더가 없으면 401을 반환한다")
    void 헤더가_없으면_401을_반환한다() throws Exception {
      mockMvc.perform(patch("/api/notifications"))
          .andExpect(status().isUnauthorized());
    }
  }
}