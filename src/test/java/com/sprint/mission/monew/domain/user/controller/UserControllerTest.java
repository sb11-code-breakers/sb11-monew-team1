package com.sprint.mission.monew.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.monew.domain.user.document.UserSession;
import com.sprint.mission.monew.domain.user.dto.LoginResult;
import com.sprint.mission.monew.domain.user.dto.UserCreateRequest;
import com.sprint.mission.monew.domain.user.dto.UserLoginRequest;
import com.sprint.mission.monew.domain.user.dto.UserPasswordResetCodeRequest;
import com.sprint.mission.monew.domain.user.dto.UserPasswordResetRequest;
import com.sprint.mission.monew.domain.user.dto.UserPasswordUpdateRequest;
import com.sprint.mission.monew.domain.user.dto.UserResponse;
import com.sprint.mission.monew.domain.user.dto.UserUnlockRequest;
import com.sprint.mission.monew.domain.user.dto.UserUpdateRequest;
import com.sprint.mission.monew.domain.user.exception.InvalidPasswordResetCodeException;
import com.sprint.mission.monew.domain.user.exception.InvalidVerificationTokenException;
import com.sprint.mission.monew.domain.user.exception.UserAccessDeniedException;
import com.sprint.mission.monew.domain.user.exception.UserAccountLockedException;
import com.sprint.mission.monew.domain.user.exception.UserEmailDuplicateException;
import com.sprint.mission.monew.domain.user.exception.UserEmailNotVerifiedException;
import com.sprint.mission.monew.domain.user.exception.UserInvalidPasswordException;
import com.sprint.mission.monew.domain.user.exception.UserInvalidUnlockTokenException;
import com.sprint.mission.monew.domain.user.exception.UserLoginFailedException;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.repository.UserSessionRepository;
import com.sprint.mission.monew.domain.user.service.UserService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

@WebMvcTest(UserController.class)
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private UserSessionRepository userSessionRepository;

  private UUID userId;
  private UUID sessionToken;

  @BeforeEach
  void setUpAuth() {
    userId = UUID.randomUUID();
    UserSession session = UserSession.create(userId, "127.0.0.1", "1acaf8f7bdf7054e8279b8a17955fc66", 30);
    sessionToken = session.getId();
    given(userSessionRepository.findById(sessionToken)).willReturn(Optional.of(session));
  }

  @Nested
  @DisplayName("POST /api/users — 회원가입")
  class Create {

    @Test
    @DisplayName("이메일 형식이 잘못되면 400 반환")
    void 이메일_형식이_잘못되면_400_반환() throws Exception {
      UserCreateRequest request = new UserCreateRequest("invalid-email", "테스터", "password123");
      mockMvc.perform(
              post("/api/users")
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비밀번호가 8자 미만이면 400 반환")
    void 비밀번호가_8자_미만이면_400_반환() throws Exception {
      UserCreateRequest request = new UserCreateRequest("test@test.com", "테스터", "abc123");
      mockMvc.perform(
              post("/api/users")
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비밀번호에 숫자가 없으면 400 반환")
    void 비밀번호에_숫자가_없으면_400_반환() throws Exception {
      UserCreateRequest request = new UserCreateRequest("test@test.com", "테스터", "abcdefgh");
      mockMvc.perform(
              post("/api/users")
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비밀번호에 영문자가 없으면 400 반환")
    void 비밀번호에_영문자가_없으면_400_반환() throws Exception {
      UserCreateRequest request = new UserCreateRequest("test@test.com", "테스터", "12345678");
      mockMvc.perform(
              post("/api/users")
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이메일 중복 시 409 반환")
    void 이메일_중복_시_409_반환() throws Exception {
      UserCreateRequest request = new UserCreateRequest("test@test.com", "테스터", "password123");
      given(userService.create(any()))
          .willThrow(UserEmailDuplicateException.withEmail("test@test.com"));
      mockMvc.perform(
              post("/api/users")
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("성공 시 201 반환")
    void 성공_시_201_반환() throws Exception {
      UserCreateRequest request = new UserCreateRequest("test@test.com", "테스터", "password123");
      UserResponse response = new UserResponse(UUID.randomUUID(), "test@test.com", "테스터",
          Instant.now());
      given(userService.create(any())).willReturn(response);
      mockMvc.perform(
              post("/api/users")
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.email").value("test@test.com"))
          .andExpect(jsonPath("$.nickname").value("테스터"));
    }
  }

  @Nested
  @DisplayName("POST /api/users/login — 로그인")
  class Login {

    @Test
    @DisplayName("이메일 형식이 잘못되면 400 반환")
    void 이메일_형식이_잘못되면_400_반환() throws Exception {
      UserLoginRequest request = new UserLoginRequest("invalid-email", "password123");
      mockMvc.perform(
              post("/api/users/login")
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 401 반환")
    void 존재하지_않는_이메일이면_401_반환() throws Exception {
      UserLoginRequest request = new UserLoginRequest("test@test.com", "password123");
      given(userService.login(any(), any(), any())).willThrow(UserLoginFailedException.withEmail());
      mockMvc.perform(
              post("/api/users/login")
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("이메일 미인증 시 401 반환")
    void 이메일_미인증_시_401_반환() throws Exception {
      UserLoginRequest request = new UserLoginRequest("test@test.com", "password123");
      given(userService.login(any(), any(), any()))
          .willThrow(UserEmailNotVerifiedException.withEmail("test@test.com"));
      mockMvc.perform(
              post("/api/users/login")
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("비밀번호가 틀리면 401 반환")
    void 비밀번호가_틀리면_401_반환() throws Exception {
      UserLoginRequest request = new UserLoginRequest("test@test.com", "wrongpassword");
      given(userService.login(any(), any(), any())).willThrow(
          UserLoginFailedException.withPassword());
      mockMvc.perform(
              post("/api/users/login")
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("성공 시 200 반환")
    void 성공_시_200_반환() throws Exception {
      UserLoginRequest request = new UserLoginRequest("test@test.com", "password123");
      UserResponse response = new UserResponse(UUID.randomUUID(), "test@test.com", "테스터",
          Instant.now());
      given(userService.login(any(), any(), any())).willReturn(
          new LoginResult(response, UUID.randomUUID()));
      mockMvc.perform(
              post("/api/users/login")
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.email").value("test@test.com"))
          .andExpect(jsonPath("$.nickname").value("테스터"));
    }

    @Test
    @DisplayName("계정이 잠긴 경우 423 반환")
    void 계정이_잠긴_경우_423_반환() throws Exception {
      UserLoginRequest request = new UserLoginRequest("test@test.com", "password123");
      given(userService.login(any(), any(), any()))
          .willThrow(UserAccountLockedException.withEmail("test@test.com"));
      mockMvc.perform(
              post("/api/users/login")
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isLocked());
    }
  }

  @Nested
  @DisplayName("POST /api/users/unlock — 계정 잠금 해제 요청")
  class UnlockRequest {

    @Test
    @DisplayName("이메일이 빈 값이면 400 반환")
    void 이메일이_빈_값이면_400_반환() throws Exception {
      // given
      UserUnlockRequest request = new UserUnlockRequest("");

      // when & then
      mockMvc.perform(
              post("/api/users/unlock")
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 404 반환")
    void 존재하지_않는_이메일이면_404_반환() throws Exception {
      // given
      UserUnlockRequest request = new UserUnlockRequest("notfound@test.com");
      willThrow(UserNotFoundException.withEmail("notfound@test.com"))
          .given(userService).requestUnlock(any());

      // when & then
      mockMvc.perform(
              post("/api/users/unlock")
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("성공 시 204 반환")
    void 성공_시_204_반환() throws Exception {
      // given
      UserUnlockRequest request = new UserUnlockRequest("test@test.com");

      // when & then
      mockMvc.perform(
              post("/api/users/unlock")
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isNoContent());
      then(userService).should().requestUnlock(any());
    }
  }

  @Nested
  @DisplayName("GET /api/users/unlock — 계정 잠금 해제")
  class Unlock {

    @Test
    @DisplayName("유효하지 않은 토큰이면 400 반환")
    void 유효하지_않은_토큰이면_400_반환() throws Exception {
      // given
      String invalidToken = UUID.randomUUID().toString();
      willThrow(UserInvalidUnlockTokenException.withToken(invalidToken))
          .given(userService).unlock(eq(invalidToken));

      // when & then
      mockMvc.perform(
              get("/api/users/unlock")
                  .param("token", invalidToken)
          )
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("성공 시 200 반환")
    void 성공_시_200_반환() throws Exception {
      // given
      String validToken = UUID.randomUUID().toString();

      // when & then
      mockMvc.perform(
              get("/api/users/unlock")
                  .param("token", validToken)
          )
          .andExpect(status().isOk());
      then(userService).should().unlock(eq(validToken));
    }
  }

  @Nested
  @DisplayName("GET /api/users/verify — 이메일 인증")
  class VerifyEmail {

    @Test
    @DisplayName("유효하지 않은 토큰이면 400 반환")
    void 유효하지_않은_토큰이면_400_반환() throws Exception {
      willThrow(InvalidVerificationTokenException.withToken("invalid-token"))
          .given(userService).verifyEmail("invalid-token");
      mockMvc.perform(
              get("/api/users/verify")
                  .param("token", "invalid-token")
          )
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("성공 시 200 반환")
    void 성공_시_200_반환() throws Exception {
      mockMvc.perform(
              get("/api/users/verify")
                  .param("token", "valid-token")
          )
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증 성공 시 HTML 페이지 반환")
    void 인증_성공_시_HTML_페이지_반환() throws Exception {
      // when & then
      mockMvc.perform(get("/api/users/verify")
              .param("token", "valid-token"))
          .andExpect(status().isOk())
          .andExpect(content().contentType("text/html;charset=UTF-8"))
          .andExpect(content().string(containsString("이메일 인증 완료")))
          .andExpect(content().string(containsString("/#/login")));
    }

  }

  @Nested
  @DisplayName("PATCH /api/users/{userId} — 닉네임 수정")
  class Update {

    @Test
    @DisplayName("닉네임이 빈 값이면 400 반환")
    void 닉네임이_빈_값이면_400_반환() throws Exception {
      UserUpdateRequest request = new UserUpdateRequest("");
      mockMvc.perform(
              patch("/api/users/{userId}", UUID.randomUUID())
                  .header("Monew-Request-User-ID", sessionToken)
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("다른 사용자가 수정하면 403 반환")
    void 다른_사용자가_수정하면_403_반환() throws Exception {
      UserUpdateRequest request = new UserUpdateRequest("새닉네임");
      given(userService.update(any(), any(), any()))
          .willThrow(UserAccessDeniedException.forUser(UUID.randomUUID()));
      mockMvc.perform(
              patch("/api/users/{userId}", UUID.randomUUID())
                  .header("Monew-Request-User-ID", sessionToken)
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("존재하지 않는 사용자면 404 반환")
    void 존재하지_않는_사용자면_404_반환() throws Exception {
      UUID targetUserId = UUID.randomUUID();
      UserUpdateRequest request = new UserUpdateRequest("새닉네임");
      given(userService.update(eq(targetUserId), eq(userId), any()))
          .willThrow(UserNotFoundException.withId(targetUserId));
      mockMvc.perform(
              patch("/api/users/{userId}", targetUserId)
                  .header("Monew-Request-User-ID", sessionToken)
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("성공 시 200 반환")
    void 성공_시_200_반환() throws Exception {
      UUID targetUserId = UUID.randomUUID();
      UserUpdateRequest request = new UserUpdateRequest("새닉네임");
      UserResponse response = new UserResponse(targetUserId, "test@test.com", "새닉네임",
          Instant.now());
      given(userService.update(any(), any(), any())).willReturn(response);
      mockMvc.perform(
              patch("/api/users/{userId}", targetUserId)
                  .header("Monew-Request-User-ID", sessionToken)
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.nickname").value("새닉네임"));
    }
  }

  @Nested
  @DisplayName("PATCH /api/users/password — 비밀번호 변경")
  class UpdatePassword {

    @Test
    @DisplayName("현재 비밀번호가 빈 값이면 400 반환")
    void 현재_비밀번호가_빈_값이면_400_반환() throws Exception {
      UserPasswordUpdateRequest request = new UserPasswordUpdateRequest("", "newPassword123");
      mockMvc.perform(
              patch("/api/users/password")
                  .header("Monew-Request-User-ID", sessionToken)
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 사용자면 404 반환")
    void 존재하지_않는_사용자면_404_반환() throws Exception {
      UserPasswordUpdateRequest request = new UserPasswordUpdateRequest(
          "currentPassword123", "newPassword123");
      willThrow(UserNotFoundException.withId(userId))
          .given(userService).updatePassword(eq(userId), any());
      mockMvc.perform(
              patch("/api/users/password")
                  .header("Monew-Request-User-ID", sessionToken)
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("현재 비밀번호가 틀리면 401 반환")
    void 현재_비밀번호가_틀리면_401_반환() throws Exception {
      UserPasswordUpdateRequest request = new UserPasswordUpdateRequest(
          "wrongPassword", "newPassword123");
      willThrow(UserInvalidPasswordException.withoutDetail())
          .given(userService).updatePassword(eq(userId), any());
      mockMvc.perform(
              patch("/api/users/password")
                  .header("Monew-Request-User-ID", sessionToken)
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("성공 시 204 반환")
    void 성공_시_204_반환() throws Exception {
      UserPasswordUpdateRequest request = new UserPasswordUpdateRequest(
          "currentPassword123", "newPassword123");
      mockMvc.perform(
              patch("/api/users/password")
                  .header("Monew-Request-User-ID", sessionToken)
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isNoContent());
      then(userService).should().updatePassword(eq(userId), any());
    }
  }

  @Nested
  @DisplayName("POST /api/users/password/reset-request — 비밀번호 재설정 요청")
  class PasswordResetRequest {

    @Test
    @DisplayName("이메일이 빈 값이면 400 반환")
    void 이메일이_빈_값이면_400_반환() throws Exception {
      // given
      UserPasswordResetRequest request = new UserPasswordResetRequest("");

      // when & then
      mockMvc.perform(
              post("/api/users/password/reset")
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 404 반환")
    void 존재하지_않는_이메일이면_404_반환() throws Exception {
      // given
      UserPasswordResetRequest request = new UserPasswordResetRequest("notfound@test.com");
      willThrow(UserNotFoundException.withEmail("notfound@test.com"))
          .given(userService).requestPasswordReset(any());

      // when & then
      mockMvc.perform(
              post("/api/users/password/reset")
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("성공 시 204 반환")
    void 성공_시_204_반환() throws Exception {
      // given
      UserPasswordResetRequest request = new UserPasswordResetRequest("test@test.com");

      // when & then
      mockMvc.perform(
              post("/api/users/password/reset")
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isNoContent());
      then(userService).should().requestPasswordReset(any());
    }
  }

  @Nested
  @DisplayName("PATCH /api/users/password/reset — 비밀번호 재설정")
  class PasswordReset {

    @Test
    @DisplayName("인증 코드가 빈 값이면 400 반환")
    void 인증_코드가_빈_값이면_400_반환() throws Exception {
      // given
      UserPasswordResetCodeRequest request = new UserPasswordResetCodeRequest("", "newPassword123");

      // when & then
      mockMvc.perform(
              patch("/api/users/password/reset")
                  .header("Monew-Request-User-ID", sessionToken)
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유효하지 않은 인증 코드면 400 반환")
    void 유효하지_않은_인증_코드면_400_반환() throws Exception {
      // given
      UserPasswordResetCodeRequest request = new UserPasswordResetCodeRequest("invalid-code",
          "newPassword123");
      willThrow(InvalidPasswordResetCodeException.withCode("invalid-code"))
          .given(userService).resetPassword(any());

      // when & then
      mockMvc.perform(
              patch("/api/users/password/reset")
                  .header("Monew-Request-User-ID", sessionToken)
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("성공 시 204 반환")
    void 성공_시_204_반환() throws Exception {
      // given
      UserPasswordResetCodeRequest request = new UserPasswordResetCodeRequest("valid-code",
          "newPassword123");

      // when & then
      mockMvc.perform(
              patch("/api/users/password/reset")
                  .header("Monew-Request-User-ID", sessionToken)
                  .contentType(APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isNoContent());
      then(userService).should().resetPassword(any());
    }
  }

  @Nested
  @DisplayName("DELETE /api/users/{userId} — 논리 삭제")
  class Delete {

    @Test
    @DisplayName("다른 사용자가 삭제하면 403 반환")
    void 다른_사용자가_삭제하면_403_반환() throws Exception {
      UUID targetUserId = UUID.randomUUID();
      willThrow(UserAccessDeniedException.forUser(UUID.randomUUID()))
          .given(userService).delete(any(), any());
      mockMvc.perform(
              delete("/api/users/{userId}", targetUserId)
                  .header("Monew-Request-User-ID", sessionToken)
          )
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("존재하지 않는 사용자면 404 반환")
    void 존재하지_않는_사용자면_404_반환() throws Exception {
      UUID targetUserId = UUID.randomUUID();
      willThrow(UserNotFoundException.withId(targetUserId))
          .given(userService).delete(eq(targetUserId), eq(userId));
      mockMvc.perform(
              delete("/api/users/{userId}", targetUserId)
                  .header("Monew-Request-User-ID", sessionToken)
          )
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("성공 시 204 반환")
    void 성공_시_204_반환() throws Exception {
      UUID targetUserId = UUID.randomUUID();
      mockMvc.perform(
              delete("/api/users/{userId}", targetUserId)
                  .header("Monew-Request-User-ID", sessionToken)
          )
          .andExpect(status().isNoContent());
    }
  }

  @Nested
  @DisplayName("DELETE /api/users/{userId}/hard — 물리 삭제")
  class HardDelete {

    @Test
    @DisplayName("존재하지 않는 사용자면 404 반환")
    void 존재하지_않는_사용자면_404_반환() throws Exception {
      UUID userId = UUID.randomUUID();
      willThrow(UserNotFoundException.withId(userId))
          .given(userService).hardDelete(eq(userId));
      mockMvc.perform(
              delete("/api/users/{userId}/hard", userId)
          )
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("성공 시 204 반환")
    void 성공_시_204_반환() throws Exception {
      UUID userId = UUID.randomUUID();
      mockMvc.perform(
              delete("/api/users/{userId}/hard", userId)
          )
          .andExpect(status().isNoContent());
      then(userService).should().hardDelete(eq(userId));
    }
  }
}