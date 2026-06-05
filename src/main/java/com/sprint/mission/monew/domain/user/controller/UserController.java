package com.sprint.mission.monew.domain.user.controller;

import com.sprint.mission.monew.domain.user.controller.api.UserApi;
import com.sprint.mission.monew.domain.user.dto.UserCreateRequest;
import com.sprint.mission.monew.domain.user.dto.UserLoginRequest;
import com.sprint.mission.monew.domain.user.dto.UserPasswordResetCodeRequest;
import com.sprint.mission.monew.domain.user.dto.UserPasswordResetRequest;
import com.sprint.mission.monew.domain.user.dto.UserPasswordUpdateRequest;
import com.sprint.mission.monew.domain.user.dto.UserResponse;
import com.sprint.mission.monew.domain.user.dto.UserUpdateRequest;
import com.sprint.mission.monew.domain.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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

@Validated
@RequestMapping("/api/users")
@RequiredArgsConstructor
@RestController
public class UserController implements UserApi {

  private final UserService userService;

  @PostMapping
  @Override
  public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
  }

  @PostMapping("/login")
  @Override
  public ResponseEntity<UserResponse> login(@Valid @RequestBody UserLoginRequest request) {
    return ResponseEntity.ok(userService.login(request));
  }

  @GetMapping("/verify")
  @Override
  public ResponseEntity<Void> verifyEmail(@NotBlank @RequestParam String token) {
    userService.verifyEmail(token);
    return ResponseEntity.ok().build();
  }

  @PatchMapping("/{userId}")
  @Override
  public ResponseEntity<UserResponse> update(
      @PathVariable UUID userId,
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId,
      @Valid @RequestBody UserUpdateRequest request) {
    return ResponseEntity.ok(userService.update(userId, requestUserId, request));
  }

  @PatchMapping("/password")
  @Override
  public ResponseEntity<Void> updatePassword(
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId,
      @Valid @RequestBody UserPasswordUpdateRequest request) {
    userService.updatePassword(requestUserId, request);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{userId}")
  @Override
  public ResponseEntity<Void> delete(
      @PathVariable UUID userId,
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId) {
    userService.delete(userId, requestUserId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{userId}/hard")
  @Override
  public ResponseEntity<Void> hardDelete(@PathVariable UUID userId) {
    userService.hardDelete(userId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/password/reset")
  @Override
  public ResponseEntity<Void> resetPassword(
      @Valid @RequestBody UserPasswordResetRequest request) {
    userService.requestPasswordReset(request);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/password/reset")
  @Override
  public ResponseEntity<Void> resetPassword(
      @Valid @RequestBody UserPasswordResetCodeRequest request) {
    userService.resetPassword(request);
    return ResponseEntity.noContent().build();
  }
}