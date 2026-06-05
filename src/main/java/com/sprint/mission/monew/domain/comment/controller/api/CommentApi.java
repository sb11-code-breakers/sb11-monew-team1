package com.sprint.mission.monew.domain.comment.controller.api;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.common.dto.ErrorResponse;
import com.sprint.mission.monew.domain.comment.dto.CommentCreateRequest;
import com.sprint.mission.monew.domain.comment.dto.CommentQueryCondition;
import com.sprint.mission.monew.domain.comment.dto.CommentUpdateRequest;
import com.sprint.mission.monew.domain.comment.dto.CommentResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "댓글 관리", description = "댓글 관련 API")
public interface CommentApi {

  @Operation(summary = "댓글 등록", description = "새로운 댓글을 등록합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "등록 성공",
          content = @Content(schema = @Schema(implementation = CommentResponse.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<CommentResponse> createComment(
      @RequestHeader("Monew-Request-User-ID") @Parameter(description = "요청자 ID") UUID requestUserId,
      @RequestBody @Valid CommentCreateRequest request);

  @Operation(summary = "댓글 정보 수정", description = "댓글의 내용을 수정합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "수정 성공",
          content = @Content(schema = @Schema(implementation = CommentResponse.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "403", description = "수정 권한 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "댓글 정보 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<CommentResponse> updateComment(
      @PathVariable @Parameter(description = "댓글 ID") UUID commentId,
      @RequestHeader("Monew-Request-User-ID") @Parameter(description = "요청자 ID") UUID userId,
      @RequestBody CommentUpdateRequest request);

  @Operation(summary = "댓글 논리 삭제", description = "댓글을 논리적으로 삭제합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "삭제 성공"),
      @ApiResponse(responseCode = "403", description = "삭제 권한 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "댓글 정보 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<Void> softDeleteComment(
      @PathVariable @Parameter(description = "댓글 ID") UUID commentId,
      @RequestHeader("Monew-Request-User-ID") @Parameter(description = "요청자 ID") UUID userId);

  @Operation(summary = "댓글 물리 삭제", description = "댓글을 물리적으로 삭제합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "삭제 성공"),
      @ApiResponse(responseCode = "404", description = "댓글 정보 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<Void> hardDeleteComment(
      @PathVariable @Parameter(description = "댓글 ID") UUID commentId
  );

  @Operation(summary = "댓글 목록 조회", description = "조건에 맞는 댓글 목록을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청(정렬 기준 오류, 페이지네이션 파라미터 오류 등)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<CursorPageResponse<CommentResponse>> getComments(
      @ModelAttribute @ParameterObject CommentQueryCondition condition,
      @RequestHeader("Monew-Request-User-ID") @Parameter(description = "요청자 ID") UUID userId);
}
