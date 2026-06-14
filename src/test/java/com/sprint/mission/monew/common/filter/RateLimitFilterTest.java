package com.sprint.mission.monew.common.filter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

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

  @AfterEach
  void tearDown() {
    rateLimitFilter.clearBuckets();
  }

  @RestController
  @Profile("test-rate")
  static class FakeController {

    @PostMapping("/api/users/login")
    void login() {}

    @PostMapping("/api/users")
    void signup() {}

    @PostMapping("/api/users/password/reset")
    void passwordReset() {}

    @PostMapping("/api/users/unlock")
    void unlock() {}

    @GetMapping("/api/articles")
    void articles() {}

    @GetMapping("/api/notifications")
    void notifications() {}

    @PostMapping("/api/notifications")
    void notificationsPost() {}

    @PostMapping("/api/comments")
    void comments() {}

    @GetMapping("/api/comments")
    void commentsGet() {}

    @PostMapping("/api/comments/{commentId}/likes")
    void commentLikes() {}

    @GetMapping("/api/comments/{commentId}/likes")
    void commentLikesGet() {}
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
                .header("X-Forwarded-For", ip))
            .andExpect(status().isOk());
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
                .header("X-Forwarded-For", ip1))
            .andExpect(status().isOk());
      }
      mockMvc
          .perform(post("/api/users/login")
              .header("X-Forwarded-For", ip2))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("X-Forwarded-For 없으면 RemoteAddr 사용")
    void X_Forwarded_For_없으면_RemoteAddr_사용() throws Exception {
      mockMvc
          .perform(post("/api/users/login"))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("X-Forwarded-For가 빈 문자열이면 RemoteAddr 사용")
    void X_Forwarded_For가_빈_문자열이면_RemoteAddr_사용() throws Exception {
      mockMvc
          .perform(post("/api/users/login")
              .header("X-Forwarded-For", ""))
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
                .header("X-Forwarded-For", ip))
            .andExpect(status().isOk());
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
          .perform(post("/api/users/password/reset")
              .header("Monew-Request-User-ID", UUID.randomUUID().toString()))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비밀번호 재설정 한도 초과 시 429 반환")
    void 비밀번호_재설정_한도_초과_시_429_반환() throws Exception {
      String userId = UUID.randomUUID().toString();
      for (int i = 0; i < 3; i++) {
        mockMvc.perform(post("/api/users/password/reset")
                .header("Monew-Request-User-ID", userId))
            .andExpect(status().isOk());
      }
      mockMvc
          .perform(post("/api/users/password/reset")
              .header("Monew-Request-User-ID", userId))
          .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("비정상 userId 헤더는 IP fallback 적용")
    void 비정상_userId_헤더는_IP_fallback_적용() throws Exception {
      mockMvc
          .perform(post("/api/users/password/reset")
              .header("Monew-Request-User-ID", "invalid-uuid")
              .header("X-Forwarded-For", "5.5.5.5"))
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
                .header("Monew-Request-User-ID", userId))
            .andExpect(status().isOk());
      }
      mockMvc
          .perform(post("/api/users/unlock")
              .header("Monew-Request-User-ID", userId))
          .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("userId null 시 IP fallback 적용")
    void userId_null_시_IP_fallback_적용() throws Exception {
      mockMvc
          .perform(post("/api/users/unlock")
              .header("X-Forwarded-For", "6.6.6.6"))
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
          .perform(get("/api/articles")
              .header("Monew-Request-User-ID", UUID.randomUUID().toString()))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 IP fallback 적용")
    void 인증되지_않은_사용자는_IP_fallback_적용() throws Exception {
      mockMvc
          .perform(get("/api/articles")
              .header("X-Forwarded-For", "7.7.7.7"))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비정상 userId 헤더는 IP fallback 적용")
    void 비정상_userId_헤더는_IP_fallback_적용() throws Exception {
      mockMvc
          .perform(get("/api/articles")
              .header("Monew-Request-User-ID", "invalid-uuid")
              .header("X-Forwarded-For", "8.8.8.8"))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("알림 조회 Rate Limiting")
  class NotificationsRateLimit {

    @Test
    @DisplayName("인증된 사용자 한도 이하 요청은 정상 처리된다")
    void 인증된_사용자_한도_이하_요청은_정상_처리된다() throws Exception {
      mockMvc
          .perform(get("/api/notifications")
              .header("Monew-Request-User-ID", UUID.randomUUID().toString()))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 IP fallback 적용")
    void 인증되지_않은_사용자는_IP_fallback_적용() throws Exception {
      mockMvc
          .perform(get("/api/notifications")
              .header("X-Forwarded-For", "9.9.9.9"))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("알림 한도 초과 시 429 반환")
    void 알림_한도_초과_시_429_반환() throws Exception {
      String userId = UUID.randomUUID().toString();
      for (int i = 0; i < 60; i++) {
        mockMvc.perform(get("/api/notifications")
                .header("Monew-Request-User-ID", userId))
            .andExpect(status().isOk());
      }
      mockMvc
          .perform(get("/api/notifications")
              .header("Monew-Request-User-ID", userId))
          .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("GET 외 메서드는 한도 미적용")
    void GET_외_메서드는_한도_미적용() throws Exception {
      mockMvc
          .perform(post("/api/notifications")
              .header("Monew-Request-User-ID", UUID.randomUUID().toString()))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("댓글 Rate Limiting")
  class CommentsRateLimit {

    @Test
    @DisplayName("댓글 한도 이하 요청은 정상 처리된다")
    void 댓글_한도_이하_요청은_정상_처리된다() throws Exception {
      mockMvc
          .perform(post("/api/comments")
              .header("Monew-Request-User-ID", UUID.randomUUID().toString()))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 IP fallback 적용")
    void 인증되지_않은_사용자는_IP_fallback_적용() throws Exception {
      mockMvc
          .perform(post("/api/comments")
              .header("X-Forwarded-For", "11.11.11.11"))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("댓글 한도 초과 시 429 반환")
    void 댓글_한도_초과_시_429_반환() throws Exception {
      String userId = UUID.randomUUID().toString();
      for (int i = 0; i < 10; i++) {
        mockMvc.perform(post("/api/comments")
                .header("Monew-Request-User-ID", userId))
            .andExpect(status().isOk());
      }
      mockMvc
          .perform(post("/api/comments")
              .header("Monew-Request-User-ID", userId))
          .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("POST 외 메서드는 한도 미적용")
    void POST_외_메서드는_한도_미적용() throws Exception {
      mockMvc
          .perform(get("/api/comments")
              .header("Monew-Request-User-ID", UUID.randomUUID().toString()))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("댓글 좋아요 Rate Limiting")
  class CommentLikesRateLimit {

    @Test
    @DisplayName("댓글 좋아요 한도 이하 요청은 정상 처리된다")
    void 댓글_좋아요_한도_이하_요청은_정상_처리된다() throws Exception {
      mockMvc
          .perform(post("/api/comments/" + UUID.randomUUID() + "/likes")
              .header("Monew-Request-User-ID", UUID.randomUUID().toString()))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 IP fallback 적용")
    void 인증되지_않은_사용자는_IP_fallback_적용() throws Exception {
      mockMvc
          .perform(post("/api/comments/" + UUID.randomUUID() + "/likes")
              .header("X-Forwarded-For", "12.12.12.12"))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("댓글 좋아요 한도 초과 시 429 반환")
    void 댓글_좋아요_한도_초과_시_429_반환() throws Exception {
      String userId = UUID.randomUUID().toString();
      for (int i = 0; i < 30; i++) {
        mockMvc.perform(post("/api/comments/" + UUID.randomUUID() + "/likes")
                .header("Monew-Request-User-ID", userId))
            .andExpect(status().isOk());
      }
      mockMvc
          .perform(post("/api/comments/" + UUID.randomUUID() + "/likes")
              .header("Monew-Request-User-ID", userId))
          .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("POST 외 메서드는 한도 미적용")
    void POST_외_메서드는_한도_미적용() throws Exception {
      mockMvc
          .perform(get("/api/comments/" + UUID.randomUUID() + "/likes")
              .header("Monew-Request-User-ID", UUID.randomUUID().toString()))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("필터 설정")
  class FilterConfig {

    @Test
    @DisplayName("enabled false 시 Rate Limiting 비활성화")
    void enabled_false_시_Rate_Limiting_비활성화() throws Exception {
      rateLimitFilter.setEnabled(false);
      mockMvc = MockMvcBuilders
          .standaloneSetup(new FakeController())
          .addFilter(rateLimitFilter)
          .build();
      mockMvc
          .perform(post("/api/users/login")
              .header("X-Forwarded-For", "1.2.3.4"))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("clearBuckets 호출 시 버킷이 초기화된다")
    void clearBuckets_호출_시_버킷이_초기화된다() {
      rateLimitFilter.clearBuckets();
    }
  }
}
