package com.sprint.mission.monew.domain.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.domain.notification.dto.NotificationResponse;
import com.sprint.mission.monew.domain.notification.exception.NotificationNotFoundException;
import com.sprint.mission.monew.domain.notification.service.NotificationService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

  @Autowired
  MockMvc mockMvc;
  @MockitoBean
  NotificationService notificationService;

  @Nested
  @DisplayName("PATCH /api/notifications/{notificationId} — 알림 단건 확인")
  class ConfirmNotification {

    @Test
    @DisplayName("Monew-Request-User-ID 헤더가 없으면 400을 반환한다")
    void 헤더가_없으면_400을_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(patch("/api/notifications/{notificationId}", UUID.randomUUID()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("해당 사용자의 알림을 찾을 수 없으면 404를 반환한다")
    void 해당_사용자의_알림을_찾을_수_없으면_404를_반환한다() throws Exception {
      // given
      UUID notificationId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      willThrow(NotificationNotFoundException.withId(notificationId))
          .given(notificationService).confirm(notificationId, userId);

      // when & then
      mockMvc
          .perform(patch("/api/notifications/{notificationId}", notificationId)
              .header("Monew-Request-User-ID", userId))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("알림 확인 성공 시 204를 반환한다")
    void 알림_확인_성공_시_204를_반환한다() throws Exception {
      // given
      UUID notificationId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();

      // when & then
      mockMvc
          .perform(patch("/api/notifications/{notificationId}", notificationId)
              .header("Monew-Request-User-ID", userId))
          .andExpect(status().isNoContent());
    }
  }

  @Nested
  @DisplayName("PATCH /api/notifications — 알림 전체 확인")
  class ConfirmAllNotifications {

    @Test
    @DisplayName("Monew-Request-User-ID 헤더가 없으면 400을 반환한다")
    void 헤더가_없으면_400을_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(patch("/api/notifications"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("알림 전체 확인 성공 시 204를 반환한다")
    void 알림_전체_확인_성공_시_204를_반환한다() throws Exception {
      // given
      UUID userId = UUID.randomUUID();

      // when & then
      mockMvc
          .perform(patch("/api/notifications")
              .header("Monew-Request-User-ID", userId))
          .andExpect(status().isNoContent());
    }
  }

  @Nested
  @DisplayName("GET /api/notifications — 미확인 알림 목록 조회")
  class FindUnconfirmed {

    @Test
    @DisplayName("Monew-Request-User-ID 헤더가 없으면 400을 반환한다")
    void 헤더가_없으면_400을_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(get("/api/notifications").param("limit", "10"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("limit이 0이면 400을 반환한다")
    void limit이_0이면_400을_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(
              get("/api/notifications")
                  .header("Monew-Request-User-ID", UUID.randomUUID())
                  .param("limit", "0"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("limit이 없으면 400을 반환한다")
    void limit이_없으면_400을_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(
              get("/api/notifications")
                  .header("Monew-Request-User-ID", UUID.randomUUID()))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("정상 요청이면 200과 CursorPageResponse를 반환한다")
    void 정상_요청이면_200과_CursorPageResponse를_반환한다() throws Exception {
      // given
      CursorPageResponse<NotificationResponse> response =
          new CursorPageResponse<>(List.of(), null, null, false, 0, 0L);
      given(notificationService.findUnconfirmed(any(), any())).willReturn(response);

      // when & then
      mockMvc
          .perform(
              get("/api/notifications")
                  .header("Monew-Request-User-ID", UUID.randomUUID())
                  .param("limit", "10"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.hasNext").value(false))
          .andExpect(jsonPath("$.totalElements").value(0));
    }
  }
}