package com.sprint.mission.monew.domain.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.monew.domain.user.dto.UserCreateRequest;
import com.sprint.mission.monew.domain.user.dto.UserLoginRequest;
import com.sprint.mission.monew.domain.user.repository.EmailVerificationRepository;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "batch.news-collect.chunk-size=10",
    "batch.notification-cleanup.chunk-size=10",
    "batch.user-cleanup.chunk-size=10"
})
class UserApiTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private EmailVerificationRepository emailVerificationRepository;

  @Autowired
  private UserRepository userRepository;

  @AfterEach
  void tearDown() {
    emailVerificationRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("회원가입 → 이메일 인증 → 로그인 전체 플로우")
  void 회원가입_이메일인증_로그인_플로우() throws Exception {
    // 1. 회원가입
    UserCreateRequest createRequest = new UserCreateRequest(
        "integration@test.com", "통합테스터", "test1234");
    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated());

    // 2. 이메일 인증 토큰 조회
    String token = emailVerificationRepository
        .findAll().stream()
        .findFirst()
        .orElseThrow()
        .getToken();

    // 3. 이메일 인증
    mockMvc.perform(get("/api/users/verify")
            .param("token", token))
        .andExpect(status().isOk());

    // 4. 로그인 성공
    UserLoginRequest loginRequest = new UserLoginRequest(
        "integration@test.com", "test1234");
    mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("integration@test.com"));
  }

  @Test
  @DisplayName("로그인 5회 실패 시 계정 잠금")
  void 로그인_5회_실패_시_계정_잠금() throws Exception {
    // 1. 회원가입
    UserCreateRequest createRequest = new UserCreateRequest(
        "lock@test.com", "잠금테스터", "test1234");
    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated());

    // 2. 이메일 인증
    String token = emailVerificationRepository.findAll().stream()
        .findFirst().orElseThrow().getToken();
    mockMvc.perform(get("/api/users/verify").param("token", token))
        .andExpect(status().isOk());

    // 3. 로그인 4회 실패
    UserLoginRequest wrongRequest = new UserLoginRequest("lock@test.com", "wrongpassword");
    for (int i = 0; i < 4; i++) {
      mockMvc.perform(post("/api/users/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(wrongRequest)))
          .andExpect(status().isUnauthorized());
    }

    // 4. 5회째 → 423
    mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(wrongRequest)))
        .andExpect(status().isLocked());
  }
}