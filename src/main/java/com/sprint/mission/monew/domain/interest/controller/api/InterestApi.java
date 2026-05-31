package com.sprint.mission.monew.domain.interest.controller.api;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.common.dto.ErrorResponse;
import com.sprint.mission.monew.domain.interest.dto.InterestCreateRequest;
import com.sprint.mission.monew.domain.interest.dto.InterestQueryCondition;
import com.sprint.mission.monew.domain.interest.dto.InterestResponse;
import com.sprint.mission.monew.domain.interest.dto.InterestUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Interest", description = "관심사 API")
public interface InterestApi {

  @Operation(summary = "관심사 목록 조회", description = "관심사 목록을 조회합니다.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공"),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청 (정렬 기준 오류, 페이지네이션 파라미터 오류 등)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<CursorPageResponse<InterestResponse>> findAll(
      @RequestHeader("Monew-Request-User-ID") UUID userId,
      @ModelAttribute InterestQueryCondition condition);

  @Operation(summary = "관심사 등록", description = "새로운 관심사를 등록합니다.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "등록 성공",
        content = @Content(schema = @Schema(implementation = InterestResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청 (입력값 검증 실패)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description = "유사 관심사 중복",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<InterestResponse> create(
      @Valid @RequestBody InterestCreateRequest request);

  @Operation(summary = "관심사 정보 수정", description = "관심사의 키워드를 수정합니다.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "수정 성공",
        content = @Content(schema = @Schema(implementation = InterestResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청 (입력값 검증 실패)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "관심사 정보 없음",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<InterestResponse> updateKeywords(
      @PathVariable UUID id,
      @Valid @RequestBody InterestUpdateRequest request);

  @Operation(summary = "관심사 물리 삭제", description = "관심사를 물리적으로 삭제합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "삭제 성공"),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청 (UUID 형식 오류)",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "관심사 정보 없음",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<Void> hardDelete(
      @PathVariable UUID id);
}