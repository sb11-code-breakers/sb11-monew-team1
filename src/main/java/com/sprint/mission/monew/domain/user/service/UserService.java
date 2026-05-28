package com.sprint.mission.monew.domain.user.service;

import com.sprint.mission.monew.domain.user.dto.UserCreateRequest;
import com.sprint.mission.monew.domain.user.dto.UserLoginRequest;
import com.sprint.mission.monew.domain.user.dto.UserResponse;
import com.sprint.mission.monew.domain.user.dto.UserUpdateRequest;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserAccessDeniedException;
import com.sprint.mission.monew.domain.user.exception.UserEmailDuplicateException;
import com.sprint.mission.monew.domain.user.exception.UserLoginFailedException;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.mapper.UserMapper;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    log.info("회원가입 완료: id={}", saved.getId());
    return userMapper.toResponse(saved);
  }

  public UserResponse login(UserLoginRequest request) {
    log.debug("로그인 시도");

    User user = userRepository.findByEmailAndDeletedAtIsNull(request.email())
        .orElseThrow(UserLoginFailedException::withEmail);

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw UserLoginFailedException.withPassword();
    }

    log.info("로그인 완료: id={}", user.getId());
    return userMapper.toResponse(user);
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
}