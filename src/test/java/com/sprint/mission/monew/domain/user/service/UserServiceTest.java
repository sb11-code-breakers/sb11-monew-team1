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
import com.sprint.mission.monew.domain.user.dto.UserPasswordResetCodeRequest;
import com.sprint.mission.monew.domain.user.dto.UserPasswordResetRequest;
import com.sprint.mission.monew.domain.user.dto.UserPasswordUpdateRequest;
import com.sprint.mission.monew.domain.user.dto.UserResponse;
import com.sprint.mission.monew.domain.user.dto.UserUpdateRequest;
import com.sprint.mission.monew.domain.user.dto.UserUnlockRequest;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.entity.EmailVerification;
import com.sprint.mission.monew.domain.user.entity.PasswordResetToken;
import com.sprint.mission.monew.domain.user.entity.UserUnlockToken;
import com.sprint.mission.monew.domain.user.exception.InvalidPasswordResetCodeException;
import com.sprint.mission.monew.domain.user.exception.InvalidVerificationTokenException;
import com.sprint.mission.monew.domain.user.exception.UserAccessDeniedException;
import com.sprint.mission.monew.domain.user.exception.UserEmailDuplicateException;
import com.sprint.mission.monew.domain.user.exception.UserEmailNotVerifiedException;
import com.sprint.mission.monew.domain.user.exception.UserInvalidPasswordException;
import com.sprint.mission.monew.domain.user.exception.UserLoginFailedException;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.exception.UserAccountLockedException;
import com.sprint.mission.monew.domain.user.exception.UserInvalidUnlockTokenException;
import com.sprint.mission.monew.domain.user.mapper.UserMapper;
import com.sprint.mission.monew.domain.user.repository.EmailVerificationRepository;
import com.sprint.mission.monew.domain.user.repository.PasswordResetTokenRepository;
import com.sprint.mission.monew.domain.user.repository.UserUnlockTokenRepository;
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
  private PasswordResetTokenRepository passwordResetTokenRepository;

  @Mock
  private EmailQueue emailQueue;

  @Mock
  private UserMetrics userMetrics;

  @Mock
  private UserUnlockTokenRepository userUnlockTokenRepository;

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
      then(emailQueue).should().enqueueVerification(anyString(), anyString());
      then(userMapper).should().toResponse(user);
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
        then(emailQueue).should(times(1)).enqueueVerification(anyString(), anyString());
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

    @Test
    @DisplayName("계정이 잠긴 경우 예외 발생")
    void 계정이_잠긴_경우_예외_발생() {
      // given
      User user = User.create("test@test.com", "테스터", "encodedPassword");
      user.verifyEmail();
      user.lock();
      given(userRepository.findByEmailAndDeletedAtIsNull(request.email()))
          .willReturn(Optional.of(user));

      // when & then
      assertThatThrownBy(() -> userService.login(request))
          .isInstanceOf(UserAccountLockedException.class);
    }

    @Test
    @DisplayName("비밀번호 틀리면 실패 횟수 증가")
    void 비밀번호_틀리면_실패_횟수_증가() {
      // given
      User user = User.create("test@test.com", "테스터", "encodedPassword");
      user.verifyEmail();
      given(userRepository.findByEmailAndDeletedAtIsNull(request.email()))
          .willReturn(Optional.of(user));
      given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(false);

      // when & then
      assertThatThrownBy(() -> userService.login(request))
          .isInstanceOf(UserLoginFailedException.class);
      assertThat(user.getLoginFailCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("비밀번호 5회 실패 시 계정 잠금")
    void 비밀번호_5회_실패_시_계정_잠금() {
      // given
      User user = User.create("test@test.com", "테스터", "encodedPassword");
      user.verifyEmail();
      given(userRepository.findByEmailAndDeletedAtIsNull(request.email()))
          .willReturn(Optional.of(user));
      given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(false);

      // when - 5회 실패
      for (int i = 0; i < 5; i++) {
        assertThatThrownBy(() -> userService.login(request))
            .isInstanceOf(UserLoginFailedException.class);
      }

      // then
      assertThat(user.isLocked()).isTrue();
    }

    @Test
    @DisplayName("성공 시 실패 횟수 초기화")
    void 성공_시_실패_횟수_초기화() {
      // given
      User user = User.create("test@test.com", "테스터", "encodedPassword");
      user.verifyEmail();
      user.incrementLoginFailCount();
      user.incrementLoginFailCount();
      UserResponse userResponse = new UserResponse(
          UUID.randomUUID(), "test@test.com", "테스터", Instant.now()
      );
      given(userRepository.findByEmailAndDeletedAtIsNull(request.email()))
          .willReturn(Optional.of(user));
      given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);
      given(userMapper.toResponse(user)).willReturn(userResponse);

      // when
      userService.login(request);

      // then
      assertThat(user.getLoginFailCount()).isEqualTo(0);
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
  @DisplayName("비밀번호 재설정 요청")
  class RequestPasswordReset {

    @Test
    @DisplayName("존재하지 않는 이메일이면 예외 발생")
    void 존재하지_않는_이메일이면_예외_발생() {
      // given
      UserPasswordResetRequest request = new UserPasswordResetRequest("notfound@test.com");
      given(userRepository.findByEmailAndDeletedAtIsNull(request.email()))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userService.requestPasswordReset(request))
          .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("성공 시 비밀번호 재설정 이메일 큐 등록")
    void 성공_시_비밀번호_재설정_이메일_큐_등록() {
      // given
      UserPasswordResetRequest request = new UserPasswordResetRequest("test@test.com");
      User user = User.create("test@test.com", "테스터", "encodedPassword");
      PasswordResetToken token = PasswordResetToken.create(UUID.randomUUID());

      given(userRepository.findByEmailAndDeletedAtIsNull(request.email()))
          .willReturn(Optional.of(user));
      given(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
          .willReturn(token);

      // when
      userService.requestPasswordReset(request);

      // then
      then(passwordResetTokenRepository).should().save(any(PasswordResetToken.class));
      then(emailQueue).should().enqueuePasswordReset(anyString(), anyString());
    }

    @Test
    @DisplayName("재설정 요청 시 기존 토큰 무효화")
    void 재설정_요청_시_기존_토큰_무효화() {
      // given
      UserPasswordResetRequest request = new UserPasswordResetRequest("test@test.com");
      User user = User.create("test@test.com", "테스터", "encodedPassword");
      PasswordResetToken token = PasswordResetToken.create(UUID.randomUUID());

      given(userRepository.findByEmailAndDeletedAtIsNull(request.email()))
          .willReturn(Optional.of(user));
      given(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
          .willReturn(token);

      // when
      userService.requestPasswordReset(request);

      // then
      then(passwordResetTokenRepository).should().deleteByUserId(user.getId());
      then(passwordResetTokenRepository).should().save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("트랜잭션 활성 시 커밋 후 비밀번호 재설정 이메일 큐 등록")
    void 트랜잭션_활성_시_커밋_후_비밀번호_재설정_이메일_큐_등록() {
      // given
      TransactionSynchronizationManager.initSynchronization();
      try {
        UserPasswordResetRequest request = new UserPasswordResetRequest("test@test.com");
        User user = User.create("test@test.com", "테스터", "encodedPassword");
        PasswordResetToken token = PasswordResetToken.create(UUID.randomUUID());

        given(userRepository.findByEmailAndDeletedAtIsNull(request.email()))
            .willReturn(Optional.of(user));
        given(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
            .willReturn(token);

        // when
        userService.requestPasswordReset(request);

        // then - 커밋 전에는 호출되지 않아야 함
        then(emailQueue).should(never()).enqueuePasswordReset(anyString(), anyString());

        // afterCommit 수동 트리거
        TransactionSynchronizationManager.getSynchronizations()
            .forEach(sync -> sync.afterCommit());

        // then - 커밋 후 1회 호출
        then(emailQueue).should(times(1)).enqueuePasswordReset(anyString(), anyString());
      } finally {
        TransactionSynchronizationManager.clearSynchronization();
      }
    }
  }

  @Nested
  @DisplayName("비밀번호 재설정")
  class ResetPassword {

    @Test
    @DisplayName("유효하지 않은 코드이면 예외 발생")
    void 유효하지_않은_코드이면_예외_발생() {
      // given
      UserPasswordResetCodeRequest request = new UserPasswordResetCodeRequest("invalid-code", "newPassword123");
      given(passwordResetTokenRepository.findByCodeAndExpiredAtAfter(
          eq("invalid-code"), any(Instant.class)))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userService.resetPassword(request))
          .isInstanceOf(InvalidPasswordResetCodeException.class);
    }

    @Test
    @DisplayName("성공 시 비밀번호 변경 및 토큰 삭제")
    void 성공_시_비밀번호_변경_및_토큰_삭제() {
      // given
      UUID userId = UUID.randomUUID();
      UserPasswordResetCodeRequest request = new UserPasswordResetCodeRequest("valid-code", "newPassword123");
      User user = User.create("test@test.com", "테스터", "encodedPassword");
      PasswordResetToken token = PasswordResetToken.create(userId);

      given(passwordResetTokenRepository.findByCodeAndExpiredAtAfter(
          eq("valid-code"), any(Instant.class)))
          .willReturn(Optional.of(token));
      given(userRepository.findByIdAndDeletedAtIsNull(token.getUserId()))
          .willReturn(Optional.of(user));
      given(passwordEncoder.encode(request.newPassword())).willReturn("newEncodedPassword");

      // when
      userService.resetPassword(request);

      // then
      assertThat(user.getPassword()).isEqualTo("newEncodedPassword");
      then(passwordResetTokenRepository).should().delete(token);
    }
  }
  @Nested
  @DisplayName("계정 잠금 해제 요청")
  class RequestUnlock {

    @Test
    @DisplayName("존재하지 않는 이메일이면 예외 발생")
    void 존재하지_않는_이메일이면_예외_발생() {
      // given
      UserUnlockRequest request = new UserUnlockRequest("notfound@test.com");
      given(userRepository.findByEmailAndDeletedAtIsNull(request.email()))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userService.requestUnlock(request))
          .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("성공 시 잠금 해제 이메일 발송")
    void 성공_시_잠금_해제_이메일_발송() {
      // given
      UserUnlockRequest request = new UserUnlockRequest("test@test.com");
      User user = User.create("test@test.com", "테스터", "encodedPassword");
      UserUnlockToken token = UserUnlockToken.create(UUID.randomUUID());

      given(userRepository.findByEmailAndDeletedAtIsNull(request.email()))
          .willReturn(Optional.of(user));
      given(userUnlockTokenRepository.save(any(UserUnlockToken.class)))
          .willReturn(token);

      // when
      userService.requestUnlock(request);

      // then
      then(userUnlockTokenRepository).should().deleteByUserId(user.getId());
      then(userUnlockTokenRepository).should().save(any(UserUnlockToken.class));
      then(emailQueue).should().enqueueUnlock(anyString(), anyString());
    }
    @Test
    @DisplayName("트랜잭션 활성 시 커밋 후 잠금 해제 이메일 큐 등록")
    void 트랜잭션_활성_시_커밋_후_잠금_해제_이메일_큐_등록() {
      // given
      TransactionSynchronizationManager.initSynchronization();
      try {
        UserUnlockRequest request = new UserUnlockRequest("test@test.com");
        User user = User.create("test@test.com", "테스터", "encodedPassword");
        UserUnlockToken token = UserUnlockToken.create(UUID.randomUUID());

        given(userRepository.findByEmailAndDeletedAtIsNull(request.email()))
            .willReturn(Optional.of(user));
        given(userUnlockTokenRepository.save(any(UserUnlockToken.class)))
            .willReturn(token);

        // when
        userService.requestUnlock(request);

        // then - 커밋 전 미호출
        then(emailQueue).should(never()).enqueueUnlock(anyString(), anyString());

        // afterCommit 수동 트리거
        TransactionSynchronizationManager.getSynchronizations()
            .forEach(sync -> sync.afterCommit());

        // then - 커밋 후 1회 호출
        then(emailQueue).should(times(1)).enqueueUnlock(anyString(), anyString());
      } finally {
        TransactionSynchronizationManager.clearSynchronization();
      }
    }
  }

  @Nested
  @DisplayName("계정 잠금 해제")
  class Unlock {

    @Test
    @DisplayName("유효하지 않은 토큰이면 예외 발생")
    void 유효하지_않은_토큰이면_예외_발생() {
      // given
      given(userUnlockTokenRepository.findByTokenAndExpiredAtAfter(
          eq("invalid-token"), any(Instant.class)))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userService.unlock("invalid-token"))
          .isInstanceOf(UserInvalidUnlockTokenException.class);
    }

    @Test
    @DisplayName("성공 시 계정 잠금 해제")
    void 성공_시_계정_잠금_해제() {
      // given
      UUID userId = UUID.randomUUID();
      User user = User.create("test@test.com", "테스터", "encodedPassword");
      user.lock();
      UserUnlockToken token = UserUnlockToken.create(userId);

      given(userUnlockTokenRepository.findByTokenAndExpiredAtAfter(
          eq(token.getToken()), any(Instant.class)))
          .willReturn(Optional.of(token));
      given(userRepository.findByIdAndDeletedAtIsNull(userId))
          .willReturn(Optional.of(user));

      // when
      userService.unlock(token.getToken());

      // then
      assertThat(user.isLocked()).isFalse();
      then(userUnlockTokenRepository).should().delete(token);
    }
  }
 }