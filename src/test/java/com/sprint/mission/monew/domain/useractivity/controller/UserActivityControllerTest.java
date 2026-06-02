package com.sprint.mission.monew.domain.useractivity.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.useractivity.activityresponse.UserActivityResponse;
import com.sprint.mission.monew.domain.useractivity.service.UserActivityService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserActivityController.class)
class UserActivityControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  UserActivityService userActivityService;

  @Nested
  @DisplayName("GET /api/user-activities/{userId} — 활동 내역 조회")
  class GetUserActivity {

    @Test
    @DisplayName("존재하지 않는 userId면 404를 반환한다")
    void 존재하지_않는_userId면_404를_반환한다() throws Exception {
      // given
      UUID userId = UUID.randomUUID();
      willThrow(UserNotFoundException.withId(userId))
          .given(userActivityService).getUserActivity(userId, userId);

      // when & then
      mockMvc.perform(get("/api/user-activities/{userId}", userId)
              .header("Monew-Request-User-ID", userId))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("soft-delete된 userId면 404를 반환한다")
    void soft_delete된_userId면_404를_반환한다() throws Exception {
      // given
      UUID userId = UUID.randomUUID();
      // soft-delete된 유저는 findByIdAndDeletedAtIsNull에서 걸러져 UserNotFoundException 발생
      willThrow(UserNotFoundException.withId(userId))
          .given(userActivityService).getUserActivity(userId, userId);

      // when & then
      mockMvc.perform(get("/api/user-activities/{userId}", userId)
              .header("Monew-Request-User-ID", userId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.message").exists());  // ← 에러 메시지 검증!
    }

    @Test
    @DisplayName("성공 시 200을 반환한다")
    void 성공_시_200을_반환한다() throws Exception {
      // given
      UUID userId = UUID.randomUUID();
      UserActivityResponse response = new UserActivityResponse(
          userId, "test@test.com", "테스터", Instant.now(),
          List.of(), List.of(), List.of(), List.of()
      );
      given(userActivityService.getUserActivity(userId, userId)).willReturn(response);

      // when & then
      mockMvc.perform(get("/api/user-activities/{userId}", userId)
              .header("Monew-Request-User-ID", userId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(userId.toString()))
          .andExpect(jsonPath("$.email").value("test@test.com"))
          .andExpect(jsonPath("$.nickname").value("테스터"))
          .andExpect(jsonPath("$.subscriptions").isArray())
          .andExpect(jsonPath("$.comments").isArray())
          .andExpect(jsonPath("$.commentLikes").isArray())
          .andExpect(jsonPath("$.articleViews").isArray());
    }
  }
}