package com.sprint.mission.monew.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.monew.common.filter.AuthFilter;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.util.UUID;
import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@WebMvcTest(
    value = GlobalExceptionHandlerTest.FakeController.class,
    excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AuthFilter.class)
)
class GlobalExceptionHandlerTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @RestController
  @RequestMapping("/test")
  static class FakeController {

    @GetMapping("/ok")
    void ok() {}

    @PostMapping("/body")
    void body(@RequestBody BodyRequest payload) {}

    @GetMapping("/type-mismatch/{id}")
    void typeMismatch(@PathVariable UUID id) {}

    @PostMapping("/valid")
    void valid(@Valid @RequestBody ValidRequest payload) {}

    @GetMapping("/monew-exception")
    void monewException() {
      throw UserNotFoundException.withId(UUID.randomUUID());
    }

    @GetMapping("/client-abort")
    void clientAbort() throws IOException {
      throw new ClientAbortException(new IOException("connection reset"));
    }

    @GetMapping("/server-error")
    void serverError() {
      throw new RuntimeException("unexpected error");
    }

    @GetMapping("/missing-header")
    void missingHeader(@RequestHeader("X-Required-Header") String header) {}

    @GetMapping("/missing-param")
    void missingParam(@RequestParam String requiredParam) {}

    @GetMapping("/converter-mismatch")
    void converterMismatch(@Valid @ModelAttribute FakeModelRequest req) {}
  }

  record BodyRequest(String name) {}

  record ValidRequest(@NotBlank String name) {}

  static class FakeModelRequest {
    private FakeOrderBy orderBy;
    public FakeOrderBy getOrderBy() { return orderBy; }
    public void setOrderBy(FakeOrderBy orderBy) { this.orderBy = orderBy; }
  }

  enum FakeOrderBy {
    LATEST;
    static FakeOrderBy from(String value) {
      for (FakeOrderBy v : values()) {
        if (v.name().equalsIgnoreCase(value)) return v;
      }
      throw new InvalidOrderByException(value, FakeOrderBy.class, "지원하는 정렬 기준이 아닙니다.");
    }
  }

  @TestConfiguration
  static class TestWebConfig implements WebMvcConfigurer {
    @Override
    public void addFormatters(FormatterRegistry registry) {
      registry.addConverter(String.class, FakeOrderBy.class, FakeOrderBy::from);
    }
  }

  @Nested
  @DisplayName("404 — 경로 없음")
  class NoResourceFound {

    @Test
    @DisplayName("존재하지 않는 경로 요청 시 404 반환")
    void 존재하지_않는_경로_404_반환() throws Exception {
      // given & when & then
      mockMvc
          .perform(get("/not-exist"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
          .andExpect(jsonPath("$.exceptionType").value("NoResourceFoundException"));
    }
  }

  @Nested
  @DisplayName("405 — HTTP 메서드 미지원")
  class MethodNotAllowed {

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드로 요청 시 405 반환")
    void 지원하지_않는_HTTP_메서드_405_반환() throws Exception {
      // given & when & then
      mockMvc
          .perform(delete("/test/ok"))
          .andExpect(status().isMethodNotAllowed())
          .andExpect(jsonPath("$.status").value(405))
          .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
          .andExpect(jsonPath("$.exceptionType").value("HttpRequestMethodNotSupportedException"));
    }
  }

  @Nested
  @DisplayName("400 — 본문 파싱 실패")
  class MessageNotReadable {

    @Test
    @DisplayName("잘못된 JSON 본문 전달 시 400 반환")
    void 잘못된_JSON_본문_400_반환() throws Exception {
      // given & when & then
      mockMvc
          .perform(post("/test/body")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{ invalid json }"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("MESSAGE_NOT_READABLE"))
          .andExpect(jsonPath("$.exceptionType").value("HttpMessageNotReadableException"));
    }
  }

  @Nested
  @DisplayName("400 — 타입 변환 실패")
  class TypeMismatch {

    @Test
    @DisplayName("UUID 경로 변수에 잘못된 값 전달 시 400 + details 반환")
    void UUID_경로_변수에_잘못된_값_400_반환() throws Exception {
      // given & when & then
      mockMvc
          .perform(get("/test/type-mismatch/not-a-uuid"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"))
          .andExpect(jsonPath("$.details").exists())
          .andExpect(jsonPath("$.exceptionType").value("MethodArgumentTypeMismatchException"));
    }
  }

  @Nested
  @DisplayName("400 — Bean Validation 실패")
  class ValidationError {

    @Test
    @DisplayName("@Valid 검증 실패 시 400 + details(필드명) 반환")
    void Valid_검증_실패_400_반환() throws Exception {
      // given
      String body = objectMapper.writeValueAsString(new ValidRequest(""));

      // when & then
      mockMvc
          .perform(post("/test/valid")
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.details.name").exists())
          .andExpect(jsonPath("$.exceptionType").value("MethodArgumentNotValidException"));
    }
  }

  @Nested
  @DisplayName("비즈니스 예외 — MonewException")
  class MonewExceptionHandler {

    @Test
    @DisplayName("MonewException 발생 시 ErrorCode 기반 상태코드와 응답 반환")
    void MonewException_ErrorCode_기반_응답_반환() throws Exception {
      // given & when & then
      mockMvc
          .perform(get("/test/monew-exception"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
          .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."))
          .andExpect(jsonPath("$.details").exists())
          .andExpect(jsonPath("$.exceptionType").value("UserNotFoundException"));
    }
  }

  @Nested
  @DisplayName("클라이언트 연결 끊김 — ClientAbortException")
  class ClientAbort {

    @Test
    @DisplayName("클라이언트 연결 끊김 시 응답 바디 없이 조용히 처리")
    void 클라이언트_연결_끊김_응답_없이_처리() throws Exception {
      // given & when & then
      mockMvc
          .perform(get("/test/client-abort"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").doesNotExist());
    }
  }

  @Nested
  @DisplayName("400 — 필수 헤더 누락")
  class MissingHeader {

    @Test
    @DisplayName("필수 헤더 누락 시 400 반환")
    void 필수_헤더_누락_시_400_반환() throws Exception {
      mockMvc
          .perform(get("/test/missing-header"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.details['X-Required-Header']").doesNotExist());
    }
  }

  @Nested
  @DisplayName("400 — 필수 파라미터 누락")
  class MissingParam {

    @Test
    @DisplayName("필수 쿼리 파라미터 누락 시 400 반환")
    void 필수_쿼리_파라미터_누락_시_400_반환() throws Exception {
      mockMvc
          .perform(get("/test/missing-param"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.details.requiredParam").value("필수 파라미터입니다"));
    }
  }

  @Nested
  @DisplayName("400 — 컨버터 TypeMismatch (root cause 메시지 추출)")
  class ConverterTypeMismatch {

    @Test
    @DisplayName("컨버터에서 InvalidOrderByException 발생 시 root cause 메시지를 details에 노출")
    void 컨버터_InvalidOrderByException_root_cause_메시지_노출() throws Exception {
      // given & when & then
      mockMvc
          .perform(get("/test/converter-mismatch")
              .param("orderBy", "invalid"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.details.orderBy").value("지원하는 정렬 기준이 아닙니다."));
    }
  }

  @Nested
  @DisplayName("500 — 서버 오류 fallback")
  class InternalError {

    @Test
    @DisplayName("처리되지 않은 예외 발생 시 500 반환")
    void 처리되지_않은_예외_500_반환() throws Exception {
      // given & when & then
      mockMvc
          .perform(get("/test/server-error"))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.status").value(500))
          .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
          .andExpect(jsonPath("$.exceptionType").value("RuntimeException"));
    }
  }
}