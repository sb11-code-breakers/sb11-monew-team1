package com.sprint.mission.monew.common.filter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.context.TestPropertySource;

@WebMvcTest(
    value = RateLimitFilterTest.FakeController.class,
    excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AuthFilter.class)
)
@Import(RateLimitFilter.class)
@TestPropertySource(properties = "monew.rate-limit.enabled=true")
class RateLimitFilterTest {

  @Autowired
  private MockMvc mockMvc;

  @RestController
  static class FakeController {

    @PostMapping("/test/rate/login")
    void login() {}

    @PostMapping("/test/rate/signup")
    void signup() {}

    @PostMapping("/test/rate/password-reset")
    void passwordReset() {}

    @PostMapping("/test/rate/unlock")
    void unlock() {}

    @GetMapping("/test/rate/articles")
    void articles() {}
  }

  @Nested
  @DisplayName("로그인 Rate Limiting")
  class LoginRateLimit {

    @Test
    @DisplayName("한도 이하 요청은 정상 처리된다")
    void 한도_이하_요청은_정상_처리된다() throws Exception {
      String ip = "1.2.3.4";
      mockMvc
          .perform(post("/test/rate/login")
              .header("X-Forwarded-For", ip))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그인 IP 한도 초과 시 429 반환")
    void 로그인_IP_한도_초과_시_429_반환() throws Exception {
      String ip = "99.99.99.99"; // 다른 테스트와 겹치지 않는 IP
      for (int i = 0; i < 3; i++) {
        mockMvc.perform(post("/test/rate/login")
            .header("X-Forwarded-For", ip));
      }
      mockMvc
          .perform(post("/test/rate/login")
              .header("X-Forwarded-For", ip))
          .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("다른 IP는 별도 한도 적용")
    void 다른_IP는_별도_한도_적용() throws Exception {
      String ip1 = "10.0.0.1";
      String ip2 = "10.0.0.2";
      for (int i = 0; i < 3; i++) {
        mockMvc.perform(post("/test/rate/login")
            .header("X-Forwarded-For", ip1));
      }
      mockMvc
          .perform(post("/test/rate/login")
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
          .perform(post("/test/rate/signup")
              .header("X-Forwarded-For", "2.2.2.2"))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("비밀번호 재설정 Rate Limiting")
  class PasswordResetRateLimit {

    @Test
    @DisplayName("비밀번호 재설정 한도 이하 요청은 정상 처리된다")
    void 비밀번호_재설정_한도_이하_요청은_정상_처리된다() throws Exception {
      mockMvc
          .perform(post("/test/rate/password-reset")
              .header("Monew-Request-User-ID", UUID.randomUUID().toString()))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("잠금 해제 Rate Limiting")
  class UnlockRateLimit {

    @Test
    @DisplayName("잠금 해제 한도 이하 요청은 정상 처리된다")
    void 잠금_해제_한도_이하_요청은_정상_처리된다() throws Exception {
      mockMvc
          .perform(post("/test/rate/unlock")
              .header("Monew-Request-User-ID", UUID.randomUUID().toString()))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("기사 조회 Rate Limiting")
  class ArticlesRateLimit {

    @Test
    @DisplayName("인증된 사용자 한도 이하 요청은 정상 처리된다")
    void 인증된_사용자_한도_이하_요청은_정상_처리된다() throws Exception {
      mockMvc
          .perform(get("/test/rate/articles")
              .header("Monew-Request-User-ID", UUID.randomUUID().toString()))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 한도 미적용")
    void 인증되지_않은_사용자는_한도_미적용() throws Exception {
      mockMvc
          .perform(get("/test/rate/articles"))
          .andExpect(status().isOk());
    }
  }
}