package com.sprint.mission.monew.domain.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.monew.common.config.MongoContainerConfig;
import com.sprint.mission.monew.domain.user.document.UserSession;
import com.sprint.mission.monew.domain.user.dto.UserCreateRequest;
import com.sprint.mission.monew.domain.user.dto.UserLoginRequest;
import com.sprint.mission.monew.domain.user.dto.UserUnlockRequest;
import com.sprint.mission.monew.domain.user.repository.EmailVerificationRepository;
import com.sprint.mission.monew.domain.user.repository.PasswordResetTokenRepository;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import com.sprint.mission.monew.domain.user.repository.UserSessionRepository;
import com.sprint.mission.monew.domain.user.repository.UserUnlockTokenRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(MongoContainerConfig.class)
class UserIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private EmailVerificationRepository emailVerificationRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserUnlockTokenRepository userUnlockTokenRepository;

  @Autowired
  private PasswordResetTokenRepository passwordResetTokenRepository;

  @Autowired
  private UserSessionRepository userSessionRepository;

  @AfterEach
  void tearDown() {
    userSessionRepository.deleteAll();
    userUnlockTokenRepository.deleteAll();
    passwordResetTokenRepository.deleteAll();
    emailVerificationRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("회원가입 → 이메일 인증 → 로그인 전체 플로우")
  void 회원가입_이메일인증_로그인_플로우() throws Exception {
    // given
    UserCreateRequest createRequest = new UserCreateRequest(
        "integration@test.com", "통합테스터", "test1234");
    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated());

    String token = emailVerificationRepository.findAll().stream()
        .findFirst().orElseThrow().getToken();
    mockMvc.perform(get("/api/users/verify").param("token", token))
        .andExpect(status().isOk());

    // when & then
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
    // given
    UserCreateRequest createRequest = new UserCreateRequest(
        "lock@test.com", "잠금테스터", "test1234");
    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated());

    String token = emailVerificationRepository.findAll().stream()
        .findFirst().orElseThrow().getToken();
    mockMvc.perform(get("/api/users/verify").param("token", token))
        .andExpect(status().isOk());

    // when - 4회 실패
    UserLoginRequest wrongRequest = new UserLoginRequest("lock@test.com", "wrongpassword");
    for (int i = 0; i < 4; i++) {
      mockMvc.perform(post("/api/users/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(wrongRequest)))
          .andExpect(status().isUnauthorized());
    }

    // then - 5회째 423
    mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(wrongRequest)))
        .andExpect(status().isLocked());
  }

  @Test
  @DisplayName("계정 잠금 해제 → 로그인 성공 플로우")
  void 계정_잠금_해제_로그인_성공_플로우() throws Exception {
    // given
    UserCreateRequest createRequest = new UserCreateRequest(
        "unlock@test.com", "잠금테스터", "test1234");
    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated());

    String verifyToken = emailVerificationRepository.findAll().stream()
        .findFirst().orElseThrow().getToken();
    mockMvc.perform(get("/api/users/verify").param("token", verifyToken))
        .andExpect(status().isOk());

    UserLoginRequest wrongRequest = new UserLoginRequest("unlock@test.com", "wrongpassword");
    for (int i = 0; i < 4; i++) {
      mockMvc.perform(post("/api/users/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(wrongRequest)))
          .andExpect(status().isUnauthorized());
    }
    mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(wrongRequest)))
        .andExpect(status().isLocked());

    // when
    UserUnlockRequest unlockRequest = new UserUnlockRequest("unlock@test.com");
    mockMvc.perform(post("/api/users/unlock")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(unlockRequest)))
        .andExpect(status().isNoContent());

    String unlockToken = userUnlockTokenRepository.findAll().stream()
        .findFirst().orElseThrow().getToken();
    mockMvc.perform(get("/api/users/unlock").param("token", unlockToken))
        .andExpect(status().isOk());

    // then
    UserLoginRequest loginRequest = new UserLoginRequest("unlock@test.com", "test1234");
    mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("unlock@test.com"));
  }

  @Test
  @DisplayName("논리 삭제 후 로그인 불가")
  void 논리_삭제_후_로그인_불가() throws Exception {
    // given
    UserCreateRequest createRequest = new UserCreateRequest(
        "delete@test.com", "삭제테스터", "test1234");
    String response = mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    UUID userId = UUID.fromString(objectMapper.readTree(response).get("id").asText());

    String token = emailVerificationRepository.findAll().stream()
        .findFirst().orElseThrow().getToken();
    mockMvc.perform(get("/api/users/verify").param("token", token))
        .andExpect(status().isOk());

    // when - 논리 삭제
    UserSession session = createAndSaveSession(userId);
    mockMvc.perform(delete("/api/users/" + userId)
            .header("Monew-Request-User-ID", session.getId()))
        .andExpect(status().isNoContent());

    // then - 로그인 불가
    UserLoginRequest loginRequest = new UserLoginRequest("delete@test.com", "test1234");
    mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isUnauthorized());
  }

  private UserSession createAndSaveSession(UUID userId) {
    UserSession session = UserSession.create(
        userId, "127.0.0.1", "1acaf8f7bdf7054e8279b8a17955fc66", 30);
    userSessionRepository.save(session);
    return session;
  }

  @Nested
  @DisplayName("DELETE /api/users/{userId}/hard — 물리 삭제")
  class HardDelete {

    private static final String ADMIN_TOKEN = "test-admin-token";

    @Test
    @DisplayName("admin token 없이 요청 시 403 반환")
    void admin_token_없이_요청_시_403_반환() throws Exception {
      // given — 헤더 없음

      // when & then
      mockMvc.perform(delete("/api/users/{userId}/hard", UUID.randomUUID()))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("잘못된 admin token으로 요청 시 403 반환")
    void 잘못된_admin_token으로_요청_시_403_반환() throws Exception {
      // given — 잘못된 토큰

      // when & then
      mockMvc.perform(
              delete("/api/users/{userId}/hard", UUID.randomUUID())
                  .header("Monew-Request-User-ID", "wrong-token"))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("유효한 admin token으로 요청 시 204 반환")
    void 유효한_admin_token으로_요청_시_204_반환() throws Exception {
      // given — 유저 생성 후 소프트딜리트(물리 삭제 대상 조건)
      UserCreateRequest createRequest = new UserCreateRequest(
          "harddelete@test.com", "삭제대상", "test1234");
      String response = mockMvc.perform(post("/api/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(createRequest)))
          .andExpect(status().isCreated())
          .andReturn().getResponse().getContentAsString();
      UUID userId = UUID.fromString(objectMapper.readTree(response).get("id").asText());

      UserSession session = createAndSaveSession(userId);
      mockMvc.perform(delete("/api/users/{userId}", userId)
              .header("Monew-Request-User-ID", session.getId()))
          .andExpect(status().isNoContent());

      // when & then
      mockMvc.perform(
              delete("/api/users/{userId}/hard", userId)
                  .header("Monew-Request-User-ID", ADMIN_TOKEN))
          .andExpect(status().isNoContent());
    }
  }
}