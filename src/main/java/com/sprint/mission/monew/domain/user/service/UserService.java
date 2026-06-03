package com.sprint.mission.monew.domain.user.service;

import com.sprint.mission.monew.domain.user.dto.UserCreateRequest;
import com.sprint.mission.monew.domain.user.dto.UserLoginRequest;
import com.sprint.mission.monew.domain.user.dto.UserPasswordUpdateRequest;
import com.sprint.mission.monew.domain.user.dto.UserResponse;
import com.sprint.mission.monew.domain.user.dto.UserUpdateRequest;
import com.sprint.mission.monew.domain.user.entity.EmailVerification;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.event.EmailVerificationCreatedEvent;
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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final EmailVerificationRepository emailVerificationRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final UserMetrics userMetrics;

  @Transactional
  public UserResponse create(UserCreateRequest request) {
    log.debug("회원가입 시도");
    if (userRepository.existsByEmail(request.email())) {
      throw UserEmailDuplicateException.withEmail(request.email());
    }
    User user = User.create(
        request.email(),
        request.nickname(),
        passwordEncoder.encode(request.password())
    );
    User saved = userRepository.save(user);

    EmailVerification verification = EmailVerification.create(saved.getId());
    EmailVerification savedVerification = emailVerificationRepository.save(verification);
    eventPublisher.publishEvent(
        new EmailVerificationCreatedEvent(saved.getEmail(), savedVerification.getToken()));

    userMetrics.countRegistered();
    log.info("회원가입 완료: id={}", saved.getId());
    return userMapper.toResponse(saved);
  }

  public UserResponse login(UserLoginRequest request) {
    log.debug("로그인 시도");
    User user = userRepository.findByEmailAndDeletedAtIsNull(request.email())
        .orElseThrow(UserLoginFailedException::withEmail);

    if (!user.isEmailVerified()) {
      throw UserEmailNotVerifiedException.withEmail(request.email());
    }

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw UserLoginFailedException.withPassword();
    }
    log.info("로그인 완료: id={}", user.getId());
    return userMapper.toResponse(user);
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
    log.info("이메일 인증 완료: userId={}", user.getId());
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
    log.info("닉네임 수정 완료: id={}", userId);
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
    log.info("논리 삭제 완료: id={}", userId);
  }

  @Transactional
  public int deleteExpiredUsers(Instant threshold) {
    log.info("물리 삭제 실행: threshold={}", threshold);
    int deleted = userRepository.deleteAllByDeletedAtBefore(threshold);
    userMetrics.countDeleted(deleted);
    log.info("물리 삭제 완료: {}건 삭제", deleted);
    return deleted;
  }

  @Transactional
  public void hardDelete(UUID userId) {
    log.debug("물리 삭제 시도");
    User user = userRepository.findByIdAndDeletedAtIsNotNull(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));
    userRepository.delete(user);
    log.info("물리 삭제 완료: id={}", userId);
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
    log.info("비밀번호 변경 완료: id={}", requestUserId);
  }
}