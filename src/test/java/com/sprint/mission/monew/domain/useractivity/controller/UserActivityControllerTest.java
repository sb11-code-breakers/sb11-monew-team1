package com.sprint.mission.monew.domain.useractivity.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.monew.domain.user.document.UserSession;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.repository.UserSessionRepository;
import com.sprint.mission.monew.domain.useractivity.activityresponse.UserActivityResponse;
import com.sprint.mission.monew.domain.useractivity.service.UserActivityService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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

  @MockitoBean
  UserSessionRepository userSessionRepository;

  private UUID userId;
  private UUID sessionToken;

  @BeforeEach
  void setUpAuth() {
    userId = UUID.randomUUID();
    UserSession session = UserSession.create(userId, "127.0.0.1", "1acaf8f7bdf7054e8279b8a17955fc66", 30);
    sessionToken = session.getId();
    given(userSessionRepository.findById(sessionToken)).willReturn(Optional.of(session));
  }

  @Nested
  @DisplayName("GET /api/user-activities/{userId} — 활동 내역 조회")
  class GetUserActivity {

    @Test
    @DisplayName("존재하지 않는 userId면 404를 반환한다")
    void 존재하지_않는_userId면_404를_반환한다() throws Exception {
      // given
      UUID targetUserId = UUID.randomUUID();
      willThrow(UserNotFoundException.withId(targetUserId))
          .given(userActivityService).getUserActivity(targetUserId, userId);

      // when & then
      mockMvc.perform(
              get("/api/user-activities/{userId}", targetUserId)
                  .header("Monew-Request-User-ID", sessionToken)
          )
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.message").exists());  // ← 에러 메시지 검증!
    }

    @Test
    @DisplayName("성공 시 200을 반환한다")
    void 성공_시_200을_반환한다() throws Exception {
      // given
      UUID targetUserId = UUID.randomUUID();
      UserActivityResponse response = new UserActivityResponse(
          targetUserId, "test@test.com", "테스터", Instant.now(),
          List.of(), List.of(), List.of(), List.of()
      );
      given(userActivityService.getUserActivity(targetUserId, userId)).willReturn(response);

      // when & then
      mockMvc.perform(
              get("/api/user-activities/{userId}", targetUserId)
                  .header("Monew-Request-User-ID", sessionToken)
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(targetUserId.toString()))
          .andExpect(jsonPath("$.email").value("test@test.com"))
          .andExpect(jsonPath("$.nickname").value("테스터"))
          .andExpect(jsonPath("$.subscriptions").isArray())
          .andExpect(jsonPath("$.comments").isArray())
          .andExpect(jsonPath("$.commentLikes").isArray())
          .andExpect(jsonPath("$.articleViews").isArray());
    }
  }
}