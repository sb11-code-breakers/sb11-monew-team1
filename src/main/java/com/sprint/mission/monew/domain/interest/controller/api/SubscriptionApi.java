package com.sprint.mission.monew.domain.interest.controller.api;

import com.sprint.mission.monew.common.dto.ErrorResponse;
import com.sprint.mission.monew.domain.interest.dto.SubscriptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Interest", description = "관심사 API")
public interface SubscriptionApi {

  @Operation(summary = "관심사 구독", description = "관심사를 구독합니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "구독 성공",
          content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))),
      @ApiResponse(
          responseCode = "404",
          description = "관심사 정보 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(
          responseCode = "409",
          description = "이미 구독 중인 관심사",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<SubscriptionResponse> subscribe(
      @PathVariable UUID interestId,
      @RequestHeader("Monew-Request-User-ID") UUID userId);

  @Operation(summary = "관심사 구독 취소", description = "관심사를 구독을 취소합니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "204",
          description = "구독 취소 성공"),
      @ApiResponse(
          responseCode = "404",
          description = "관심사 정보 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<Void> unsubscribe(
      @PathVariable UUID interestId,
      @RequestHeader("Monew-Request-User-ID") UUID userId);
}