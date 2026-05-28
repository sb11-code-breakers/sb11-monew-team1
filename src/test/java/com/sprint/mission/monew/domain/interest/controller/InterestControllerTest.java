package com.sprint.mission.monew.domain.interest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.monew.domain.interest.dto.InterestCreateRequest;
import com.sprint.mission.monew.domain.interest.dto.InterestResponse;
import com.sprint.mission.monew.domain.interest.dto.InterestUpdateRequest;
import com.sprint.mission.monew.domain.interest.service.InterestService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InterestController.class)
class InterestControllerTest {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  ObjectMapper objectMapper;

  @MockitoBean
  InterestService interestService;

  @Nested
  @DisplayName("POST /api/interests — 관심사 등록")
  class CreateInterest {

    @Test
    @DisplayName("name이 blank이면 400을 반환한다")
    void name이_blank이면_400을_반환한다() throws Exception {
      // given
      InterestCreateRequest request = new InterestCreateRequest("", List.of("AI"));

      // when & then
      mockMvc
          .perform(
              post("/api/interests")
                  .header("Monew-Request-User-ID", UUID.randomUUID())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
      verifyNoInteractions(interestService);
    }

    @Test
    @DisplayName("정상 요청이면 201과 InterestDto를 반환한다")
    void 정상_요청이면_201과_InterestDto를_반환한다() throws Exception {
      // given
      UUID requestUserId = UUID.randomUUID();
      InterestCreateRequest request = new InterestCreateRequest("인공지능", List.of("AI", "머신러닝"));
      InterestResponse response =
          new InterestResponse(UUID.randomUUID(), "인공지능", List.of("AI", "머신러닝"), 0L, false);

      given(interestService.create(any(InterestCreateRequest.class), any(UUID.class)))
          .willReturn(response);

      // when & then
      mockMvc
          .perform(
              post("/api/interests")
                  .header("Monew-Request-User-ID", requestUserId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.name").value("인공지능"));
    }
  }

  @Nested
  @DisplayName("PATCH /api/interests/{interestId} — 관심사 키워드 수정")
  class UpdateKeywords {

    @Test
    @DisplayName("keywords가 null이면 400을 반환한다")
    void keywords가_null이면_400을_반환한다() throws Exception {
      // given
      InterestUpdateRequest request = new InterestUpdateRequest(null);

      // when & then
      mockMvc
          .perform(
              patch("/api/interests/{interestId}", UUID.randomUUID())
                  .header("Monew-Request-User-ID", UUID.randomUUID())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
      verifyNoInteractions(interestService);
    }

    @Test
    @DisplayName("정상 요청이면 200과 InterestResponse를 반환한다")
    void 정상_요청이면_200과_InterestResponse를_반환한다() throws Exception {
      // given
      UUID interestId = UUID.randomUUID();
      UUID requestUserId = UUID.randomUUID();
      InterestUpdateRequest request = new InterestUpdateRequest(List.of("자연어처리", "GPT"));
      InterestResponse response =
          new InterestResponse(interestId, "인공지능", List.of("자연어처리", "GPT"), 0L, false);

      given(interestService.updateKeywords(any(UUID.class), any(InterestUpdateRequest.class), any(UUID.class)))
          .willReturn(response);

      // when & then
      mockMvc
          .perform(
              patch("/api/interests/{interestId}", interestId)
                  .header("Monew-Request-User-ID", requestUserId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.keywords[0]").value("자연어처리"));
    }
  }
}
