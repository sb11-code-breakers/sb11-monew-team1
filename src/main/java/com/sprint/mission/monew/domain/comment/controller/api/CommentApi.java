package com.sprint.mission.monew.domain.comment.controller.api;

import com.sprint.mission.monew.domain.comment.dto.request.CommentCreateRequest;
import com.sprint.mission.monew.domain.comment.dto.request.CommentUpdateRequest;
import com.sprint.mission.monew.domain.comment.dto.response.CommentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "댓글 관리", description = "댓글 관련 API")
public interface CommentApi {

  @Operation(summary = "댓글 등록", description = "새로운 댓글을 등록합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "등록 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  ResponseEntity<CommentResponse> createComment(@RequestBody @Valid CommentCreateRequest request);

  @Operation(summary = "댓글 정보 수정", description = "댓글의 내용을 수정합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "수정 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)"),
      @ApiResponse(responseCode = "403", description = "수정 권한 없음"),
      @ApiResponse(responseCode = "404", description = "댓글 정보 없음"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  ResponseEntity<CommentResponse> updateComment(
      @PathVariable UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID userId,
      @RequestBody CommentUpdateRequest request);
}
