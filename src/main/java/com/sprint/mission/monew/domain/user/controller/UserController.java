package com.sprint.mission.monew.domain.user.controller;

import com.sprint.mission.monew.domain.user.controller.api.UserApi;
import com.sprint.mission.monew.domain.user.dto.UserCreateRequest;
import com.sprint.mission.monew.domain.user.dto.UserLoginRequest;
import com.sprint.mission.monew.domain.user.dto.UserPasswordUpdateRequest;
import com.sprint.mission.monew.domain.user.dto.UserUpdateRequest;
import com.sprint.mission.monew.domain.user.dto.UserResponse;
import com.sprint.mission.monew.domain.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
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

  @GetMapping("/verify")
  @Override
  public ResponseEntity<Void> verifyEmail(@NotBlank @RequestParam String token) {
    log.debug("이메일 인증 요청 수신");
    userService.verifyEmail(token);
    log.info("이메일 인증 성공");
    return ResponseEntity.ok().build();
  }

  @PatchMapping("/{userId}")
  @Override
  public ResponseEntity<UserResponse> update(
      @PathVariable UUID userId,
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId,
      @Valid @RequestBody UserUpdateRequest request) {
    log.debug("닉네임 수정 요청 수신");
    UserResponse response = userService.update(userId, requestUserId, request);
    log.info("닉네임 수정 성공: id={}", userId);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/password")
  @Override
  public ResponseEntity<Void> updatePassword(
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId,
      @Valid @RequestBody UserPasswordUpdateRequest request) {
    log.debug("비밀번호 변경 요청 수신");
    userService.updatePassword(requestUserId, request);
    log.info("비밀번호 변경 성공: id={}", requestUserId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{userId}")
  @Override
  public ResponseEntity<Void> delete(
      @PathVariable UUID userId,
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId) {
    log.debug("논리 삭제 요청 수신");
    userService.delete(userId, requestUserId);
    log.info("논리 삭제 성공: id={}", userId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{userId}/hard")
  @Override
  public ResponseEntity<Void> hardDelete(@PathVariable UUID userId) {
    log.debug("물리 삭제 요청 수신");
    userService.hardDelete(userId);
    log.info("물리 삭제 성공: id={}", userId);
    return ResponseEntity.noContent().build();
  }
}