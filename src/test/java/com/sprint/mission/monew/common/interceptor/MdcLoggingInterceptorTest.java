package com.sprint.mission.monew.common.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class MdcLoggingInterceptorTest {

  MdcLoggingInterceptor interceptor = new MdcLoggingInterceptor();
  MockHttpServletRequest request = new MockHttpServletRequest();
  MockHttpServletResponse response = new MockHttpServletResponse();

  @BeforeEach
  void setUp() {
    MDC.clear();
  }

  @Nested
  @DisplayName("preHandle")
  class PreHandle {

    @Test
    @DisplayName("MDC에 requestId가 UUID 형식(36자)으로 세팅된다")
    void MDC에_requestId가_UUID_형식으로_세팅된다() {
      // given & when
      interceptor.preHandle(request, response, new Object());

      // then
      assertThat(MDC.get("requestId"))
          .hasSize(36)
          .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("MDC에 method가 세팅된다")
    void MDC에_method가_세팅된다() {
      // given
      request.setMethod("GET");

      // when
      interceptor.preHandle(request, response, new Object());

      // then
      assertThat(MDC.get("method")).isEqualTo("GET");
    }

    @Test
    @DisplayName("MDC에 url이 세팅된다")
    void MDC에_url이_세팅된다() {
      // given
      request.setRequestURI("/api/users");

      // when
      interceptor.preHandle(request, response, new Object());

      // then
      assertThat(MDC.get("url")).isEqualTo("/api/users");
    }

    @Test
    @DisplayName("CF-Connecting-IP 헤더가 있으면 X-Forwarded-For보다 우선 적용한다")
    void CF_Connecting_IP_헤더가_있으면_X_Forwarded_For보다_우선_적용한다() {
      // given
      request.addHeader("CF-Connecting-IP", "203.0.113.1");
      request.addHeader("X-Forwarded-For", "10.0.0.1, 172.16.0.1");
      request.setRemoteAddr("192.168.0.1");

      // when
      interceptor.preHandle(request, response, new Object());

      // then
      assertThat(MDC.get("clientIp")).isEqualTo("203.0.113.1");
    }

    @Test
    @DisplayName("X-Forwarded-For 헤더가 없으면 remoteAddr을 clientIp로 세팅한다")
    void X_Forwarded_For_헤더가_없으면_remoteAddr을_clientIp로_세팅한다() {
      // given
      request.setRemoteAddr("192.168.0.1");

      // when
      interceptor.preHandle(request, response, new Object());

      // then
      assertThat(MDC.get("clientIp")).isEqualTo("192.168.0.1");
    }

    @Test
    @DisplayName("X-Forwarded-For 헤더가 있으면 첫 번째 IP를 clientIp로 세팅한다")
    void X_Forwarded_For_헤더가_있으면_첫_번째_IP를_clientIp로_세팅한다() {
      // given
      request.addHeader("X-Forwarded-For", "10.0.0.1, 172.16.0.1, 192.168.0.1");

      // when
      interceptor.preHandle(request, response, new Object());

      // then
      assertThat(MDC.get("clientIp")).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("응답 헤더에 Monew-Request-ID가 UUID 형식(36자)으로 세팅된다")
    void 응답_헤더에_Monew_Request_ID가_세팅된다() {
      // given & when
      interceptor.preHandle(request, response, new Object());

      // then
      assertThat(response.getHeader("Monew-Request-ID"))
          .isNotNull()
          .hasSize(36)
          .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("응답 헤더의 Monew-Request-ID와 MDC의 requestId가 일치한다")
    void 응답_헤더의_Monew_Request_ID와_MDC의_requestId가_일치한다() {
      // given & when
      interceptor.preHandle(request, response, new Object());

      // then
      assertThat(response.getHeader("Monew-Request-ID")).isEqualTo(MDC.get("requestId"));
    }

    @Test
    @DisplayName("true를 반환한다")
    void true를_반환한다() {
      // given & when
      boolean result = interceptor.preHandle(request, response, new Object());

      // then
      assertThat(result).isTrue();
    }
  }

  @Nested
  @DisplayName("afterCompletion")
  class AfterCompletion {

    @Test
    @DisplayName("MDC가 클리어된다")
    void MDC가_클리어된다() {
      // given
      interceptor.preHandle(request, response, new Object());
      assertThat(MDC.get("requestId")).isNotNull();

      // when
      interceptor.afterCompletion(request, response, new Object(), null);

      // then
      assertThat(MDC.get("requestId")).isNull();
      assertThat(MDC.get("method")).isNull();
      assertThat(MDC.get("url")).isNull();
      assertThat(MDC.get("clientIp")).isNull();
    }
  }
}
