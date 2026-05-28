package com.sprint.mission.monew.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.monew.domain.user.dto.UserCreateRequest;
import com.sprint.mission.monew.domain.user.dto.UserLoginRequest;
import com.sprint.mission.monew.domain.user.dto.UserResponse;
import com.sprint.mission.monew.domain.user.exception.UserEmailDuplicateException;
import com.sprint.mission.monew.domain.user.exception.UserInvalidPasswordException;
import com.sprint.mission.monew.domain.user.service.UserService;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserService userService;

  @Nested
  @DisplayName("POST /api/users — 회원가입")
  class 회원가입 {

    @Test
    @DisplayName("이메일 형식이 잘못되면 400 반환")
    void 이메일_형식이_잘못되면_400_반환() throws Exception {
      // given
      UserCreateRequest request = new UserCreateRequest(
          "invalid-email", "테스터", "password123"
      );

      // when & then
      mockMvc.perform(post("/api/users")
              .contentType(APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이메일 중복 시 409 반환")
    void 이메일_중복_시_409_반환() throws Exception {
      // given
      UserCreateRequest request = new UserCreateRequest(
          "test@test.com", "테스터", "password123"
      );

      given(userService.create(any()))
          .willThrow(UserEmailDuplicateException.withEmail("test@test.com"));

      // when & then
      mockMvc.perform(post("/api/users")
              .contentType(APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("성공 시 201 반환")
    void 성공_시_201_반환() throws Exception {
      // given
      UserCreateRequest request = new UserCreateRequest(
          "test@test.com", "테스터", "password123"
      );
      UserResponse response = new UserResponse(
          UUID.randomUUID(), "test@test.com", "테스터", Instant.now()
      );

      given(userService.create(any())).willReturn(response);

      // when & then
      mockMvc.perform(post("/api/users")
              .contentType(APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.email").value("test@test.com"))
          .andExpect(jsonPath("$.nickname").value("테스터"));
    }
  }

  @Nested
  @DisplayName("POST /api/users/login — 로그인")
  class 로그인 {

    @Test
    @DisplayName("이메일 형식이 잘못되면 400 반환")
    void 이메일_형식이_잘못되면_400_반환() throws Exception {
      // given
      UserLoginRequest request = new UserLoginRequest(
          "invalid-email", "password123"
      );

      // when & then
      mockMvc.perform(post("/api/users/login")
              .contentType(APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 404 반환")
    void 존재하지_않는_이메일이면_404_반환() throws Exception {
      // given
      UserLoginRequest request = new UserLoginRequest(
          "test@test.com", "password123"
      );

      given(userService.login(any()))
          .willThrow(UserNotFoundException.withEmail("test@test.com"));

      // when & then
      mockMvc.perform(post("/api/users/login")
              .contentType(APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("비밀번호가 틀리면 401 반환")
    void 비밀번호가_틀리면_401_반환() throws Exception {
      // given
      UserLoginRequest request = new UserLoginRequest(
          "test@test.com", "wrongpassword"
      );

      given(userService.login(any()))
          .willThrow(UserInvalidPasswordException.withoutDetail());

      // when & then
      mockMvc.perform(post("/api/users/login")
              .contentType(APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("성공 시 200 반환")
    void 성공_시_200_반환() throws Exception {
      // given
      UserLoginRequest request = new UserLoginRequest(
          "test@test.com", "password123"
      );
      UserResponse response = new UserResponse(
          UUID.randomUUID(), "test@test.com", "테스터", Instant.now()
      );

      given(userService.login(any())).willReturn(response);

      // when & then
      mockMvc.perform(post("/api/users/login")
              .contentType(APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.email").value("test@test.com"))
          .andExpect(jsonPath("$.nickname").value("테스터"));
    }
  }
}