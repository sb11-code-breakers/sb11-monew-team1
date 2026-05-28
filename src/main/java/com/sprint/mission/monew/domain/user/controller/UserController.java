package com.sprint.mission.monew.domain.user.controller;

import com.sprint.mission.monew.domain.user.controller.api.UserApi;
import com.sprint.mission.monew.domain.user.dto.UserCreateRequest;
import com.sprint.mission.monew.domain.user.dto.UserLoginRequest;
import com.sprint.mission.monew.domain.user.dto.UserResponse;
import com.sprint.mission.monew.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
  public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
    log.debug("회원가입 요청 수신");

    UserResponse response = userService.create(request);

    log.info("회원가입 성공: id={}", response.id());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/login")
  @Override
  public ResponseEntity<UserResponse> login(@Valid @RequestBody UserLoginRequest request) {
    log.debug("로그인 요청 수신");

    UserResponse response = userService.login(request);

    log.info("로그인 성공: id={}", response.id());
    return ResponseEntity.ok(response);
  }
}