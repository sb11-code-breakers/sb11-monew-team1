package com.sprint.mission.monew.domain.user.controller;

import com.sprint.mission.monew.domain.user.controller.api.UserApi;
import com.sprint.mission.monew.domain.user.dto.UserRegisterRequest;
import com.sprint.mission.monew.domain.user.dto.UserResponse;
import com.sprint.mission.monew.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping("/api/users")
@RequiredArgsConstructor
@RestController
public class UserController implements UserApi {

  private final UserService userService;

  @PostMapping
  @Override
  public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRegisterRequest request) {
    log.debug("회원가입 요청: email={}", request.email());

    UserResponse response = userService.create(request);

    log.info("회원가입 응답: id={}, email={}", response.id(), response.email());
    return ResponseEntity.ok(response);
  }
}