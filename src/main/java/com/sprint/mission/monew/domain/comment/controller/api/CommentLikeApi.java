package com.sprint.mission.monew.domain.comment.controller.api;

import com.sprint.mission.monew.common.dto.ErrorResponse;
import com.sprint.mission.monew.domain.comment.dto.CommentLikeResponse;
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

@Tag(name = "댓글 관리", description = "댓글 관련 API")
public interface CommentLikeApi {

  @Operation(summary = "댓글 좋아요", description = "댓글 좋아요를 등록합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "댓글 좋아요 성공",
          content = @Content(schema = @Schema(implementation = CommentLikeResponse.class))),
      @ApiResponse(responseCode = "404", description = "댓글 정보 또는 사용자 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "409", description = "이미 댓글에 좋아요를 등록함",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<CommentLikeResponse> createCommentLike(
      @PathVariable @Parameter(description = "댓글 ID") UUID commentId,
      @RequestHeader("Monew-Request-User-ID") @Parameter(description = "요청자 ID") UUID userId
  );

  @Operation(summary = "댓글 좋아요 취소", description = "댓글 좋아요를 취소합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "댓글 좋아요 취소 성공"),
      @ApiResponse(responseCode = "404", description = "좋아요 정보 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<Void> cancelCommentLike(
      @PathVariable @Parameter(description = "댓글 ID") UUID commentId,
      @RequestHeader("Monew-Request-User-ID") @Parameter(description = "요청자 ID") UUID userId
  );

}
