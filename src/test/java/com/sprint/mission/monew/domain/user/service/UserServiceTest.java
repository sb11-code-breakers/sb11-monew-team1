package com.sprint.mission.monew.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.sprint.mission.monew.domain.user.dto.UserCreateRequest;
import com.sprint.mission.monew.domain.user.dto.UserLoginRequest;
import com.sprint.mission.monew.domain.user.dto.UserPasswordUpdateRequest;
import com.sprint.mission.monew.domain.user.dto.UserResponse;
import com.sprint.mission.monew.domain.user.dto.UserUpdateRequest;
import com.sprint.mission.monew.domain.user.entity.EmailVerification;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.InvalidVerificationTokenException;
import com.sprint.mission.monew.domain.user.exception.UserAccessDeniedException;
import com.sprint.mission.monew.domain.user.exception.UserEmailDuplicateException;
import com.sprint.mission.monew.domain.user.exception.UserEmailNotVerifiedException;
import com.sprint.mission.monew.domain.user.exception.UserInvalidPasswordException;
import com.sprint.mission.monew.domain.user.exception.UserLoginFailedException;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.mapper.UserMapper;
import com.sprint.mission.monew.domain.user.repository.EmailVerificationRepository;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
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
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

  @Mock
  private EmailVerificationRepository emailVerificationRepository;

  @Mock
  private EmailQueue emailQueue;

  @Mock
  private UserMetrics userMetrics;

  @Nested
  @DisplayName("회원가입")
  class Create {

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
    @DisplayName("성공 시 저장된 사용자 반환 및 이메일 큐 등록")
    void 성공_시_저장된_사용자_반환_및_이메일_큐_등록() {
      // given
      User user = User.create("test@test.com", "테스터", "encodedPassword");
      UserResponse userResponse = new UserResponse(
          UUID.randomUUID(), "test@test.com", "테스터", Instant.now()
      );

      given(userRepository.existsByEmail(request.email())).willReturn(false);
      given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");
      given(userRepository.save(any(User.class))).willReturn(user);
      given(emailVerificationRepository.save(any(EmailVerification.class)))
          .willReturn(EmailVerification.create(UUID.randomUUID()));
      given(userMapper.toResponse(user)).willReturn(userResponse);

      // when
      UserResponse result = userService.create(request);

      // then
      then(passwordEncoder).should().encode(request.password());
      then(userRepository).should().save(any(User.class));
      then(emailVerificationRepository).should().save(any(EmailVerification.class));
      then(emailQueue).should().enqueue(anyString(), anyString());
      then(userMapper).should().toResponse(user);
      then(userMetrics).should().countRegistered();
      assertThat(result).isNotNull();
      assertThat(result.email()).isEqualTo("test@test.com");
      assertThat(result.nickname()).isEqualTo("테스터");
    }

    @Test
    @DisplayName("트랜잭션 활성 시 커밋 후 이메일 큐 등록")
    void 트랜잭션_활성_시_커밋_후_이메일_큐_등록() {
      // given
      TransactionSynchronizationManager.initSynchronization();
      try {
        User user = User.create("test@test.com", "테스터", "encodedPassword");
        UserResponse userResponse = new UserResponse(
            UUID.randomUUID(), "test@test.com", "테스터", Instant.now()
        );

        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willReturn(user);
        given(emailVerificationRepository.save(any(EmailVerification.class)))
            .willReturn(EmailVerification.create(UUID.randomUUID()));
        given(userMapper.toResponse(user)).willReturn(userResponse);

        // when
        userService.create(request);

        // afterCommit 수동 트리거
        TransactionSynchronizationManager.getSynchronizations()
            .forEach(sync -> sync.afterCommit());

        // then
        then(emailQueue).should(times(1)).enqueue(anyString(), anyString());
      } finally {
        TransactionSynchronizationManager.clearSynchronization();
      }
    }
  }

  @Nested
  @DisplayName("로그인")
  class Login {

    private UserLoginRequest request;

    @BeforeEach
    void setUp() {
      request = new UserLoginRequest("test@test.com", "password123");
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 예외 발생")
    void 존재하지_않는_이메일이면_예외_발생() {
      // given
      given(userRepository.findByEmailAndDeletedAtIsNull(request.email()))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userService.login(request))
          .isInstanceOf(UserLoginFailedException.class);
    }

    @Test
    @DisplayName("이메일 미인증 시 예외 발생")
    void 이메일_미인증_시_예외_발생() {
      // given
      User user = User.create("test@test.com", "테스터", "encodedPassword");
      given(userRepository.findByEmailAndDeletedAtIsNull(request.email()))
          .willReturn(Optional.of(user));

      // when & then
      assertThatThrownBy(() -> userService.login(request))
          .isInstanceOf(UserEmailNotVerifiedException.class);
    }

    @Test
    @DisplayName("비밀번호가 틀리면 예외 발생")
    void 비밀번호가_틀리면_예외_발생() {
      // given
      User user = User.create("test@test.com", "테스터", "encodedPassword");
      user.verifyEmail();
      given(userRepository.findByEmailAndDeletedAtIsNull(request.email()))
          .willReturn(Optional.of(user));
      given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(false);

      // when & then
      assertThatThrownBy(() -> userService.login(request))
          .isInstanceOf(UserLoginFailedException.class);
    }

    @Test
    @DisplayName("성공 시 사용자 반환")
    void 성공_시_사용자_반환() {
      // given
      User user = User.create("test@test.com", "테스터", "encodedPassword");
      user.verifyEmail();
      UserResponse userResponse = new UserResponse(
          UUID.randomUUID(), "test@test.com", "테스터", Instant.now()
      );
      given(userRepository.findByEmailAndDeletedAtIsNull(request.email()))
          .willReturn(Optional.of(user));
      given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);
      given(userMapper.toResponse(user)).willReturn(userResponse);

      // when
      UserResponse result = userService.login(request);

      // then
      assertThat(result).isNotNull();
      assertThat(result.email()).isEqualTo("test@test.com");
    }
  }

  @Nested
  @DisplayName("이메일 인증")
  class VerifyEmail {

    @Test
    @DisplayName("유효하지 않은 토큰이면 예외 발생")
    void 유효하지_않은_토큰이면_예외_발생() {
      // given
      given(emailVerificationRepository.findByTokenAndExpiredAtAfter(
          eq("invalid-token"), any(Instant.class)))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userService.verifyEmail("invalid-token"))
          .isInstanceOf(InvalidVerificationTokenException.class);
    }

    @Test
    @DisplayName("성공 시 이메일 인증 완료")
    void 성공_시_이메일_인증_완료() {
      // given
      UUID userId = UUID.randomUUID();
      User user = User.create("test@test.com", "테스터", "encodedPassword");
      EmailVerification verification = EmailVerification.create(userId);

      given(emailVerificationRepository.findByTokenAndExpiredAtAfter(
          eq(verification.getToken()), any(Instant.class)))
          .willReturn(Optional.of(verification));
      given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));

      // when
      userService.verifyEmail(verification.getToken());

      // then
      assertThat(user.isEmailVerified()).isTrue();
      then(emailVerificationRepository).should().delete(verification);
    }
  }

  @Nested
  @DisplayName("닉네임 수정")
  class Update {

    private UUID userId;
    private UUID requestUserId;
    private UserUpdateRequest request;

    @BeforeEach
    void setUp() {
      userId = UUID.randomUUID();
      requestUserId = userId;
      request = new UserUpdateRequest("새닉네임");
    }

    @Test
    @DisplayName("다른 사용자가 수정하면 예외 발생")
    void 다른_사용자가_수정하면_예외_발생() {
      // given
      UUID anotherUserId = UUID.randomUUID();

      // when & then
      assertThatThrownBy(() -> userService.update(userId, anotherUserId, request))
          .isInstanceOf(UserAccessDeniedException.class);
    }

    @Test
    @DisplayName("존재하지 않는 사용자면 예외 발생")
    void 존재하지_않는_사용자면_예외_발생() {
      // given
      given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userService.update(userId, requestUserId, request))
          .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("성공 시 수정된 사용자 반환")
    void 성공_시_수정된_사용자_반환() {
      // given
      User user = User.create("test@test.com", "테스터", "encodedPassword");
      UserResponse userResponse = new UserResponse(
          userId, "test@test.com", "새닉네임", Instant.now()
      );

      given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
      given(userMapper.toResponse(user)).willReturn(userResponse);

      // when
      UserResponse result = userService.update(userId, requestUserId, request);

      // then
      assertThat(result).isNotNull();
      assertThat(result.nickname()).isEqualTo("새닉네임");
    }
  }

  @Nested
  @DisplayName("논리 삭제")
  class Delete {

    private UUID userId;
    private UUID requestUserId;

    @BeforeEach
    void setUp() {
      userId = UUID.randomUUID();
      requestUserId = userId;
    }

    @Test
    @DisplayName("다른 사용자가 삭제하면 예외 발생")
    void 다른_사용자가_삭제하면_예외_발생() {
      // given
      UUID anotherUserId = UUID.randomUUID();

      // when & then
      assertThatThrownBy(() -> userService.delete(userId, anotherUserId))
          .isInstanceOf(UserAccessDeniedException.class);
    }

    @Test
    @DisplayName("존재하지 않는 사용자면 예외 발생")
    void 존재하지_않는_사용자면_예외_발생() {
      // given
      given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userService.delete(userId, requestUserId))
          .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("성공 시 논리 삭제 처리")
    void 성공_시_논리_삭제_처리() {
      // given
      User user = User.create("test@test.com", "테스터", "encodedPassword");
      given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));

      // when
      userService.delete(userId, requestUserId);

      // then
      assertThat(user.isDeleted()).isTrue();
    }
  }

  @Nested
  @DisplayName("물리 삭제")
  class HardDelete {

    private UUID userId;

    @BeforeEach
    void setUp() {
      userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("존재하지 않는 사용자면 예외 발생")
    void 존재하지_않는_사용자면_예외_발생() {
      // given
      given(userRepository.findByIdAndDeletedAtIsNotNull(userId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userService.hardDelete(userId))
          .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("성공 시 사용자 물리 삭제")
    void 성공_시_사용자_물리_삭제() {
      // given
      User user = User.create("test@test.com", "테스터", "encodedPassword");
      given(userRepository.findByIdAndDeletedAtIsNotNull(userId)).willReturn(Optional.of(user));

      // when
      userService.hardDelete(userId);

      // then
      then(userRepository).should().delete(user);
    }
  }

  @Nested
  @DisplayName("비밀번호 변경")
  class UpdatePassword {

    private UUID requestUserId;
    private UserPasswordUpdateRequest request;

    @BeforeEach
    void setUp() {
      requestUserId = UUID.randomUUID();
      request = new UserPasswordUpdateRequest("currentPassword123", "newPassword123");
    }

    @Test
    @DisplayName("존재하지 않는 사용자면 예외 발생")
    void 존재하지_않는_사용자면_예외_발생() {
      // given
      given(userRepository.findByIdAndDeletedAtIsNull(requestUserId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userService.updatePassword(requestUserId, request))
          .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("현재 비밀번호가 틀리면 예외 발생")
    void 현재_비밀번호가_틀리면_예외_발생() {
      // given
      User user = User.create("test@test.com", "테스터", "encodedPassword");
      given(userRepository.findByIdAndDeletedAtIsNull(requestUserId)).willReturn(Optional.of(user));
      given(passwordEncoder.matches(request.currentPassword(), user.getPassword()))
          .willReturn(false);

      // when & then
      assertThatThrownBy(() -> userService.updatePassword(requestUserId, request))
          .isInstanceOf(UserInvalidPasswordException.class);
    }

    @Test
    @DisplayName("성공 시 비밀번호 변경")
    void 성공_시_비밀번호_변경() {
      // given
      User user = User.create("test@test.com", "테스터", "encodedPassword");
      given(userRepository.findByIdAndDeletedAtIsNull(requestUserId)).willReturn(Optional.of(user));
      given(passwordEncoder.matches(request.currentPassword(), user.getPassword()))
          .willReturn(true);
      given(passwordEncoder.encode(request.newPassword())).willReturn("newEncodedPassword");

      // when
      userService.updatePassword(requestUserId, request);

      // then
      assertThat(user.getPassword()).isEqualTo("newEncodedPassword");
    }
  }

  @Nested
  @DisplayName("만료 사용자 물리 삭제")
  class DeleteExpiredUsers {

    @Test
    @DisplayName("물리 삭제된 사용자 건수를 메트릭으로 집계한다")
    void 물리_삭제된_사용자_건수를_메트릭으로_집계한다() {
      // given
      Instant threshold = Instant.now();
      given(userRepository.deleteAllByDeletedAtBefore(threshold)).willReturn(3);

      // when
      userService.deleteExpiredUsers(threshold);

      // then
      then(userMetrics).should().countDeleted(3);
    }
  }
}