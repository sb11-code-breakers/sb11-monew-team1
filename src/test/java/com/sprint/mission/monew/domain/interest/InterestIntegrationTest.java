package com.sprint.mission.monew.domain.interest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.monew.domain.interest.dto.InterestCreateRequest;
import com.sprint.mission.monew.domain.interest.dto.InterestUpdateRequest;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.entity.Subscription;
import com.sprint.mission.monew.domain.interest.repository.InterestRepository;
import com.sprint.mission.monew.domain.interest.repository.SubscriptionRepository;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
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
  @Autowired SubscriptionRepository subscriptionRepository;
  @Autowired UserRepository userRepository;

  @BeforeEach
  void setUp() {
    subscriptionRepository.deleteAll();
    interestRepository.deleteAll();
    userRepository.deleteAll();
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
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.keywords[0]").value("GPT"))
          .andExpect(jsonPath("$.keywords[1]").value("자연어처리"));
    }
  }

  @Nested
  @DisplayName("DELETE /api/interests/{interestId} — 관심사 물리 삭제")
  class HardDelete {

    @Test
    @DisplayName("존재하지 않는 관심사 삭제 시 404를 반환한다")
    void 존재하지_않는_관심사_삭제_시_404를_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(
              delete("/api/interests/{id}", UUID.randomUUID()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("INTEREST_NOT_FOUND"));
    }

    @Test
    @DisplayName("정상 요청이면 204를 반환하고 DB에서 삭제된다")
    void 정상_요청이면_204를_반환하고_DB에서_삭제된다() throws Exception {
      // given
      Interest interest = interestRepository.save(Interest.create("블록체인", List.of("비트코인")));

      // when & then
      mockMvc
          .perform(
              delete("/api/interests/{id}", interest.getId()))
          .andExpect(status().isNoContent());

      assertThat(interestRepository.findById(interest.getId())).isEmpty();
    }
  }

  @Nested
  @DisplayName("DELETE /api/interests/{interestId}/subscriptions — 관심사 구독 취소")
  class Unsubscribe {

    Interest interest;
    User user;

    @BeforeEach
    void setUp() {
      interest = interestRepository.save(Interest.create("인공지능", List.of("AI", "머신러닝")));
      user = userRepository.save(User.create("test@test.com", "테스터", "password123!"));
    }

    @Test
    @DisplayName("존재하지 않는 관심사 구독 취소 시 404를 반환한다")
    void 존재하지_않는_관심사_구독_취소_시_404를_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(delete("/api/interests/{interestId}/subscriptions", UUID.randomUUID())
              .header("Monew-Request-User-ID", user.getId()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("INTEREST_NOT_FOUND"));
    }

    @Test
    @DisplayName("구독하지 않은 관심사 취소 시 404를 반환한다")
    void 구독하지_않은_관심사_취소_시_404를_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(delete("/api/interests/{interestId}/subscriptions", interest.getId())
              .header("Monew-Request-User-ID", user.getId()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("SUBSCRIPTION_NOT_FOUND"));
    }

    @Test
    @DisplayName("정상 취소 시 204를 반환하고 DB에서 삭제되며 subscriberCount가 감소한다")
    void 정상_취소_시_204를_반환하고_DB에서_삭제되며_subscriberCount가_감소한다() throws Exception {
      // given
      subscriptionRepository.save(Subscription.create(interest, user));
      interest.increaseSubscriberCount();
      interestRepository.save(interest);

      // when & then
      mockMvc
          .perform(delete("/api/interests/{interestId}/subscriptions", interest.getId())
              .header("Monew-Request-User-ID", user.getId()))
          .andExpect(status().isNoContent());

      assertThat(subscriptionRepository.existsByInterestIdAndUserId(
          interest.getId(), user.getId())).isFalse();
      assertThat(interestRepository.findById(interest.getId())
          .get().getSubscriberCount()).isEqualTo(0L);
    }
  }

  @Nested
  @DisplayName("POST /api/interests/{interestId}/subscriptions — 관심사 구독")
  class Subscribe {

    Interest interest;
    User user;

    @BeforeEach
    void setUp() {
      interest = interestRepository.save(Interest.create("인공지능", List.of("AI", "머신러닝")));
      user = userRepository.save(User.create("test@test.com", "테스터", "password123!"));
    }

    @Test
    @DisplayName("존재하지 않는 관심사 구독 시 404를 반환한다")
    void 존재하지_않는_관심사_구독_시_404를_반환한다() throws Exception {
      // when & then
      mockMvc
          .perform(post("/api/interests/{interestId}/subscriptions", UUID.randomUUID())
              .header("Monew-Request-User-ID", user.getId()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("INTEREST_NOT_FOUND"));
    }

    @Test
    @DisplayName("이미 구독 중인 경우 409를 반환한다")
    void 이미_구독_중인_경우_409를_반환한다() throws Exception {
      // given
      subscriptionRepository.save(Subscription.create(interest, user));

      // when & then
      mockMvc
          .perform(post("/api/interests/{interestId}/subscriptions", interest.getId())
              .header("Monew-Request-User-ID", user.getId()))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("SUBSCRIPTION_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("정상 구독 시 201과 SubscriptionResponse를 반환하고 subscriberCount가 증가한다")
    void 정상_구독_시_201과_SubscriptionResponse를_반환하고_subscriberCount가_증가한다() throws Exception {
      // when & then
      mockMvc
          .perform(post("/api/interests/{interestId}/subscriptions", interest.getId())
              .header("Monew-Request-User-ID", user.getId()))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.interestId").value(interest.getId().toString()))
          .andExpect(jsonPath("$.interestName").value("인공지능"))
          .andExpect(jsonPath("$.interestSubscriberCount").value(1));

      assertThat(subscriptionRepository.existsByInterestIdAndUserId(
          interest.getId(), user.getId())).isTrue();
      assertThat(interestRepository.findById(interest.getId())
          .get().getSubscriberCount()).isEqualTo(1L);
    }
  }
}