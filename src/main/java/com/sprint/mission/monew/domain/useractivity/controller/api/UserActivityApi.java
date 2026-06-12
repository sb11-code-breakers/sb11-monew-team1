package com.sprint.mission.monew.domain.useractivity.controller.api;

import com.sprint.mission.monew.common.dto.ErrorResponse;
import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;



@Tag(name = "활동 내역 관리", description = "사용자 활동 내역 관련 API")
public interface UserActivityApi {

  @Operation(summary = "사용자 활동 내역 조회", description = "사용자 ID로 활동 내역을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공",
          content = @Content(schema = @Schema(implementation = UserActivity.class))),
      @ApiResponse(responseCode = "404", description = "사용자 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<UserActivity> getUserActivity(
      @PathVariable @Parameter(description = "사용자 ID") UUID userId,
      @RequestHeader("Monew-Request-User-ID") @Parameter(description = "요청자 ID") UUID requestUserId);

  @Operation(summary = "기사 조회 기록 단건 삭제", description = "내 최근 본 기사 목록에서 특정 기사를 제거합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "삭제 성공"),
      @ApiResponse(responseCode = "403", description = "본인 활동 내역만 수정 가능",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "사용자 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<Void> deleteArticleView(
      @PathVariable @Parameter(description = "사용자 ID") UUID userId,
      @PathVariable @Parameter(description = "기사 ID") UUID articleId,
      @RequestHeader("Monew-Request-User-ID") @Parameter(description = "요청자 ID") UUID requestUserId);
}
