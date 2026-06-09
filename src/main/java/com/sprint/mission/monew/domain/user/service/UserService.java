package com.sprint.mission.monew.domain.user.service;

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
import com.sprint.mission.monew.domain.user.entity.UserUnlockToken;
import com.sprint.mission.monew.domain.user.entity.EmailVerification;
import com.sprint.mission.monew.domain.user.entity.PasswordResetToken;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.InvalidPasswordResetCodeException;
import com.sprint.mission.monew.domain.user.exception.UserInvalidUnlockTokenException;
import com.sprint.mission.monew.domain.user.exception.InvalidVerificationTokenException;
import com.sprint.mission.monew.domain.user.exception.UserAccessDeniedException;
import com.sprint.mission.monew.domain.user.exception.UserAccountLockedException;
import com.sprint.mission.monew.domain.user.exception.UserEmailDuplicateException;
import com.sprint.mission.monew.domain.user.exception.UserEmailNotVerifiedException;
import com.sprint.mission.monew.domain.user.exception.UserInvalidPasswordException;
import com.sprint.mission.monew.domain.user.exception.UserLoginFailedException;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.mapper.UserMapper;
import com.sprint.mission.monew.domain.user.metrics.UserMetrics;
import com.sprint.mission.monew.domain.user.repository.UserSessionRepository;
import com.sprint.mission.monew.domain.user.repository.UserUnlockTokenRepository;
import com.sprint.mission.monew.domain.user.repository.EmailVerificationRepository;
import com.sprint.mission.monew.domain.user.repository.PasswordResetTokenRepository;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final EmailVerificationRepository emailVerificationRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final UserUnlockTokenRepository userUnlockTokenRepository;
  private final UserSessionRepository userSessionRepository;
  private final EmailQueue emailQueue;
  private final UserMetrics userMetrics;
  private final LoginFailureHandler loginFailureHandler;
  private final LoginSuccessHandler loginSuccessHandler;

  @Value("${monew.session.timeout-minutes}")
  private int sessionTimeoutMinutes;

  @Transactional
  public UserResponse create(UserCreateRequest request) {
    log.debug("회원가입 시도");
    if (userRepository.existsByEmail(request.email())) {
      throw UserEmailDuplicateException.withEmail(request.email());
    }
    User user = User.create(request.email(), request.nickname(),
        passwordEncoder.encode(request.password()));
    User saved = userRepository.save(user);

    EmailVerification verification = EmailVerification.create(saved.getId());
    EmailVerification savedVerification = emailVerificationRepository.save(verification);

    String email = saved.getEmail();
    String token = savedVerification.getToken();

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          emailQueue.enqueueVerification(email, token);
        }
      });
    } else {
      emailQueue.enqueueVerification(email, token);
    }

    log.info("회원가입 완료 | userId={}", saved.getId());
    return userMapper.toResponse(saved);
  }

  public LoginResult login(UserLoginRequest request, String ip, String deviceFingerprint) {
    log.debug("로그인 시도");
    User user = userRepository.findByEmailAndDeletedAtIsNull(request.email())
        .orElseThrow(UserLoginFailedException::withEmail);

    if (user.isLocked()) {
      throw UserAccountLockedException.withEmail(request.email());
    }

    if (!user.isEmailVerified()) {
      throw UserEmailNotVerifiedException.withEmail(request.email());
    }

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      boolean isLocked = loginFailureHandler.handle(user.getId());
      if (isLocked) {
        throw UserAccountLockedException.withEmail(request.email());
      }
      throw UserLoginFailedException.withPassword();
    }

    loginSuccessHandler.handle(user.getId());
    UserSession session = userSessionRepository.save(
        UserSession.create(user.getId(), ip, deviceFingerprint, sessionTimeoutMinutes));
    log.info("로그인 완료 | userId={}", user.getId());
    return new LoginResult(userMapper.toResponse(user), session.getId());
  }

  @Transactional
  public void verifyEmail(String token) {
    log.debug("이메일 인증 시도");
    EmailVerification verification = emailVerificationRepository
        .findByTokenAndExpiredAtAfter(token, Instant.now())
        .orElseThrow(() -> InvalidVerificationTokenException.withToken(token));
    User user = userRepository.findByIdAndDeletedAtIsNull(verification.getUserId())
        .orElseThrow(() -> UserNotFoundException.withId(verification.getUserId()));
    user.verifyEmail();
    emailVerificationRepository.delete(verification);
    log.info("이메일 인증 완료 | userId={}", user.getId());
  }

  @Transactional
  public UserResponse update(UUID userId, UUID requestUserId, UserUpdateRequest request) {
    log.debug("닉네임 수정 시도");
    if (!userId.equals(requestUserId)) {
      throw UserAccessDeniedException.forUser(requestUserId);
    }
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));
    user.updateNickname(request.nickname());
    log.info("닉네임 수정 완료 | userId={}", userId);
    return userMapper.toResponse(user);
  }

  @Transactional
  public void delete(UUID userId, UUID requestUserId) {
    log.debug("논리 삭제 시도");
    if (!userId.equals(requestUserId)) {
      throw UserAccessDeniedException.forUser(requestUserId);
    }
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));
    user.softDelete();
    userSessionRepository.deleteByUserId(userId);
    log.info("사용자 논리 삭제 완료 | userId={}", userId);
  }

  @Transactional
  public void hardDelete(UUID userId) {
    log.debug("물리 삭제 시도");
    User user = userRepository.findByIdAndDeletedAtIsNotNull(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));
    userRepository.delete(user);
    log.info("사용자 물리 삭제 완료 | userId={}", userId);
  }

  @Transactional
  public void updatePassword(UUID requestUserId, UserPasswordUpdateRequest request) {
    log.debug("비밀번호 변경 시도");
    User user = userRepository.findByIdAndDeletedAtIsNull(requestUserId)
        .orElseThrow(() -> UserNotFoundException.withId(requestUserId));
    if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
      throw UserInvalidPasswordException.withoutDetail();
    }
    user.updatePassword(passwordEncoder.encode(request.newPassword()));
    log.info("비밀번호 변경 완료 | userId={}", requestUserId);
  }

  @Transactional
  public void requestPasswordReset(UserPasswordResetRequest request) {
    log.debug("비밀번호 재설정 요청 시도");
    User user = userRepository.findByEmailAndDeletedAtIsNull(request.email())
        .orElseThrow(() -> UserNotFoundException.withEmail(request.email()));

    passwordResetTokenRepository.deleteByUserId(user.getId());

    PasswordResetToken token = PasswordResetToken.create(user.getId());
    PasswordResetToken savedToken = passwordResetTokenRepository.save(token);

    String email = user.getEmail();
    String code = savedToken.getCode();

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          emailQueue.enqueuePasswordReset(email, code);
        }
      });
    } else {
      emailQueue.enqueuePasswordReset(email, code);
    }
    log.info("비밀번호 재설정 이메일 발송 | userId={}", user.getId());
  }

  @Transactional
  public void resetPassword(UserPasswordResetCodeRequest request) {
    log.debug("비밀번호 재설정 시도");
    PasswordResetToken token = passwordResetTokenRepository
        .findByCodeAndExpiredAtAfter(request.code(), Instant.now())
        .orElseThrow(() -> InvalidPasswordResetCodeException.withCode(request.code()));

    User user = userRepository.findByIdAndDeletedAtIsNull(token.getUserId())
        .orElseThrow(() -> UserNotFoundException.withId(token.getUserId()));

    user.updatePassword(passwordEncoder.encode(request.newPassword()));
    passwordResetTokenRepository.delete(token);
    log.info("비밀번호 재설정 완료 | userId={}", user.getId());
  }

  @Transactional
  public void requestUnlock(UserUnlockRequest request) {
    log.debug("계정 잠금 해제 요청 시도");
    User user = userRepository.findByEmailAndDeletedAtIsNull(request.email())
        .orElseThrow(() -> UserNotFoundException.withEmail(request.email()));

    userUnlockTokenRepository.deleteByUserId(user.getId());

    UserUnlockToken token = UserUnlockToken.create(user.getId());
    UserUnlockToken savedToken = userUnlockTokenRepository.save(token);

    String email = user.getEmail();
    String tokenValue = savedToken.getToken();

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          emailQueue.enqueueUnlock(email, tokenValue);
        }
      });
    } else {
      emailQueue.enqueueUnlock(email, tokenValue);
    }
    log.info("계정 잠금 해제 이메일 발송 | userId={}", user.getId());
  }

  @Transactional
  public void unlock(String token) {
    log.debug("계정 잠금 해제 시도");
    UserUnlockToken unlockToken = userUnlockTokenRepository
        .findByTokenAndExpiredAtAfter(token, Instant.now())
        .orElseThrow(() -> UserInvalidUnlockTokenException.withToken(token));

    User user = userRepository.findByIdAndDeletedAtIsNull(unlockToken.getUserId())
        .orElseThrow(() -> UserNotFoundException.withId(unlockToken.getUserId()));

    user.unlock();
    userUnlockTokenRepository.delete(unlockToken);
    log.info("계정 잠금 해제 완료 | userId={}", user.getId());
  }
  @Transactional
  public void logout(UUID userId) {
    log.debug("로그아웃 시도");
    userSessionRepository.deleteByUserId(userId);
    log.info("로그아웃 완료 | userId={}", userId);
  }

}