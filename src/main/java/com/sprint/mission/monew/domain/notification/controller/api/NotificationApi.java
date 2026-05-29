package com.sprint.mission.monew.domain.notification.controller.api;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.common.dto.ErrorResponse;
import com.sprint.mission.monew.domain.notification.dto.NotificationQueryCondition;
import com.sprint.mission.monew.domain.notification.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "알림 관리", description = "알림 관련 API")
public interface NotificationApi {

  @Operation(summary = "알림 목록 조회", description = "알림 목록을 조회합니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "조회 성공",
          content = @Content(schema = @Schema(implementation = CursorPageResponse.class))),
      @ApiResponse(
          responseCode = "400",
          description = "잘못된 요청 (정렬 기준 오류, 페이지네이션 파라미터 오류 등)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<CursorPageResponse<NotificationResponse>> findUnconfirmed(
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID") UUID userId,
      @ParameterObject @ModelAttribute @Valid NotificationQueryCondition request);

  @Operation(summary = "알림 확인", description = "알림을 확인합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "알림 확인 성공"),
      @ApiResponse(
          responseCode = "400",
          description = "잘못된 요청 (입력값 검증 실패)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(
          responseCode = "404",
          description = "사용자 정보 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<Void> confirm(
      @Parameter(description = "알림 ID") @PathVariable UUID notificationId,
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID") UUID userId);
}