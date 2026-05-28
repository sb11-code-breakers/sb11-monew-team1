package com.sprint.mission.monew.domain.interest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.monew.domain.interest.dto.InterestCreateRequest;
import com.sprint.mission.monew.domain.interest.dto.InterestUpdateRequest;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.repository.InterestRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InterestIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired InterestRepository interestRepository;

  @BeforeEach
  void setUp() {
    interestRepository.deleteAll();
  }

  @Nested
  @DisplayName("POST /api/interests — 관심사 등록")
  class CreateInterest {

    @Test
    @DisplayName("유사한 관심사가 이미 존재하면 409를 반환한다")
    void 유사한_관심사가_이미_존재하면_409를_반환한다() throws Exception {
      // given
      interestRepository.save(Interest.create("인공지능X", List.of("머신러닝")));

      InterestCreateRequest request = new InterestCreateRequest("인공지능", List.of("AI"));

      // when & then
      mockMvc
          .perform(
              post("/api/interests")
                  .header("Monew-Request-User-ID", UUID.randomUUID())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("INTEREST_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("정상 요청이면 201과 저장된 관심사를 반환한다")
    void 정상_요청이면_201과_저장된_관심사를_반환한다() throws Exception {
      // given
      InterestCreateRequest request = new InterestCreateRequest("인공지능", List.of("AI", "머신러닝"));

      // when & then
      mockMvc
          .perform(
              post("/api/interests")
                  .header("Monew-Request-User-ID", UUID.randomUUID())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.name").value("인공지능"))
          .andExpect(jsonPath("$.subscriberCount").value(0))
          .andExpect(jsonPath("$.subscribedByMe").value(false));
    }
  }

  @Nested
  @DisplayName("PATCH /api/interests/{interestId} — 관심사 키워드 수정")
  class UpdateKeywords {

    @Test
    @DisplayName("존재하지 않는 관심사 수정 시 404를 반환한다")
    void 존재하지_않는_관심사_수정_시_404를_반환한다() throws Exception {
      // given
      InterestUpdateRequest request = new InterestUpdateRequest(List.of("GPT", "자연어처리"));

      // when & then
      mockMvc
          .perform(
              patch("/api/interests/{id}", UUID.randomUUID())
                  .header("Monew-Request-User-ID", UUID.randomUUID())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("INTEREST_NOT_FOUND"));
    }

    @Test
    @DisplayName("정상 요청이면 200과 수정된 키워드를 반환한다")
    void 정상_요청이면_200과_수정된_키워드를_반환한다() throws Exception {
      // given
      Interest interest = interestRepository.save(Interest.create("인공지능", List.of("AI")));
      InterestUpdateRequest request = new InterestUpdateRequest(List.of("GPT", "자연어처리"));

      // when & then
      mockMvc
          .perform(
              patch("/api/interests/{id}", interest.getId())
                  .header("Monew-Request-User-ID", UUID.randomUUID())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.keywords[0]").value("GPT"))
          .andExpect(jsonPath("$.keywords[1]").value("자연어처리"));
    }
  }
}
