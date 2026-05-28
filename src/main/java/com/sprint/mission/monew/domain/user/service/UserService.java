package com.sprint.mission.monew.domain.user.service;

import com.sprint.mission.monew.domain.user.dto.UserCreateRequest;
import com.sprint.mission.monew.domain.user.dto.UserLoginRequest;
import com.sprint.mission.monew.domain.user.dto.UserResponse;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserEmailDuplicateException;
import com.sprint.mission.monew.domain.user.exception.UserInvalidPasswordException;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.mapper.UserMapper;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
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
        .orElseThrow(() -> UserNotFoundException.withEmail(request.email()));

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw UserInvalidPasswordException.withoutDetail();
    }

    log.info("로그인 완료: id={}", user.getId());
    return userMapper.toResponse(user);
  }
}