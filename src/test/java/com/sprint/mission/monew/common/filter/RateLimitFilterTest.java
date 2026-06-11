package com.sprint.mission.monew.common.filter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@ActiveProfiles("test-rate")
class RateLimitFilterTest {

  private MockMvc mockMvc;
  private RateLimitFilter rateLimitFilter;

  @BeforeEach
  void setUp() {
    rateLimitFilter = new RateLimitFilter(
        new ObjectMapper().registerModule(new JavaTimeModule())
    );
    rateLimitFilter.setEnabled(true);
    rateLimitFilter.clearBuckets();
    mockMvc = MockMvcBuilders
        .standaloneSetup(new FakeController())
        .addFilter(rateLimitFilter)
        .build();
  }

  @RestController
  @Profile("test-rate")
  static class FakeController {

    @PostMapping("/api/users/login")
    void login() {}

    @PostMapping("/api/users")
    void signup() {}

    @PostMapping("/api/users/password-reset")
    void passwordReset() {}

    @PostMapping("/api/users/unlock")
    void unlock() {}

    @GetMapping("/api/articles")
    void articles() {}
  }

  @Nested
  @DisplayName("로그인 Rate Limiting")
  class LoginRateLimit {

    @Test
    @DisplayName("한도 이하 요청은 정상 처리된다")
    void 한도_이하_요청은_정상_처리된다() throws Exception {
      mockMvc
          .perform(post("/api/users/login")
              .header("X-Forwarded-For", "1.2.3.4"))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그인 IP 한도 초과 시 429 반환")
    void 로그인_IP_한도_초과_시_429_반환() throws Exception {
      String ip = "99.99.99.99";
      for (int i = 0; i < 3; i++) {
        mockMvc.perform(post("/api/users/login")
            .header("X-Forwarded-For", ip));
      }
      mockMvc
          .perform(post("/api/users/login")
              .header("X-Forwarded-For", ip))
          .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("다른 IP는 별도 한도 적용")
    void 다른_IP는_별도_한도_적용() throws Exception {
      String ip1 = "10.0.0.1";
      String ip2 = "10.0.0.2";
      for (int i = 0; i < 3; i++) {
        mockMvc.perform(post("/api/users/login")
            .header("X-Forwarded-For", ip1));
      }
      mockMvc
          .perform(post("/api/users/login")
              .header("X-Forwarded-For", ip2))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("회원가입 Rate Limiting")
  class SignupRateLimit {

    @Test
    @DisplayName("회원가입 한도 이하 요청은 정상 처리된다")
    void 회원가입_한도_이하_요청은_정상_처리된다() throws Exception {
      mockMvc
          .perform(post("/api/users")
              .header("X-Forwarded-For", "2.2.2.2"))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("회원가입 IP 한도 초과 시 429 반환")
    void 회원가입_IP_한도_초과_시_429_반환() throws Exception {
      String ip = "3.3.3.3";
      for (int i = 0; i < 5; i++) {
        mockMvc.perform(post("/api/users")
            .header("X-Forwarded-For", ip));
      }
      mockMvc
          .perform(post("/api/users")
              .header("X-Forwarded-For", ip))
          .andExpect(status().isTooManyRequests());
    }
  }

  @Nested
  @DisplayName("비밀번호 재설정 Rate Limiting")
  class PasswordResetRateLimit {

    @Test
    @DisplayName("비밀번호 재설정 한도 이하 요청은 정상 처리된다")
    void 비밀번호_재설정_한도_이하_요청은_정상_처리된다() throws Exception {
      mockMvc
          .perform(post("/api/users/password-reset")
              .header("Monew-Request-User-ID", UUID.randomUUID().toString()))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비밀번호 재설정 한도 초과 시 429 반환")
    void 비밀번호_재설정_한도_초과_시_429_반환() throws Exception {
      String userId = UUID.randomUUID().toString();
      for (int i = 0; i < 3; i++) {
        mockMvc.perform(post("/api/users/password-reset")
            .header("Monew-Request-User-ID", userId));
      }
      mockMvc
          .perform(post("/api/users/password-reset")
              .header("Monew-Request-User-ID", userId))
          .andExpect(status().isTooManyRequests());
    }
  }

  @Nested
  @DisplayName("잠금 해제 Rate Limiting")
  class UnlockRateLimit {

    @Test
    @DisplayName("잠금 해제 한도 이하 요청은 정상 처리된다")
    void 잠금_해제_한도_이하_요청은_정상_처리된다() throws Exception {
      mockMvc
          .perform(post("/api/users/unlock")
              .header("Monew-Request-User-ID", UUID.randomUUID().toString()))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("잠금 해제 한도 초과 시 429 반환")
    void 잠금_해제_한도_초과_시_429_반환() throws Exception {
      String userId = UUID.randomUUID().toString();
      for (int i = 0; i < 3; i++) {
        mockMvc.perform(post("/api/users/unlock")
            .header("Monew-Request-User-ID", userId));
      }
      mockMvc
          .perform(post("/api/users/unlock")
              .header("Monew-Request-User-ID", userId))
          .andExpect(status().isTooManyRequests());
    }
  }

  @Nested
  @DisplayName("기사 조회 Rate Limiting")
  class ArticlesRateLimit {

    @Test
    @DisplayName("인증된 사용자 한도 이하 요청은 정상 처리된다")
    void 인증된_사용자_한도_이하_요청은_정상_처리된다() throws Exception {
      mockMvc
          .perform(get("/api/articles")
              .header("Monew-Request-User-ID", UUID.randomUUID().toString()))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 한도 미적용")
    void 인증되지_않은_사용자는_한도_미적용() throws Exception {
      mockMvc
          .perform(get("/api/articles"))
          .andExpect(status().isOk());
    }
  }
}