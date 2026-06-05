package com.sprint.mission.monew.domain.user.controller.api;

import com.sprint.mission.monew.common.dto.ErrorResponse;
import com.sprint.mission.monew.domain.user.dto.UserCreateRequest;
import com.sprint.mission.monew.domain.user.dto.UserLoginRequest;
import com.sprint.mission.monew.domain.user.dto.UserPasswordResetCodeRequest;
import com.sprint.mission.monew.domain.user.dto.UserPasswordResetRequest;
import com.sprint.mission.monew.domain.user.dto.UserPasswordUpdateRequest;
import com.sprint.mission.monew.domain.user.dto.UserResponse;
import com.sprint.mission.monew.domain.user.dto.UserUnlockRequest;
import com.sprint.mission.monew.domain.user.dto.UserUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "User", description = "사용자 API")
public interface UserApi {

  @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "회원가입 성공",
          content = @Content(schema = @Schema(implementation = UserResponse.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "409", description = "이메일 중복",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request);

  @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "로그인 성공",
          content = @Content(schema = @Schema(implementation = UserResponse.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치 / 이메일 미인증",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "423", description = "계정 잠금",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<UserResponse> login(@Valid @RequestBody UserLoginRequest request);

  @Operation(summary = "이메일 인증", description = "이메일 인증 토큰을 검증합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "이메일 인증 성공"),
      @ApiResponse(responseCode = "400", description = "유효하지 않거나 만료된 토큰",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<String> verifyEmail(@NotBlank @RequestParam String token);

  @Operation(summary = "닉네임 수정", description = "사용자의 닉네임을 수정합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "닉네임 수정 성공",
          content = @Content(schema = @Schema(implementation = UserResponse.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "403", description = "수정 권한 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "사용자 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<UserResponse> update(
      @PathVariable UUID userId,
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId,
      @Valid @RequestBody UserUpdateRequest request);

  @Operation(summary = "비밀번호 변경", description = "사용자의 비밀번호를 변경합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "비밀번호 변경 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "현재 비밀번호 불일치",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "사용자 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<Void> updatePassword(
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId,
      @Valid @RequestBody UserPasswordUpdateRequest request);

  @Operation(summary = "사용자 논리 삭제", description = "사용자를 논리적으로 삭제합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "삭제 성공"),
      @ApiResponse(responseCode = "403", description = "삭제 권한 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "사용자 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<Void> delete(
      @PathVariable UUID userId,
      @RequestHeader("Monew-Request-User-ID") UUID requestUserId);

  @Operation(summary = "사용자 물리 삭제", description = "사용자를 즉시 물리적으로 삭제합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "물리 삭제 성공"),
      @ApiResponse(responseCode = "404", description = "사용자 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<Void> hardDelete(@PathVariable UUID userId);

  @Operation(summary = "비밀번호 재설정 요청", description = "이메일로 비밀번호 재설정 코드를 발송합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "재설정 코드 발송 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "사용자 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<Void> resetPassword(
      @Valid @RequestBody UserPasswordResetRequest request);

  @Operation(summary = "비밀번호 재설정", description = "인증 코드로 비밀번호를 재설정합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "비밀번호 재설정 성공"),
      @ApiResponse(responseCode = "400", description = "유효하지 않거나 만료된 인증 코드",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<Void> resetPassword(
      @Valid @RequestBody UserPasswordResetCodeRequest request);

  @Operation(summary = "계정 잠금 해제 요청", description = "이메일로 계정 잠금 해제 토큰을 발송합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "잠금 해제 이메일 발송 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "사용자 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<Void> requestUnlock(@Valid @RequestBody UserUnlockRequest request);

  @Operation(summary = "계정 잠금 해제", description = "토큰으로 계정 잠금을 해제합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "잠금 해제 성공"),
      @ApiResponse(responseCode = "400", description = "유효하지 않거나 만료된 토큰",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<Void> unlock(@RequestParam UUID token);
}