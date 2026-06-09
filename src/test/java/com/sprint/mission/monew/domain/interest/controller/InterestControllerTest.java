package com.sprint.mission.monew.domain.interest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.domain.interest.dto.InterestCreateRequest;
import com.sprint.mission.monew.domain.interest.dto.InterestResponse;
import com.sprint.mission.monew.domain.interest.dto.InterestUpdateRequest;
import com.sprint.mission.monew.domain.interest.exception.InterestNotFoundException;
import com.sprint.mission.monew.domain.interest.service.InterestService;
import com.sprint.mission.monew.domain.user.document.UserSession;
import com.sprint.mission.monew.domain.user.repository.UserSessionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InterestController.class)
class InterestControllerTest {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  ObjectMapper objectMapper;

  @MockitoBean
  InterestService interestService;

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
  @DisplayName("GET /api/interests — 관심사 목록 조회")
  class FindAll {

    @Test
    @DisplayName("Monew-Request-User-ID 헤더가 없으면 401을 반환한다")
    void Monew_Request_User_ID_헤더가_없으면_401을_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(
              get("/api/interests")
                  .param("orderBy", "name")
                  .param("direction", "ASC")
                  .param("limit", "10")
          )
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("orderBy가 없으면 400을 반환한다")
    void orderBy가_없으면_400을_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(
              get("/api/interests")
                  .header("Monew-Request-User-ID", sessionToken)
                  .param("direction", "ASC")
                  .param("limit", "10")
          )
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("direction이 없으면 400을 반환한다")
    void direction이_없으면_400을_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(
              get("/api/interests")
                  .header("Monew-Request-User-ID", sessionToken)
                  .param("orderBy", "name")
                  .param("limit", "10")
          )
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("limit이 없으면 400을 반환한다")
    void limit이_없으면_400을_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(
              get("/api/interests")
                  .header("Monew-Request-User-ID", sessionToken)
                  .param("orderBy", "name")
                  .param("direction", "ASC")
          )
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("limit이 0이면 400을 반환한다")
    void limit이_0이면_400을_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(
              get("/api/interests")
                  .header("Monew-Request-User-ID", sessionToken)
                  .param("orderBy", "name")
                  .param("direction", "ASC")
                  .param("limit", "0")
          )
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("cursor만 있고 after가 없으면 400을 반환한다")
    void cursor만_있고_after가_없으면_400을_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(
              get("/api/interests")
                  .header("Monew-Request-User-ID", sessionToken)
                  .param("orderBy", "name")
                  .param("direction", "ASC")
                  .param("limit", "10")
                  .param("cursor", "Baseball")
          )
          .andExpect(status().isBadRequest());
      verifyNoInteractions(interestService);
    }

    @Test
    @DisplayName("after만 있고 cursor가 없으면 400을 반환한다")
    void after만_있고_cursor가_없으면_400을_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(
              get("/api/interests")
                  .header("Monew-Request-User-ID", sessionToken)
                  .param("orderBy", "name")
                  .param("direction", "ASC")
                  .param("limit", "10")
                  .param("after", java.time.Instant.now().toString())
          )
          .andExpect(status().isBadRequest());
      verifyNoInteractions(interestService);
    }

    @Test
    @DisplayName("orderBy=subscriberCount이고 cursor가 숫자가 아니면 400을 반환한다")
    void orderBy가_subscriberCount이고_cursor가_숫자가_아니면_400을_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(
              get("/api/interests")
                  .header("Monew-Request-User-ID", sessionToken)
                  .param("orderBy", "subscriberCount")
                  .param("direction", "ASC")
                  .param("limit", "10")
                  .param("cursor", "invalid")
                  .param("after", java.time.Instant.now().toString())
                  .param("idAfter", UUID.randomUUID().toString())
          )
          .andExpect(status().isBadRequest());
      verifyNoInteractions(interestService);
    }

    @Test
    @DisplayName("정상 요청이면 200과 CursorPageResponse를 반환한다")
    void 정상_요청이면_200과_CursorPageResponse를_반환한다() throws Exception {
      // given
      CursorPageResponse<InterestResponse> response =
          CursorPageResponse.of(List.of(), null, null, null, false, 0, 0L);
      given(interestService.findAll(any(), any(UUID.class))).willReturn(response);

      // when & then
      mockMvc
          .perform(
              get("/api/interests")
                  .header("Monew-Request-User-ID", sessionToken)
                  .param("orderBy", "name")
                  .param("direction", "ASC")
                  .param("limit", "10")
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.hasNext").value(false))
          .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("orderBy=subscriberCount이고 cursor가 유효한 숫자면 200을 반환한다")
    void orderBy가_subscriberCount이고_cursor가_유효한_숫자면_200을_반환한다() throws Exception {
      // given
      CursorPageResponse<InterestResponse> response =
          CursorPageResponse.of(List.of(), null, null, null, false, 0, 0L);
      given(interestService.findAll(any(), any(UUID.class))).willReturn(response);

      // when & then
      mockMvc
          .perform(
              get("/api/interests")
                  .header("Monew-Request-User-ID", sessionToken)
                  .param("orderBy", "subscriberCount")
                  .param("direction", "ASC")
                  .param("limit", "10")
                  .param("cursor", "5")
                  .param("after", java.time.Instant.now().toString())
                  .param("idAfter", UUID.randomUUID().toString())
          )
          .andExpect(status().isOk());
    }
  }

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
                  .header("Monew-Request-User-ID", sessionToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest());
      verifyNoInteractions(interestService);
    }

    @Test
    @DisplayName("정상 요청이면 201과 InterestDto를 반환한다")
    void 정상_요청이면_201과_InterestDto를_반환한다() throws Exception {
      // given
      InterestCreateRequest request = new InterestCreateRequest("인공지능", List.of("AI", "머신러닝"));
      InterestResponse response =
          new InterestResponse(UUID.randomUUID(), "인공지능", List.of("AI", "머신러닝"), 0L, false);

      given(interestService.create(any(InterestCreateRequest.class))).willReturn(response);

      // when & then
      mockMvc
          .perform(
              post("/api/interests")
                  .header("Monew-Request-User-ID", sessionToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
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
                  .header("Monew-Request-User-ID", sessionToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest());
      verifyNoInteractions(interestService);
    }

    @Test
    @DisplayName("정상 요청이면 200과 InterestResponse를 반환한다")
    void 정상_요청이면_200과_InterestResponse를_반환한다() throws Exception {
      // given
      UUID interestId = UUID.randomUUID();
      InterestUpdateRequest request = new InterestUpdateRequest(List.of("자연어처리", "GPT"));
      InterestResponse response =
          new InterestResponse(interestId, "인공지능", List.of("자연어처리", "GPT"), 0L, false);

      given(interestService.updateKeywords(any(UUID.class), any(InterestUpdateRequest.class)))
          .willReturn(response);

      // when & then
      mockMvc
          .perform(
              patch("/api/interests/{interestId}", interestId)
                  .header("Monew-Request-User-ID", sessionToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.keywords[0]").value("자연어처리"));
    }
  }

  @Nested
  @DisplayName("DELETE /api/interests/{interestId} — 관심사 물리 삭제")
  class HardDelete {

    @Test
    @DisplayName("존재하지 않는 관심사 삭제 시 404를 반환한다")
    void 존재하지_않는_관심사_삭제_시_404를_반환한다() throws Exception {
      // given
      UUID interestId = UUID.randomUUID();
      willThrow(InterestNotFoundException.withId(interestId))
          .given(interestService).hardDelete(interestId);

      // when & then
      mockMvc
          .perform(
              delete("/api/interests/{interestId}", interestId)
                  .header("Monew-Request-User-ID", sessionToken)
          )
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("정상 요청이면 204를 반환한다")
    void 정상_요청이면_204를_반환한다() throws Exception {
      UUID interestId = UUID.randomUUID();

      // when & then
      mockMvc
          .perform(
              delete("/api/interests/{interestId}", interestId)
                  .header("Monew-Request-User-ID", sessionToken)
          )
          .andExpect(status().isNoContent());

      verify(interestService).hardDelete(interestId);
    }
  }
}