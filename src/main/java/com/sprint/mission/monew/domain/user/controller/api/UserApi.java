package com.sprint.mission.monew.domain.user.controller.api;

import com.sprint.mission.monew.domain.user.dto.UserDto;
import com.sprint.mission.monew.domain.user.dto.UserRegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface UserApi {

  ResponseEntity<UserDto> register(@Valid @RequestBody UserRegisterRequest request);
}