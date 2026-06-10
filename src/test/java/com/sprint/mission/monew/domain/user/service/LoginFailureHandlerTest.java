package com.sprint.mission.monew.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class LoginFailureHandlerTest {

  @InjectMocks
  private LoginFailureHandler loginFailureHandler;

  @Mock
  private UserRepository userRepository;

  @Nested
  @DisplayName("로그인 실패 처리")
  class Handle {

    @Test
    @DisplayName("낙관적락 충돌 시 재시도 후 성공")
    void 낙관적락_충돌_시_재시도_후_성공() {
      // given
      UUID userId = UUID.randomUUID();
      User user = User.create("test@test.com", "테스터", "encodedPassword");

      given(userRepository.findById(userId))
          .willReturn(Optional.of(user));

      // when
      boolean result = loginFailureHandler.handle(userId);

      // then
      then(userRepository).should(times(1)).findById(userId);
      assertThat(result).isFalse();
      assertThat(user.getLoginFailCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("낙관적락 충돌이 3회 초과하면 예외 발생")
    void 낙관적락_충돌이_3회_초과하면_예외_발생() {
      // given
      UUID userId = UUID.randomUUID();

      given(userRepository.findById(userId))
          .willThrow(ObjectOptimisticLockingFailureException.class);

      // when & then
      assertThatThrownBy(() -> loginFailureHandler.handle(userId))
          .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
  }
}