package com.sprint.mission.monew.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.sprint.mission.monew.domain.user.dto.UserPasswordUpdateRequest;
import com.sprint.mission.monew.domain.user.dto.UserResponse;
import com.sprint.mission.monew.domain.user.dto.UserUpdateRequest;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.mapper.UserMapper;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import com.sprint.mission.monew.domain.user.repository.UserSessionRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceRetryTest {

  @Autowired
  private UserService userService;

  @MockitoBean
  private UserRepository userRepository;

  @MockitoBean
  private UserMapper userMapper;

  @MockitoBean
  private PasswordEncoder passwordEncoder;

  @MockitoBean
  private UserSessionRepository userSessionRepository;

  @Nested
  @DisplayName("닉네임 수정 낙관적락 재시도")
  class UpdateRetry {

    @Test
    @DisplayName("낙관적락 충돌 시 3회 재시도 후 성공")
    void 낙관적락_충돌_시_3회_재시도_후_성공() {
      // given
      UUID userId = UUID.randomUUID();
      UserUpdateRequest request = new UserUpdateRequest("새닉네임");
      User user = User.create("test@test.com", "테스터", "encodedPassword");

      given(userRepository.findByIdAndDeletedAtIsNull(userId))
          .willThrow(ObjectOptimisticLockingFailureException.class)
          .willThrow(ObjectOptimisticLockingFailureException.class)
          .willReturn(Optional.of(user));
      given(userMapper.toResponse(user))
          .willReturn(new UserResponse(userId, "test@test.com", "새닉네임", user.getCreatedAt()));

      // when
      UserResponse response = userService.update(userId, userId, request);

      // then
      then(userRepository).should(times(3)).findByIdAndDeletedAtIsNull(userId);
      assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("낙관적락 충돌이 3회 초과하면 예외 발생")
    void 낙관적락_충돌이_3회_초과하면_예외_발생() {
      // given
      UUID userId = UUID.randomUUID();
      UserUpdateRequest request = new UserUpdateRequest("새닉네임");

      given(userRepository.findByIdAndDeletedAtIsNull(userId))
          .willThrow(ObjectOptimisticLockingFailureException.class);

      // when & then
      assertThatThrownBy(() -> userService.update(userId, userId, request))
          .isInstanceOf(ObjectOptimisticLockingFailureException.class);
      then(userRepository).should(times(3)).findByIdAndDeletedAtIsNull(userId);
    }
  }

  @Nested
  @DisplayName("비밀번호 변경 낙관적락 재시도")
  class UpdatePasswordRetry {

    @Test
    @DisplayName("낙관적락 충돌 시 3회 재시도 후 성공")
    void 낙관적락_충돌_시_3회_재시도_후_성공() {
      // given
      UUID userId = UUID.randomUUID();
      UserPasswordUpdateRequest request = new UserPasswordUpdateRequest("currentPassword", "newPassword123");
      User user = User.create("test@test.com", "테스터", "encodedPassword");

      given(userRepository.findByIdAndDeletedAtIsNull(userId))
          .willThrow(ObjectOptimisticLockingFailureException.class)
          .willThrow(ObjectOptimisticLockingFailureException.class)
          .willReturn(Optional.of(user));
      given(passwordEncoder.matches(request.currentPassword(), user.getPassword()))
          .willReturn(true);

      // when
      userService.updatePassword(userId, request);

      // then
      then(userRepository).should(times(3)).findByIdAndDeletedAtIsNull(userId);
    }

    @Test
    @DisplayName("낙관적락 충돌이 3회 초과하면 예외 발생")
    void 낙관적락_충돌이_3회_초과하면_예외_발생() {
      // given
      UUID userId = UUID.randomUUID();
      UserPasswordUpdateRequest request = new UserPasswordUpdateRequest("currentPassword", "newPassword123");

      given(userRepository.findByIdAndDeletedAtIsNull(userId))
          .willThrow(ObjectOptimisticLockingFailureException.class);

      // when & then
      assertThatThrownBy(() -> userService.updatePassword(userId, request))
          .isInstanceOf(ObjectOptimisticLockingFailureException.class);
      then(userRepository).should(times(3)).findByIdAndDeletedAtIsNull(userId);
    }
  }

  @Nested
  @DisplayName("논리 삭제 낙관적락 재시도")
  class DeleteRetry {

    @Test
    @DisplayName("낙관적락 충돌 시 3회 재시도 후 성공")
    void 낙관적락_충돌_시_3회_재시도_후_성공() {
      // given
      UUID userId = UUID.randomUUID();
      User user = User.create("test@test.com", "테스터", "encodedPassword");

      given(userRepository.findByIdAndDeletedAtIsNull(userId))
          .willThrow(ObjectOptimisticLockingFailureException.class)
          .willThrow(ObjectOptimisticLockingFailureException.class)
          .willReturn(Optional.of(user));

      // when
      userService.delete(userId, userId);

      // then
      then(userRepository).should(times(3)).findByIdAndDeletedAtIsNull(userId);
    }

    @Test
    @DisplayName("낙관적락 충돌이 3회 초과하면 예외 발생")
    void 낙관적락_충돌이_3회_초과하면_예외_발생() {
      // given
      UUID userId = UUID.randomUUID();

      given(userRepository.findByIdAndDeletedAtIsNull(userId))
          .willThrow(ObjectOptimisticLockingFailureException.class);

      // when & then
      assertThatThrownBy(() -> userService.delete(userId, userId))
          .isInstanceOf(ObjectOptimisticLockingFailureException.class);
      then(userRepository).should(times(3)).findByIdAndDeletedAtIsNull(userId);
    }
  }
}