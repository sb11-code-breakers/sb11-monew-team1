package com.sprint.mission.monew.domain.interest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.monew.domain.interest.dto.SubscriptionResponse;
import com.sprint.mission.monew.domain.interest.exception.InterestNotFoundException;
import com.sprint.mission.monew.domain.interest.exception.SubscriptionAlreadyExistsException;
import com.sprint.mission.monew.domain.interest.service.SubscriptionService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SubscriptionController.class)
class SubscriptionControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  SubscriptionService subscriptionService;

  @Nested
  @DisplayName("POST /api/interests/{interestId}/subscriptions — 관심사 구독")
  class Subscribe {

    @Test
    @DisplayName("Monew-Request-User-ID 헤더가 없으면 400을 반환한다")
    void Monew_Request_User_ID_헤더가_없으면_400을_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(post("/api/interests/{interestId}/subscriptions", UUID.randomUUID()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 관심사 구독 시 404를 반환한다")
    void 존재하지_않는_관심사_구독_시_404를_반환한다() throws Exception {
      // given
      UUID interestId = UUID.randomUUID();
      willThrow(InterestNotFoundException.withId(interestId))
          .given(subscriptionService).subscribe(any(UUID.class), any(UUID.class));

      // when & then
      mockMvc
          .perform(post("/api/interests/{interestId}/subscriptions", interestId)
              .header("Monew-Request-User-ID", UUID.randomUUID()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("INTEREST_NOT_FOUND"));
    }

    @Test
    @DisplayName("이미 구독 중인 경우 409를 반환한다")
    void 이미_구독_중인_경우_409를_반환한다() throws Exception {
      // given
      UUID interestId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      willThrow(SubscriptionAlreadyExistsException.of(interestId, userId))
          .given(subscriptionService).subscribe(any(UUID.class), any(UUID.class));

      // when & then
      mockMvc
          .perform(post("/api/interests/{interestId}/subscriptions", interestId)
              .header("Monew-Request-User-ID", userId))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("SUBSCRIPTION_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("정상 요청이면 201과 SubscriptionResponse를 반환한다")
    void 정상_요청이면_201과_SubscriptionResponse를_반환한다() throws Exception {
      // given
      UUID interestId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      SubscriptionResponse response = new SubscriptionResponse(
          UUID.randomUUID(), interestId, "인공지능", List.of("AI"), 1L, null);

      given(subscriptionService.subscribe(any(UUID.class), any(UUID.class))).willReturn(response);

      // when & then
      mockMvc
          .perform(post("/api/interests/{interestId}/subscriptions", interestId)
              .header("Monew-Request-User-ID", userId))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.interestName").value("인공지능"))
          .andExpect(jsonPath("$.interestSubscriberCount").value(1));
    }
  }
}