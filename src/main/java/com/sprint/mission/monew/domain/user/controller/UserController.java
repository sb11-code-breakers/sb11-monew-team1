package com.sprint.mission.monew.domain.user.controller;

import com.sprint.mission.monew.domain.user.controller.api.UserApi;
import com.sprint.mission.monew.domain.user.dto.UserCreateRequest;
import com.sprint.mission.monew.domain.user.dto.UserLoginRequest;
import com.sprint.mission.monew.domain.user.dto.UserPasswordResetCodeRequest;
import com.sprint.mission.monew.domain.user.dto.UserPasswordResetRequest;
import com.sprint.mission.monew.domain.user.dto.UserPasswordUpdateRequest;
import com.sprint.mission.monew.domain.user.dto.UserResponse;
import com.sprint.mission.monew.domain.user.dto.UserUnlockRequest;
import com.sprint.mission.monew.domain.user.dto.UserUpdateRequest;
import com.sprint.mission.monew.domain.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

  @Value("${monew.base-url}")
  private String baseUrl;

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

  @GetMapping(value = "/verify", produces = "text/html;charset=UTF-8")
  @Override
  public ResponseEntity<String> verifyEmail(@NotBlank @RequestParam String token) {
    userService.verifyEmail(token);
    String html = "<html><head><meta charset='UTF-8'>"
        + "<meta http-equiv='refresh' content='5;url=" + baseUrl + "/#/login'>"
        + "<title>이메일 인증 완료</title></head>"
        + "<body style='display:flex;justify-content:center;align-items:center;"
        + "height:100vh;font-family:Arial'>"
        + "<div style='text-align:center'>"
        + "<h1>✅ 이메일 인증 완료!</h1>"
        + "<p>5초 후 로그인 페이지로 이동합니다.</p>"
        + "</div></body></html>";
    return ResponseEntity.ok()
        .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
        .body(html);
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

  @PostMapping("/unlock")
  @Override
  public ResponseEntity<Void> requestUnlock(
      @Valid @RequestBody UserUnlockRequest request) {
    userService.requestUnlock(request);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/unlock")
  @Override
  public ResponseEntity<Void> unlock(@RequestParam UUID token) {
    userService.unlock(token.toString());
    return ResponseEntity.ok().build();
  }
}