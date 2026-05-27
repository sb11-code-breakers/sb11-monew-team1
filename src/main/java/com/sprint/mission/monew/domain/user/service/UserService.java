package com.sprint.mission.monew.domain.user.service;

import com.sprint.mission.monew.domain.user.dto.UserDto;
import com.sprint.mission.monew.domain.user.dto.UserRegisterRequest;

public interface UserService {

  UserDto register(UserRegisterRequest request);
}