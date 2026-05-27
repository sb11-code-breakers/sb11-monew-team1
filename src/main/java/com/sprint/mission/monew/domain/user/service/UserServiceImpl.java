package com.sprint.mission.monew.domain.user.service;

import com.sprint.mission.monew.domain.user.dto.UserDto;
import com.sprint.mission.monew.domain.user.dto.UserRegisterRequest;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserEmailDuplicateException;
import com.sprint.mission.monew.domain.user.mapper.UserMapper;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    if (userRepository.existsByEmail(request.email())) {
      throw UserEmailDuplicateException.withEmail(request.email());
    }

    User user = User.create(
        request.email(),
        request.nickname(),
        passwordEncoder.encode(request.password())
    );

    User saved = userRepository.save(user);
    return userMapper.toDto(saved);
  }
}