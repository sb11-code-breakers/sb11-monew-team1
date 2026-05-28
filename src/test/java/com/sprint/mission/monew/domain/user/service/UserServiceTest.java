package com.sprint.mission.monew.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.sprint.mission.monew.domain.user.dto.UserCreateRequest;
import com.sprint.mission.monew.domain.user.dto.UserResponse;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserEmailDuplicateException;
import com.sprint.mission.monew.domain.user.mapper.UserMapper;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @InjectMocks
  private UserService userService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Nested
  @DisplayName("회원가입")
  class 회원가입 {

    private UserCreateRequest request;

    @BeforeEach
    void setUp() {
      request = new UserCreateRequest("test@test.com", "테스터", "password123");
    }

    @Test
    @DisplayName("이메일 중복 시 예외 발생")
    void 이메일_중복_시_예외_발생() {
      // given
      given(userRepository.existsByEmail(request.email())).willReturn(true);

      // when & then
      assertThatThrownBy(() -> userService.create(request))
          .isInstanceOf(UserEmailDuplicateException.class);

      then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    @DisplayName("성공 시 저장된 사용자 반환")
    void 성공_시_저장된_사용자_반환() {
      // given
      User user = User.create("test@test.com", "테스터", "encodedPassword");
      UserResponse userResponse = new UserResponse(
          UUID.randomUUID(), "test@test.com", "테스터", Instant.now()
      );

      given(userRepository.existsByEmail(request.email())).willReturn(false);
      given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");
      given(userRepository.save(any(User.class))).willReturn(user);
      given(userMapper.toResponse(user)).willReturn(userResponse);

      // when
      UserResponse result = userService.create(request);

      // then
      then(passwordEncoder).should().encode(request.password());
      then(userRepository).should().save(any(User.class));
      then(userMapper).should().toResponse(user);
      assertThat(result).isNotNull();
      assertThat(result.email()).isEqualTo("test@test.com");
      assertThat(result.nickname()).isEqualTo("테스터");
    }
  }
}