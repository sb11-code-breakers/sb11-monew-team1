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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(
    value = RateLimitFilterTest.FakeController.class,
    excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AuthFilter.class)
)
@Import(RateLimitFilter.class)
class RateLimitFilterTest {

  @Autowired
  private MockMvc mockMvc;

  @RestController
  static class FakeController {

    @PostMapping("/api/users/login")
    void login() {}

    @GetMapping("/api/articles")
    void articles() {}
  }

  @Nested
  @DisplayName("Rate Limiting")
  class RateLimit {

    @Test
    @DisplayName("한도 이하 요청은 정상 처리된다")
    void 한도_이하_요청은_정상_처리된다() throws Exception {
      String ip = "1.2.3.4";
      mockMvc
          .perform(post("/api/users/login")
              .header("X-Forwarded-For", ip))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그인 IP 한도 초과 시 429 반환")
    void 로그인_IP_한도_초과_시_429_반환() throws Exception {
      String ip = "9.9.9.9";
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
    @DisplayName("인증된 사용자 한도 이하 요청은 정상 처리된다")
    void 인증된_사용자_한도_이하_요청은_정상_처리된다() throws Exception {
      mockMvc
          .perform(get("/api/articles")
              .header("Monew-Request-User-ID", UUID.randomUUID().toString()))
          .andExpect(status().isOk());
    }
  }
}