package com.sprint.mission.monew.domain.user.service;

import com.sprint.mission.monew.domain.user.dto.UserDto;
import com.sprint.mission.monew.domain.user.dto.UserRegisterRequest;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserEmailDuplicateException;
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
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  @Override
  public UserDto register(UserRegisterRequest request) {
    log.debug("회원가입 시도: email={}", request.email());

    if (userRepository.existsByEmail(request.email())) {
      log.warn("회원가입 실패 - 이메일 중복: email={}", request.email());
      throw UserEmailDuplicateException.withEmail(request.email());
    }

    User user = User.create(
        request.email(),
        request.nickname(),
        passwordEncoder.encode(request.password())
    );

    User saved = userRepository.save(user);
    log.info("회원가입 완료: id={}, email={}", saved.getId(), saved.getEmail());
    return userMapper.toDto(saved);
  }
}